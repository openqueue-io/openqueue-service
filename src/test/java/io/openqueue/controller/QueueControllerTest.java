package io.openqueue.controller;

import io.openqueue.dto.QueueConfigDto;
import io.openqueue.service.QueueService;
import org.junit.jupiter.api.Test;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@WebFluxTest(controllers = QueueController.class)
class QueueControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private QueueService queueService;

    @Test
    void testSetupQueue() {
        QueueConfigDto queueConfigDto = QueueConfigDto.builder()
                .permissionExpirationSeconds(300)
                .callbackURL("openqueue.cloud")
                .capacity(1000000)
                .maxActiveUsers(1000)
                .name("opq_test")
                .build();

        webTestClient.post()
                .uri("/v1/queue/setup")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(queueConfigDto))
                .exchange();

        verify(queueService).setupQueue(queueConfigDto);
    }

    @Test
    void testGetQueueStatus() {
        webTestClient.get()
                .uri("/v1/queue/1234/status")
                .exchange();

        verify(queueService).getQueueStatus("1234");
    }

    @Test
    void testGetQueueConfig() {
        webTestClient.get().uri("/v1/queue/1234/config").exchange();

        verify(queueService).getQueueConfig("1234");
    }

    @Test
    void testUpdateQueueConfig() throws Exception {
        QueueConfigDto queueConfigDto = QueueConfigDto.builder()
                .permissionExpirationSeconds(300)
                .callbackURL("openqueue.cloud")
                .capacity(1000000)
                .maxActiveUsers(1000)
                .name("opq_test")
                .build();

        webTestClient.put()
                .uri("/v1/queue/1234/config")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(queueConfigDto))
                .exchange();

        verify(queueService).updateQueueConfig("1234", queueConfigDto);
    }

    @Test
    void testCloseQueue() throws Exception {
        webTestClient.delete()
                .uri("/v1/queue/1234/close")
                .exchange();

        verify(queueService).closeQueue("1234");
    }
}
