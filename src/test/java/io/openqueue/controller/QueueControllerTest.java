package io.openqueue.controller;

import com.alibaba.fastjson.JSON;
import io.openqueue.OpenqueueApplication;
import io.openqueue.dto.QueueConfigDto;
import io.openqueue.service.QueueService;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.Mockito.verify;
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
        QueueConfigDto queueConfigDto = QueueConfigDto.builder()
                .availableMinutePerUser(5)
                .callbackWebSite("openqueue.cloud")
                .capacity(1000000)
                .maxActiveUsers(1000)
                .name("opq_test")
                .build();

        mockMvc.perform(
                post("/v1/queue/setup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(queueConfigDto)))
                .andReturn();

        verify(queueService).setupQueue(queueConfigDto);
    }

    @Test
    void testGetQueueStatus() throws Exception {
        MvcResult mvcResult = mockMvc.perform(
                get("/v1/queue/1234/status"))
                .andReturn();

        verify(queueService).getQueueStatus("1234");
    }

    @Test
    void testGetQueueConfig() throws Exception {
        mockMvc.perform(
                get("/v1/queue/1234/config"))
                .andReturn();

        verify(queueService).getQueueConfig("1234");
    }

    @Test
    void testUpdateQueueConfig() throws Exception {
        QueueConfigDto queueConfigDto = QueueConfigDto.builder()
                .availableMinutePerUser(5)
                .callbackWebSite("openqueue.cloud")
                .capacity(1000000)
                .maxActiveUsers(1000)
                .name("opq_test")
                .build();

        mockMvc.perform(
                put("/v1/queue/1234/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJSONString(queueConfigDto)))
                .andReturn();

        verify(queueService).updateQueueConfig("1234", queueConfigDto);
    }

    @Test
    void testCloseQueue() throws Exception {
        mockMvc.perform(
                delete("/v1/queue/1234/close"))
                .andReturn();

        verify(queueService).closeQueue("1234");
    }
}
