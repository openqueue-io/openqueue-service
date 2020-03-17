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
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.Serializable;
import java.util.Map;
import java.util.stream.IntStream;

import static io.openqueue.common.constant.Keys.ALL_QUEUES_SET;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class QueueRepoTest {

    @Autowired
    private QueueRepo queueRepo;

    @Autowired
    private ReactiveRedisTemplate<String, Serializable> reactiveRedisTemplate;

    private static Queue queueTest;

    private static String testQueueId;

    @BeforeAll
    static void runBeforeAllTestMethod() {
        queueTest = Queue.builder()
                .id("q:20sDO3")
                .availableSecondPerUser(300)
                .callbackURL("openqueue.io")
                .capacity(100000)
                .maxActiveUsers(1000)
                .name("test_queue")
                .ownerId("admin")
                .build();
        testQueueId = "q:20sDO3";
    }

    @BeforeEach
    void runBeforeEachTestMethod() {
        Map<String, Object> attrMap = (JSONObject) JSON.toJSON(queueTest);
        reactiveRedisTemplate.opsForHash().putAll(testQueueId, attrMap)
                .then(reactiveRedisTemplate.opsForSet().add(ALL_QUEUES_SET, testQueueId))
                .block();
    }

    @AfterEach
    void runAfterEachTestMethod() {
        cleanup();
    }

    @Test
    void testSetupQueue() {
        cleanup();

        StepVerifier.create(queueRepo.setupQueue(queueTest))
                .expectNext(Boolean.TRUE)
                .expectComplete()
                .verify();

        StepVerifier.create(queueRepo.findQueue(testQueueId))
                .expectNext(queueTest)
                .expectComplete()
                .verify();

        StepVerifier.create(queueRepo.findAllQueues())
                .expectNext(testQueueId)
                .expectComplete()
                .verify();
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

        StepVerifier.create(queueRepo.updateQueueConfig(testQueueId, queueConfigDto))
                .expectNext(Boolean.TRUE)
                .expectComplete()
                .verify();

        StepVerifier.create(queueRepo.findQueue(testQueueId))
                .assertNext(queue -> {
                    assertThat(queueConfigDto.getCallbackURL())
                            .isEqualTo(queue.getCallbackURL());
                    assertThat(queueConfigDto.getName())
                            .isEqualTo(queue.getName());
                    assertThat(queueConfigDto.getAvailableSecondPerUser())
                            .isEqualTo(queue.getAvailableSecondPerUser());
                    assertThat(queueConfigDto.getCapacity())
                            .isEqualTo(queue.getCapacity());
                    assertThat(queueConfigDto.getMaxActiveUsers())
                            .isEqualTo(queue.getMaxActiveUsers());
                })
                .expectComplete()
                .verify();
    }

    @Test
    void testIncAndGetQueueTail() {
        Mono<Long> flux = Flux.fromStream(IntStream.range(1, 5001).boxed())
                .flatMap(i -> queueRepo.incAndGetQueueTail(testQueueId))
                .reduce(0L, Long::sum);

        StepVerifier.create(flux)
                .expectNextCount(1)
                .expectComplete()
                .verify();

        StepVerifier.create(queueRepo.findQueue(testQueueId))
                .assertNext(queue -> {
                    assertThat(queue.getTail()).isEqualTo(5000);
                })
                .expectComplete()
                .verify();
    }

    @Test
    void testCloseQueue() {
        StepVerifier.create(queueRepo.findQueue(testQueueId))
                .expectNextCount(1)
                .expectComplete()
                .verify();

        StepVerifier.create(queueRepo.closeQueue(testQueueId))
                .expectNext(Boolean.TRUE)
                .expectComplete()
                .verify();

        StepVerifier.create(queueRepo.findQueue(testQueueId))
                .expectNextCount(0)
                .expectComplete()
                .verify();
    }

    @Test
    void testGetQueueLock() {
        StepVerifier.create(queueRepo.getQueueLock(testQueueId, 5))
                .expectNext(Boolean.TRUE)
                .expectComplete()
                .verify();

        StepVerifier.create(queueRepo.getQueueLock(testQueueId, 5))
                .expectNext(Boolean.FALSE)
                .expectComplete()
                .verify();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        StepVerifier.create(queueRepo.getQueueLock(testQueueId, 5))
                .expectNext(Boolean.TRUE)
                .expectComplete()
                .verify();
    }

    @Test
    void testIncQueueHead() {
        StepVerifier.create(queueRepo.incQueueHead(testQueueId, 100))
                .expectNext(100L)
                .expectComplete()
                .verify();

        StepVerifier.create(queueRepo.findQueue(testQueueId))
                .assertNext(queue -> {
                    assertThat(queue.getHead() == 100);
                }).expectComplete()
                .verify();
    }
    
    void cleanup() {
        reactiveRedisTemplate
                .keys("*")
                .flatMap(key -> reactiveRedisTemplate.delete(key))
                .blockLast();
    }

}
