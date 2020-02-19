package io.openqueue.repo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.openqueue.model.Ticket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
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
    private RedisTemplate<String, Serializable> redisTemplate;

    @Autowired
    private ReactiveRedisTemplate<String, Serializable> reactiveRedisTemplate;

    public TicketRepo() {
    }

    public void createTicket(Ticket ticket){
        JSONObject jsonObject = (JSONObject) JSON.toJSON(ticket);
        Mono mono = reactiveRedisTemplate.opsForHash().putAll(ticket.getId(), jsonObject);
        mono.subscribe();
    }

    public Ticket findTicket(String ticketId){
        Map queueMap = redisTemplate.opsForHash().entries(ticketId);
        if(queueMap.size() == 0) {
            return null;
        }
        Map<String, Object> tmp = new HashMap<>(queueMap);
        JSONObject jsonObject = (JSONObject) JSON.toJSON(tmp);
        return jsonObject.toJavaObject(Ticket.class);
    }

    public boolean isElementInSet(String element, String setKey){
        return redisTemplate.opsForZSet().score(setKey, element) != null;
    }

    public void addElementToSet(String element, String setKey, long expirationTime){
        redisTemplate.opsForZSet().add(setKey, element, expirationTime);
    }

    public void removeElementOutOfSet(String element, String setKey){
        redisTemplate.opsForZSet().remove(setKey, element);
    }

    public void setTicketOccupied(String ticketId){
        redisTemplate.opsForHash().put(ticketId, "occupied", Boolean.TRUE);
    }

    public void revokeTicket(String ticketId){
        redisTemplate.delete(ticketId);
    }
}
