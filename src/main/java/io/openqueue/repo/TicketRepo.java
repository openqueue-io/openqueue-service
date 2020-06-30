package io.openqueue.repo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;
import io.openqueue.model.Ticket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisElementReader;
import org.springframework.data.redis.serializer.RedisElementWriter;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author chenjing
 */
@Repository
public class TicketRepo {

    @Autowired
    private ReactiveRedisTemplate<String, Serializable> reactiveRedisTemplate;

    private DefaultRedisScript<List> applyTicketScript;


    @PostConstruct
    public void init() {
        applyTicketScript = new DefaultRedisScript<>();
        applyTicketScript.setResultType(List.class);
        applyTicketScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/apply_ticket.lua")));
    }

    public Mono<Long> applyOne(Ticket ticket) {
        return reactiveRedisTemplate
                .execute(applyTicketScript,
                        Collections.singletonList(ticket.getQueueId()),
                        Collections.singletonList(ticket),
                        RedisElementWriter.from(new Jackson2JsonRedisSerializer<>(Ticket.class)),
                        RedisElementReader.from(new Jackson2JsonRedisSerializer<>(List.class)))
                .flatMap(list -> Mono.just((Long) list.get(0)))
                .single();

    }

    public Mono<Ticket> findById(String ticketId) {
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

    public Mono<Long> incUsage(String ticketId) {
        return reactiveRedisTemplate.opsForHash().increment(ticketId, "countOfUsage", 1);
    }

    public Mono<Void> setActivateTime(String ticketId, long currentTime) {
        return reactiveRedisTemplate.opsForHash().put(ticketId, "activateTime", currentTime).then();
    }

    public Mono<Void> setOccupied(String ticketId) {
        return reactiveRedisTemplate.opsForHash().put(ticketId, "occupied", Boolean.TRUE).then();
    }

    public Mono<Boolean> isTicketInSet(String setKey, String ticketId) {
        return reactiveRedisTemplate.opsForZSet().score(setKey, ticketId)
                .filter(score -> score > 0)
                .hasElement();
    }

    public Mono<Void> addToSet(String setKey, String ticketId, long expirationTime) {
        return reactiveRedisTemplate.opsForZSet().add(setKey, ticketId, expirationTime).then();
    }

    public Mono<Long> removeOutOfSetById(String setKey, String ticketId) {
        return reactiveRedisTemplate.opsForZSet().remove(setKey, ticketId);
    }

    public Mono<Long> removeOutOfSetByTime(String setKey, long expirationTime) {
        return reactiveRedisTemplate.opsForZSet().removeRangeByScore(setKey, Range.closed((double) 0, (double) expirationTime));
    }

    public Mono<Long> countTicketInSet(String setKey) {
        return reactiveRedisTemplate.opsForZSet().size(setKey);
    }

    public Mono<Long> revoke(String ticketId) {
        return reactiveRedisTemplate.delete(ticketId);
    }

}
