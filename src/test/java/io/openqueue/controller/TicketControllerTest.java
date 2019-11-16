package io.openqueue.controller;

import io.openqueue.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
class TicketControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketService ticketService;

    @Test
    void testApplyTicket() throws Exception {
        mockMvc.perform(
                post("/v1/ticket/apply?qid=1234"))
                .andReturn();

        verify(ticketService).applyTicket("1234");
    }

    @Test
    void testGetTicketUsage() throws Exception {
        mockMvc.perform(
                get("/v1/ticket/1234/stat"))
                .andReturn();
        verify(ticketService).getTicketUsageStat("1234");
    }

    @Test
    void testCheckTicketAvailability() throws Exception {
        mockMvc.perform(
                get("/v1/ticket/1234/authorization"))
                .andReturn();
        verify(ticketService).getTicketAuthorization("1234");
    }

    @Test
    void testMarkTicketInUse() throws Exception {
        mockMvc.perform(
                put("/v1/ticket/1234/state?state=USED"))
                .andReturn();
        verify(ticketService).markTicketInUse("1234");
    }

    @Test
    void testActivateTicket() throws Exception {
        mockMvc.perform(
                put("/v1/ticket/1234/state?state=ACTIVE"))
                .andReturn();
        verify(ticketService).activateTicket("1234");
    }

    @Test
    void testRevokeTicket() throws Exception {
        mockMvc.perform(
                put("/v1/ticket/1234/state?state=REVOKED"))
                .andReturn();
        verify(ticketService).revokeTicket("1234");
    }
}
