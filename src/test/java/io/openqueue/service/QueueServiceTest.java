package io.openqueue.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.openqueue.OpenqueueApplication;
import io.openqueue.dto.QueueConfigDto;
import io.openqueue.repo.QueueRepo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,classes = OpenqueueApplication.class)
class QueueServiceTest {
    @Autowired
    private QueueService queueService;

    @MockBean
    private QueueRepo queueRepo;

    private static QueueConfigDto queueConfigDto;

    @BeforeAll
    static void runBeforeTestMethod() {
        queueConfigDto = QueueConfigDto.builder()
                .availableMinutePerUser(5)
                .callbackWebSite("openqueue.cloud")
                .capacity(1000000)
                .maxActiveUsers(1000)
                .name("opq_test")
                .build();
    }

    @Test
    void testSetupQueue() {
        ResponseEntity<Object> responseEntity = queueService.setupQueue(queueConfigDto);
        JSONObject jsonRes = (JSONObject)JSON.toJSON(responseEntity.getBody());
        System.out.println("queue_id:" + jsonRes.getJSONObject("data").getString("queue_id"));
        assertThat(jsonRes.getJSONObject("data").containsKey("queue_id")).isTrue();
    }

    @Test
    void testGetQueueStatus() {
        queueService.getQueueStatus("1234");

        verify(queueRepo).getQueueStatus("1234");
    }

    @Test
    void testGetQueueConfig() {
        queueService.getQueueConfig("1234");

        verify(queueRepo).getQueueConfig("1234");
    }

    @Test
    void testUpdateQueueConfig() {
        queueService.updateQueueConfig("1234", queueConfigDto);

        verify(queueRepo).updateQueueConfig("1234", queueConfigDto);
    }

    @Test
    void testCloseQueue() {
        queueService.closeQueue("1234");

        verify(queueRepo).closeQueue("1234");
    }
}
