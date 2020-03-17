//package io.openqueue.service;
//
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONObject;
//import io.openqueue.dto.QueueConfigDto;
//import io.openqueue.dto.QueueSetupDto;
//import io.openqueue.dto.QueueStatusDto;
//import io.openqueue.model.Queue;
//import io.openqueue.repo.QueueRepo;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.ResponseEntity;
//
//import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//
//@SpringBootTest
//class QueueServiceTest {
//    @Autowired
//    private QueueService queueService;
//
//    @MockBean
//    private QueueRepo queueRepo;
//
//    private static QueueConfigDto queueConfigDto;
//
//    @BeforeAll
//    static void runBeforeTestMethod() {
//        queueConfigDto = QueueConfigDto.builder()
//                .availableSecondPerUser(300)
//                .callbackURL("openqueue.cloud")
//                .capacity(1000000)
//                .maxActiveUsers(1000)
//                .name("opq_test")
//                .build();
//    }
//
//    @Test
//    void testSetupQueue() {
//        ResponseEntity responseEntity = queueService.setupQueue(queueConfigDto);
//        JSONObject jsonRes = (JSONObject)JSON.toJSON(responseEntity.getBody());
//        QueueSetupDto queueSetupDto = jsonRes.getJSONObject("data").toJavaObject(QueueSetupDto.class);
//        assertThat(queueSetupDto.getQueueId()).isNotNull();
//        assertThat(queueSetupDto.getCallbackFormat()).isNotNull();
//        assertThat(queueSetupDto.getQueueUrl()).isNotNull();
//    }
//
//    @Test
//    void testGetQueueStatus() {
//        Queue mocQueue = Queue.builder()
//                .id("visitor")
//                .availableSecondPerUser(300)
//                .callbackURL("openqueue.cloud")
//                .capacity(1000000)
//                .maxActiveUsers(1000)
//                .name("opq_test")
//                .head(3)
//                .tail(233)
//                .build();
//        when(queueRepo.getQueue(anyString())).thenReturn(mocQueue);
//
//        ResponseEntity responseEntity = queueService.getQueueStatus("visitor");
//        JSONObject jsonRes = (JSONObject)JSON.toJSON(responseEntity.getBody());
//        QueueStatusDto tmp = jsonRes.getJSONObject("data").toJavaObject(QueueStatusDto.class);
//        assertThat(tmp.getHead()).isEqualTo(3);
//        assertThat(tmp.getTail()).isEqualTo(233);
//    }
//
//    @Test
//    void testGetQueueConfig() {
//        Queue mocQueue = Queue.builder()
//                .id("visitor")
//                .availableSecondPerUser(300)
//                .callbackURL("openqueue.cloud")
//                .capacity(1000000)
//                .maxActiveUsers(1000)
//                .name("opq_test")
//                .build();
//        when(queueRepo.getQueue(anyString())).thenReturn(mocQueue);
//
//        ResponseEntity responseEntity = queueService.getQueueConfig("visitor");
//        JSONObject jsonRes = (JSONObject)JSON.toJSON(responseEntity.getBody());
//        QueueConfigDto tmp = jsonRes.getJSONObject("data").toJavaObject(QueueConfigDto.class);
//        assertThat(tmp).isEqualTo(queueConfigDto);
//    }
//
//    @Test
//    void testUpdateQueueConfig() {
//        queueService.updateQueueConfig("1234", queueConfigDto);
//
//        verify(queueRepo).updateQueueConfig("1234", queueConfigDto);
//    }
//
//    @Test
//    void testCloseQueue() {
//        queueService.closeQueue("1234");
//
//        verify(queueRepo).closeQueue("1234");
//    }
//}
