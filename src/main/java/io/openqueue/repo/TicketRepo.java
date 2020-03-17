package io.openqueue.repo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.openqueue.model.Ticket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author chenjing
 */
@Repository
public class TicketRepo {

    @Autowired
    private ReactiveRedisTemplate<String, Serializable> reactiveRedisTemplate;

    public Mono<Ticket> createTicket(Ticket ticket){
        Map<String, Object> attrMap = (JSONObject) JSON.toJSON(ticket);
        return reactiveRedisTemplate.opsForHash().putAll(ticket.getId(), attrMap)
                .thenReturn(ticket);
    }

    public Mono<Ticket> findTicket(String ticketId){
        Flux<Map.Entry<Object, Object>> ticketMap = reactiveRedisTemplate.opsForHash().entries(ticketId);
        return ticketMap
                .reduce(new HashMap<>(), (map, entry) -> {
                    map.put(entry.getKey(), entry.getValue());
                    return map;
                })
                .flatMap(map -> {
                    if (map.isEmpty()) {
                        return Mono.empty();
                    } else {
                        JSON map2Json = (JSON) JSON.toJSON(map);
                        Ticket ticket = map2Json.toJavaObject(Ticket.class);
                        return Mono.just(ticket);
                    }
                });
    }

    public Mono<Long> incTicketUsage(String ticketId) {
        return reactiveRedisTemplate.opsForHash().increment(ticketId, "countOfUsage", 1);
    }

    public Mono<Boolean> setTicketActivateTime(String ticketId, long currentTime) {
        return reactiveRedisTemplate.opsForHash().put(ticketId, "activateTime", currentTime);
    }

    public Mono<Boolean> setTicketOccupied(String ticketId){
        return reactiveRedisTemplate.opsForHash().put(ticketId, "occupied", Boolean.TRUE);
    }

    public Mono<Boolean> isTicketInSet(String setKey, String ticketId){
        return reactiveRedisTemplate.opsForZSet().score(setKey, ticketId)
                .filter(score -> score > 0)
                .hasElement();
    }

    public Mono<Boolean> addTicketToSet(String setKey, String ticketId, long expirationTime){
        return reactiveRedisTemplate.opsForZSet().add(setKey, ticketId, expirationTime);
    }

    public Mono<Long> removeTicketOutOfSetById(String setKey, String ticketId){
        return reactiveRedisTemplate.opsForZSet().remove(setKey, ticketId);
    }

    public Mono<Long> removeTicketOutOfSetByTime(String setKey, long expirationTime) {
        return reactiveRedisTemplate.opsForZSet().removeRangeByScore(setKey, Range.closed((double) 0, (double) expirationTime));
    }

    public Mono<Long> countTicketInSet(String setKey) {
        return reactiveRedisTemplate.opsForZSet().size(setKey);
    }

    public Mono<Long> revokeTicket(String ticketId){
        return reactiveRedisTemplate.delete(ticketId);
    }

//    public Mono<Long> addTicketToSet(String setKey, String ticketPrefix, int start, int increment, int timeout) {
//        return Flux.fromStream(
//                IntStream.range(start + 1, start + increment + 1)
//                        .boxed()
//                        .map(index -> ticketPrefix + ":" + index)
//        )
//                .flatMap(ticket -> reactiveRedisTemplate.opsForSet().add(setKey, ticket, Instant.now().getEpochSecond() + timeout))
//                .reduce(0L, Long::sum);
//    }

}
