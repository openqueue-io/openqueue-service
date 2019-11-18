package io.openqueue.repo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.openqueue.dto.QueueConfigDto;
import io.openqueue.model.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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

    public int getAndPlusQueueTail(String queueId) {
        return 0;
    }

    public void updateQueueConfig(String queueId, QueueConfigDto queueConfigDto) {
        JSONObject jsonObject = (JSONObject) JSON.toJSON(queueConfigDto);
        redisTemplate.opsForHash().putAll(queueId, jsonObject);
    }

    public void closeQueue(String queueId) {
        redisTemplate.delete(queueId);
    }
}
