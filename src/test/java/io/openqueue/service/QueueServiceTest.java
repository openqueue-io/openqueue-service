package io.openqueue.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.openqueue.dto.QueueConfigDto;
import io.openqueue.dto.QueueSetupDto;
import io.openqueue.dto.QueueStatusDto;
import io.openqueue.model.Queue;
import io.openqueue.repo.QueueRepo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class QueueServiceTest {
    @Autowired
    private QueueService queueService;

    @MockBean
    private QueueRepo queueRepo;

    private static QueueConfigDto queueConfigDto;
    private static Queue mocQueue;

    @BeforeAll
    static void runBeforeTestMethod() {
        queueConfigDto = QueueConfigDto.builder()
                .availableSecondPerUser(300)
                .callbackURL("openqueue.cloud")
                .capacity(1000000)
                .maxActiveUsers(1000)
                .name("opq_test")
                .build();

        mocQueue = Queue.builder()
                .id("visitor")
                .availableSecondPerUser(300)
                .callbackURL("openqueue.cloud")
                .capacity(1000000)
                .maxActiveUsers(1000)
                .name("opq_test")
                .head(3)
                .tail(233)
                .build();
    }

    @Test
    void testSetupQueue() {
        when(queueRepo.findAllId()).thenReturn(Flux.just("1", "2"));
        when(queueRepo.createOrUpdate(any(Queue.class))).thenReturn(Mono.just(mocQueue));
        when(queueRepo.addToSet(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(queueService.setupQueue(queueConfigDto))
                .assertNext(responseBodyResponseEntity -> {
                    JSONObject jsonRes = (JSONObject)JSON.toJSON(responseBodyResponseEntity.getBody());
                    QueueSetupDto queueSetupDto = jsonRes.getJSONObject("data").toJavaObject(QueueSetupDto.class);
                    assertThat(queueSetupDto.getQueueId()).isNotNull();
                    assertThat(queueSetupDto.getCallbackFormat()).isNotNull();
                    assertThat(queueSetupDto.getQueueUrl()).isNotNull();
                }).verifyComplete();
    }

    @Test
    void testGetQueueStatus() {
        when(queueRepo.findById(anyString())).thenReturn(Mono.just(mocQueue));

        StepVerifier.create(queueService.getQueueStatus("visitor"))
                .assertNext(responseBodyResponseEntity -> {
                    JSONObject jsonRes = (JSONObject)JSON.toJSON(responseBodyResponseEntity.getBody());
                    QueueStatusDto tmp = jsonRes.getJSONObject("data").toJavaObject(QueueStatusDto.class);
                    assertThat(tmp.getHead()).isEqualTo(3);
                    assertThat(tmp.getTail()).isEqualTo(233);
                }).verifyComplete();

        when(queueRepo.findById(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(queueService.getQueueStatus("visitor"))
                .assertNext(responseBodyResponseEntity -> {
                    assertThat(responseBodyResponseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                }).verifyComplete();

    }

    @Test
    void testGetQueueConfig() {
        when(queueRepo.findById(anyString())).thenReturn(Mono.just(mocQueue));

        StepVerifier.create(queueService.getQueueConfig("visitor"))
                .assertNext(responseBodyResponseEntity -> {
                    JSONObject jsonRes = (JSONObject)JSON.toJSON(responseBodyResponseEntity.getBody());
                    QueueConfigDto tmp = jsonRes.getJSONObject("data").toJavaObject(QueueConfigDto.class);
                    assertThat(tmp).isEqualTo(queueConfigDto);
                }).verifyComplete();

        when(queueRepo.findById(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(queueService.getQueueConfig("visitor"))
                .assertNext(responseBodyResponseEntity -> {
                    assertThat(responseBodyResponseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                }).verifyComplete();

    }

    @Test
    void testUpdateQueueConfig() {
        when(queueRepo.findById(anyString())).thenReturn(Mono.just(mocQueue));
        when(queueRepo.createOrUpdate(any(Queue.class))).thenReturn(Mono.just(mocQueue));

        StepVerifier.create(queueService.updateQueueConfig("1234", queueConfigDto))
                .assertNext(responseBodyResponseEntity -> {
                    assertThat(responseBodyResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                    JSONObject jsonRes = (JSONObject)JSON.toJSON(responseBodyResponseEntity.getBody());
                    int code = jsonRes.getIntValue("code");
                    assertThat(code).isEqualTo(20003);
                }).verifyComplete();

        when(queueRepo.findById(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(queueService.updateQueueConfig("1234", queueConfigDto))
                .assertNext(responseBodyResponseEntity -> {
                    assertThat(responseBodyResponseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                }).verifyComplete();
    }

    @Test
    void testCloseQueue() {
        when(queueRepo.findById(anyString())).thenReturn(Mono.just(mocQueue));
        when(queueRepo.close(anyString())).thenReturn(Mono.just(Boolean.TRUE));

        StepVerifier.create(queueService.closeQueue("1234"))
                .assertNext(responseBodyResponseEntity -> {
                    assertThat(responseBodyResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                }).verifyComplete();

        when(queueRepo.close(anyString())).thenReturn(Mono.just(Boolean.FALSE));

        StepVerifier.create(queueService.closeQueue("1234"))
                .assertNext(responseBodyResponseEntity -> {
                    assertThat(responseBodyResponseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                }).verifyComplete();
    }
}
