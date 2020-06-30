package io.openqueue.task;

import io.openqueue.model.Queue;
import io.openqueue.repo.QueueRepo;
import io.openqueue.repo.TicketRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import static io.openqueue.common.constant.Keys.*;

/**
 * @author chenjing
 */
@Component
@Slf4j
public class ScheduledQueueTask {
    @Autowired
    private QueueRepo queueRepo;

    @Autowired
    private TicketRepo ticketRepo;

    private static final int LOCK_TIME_FOR_EACH_QUEUE = 3;

//    @Scheduled(fixedRate = 5000)
    public void pushAllQueuesForward() {
        queueRepo.findAllId()
                .flatMap(this::pushQueueForward)
                .subscribe();
        log.info("=======================checking queues and pushing them forward==========================");
    }

    private Mono<Void> pushQueueForward(String queueId) {
        return queueRepo.getQueueLock(queueId, LOCK_TIME_FOR_EACH_QUEUE)
                .flatMap(success -> {
                    if (success) {
                        return queueRepo.findById(queueId).flatMap(this::doPush);
                    }
                    return Mono.empty();
                });

    }

    private Mono<Void> doPush(Queue queue) {
        String queueId = queue.getId();
        Map<String, Long> cache = new HashMap<>();

        // Clean expired user in active set and ready set.
        Mono<Long> removeExpiredTicket = ticketRepo.removeOutOfSetByTime(ACTIVE_SET_PREFIX + queueId, Instant.now().getEpochSecond())
                .then(ticketRepo.removeOutOfSetByTime(READY_SET_PREFIX + queueId, Instant.now().getEpochSecond()));

        Mono<Long> countNewUser = removeExpiredTicket.then(ticketRepo.countTicketInSet(READY_SET_PREFIX + queueId))
                .flatMap(readyUsers -> {
                    cache.put("readyUsers", readyUsers);
                    return ticketRepo.countTicketInSet(ACTIVE_SET_PREFIX + queueId);
                })
                .flatMap(activeUsers -> Mono.just(Math.min(queue.getMaxActiveUsers() - activeUsers - cache.get("readyUsers"),
                        queue.getTail() - queue.getHead())));

        // Add new users to ready set.
        return countNewUser.flatMap(newUser -> {
            int start = queue.getHead() + 1;
            return Flux.fromStream(
                    IntStream.range(start, start + newUser.intValue())
                            .boxed()
                            .map(index -> TICKET_PREFIX + queueId + ":" + index)
            )
                    .flatMap(ticket -> ticketRepo.addToSet(READY_SET_PREFIX + queueId, ticket,
                            Instant.now().getEpochSecond() + queue.getHoldTimeForActivate()))
                    .then(queueRepo.incHead(queueId, newUser.intValue()))
                    .flatMap(head -> {
                        log.info(String.format("Queue:%s | Pushed new users: %d |Current head:%d | Current tail:%d",
                                queueId, newUser, head, queue.getTail()));
                        return Mono.empty();
                    });
        });
    }
}
