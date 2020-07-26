package io.openqueue.repo;

import io.openqueue.common.util.TypeConverter;
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
import java.util.stream.IntStream;

import static io.openqueue.common.constant.Keys.ALL_QUEUES_SET;
import static io.openqueue.common.constant.Keys.REFRESH_QUEUE_LOCK;
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
        testQueueId = "q:test";
        queueTest = Queue.builder()
                .id(testQueueId)
                .permissionExpirationSeconds(300)
                .callbackURL("openqueue.io")
                .capacity(100000)
                .maxActiveUsers(1000)
                .name("test_queue")
                .build();

    }

    @BeforeEach
    void runBeforeEachTestMethod() {
        reactiveRedisTemplate.opsForHash().putAll(testQueueId, TypeConverter.pojo2Map(queueTest))
                .then(reactiveRedisTemplate.opsForSet().add(ALL_QUEUES_SET, testQueueId))
                .block();
    }

    @AfterEach
    void runAfterEachTestMethod() {
        cleanup();
    }

    @Test
    void testCreateQueue() {
        cleanup();

        StepVerifier.create(queueRepo.createOrUpdate(queueTest))
                .expectNext(queueTest)
                .verifyComplete();

        StepVerifier.create(queueRepo.findById(testQueueId))
                .expectNext(queueTest)
                .expectComplete()
                .verify();

        StepVerifier.create(queueRepo.findAllId())
                .expectComplete()
                .verify();
    }

    @Test
    void testAddToSet() {
        cleanup();

        StepVerifier.create(queueRepo.findAllId())
                .expectComplete()
                .verify();

        StepVerifier.create(queueRepo.addToSet(testQueueId).then(queueRepo.findAllId().last()))
                .expectNext(testQueueId)
                .expectComplete()
                .verify();
    }

    @Test
    void testUpdateQueueConfig() {
        QueueConfigDto queueConfigDto = QueueConfigDto.builder()
                .permissionExpirationSeconds(100)
                .name("new_name")
                .maxActiveUsers(3000)
                .capacity(100000)
                .callbackURL("openqueue.io")
                .timeoutForActivateSeconds(20)
                .build();

        Queue newQueue = TypeConverter.cast(queueConfigDto, Queue.class);
        newQueue.setId(testQueueId);

        StepVerifier.create(queueRepo.createOrUpdate(newQueue))
                .expectNext(newQueue)
                .verifyComplete();

        StepVerifier.create(queueRepo.findById(testQueueId))
                .assertNext(queue -> {
                    assertThat(queueConfigDto.getCallbackURL())
                            .isEqualTo(queue.getCallbackURL());
                    assertThat(queueConfigDto.getName())
                            .isEqualTo(queue.getName());
                    assertThat(queueConfigDto.getPermissionExpirationSeconds())
                            .isEqualTo(queue.getPermissionExpirationSeconds());
                    assertThat(queueConfigDto.getCapacity())
                            .isEqualTo(queue.getCapacity());
                    assertThat(queueConfigDto.getMaxActiveUsers())
                            .isEqualTo(queue.getMaxActiveUsers());
                    assertThat(queueConfigDto.getTimeoutForActivateSeconds())
                            .isEqualTo(queue.getTimeoutForActivateSeconds());
                })
                .expectComplete()
                .verify();
    }

    @Test
    void testIncAndGetQueueTail() {
        Mono<Long> flux = Flux.fromStream(IntStream.range(1, 5001).boxed())
                .flatMap(i -> queueRepo.incAndGetTail(testQueueId))
                .reduce(0L, Long::sum);

        StepVerifier.create(flux)
                .expectNextCount(1)
                .expectComplete()
                .verify();

        StepVerifier.create(queueRepo.findById(testQueueId))
                .assertNext(queue -> {
                    assertThat(queue.getTail()).isEqualTo(5000);
                })
                .expectComplete()
                .verify();
    }

    @Test
    void testCloseQueue() {
        StepVerifier.create(queueRepo.findById(testQueueId))
                .expectNextCount(1)
                .expectComplete()
                .verify();

        StepVerifier.create(queueRepo.close(testQueueId))
                .expectNext(Boolean.TRUE)
                .expectComplete()
                .verify();

        StepVerifier.create(queueRepo.findById(testQueueId))
                .expectNextCount(0)
                .expectComplete()
                .verify();
    }

    @Test
    void testGetQueueLock() {
        StepVerifier.create(queueRepo.getRefreshQueueLock(REFRESH_QUEUE_LOCK, 5))
                .expectNext(Boolean.TRUE)
                .expectComplete()
                .verify();

        StepVerifier.create(queueRepo.getRefreshQueueLock(REFRESH_QUEUE_LOCK, 5))
                .expectNext(Boolean.FALSE)
                .expectComplete()
                .verify();

        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        StepVerifier.create(queueRepo.getRefreshQueueLock(REFRESH_QUEUE_LOCK, 5))
                .expectNext(Boolean.TRUE)
                .expectComplete()
                .verify();
    }

    @Test
    void testIncQueueHead() {
        StepVerifier.create(queueRepo.incHead(testQueueId, 100))
                .expectNext(100L)
                .expectComplete()
                .verify();

        StepVerifier.create(queueRepo.findById(testQueueId))
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
