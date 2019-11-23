package io.openqueue.task;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.openqueue.model.Queue;
import io.openqueue.repo.QueueRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.*;

import static io.openqueue.common.constant.Keys.*;

/**
 * @author chenjing
 */
@Component
public class ScheduledQueueTask {
    @Autowired
    private QueueRepo queueRepo;

    @Autowired
    private RedisTemplate<String, Serializable> redisTemplate;

    private static final Logger logger = LoggerFactory.getLogger(ScheduledQueueTask.class);
    private static final int LOCK_TIME_FOR_EACH_QUEUE = 3;

    @Scheduled(fixedRate = 5000)
    public void pushAllQueues() {
        Set queues = queueRepo.getAllQueues();
        logger.info(String.format("Pushing queues...Total queues:%d", queues.size()));

        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("push-queue-%d").build();
        ExecutorService executorService =
                new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS,
                        new LinkedBlockingQueue<Runnable>(), threadFactory, new ThreadPoolExecutor.AbortPolicy());

        for (Object queueId: queues) {
            if (queueRepo.getQueueLock(queueId.toString(), LOCK_TIME_FOR_EACH_QUEUE)) {
                executorService.execute(() -> pushQueue(queueId.toString()));
            }
        }
        executorService.shutdown();
    }

    private void pushQueue(String queueId) {
        Queue queue = queueRepo.getQueue(queueId);

        if(queue.getHead() == queue.getTail()) {
            return;
        }

        // Clean expired user in active set and ready set.
        queueRepo.removeExpiredTickets(ACTIVE_SET_PREFIX + queueId, Instant.now().getEpochSecond());
        queueRepo.removeExpiredTickets(READY_SET_PREFIX + queueId, Instant.now().getEpochSecond());

        // Compute new allowed users.
        int readyUsers = queueRepo.getTicketNumInSet(READY_SET_PREFIX + queueId);
        int activeUsers = queueRepo.getTicketNumInSet(ACTIVE_SET_PREFIX + queueId);
        int newUser = Math.min(queue.getMaxActiveUsers() - activeUsers - readyUsers,
                queue.getTail() - queue.getHead());

        // Add new users to ready set.
        int head = queue.getHead();
        queueRepo.addTicketToSet(READY_SET_PREFIX + queueId, TICKET_PREFIX + queueId,
                head, newUser, queue.getHoldTimeForActivate());

        queueRepo.incQueueHead(queueId, newUser);
        logger.info(String.format("Queue:%s | Pushed new users: %d |Current head:%d | Current tail:%d",
                queueId, newUser, queue.getHead() + newUser, queue.getTail()));
    }
}
