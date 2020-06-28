package io.openqueue.repo;

import io.openqueue.common.util.TypeConverter;
import io.openqueue.model.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;

import static io.openqueue.common.constant.Keys.*;

/**
 * @author chenjing
 */
@Repository
public class QueueRepo {

    @Autowired
    private ReactiveRedisTemplate<String, Serializable> reactiveRedisTemplate;

    public Mono<Queue> createOrUpdate(Queue queue) {
        reactiveRedisTemplate.ex
        return reactiveRedisTemplate.opsForHash().putAll(queue.getId(), TypeConverter.pojo2Map(queue)).thenReturn(queue);
    }

    public Mono<Void> addToSet(String queueId) {
        return reactiveRedisTemplate.opsForSet().add(ALL_QUEUES_SET, queueId).then();
    }

    public Mono<Queue> findById(String queueId) {
        return reactiveRedisTemplate.opsForHash().entries(queueId)
                .reduce(new HashMap<>(), (map, entry) -> {
                    map.put(entry.getKey(), entry.getValue());
                    return map;
                })
                .flatMap(map -> {
                    if (map.isEmpty()) {
                        return Mono.empty();
                    } else {
                        Queue queue = TypeConverter.map2Pojo(map, Queue.class);
                        return Mono.just(queue);
                    }
                });
    }

    public Flux<String> findAllId() {
        return reactiveRedisTemplate.opsForSet().members(ALL_QUEUES_SET).cast(String.class);
    }

    public Mono<Boolean> getQueueLock(String queueId, int timeout) {
        return reactiveRedisTemplate.opsForValue().setIfAbsent(LOCK_PREFIX + queueId, "Locked", Duration.ofSeconds(timeout));
    }

    public Mono<Long> incAndGetTail(String queueId) {
        return reactiveRedisTemplate.opsForHash().increment(queueId, "tail", 1);
    }

    public Mono<Long> incHead(String queueId, int increment) {
        return reactiveRedisTemplate.opsForHash().increment(queueId, "head", increment);
    }

    public Mono<Boolean> close(String queueId) {
        return reactiveRedisTemplate.delete(queueId)
                .concatWith(reactiveRedisTemplate.opsForSet().remove(ALL_QUEUES_SET, queueId))
                .reduce(0L, Long::sum)
                .flatMap(count -> Mono.just(count == 2));
    }
}
