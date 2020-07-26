package io.openqueue.task;

import io.openqueue.model.Queue;
import io.openqueue.model.WebSocketSender;
import io.openqueue.repo.QueueRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private ReactiveRedisTemplate<String, Serializable> reactiveRedisTemplate;

    @Autowired
    public ConcurrentHashMap<String, Set<WebSocketSender>> queueSubscriber;

    private DefaultRedisScript<Object> refreshQueueScript;

    @PostConstruct
    public void init() {
        refreshQueueScript = new DefaultRedisScript<>();
        refreshQueueScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/refresh_all_queues.lua")));
    }

    private static final int LOCK_TIME_FOR_EACH_QUEUE = 3;

    @Scheduled(fixedRate = 5000)
    private void refreshQueue() {
        log.info("Refresh queues...");
        List<String> keys = Collections.emptyList();
        List<String> args = Collections.singletonList(String.valueOf(Instant.now().getEpochSecond()));

        queueRepo.getRefreshQueueLock(REFRESH_QUEUE_LOCK, LOCK_TIME_FOR_EACH_QUEUE)
            .flatMap(success -> {
                if (success) {
                    return reactiveRedisTemplate
                            .execute(refreshQueueScript, keys, args)
                            .then();
                }
                return Mono.empty();
            })
        .block();
    }

    @Scheduled(fixedRate = 6000)
    private void updateQueueStatus() {
        Map<String, Queue> queues = new HashMap<>();

        queueRepo.findAllId()
                .flatMap(queueId -> queueRepo.findById(queueId))
                .flatMap(queue -> {
                    queues.put(queue.getId(), queue);

                    Set<WebSocketSender> subscribers = queueSubscriber.get(queue.getId());
                    if (subscribers == null) {
                        subscribers = Collections.newSetFromMap(new ConcurrentHashMap<>());
                        queueSubscriber.put(queue.getId(), subscribers);
                    }
                    log.info("queue id:" + queue.getId() + ", head:" + queue.getHead() + ", tail:" + queue.getTail());
                    return Mono.empty();
                })
                .blockLast();

        queueSubscriber.forEach((queueId, subscribers) -> {
            Queue queue = queues.get(queueId);
            subscribers.stream().parallel()
                    .forEach(webSocketSender ->
                            webSocketSender.send(String.format("{head: %d, tail: %d}", queue.getHead(), queue.getTail())));
        });
    }
}
