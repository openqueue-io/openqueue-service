package io.openqueue.repo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.openqueue.dto.QueueConfigDto;
import io.openqueue.model.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static io.openqueue.common.constant.Keys.*;
import static io.openqueue.common.constant.Keys.TICKET_PREFIX;

/**
 * @author chenjing
 */
@Repository
public class QueueRepo {

    @Autowired
    private RedisTemplate<String, Serializable> redisTemplate;

    public void setupQueue(Queue queue) {
        JSONObject jsonObject = (JSONObject) JSON.toJSON(queue);
        redisTemplate.opsForHash().putAll(queue.getId(), jsonObject);
        redisTemplate.opsForSet().add(ALL_QUEUES_SET, queue.getId());
    }

    public Queue getQueue(String queueId) {
        Map queueMap = redisTemplate.opsForHash().entries(queueId);
        if(queueMap.size() == 0) {
            return null;
        }
        Map<String, Object> tmp = new HashMap<>(queueMap);
        JSONObject jsonObject = (JSONObject) JSON.toJSON(tmp);
        return jsonObject.toJavaObject(Queue.class);
    }

    public Set getAllQueues() {
        return redisTemplate.opsForSet().members(ALL_QUEUES_SET);
    }

    public boolean getQueueLock(String queueId, int timeout) {
        return redisTemplate.opsForValue().setIfAbsent(LOCK_PREFIX + queueId, "Locked", Duration.ofSeconds(timeout));
    }

    public int incAndGetQueueTail(String queueId) {
        return redisTemplate.opsForHash().increment(queueId, "tail", 1).intValue();
    }

    public void incQueueHead(String queueId, int increment) {
        redisTemplate.opsForHash().increment(queueId, "head", increment);
    }

    public void removeExpiredTickets(String setKey, long timeout) {
        redisTemplate.opsForZSet().removeRangeByScore(setKey, 0, timeout);
    }

    public int getTicketNumInSet(String setKey) {
        return redisTemplate.opsForZSet().size(setKey).intValue();
    }

    public void addTicketToSet(String setKey, String ticketPrefix, int start, int increment, int timeout) {
        for(int i = 1; i <= increment; ++i) {
            redisTemplate.opsForZSet().add(setKey,
                    ticketPrefix + ":" + (start + i),
                    Instant.now().getEpochSecond() + timeout);
        }
    }

    public void updateQueueConfig(String queueId, QueueConfigDto queueConfigDto) {
        JSONObject jsonObject = (JSONObject) JSON.toJSON(queueConfigDto);
        redisTemplate.opsForHash().putAll(queueId, jsonObject);
    }

    public void closeQueue(String queueId) {
        redisTemplate.delete(queueId);
        redisTemplate.opsForSet().remove(ALL_QUEUES_SET, queueId);
    }
}
