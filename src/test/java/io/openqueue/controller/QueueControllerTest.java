package io.openqueue.controller;

import com.alibaba.fastjson.JSON;
import io.openqueue.OpenqueueApplication;
import io.openqueue.dto.QueueConfigDto;
import io.openqueue.service.QueueService;
import org.junit.jupiter.api.Test;

import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,classes = OpenqueueApplication.class)
@AutoConfigureMockMvc
class QueueControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QueueService queueService;

    @Test
    void testSetupQueue() throws Exception {
        when(this.queueService.setupQueue(any(QueueConfigDto.class)))
                .thenAnswer((Answer) invocationOnMock -> {
                    QueueConfigDto queueConfigDto = invocationOnMock.getArgument(0);
                    return new ResponseEntity<>(queueConfigDto, HttpStatus.OK);
                });

        QueueConfigDto queueConfigDto = QueueConfigDto.builder()
                .availableMinutePerUser(5)
                .callbackWebSite("openqueue.cloud")
                .capacity(1000000)
                .maxActiveUsers(1000)
                .name("opq_test")
                .build();

        MvcResult mvcResult = mockMvc.perform(
                post("/v1/queue/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(queueConfigDto)))
                .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);
        System.out.println(mvcResult.getResponse().getContentAsString());
    }

    @Test
    void testGetQueueStatus() throws Exception {
        when(this.queueService.getQueueStatus(anyString()))
                .thenAnswer((Answer) invocationOnMock -> {
                    String queueId = invocationOnMock.getArgument(0);
                    return new ResponseEntity<>("getQueueStatus:" + queueId, HttpStatus.OK);
                });

        MvcResult mvcResult = mockMvc.perform(
                get("/v1/queue/1234/status"))
                .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);
        assertThat(mvcResult.getResponse().getContentAsString()).isEqualTo("getQueueStatus:1234");
    }

    @Test
    void testGetQueueConfig() throws Exception {
        when(this.queueService.getQueueConfig(anyString()))
                .thenAnswer((Answer) invocationOnMock -> {
                    String queueId = invocationOnMock.getArgument(0);
                    return new ResponseEntity<>("getQueueConfig:" + queueId, HttpStatus.OK);
                });

        MvcResult mvcResult = mockMvc.perform(
                get("/v1/queue/1234/config"))
                .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);
        assertThat(mvcResult.getResponse().getContentAsString()).isEqualTo("getQueueConfig:1234");
    }

    @Test
    void testUpdateQueueConfig() throws Exception {
        when(this.queueService.updateQueueConfig(anyString(), any(QueueConfigDto.class)))
                .thenAnswer((Answer) invocationOnMock -> {
                    String queueId = invocationOnMock.getArgument(0);
                    QueueConfigDto queueConfigDto = invocationOnMock.getArgument(1);
                    return new ResponseEntity<>("updateQueueConfig:" + queueId
                            + JSON.toJSONString(queueConfigDto), HttpStatus.OK);
                });

        QueueConfigDto queueConfigDto = QueueConfigDto.builder()
                .availableMinutePerUser(5)
                .callbackWebSite("openqueue.cloud")
                .capacity(1000000)
                .maxActiveUsers(1000)
                .name("opq_test")
                .build();

        MvcResult mvcResult = mockMvc.perform(
                put("/v1/queue/1234/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(queueConfigDto)))
                .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);
        assertThat(mvcResult.getResponse().getContentAsString()).startsWith("updateQueueConfig:1234");
        System.out.println(mvcResult.getResponse().getContentAsString());
    }

    @Test
    void testCloseQueue() throws Exception {
        when(this.queueService.closeQueue(anyString()))
                .thenAnswer((Answer) invocationOnMock -> {
                    String queueId = invocationOnMock.getArgument(0);
                    return new ResponseEntity<>("closeQueue:" + queueId, HttpStatus.OK);
                });

        MvcResult mvcResult = mockMvc.perform(
                delete("/v1/queue/1234/close"))
                .andReturn();

        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(200);
        assertThat(mvcResult.getResponse().getContentAsString()).isEqualTo("closeQueue:1234");
    }
}
