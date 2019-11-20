package io.openqueue.repo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.openqueue.dto.QueueConfigDto;
import io.openqueue.model.Queue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class QueueRepoTest {

    @Autowired
    private QueueRepo queueRepo;

    @Autowired
    private RedisTemplate<String, Serializable> redisTemplate;

    private static Queue queueTest;

    private static String testQueueId;

    @BeforeAll
    static void runBeforeAllTestMethod() {
        queueTest = Queue.builder()
                .id("20sDO3")
                .availableSecondPerUser(300)
                .callbackWebSite("openqueue.io")
                .capacity(100000)
                .maxActiveUsers(1000)
                .name("test_queue")
                .ownerId("admin")
                .build();
        testQueueId = "20sDO3";
    }

    @BeforeEach
    void runBeforeEachTestMethod() {
        JSONObject jsonObject = (JSONObject) JSON.toJSON(queueTest);
        redisTemplate.opsForHash().putAll(testQueueId, jsonObject);
    }

    @AfterEach
    void runAfterEachTestMethod() {
        cleanup();
    }

    @Test
    void testSetupQueue() {
        cleanup();
        queueRepo.setupQueue(queueTest);
        Queue expectQueue = queueRepo.getQueue(testQueueId);
        assertThat(expectQueue.equals(queueTest)).isTrue();
    }

    @Test
    void testUpdateQueueConfig() {
        QueueConfigDto queueConfigDto = QueueConfigDto.builder()
                .availableSecondPerUser(100)
                .name("new_name")
                .maxActiveUsers(3000)
                .capacity(100000)
                .callbackWebSite("openqueue.io")
                .build();
        queueRepo.updateQueueConfig(testQueueId, queueConfigDto);

        Queue expectQueue = queueRepo.getQueue(testQueueId);
        assertThat(queueConfigDto.getCallbackWebSite())
                .isEqualTo(expectQueue.getCallbackWebSite());
        assertThat(queueConfigDto.getName())
                .isEqualTo(expectQueue.getName());
        assertThat(queueConfigDto.getAvailableSecondPerUser())
                .isEqualTo(expectQueue.getAvailableSecondPerUser());
        assertThat(queueConfigDto.getCapacity())
                .isEqualTo(expectQueue.getCapacity());
        assertThat(queueConfigDto.getMaxActiveUsers())
                .isEqualTo(expectQueue.getMaxActiveUsers());
    }

    @Test
    void testIncAndGetQueueTail() {
        ExecutorService executorService = Executors.newCachedThreadPool();

        for (int i = 0; i < 5000; i++) {
            executorService.execute(() -> queueRepo.incAndGetQueueTail(testQueueId));
        }
        executorService.shutdown();

        Queue queue = queueRepo.getQueue(testQueueId);
        assertThat(queue.getTail()).isEqualTo(5000);
    }

    @Test
    void testCloseQueue() {
        queueRepo.closeQueue(testQueueId);
        Queue queue = queueRepo.getQueue(testQueueId);
        assertThat(queue).isNull();
    }

    void cleanup() {
        Set<String> keys = redisTemplate.keys("*");
        redisTemplate.delete(keys);
    }

}
