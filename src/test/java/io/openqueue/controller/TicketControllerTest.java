package io.openqueue.controller;

import io.openqueue.dto.TicketAuthDto;
import io.openqueue.service.TicketService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
class TicketControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketService ticketService;

    private static String token;
    private static TicketAuthDto ticketAuthDto;

    @BeforeAll
    static void runBeforeAllTestMethod() {
        token = Base64.getUrlEncoder().encodeToString("t:q:sad1fghS:123:oi2sdfD".getBytes());
        System.out.println("Before encode:" + "t:q:sad1fghS:123:oi2sdfD");
        System.out.println("testTicketId encode to base64url: " + token);
        String decode = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        System.out.println("After decode:" + decode);

        ticketAuthDto = TicketAuthDto.builder()
                .authCode("oi2sdfD")
                .ticketId("t:q:sad1fghS:123")
                .position(123)
                .queueId("q:sad1fghS")
                .token("t:q:sad1fghS:123:oi2sdfD")
                .build();
    }

    @Test
    void test() {
        String token = Base64.getUrlEncoder().encodeToString("t:q:3nHFKz:30:pWtrL".getBytes());
        System.out.println(token);
    }

    @Test
    void testApplyTicket() throws Exception {
        mockMvc.perform(
                post("/v1/ticket/apply?qid=1234"))
                .andReturn();

        verify(ticketService).applyTicket("q:1234");
    }

    @Test
    void testGetTicketUsage() throws Exception {
        String reqUrl = String.format("/v1/ticket/%s/stat", token);
        System.out.println(reqUrl);
        mockMvc.perform(
                get(reqUrl))
                .andReturn();
        verify(ticketService).getTicketUsageStat(ticketAuthDto);
    }

    @Test
    void testGetTicketAuthorization() throws Exception {
        String reqUrl = String.format("/v1/ticket/%s/authorization?qid=1234", token);
        System.out.println(reqUrl);
        mockMvc.perform(
                get(reqUrl))
                .andReturn();
        verify(ticketService).getTicketAuthorization(ticketAuthDto, "q:1234");
    }

    @Test
    void testMarkTicketInUse() throws Exception {
        String reqUrl = String.format("/v1/ticket/%s/state?state=OCCUPIED", token);
        System.out.println(reqUrl);
        mockMvc.perform(
                put(reqUrl))
                .andReturn();
        verify(ticketService).setTicketOccupied(ticketAuthDto);
    }

    @Test
    void testActivateTicket() throws Exception {
        String reqUrl = String.format("/v1/ticket/%s/state?state=ACTIVE", token);
        System.out.println(reqUrl);
        mockMvc.perform(
                put(reqUrl))
                .andReturn();
        verify(ticketService).activateTicket(ticketAuthDto);
    }

    @Test
    void testRevokeTicket() throws Exception {
        String reqUrl = String.format("/v1/ticket/%s/state?state=REVOKED", token);
        System.out.println(reqUrl);
        mockMvc.perform(
                put(reqUrl))
                .andReturn();
        verify(ticketService).revokeTicket(ticketAuthDto);
    }
}
