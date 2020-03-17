package io.openqueue.repo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.openqueue.dto.QueueConfigDto;
import io.openqueue.model.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static io.openqueue.common.constant.Keys.*;

/**
 * @author chenjing
 */
@Repository
public class QueueRepo {

    @Autowired
    private ReactiveRedisTemplate<String, Serializable> reactiveRedisTemplate;

    public Mono<Boolean> setupQueue(Queue queue) {
        Map<String, Object> attrMap = (JSONObject) JSON.toJSON(queue);
        return reactiveRedisTemplate.opsForHash().putAll(queue.getId(), attrMap)
                .filter(success -> success)
                .flatMap(success -> reactiveRedisTemplate.opsForSet().add(ALL_QUEUES_SET, queue.getId()))
                .filter(count -> count > 0)
                .flatMap(count -> Mono.just(Boolean.TRUE))
                .defaultIfEmpty(Boolean.FALSE);
    }

    public Mono<Queue> findQueue(String queueId) {
        Flux<Map.Entry<Object, Object>> queueMap = reactiveRedisTemplate.opsForHash().entries(queueId);
        return queueMap
                .reduce(new HashMap<>(), (map, entry) -> {
                    map.put(entry.getKey(), entry.getValue());
                    return map;
                })
                .flatMap(map -> {
                    if (map.isEmpty()) {
                        return Mono.empty();
                    } else {
                        JSON map2Json = (JSON) JSON.toJSON(map);
                        Queue queue = map2Json.toJavaObject(Queue.class);
                        return Mono.just(queue);
                    }
                });
    }

    public Flux<String> findAllQueues() {
        return reactiveRedisTemplate.opsForSet().members(ALL_QUEUES_SET).cast(String.class);
    }

    public Mono<Boolean> getQueueLock(String queueId, int timeout) {
        return reactiveRedisTemplate.opsForValue().setIfAbsent(LOCK_PREFIX + queueId, "Locked", Duration.ofSeconds(timeout));
    }

    public Mono<Long> incAndGetQueueTail(String queueId) {
        return reactiveRedisTemplate.opsForHash().increment(queueId, "tail", 1);
    }

    public Mono<Long> incQueueHead(String queueId, int increment) {
        return reactiveRedisTemplate.opsForHash().increment(queueId, "head", increment);
    }

    public Mono<Boolean> updateQueueConfig(String queueId, QueueConfigDto queueConfigDto) {
        JSONObject jsonObject = (JSONObject) JSON.toJSON(queueConfigDto);
        return reactiveRedisTemplate.opsForHash().putAll(queueId, jsonObject);
    }

    public Mono<Boolean> closeQueue(String queueId) {
        return reactiveRedisTemplate.delete(queueId)
                .concatWith(reactiveRedisTemplate.opsForSet().remove(ALL_QUEUES_SET, queueId))
                .reduce(0L, Long::sum)
                .flatMap(count -> Mono.just(count == 2));
    }
}
