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

import static io.openqueue.common.constant.Keys.READY_SET_PREFIX;
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
                .callbackURL("openqueue.io")
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
        redisTemplate.opsForSet().add("queue", testQueueId);
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
        Set queues = queueRepo.getAllQueues();
        for (Object queueId: queues) {
            assertThat(queueId.toString()).isEqualTo(testQueueId);
        }
    }

    @Test
    void testUpdateQueueConfig() {
        QueueConfigDto queueConfigDto = QueueConfigDto.builder()
                .availableSecondPerUser(100)
                .name("new_name")
                .maxActiveUsers(3000)
                .capacity(100000)
                .callbackURL("openqueue.io")
                .build();
        queueRepo.updateQueueConfig(testQueueId, queueConfigDto);

        Queue expectQueue = queueRepo.getQueue(testQueueId);
        assertThat(queueConfigDto.getCallbackURL())
                .isEqualTo(expectQueue.getCallbackURL());
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
    void testGetAllQueues() {
        Set queues = queueRepo.getAllQueues();
        for (Object queueId: queues) {
            System.out.println(queueId);
        }
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
