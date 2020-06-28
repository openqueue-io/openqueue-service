package io.openqueue.controller;

import io.openqueue.dto.TicketAuthDto;
import io.openqueue.service.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@WebFluxTest(controllers = TicketController.class)
@Slf4j
class TicketControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private TicketService ticketService;

    private static String token;
    private static TicketAuthDto ticketAuthDto;

    @BeforeAll
    static void runBeforeAllTestMethod() {
        token = Base64.getUrlEncoder().encodeToString("t:q:sad1fghS:123:oi2sdfD".getBytes());
        log.info("Before encode:" + "t:q:sad1fghS:123:oi2sdfD");
        log.info("testTicketId encode to base64url: " + token);
        String decode = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        log.info("After decode:" + decode);

        ticketAuthDto = TicketAuthDto.builder()
                .authCode("oi2sdfD")
                .ticketId("t:q:sad1fghS:123")
                .position(123)
                .queueId("q:sad1fghS")
                .token("t:q:sad1fghS:123:oi2sdfD")
                .build();
    }

    @Test
    void testApplyTicket() throws Exception {
        String reqUrl = "/api/v1/ticket?qid=1234";
        log.info("Apply Ticket: POST " + reqUrl);

        webTestClient.post()
                .uri(reqUrl)
                .exchange();

        verify(ticketService).applyTicket("q:1234");
    }


    @Test
    void testVerifyTicket() throws Exception {
        String reqUrl = "/api/v1/ticket/?query=verify&ticket=" + token;
        log.info("Verify Ticket: GET " + reqUrl);

        webTestClient.get()
                .uri(reqUrl)
                .exchange();

        verify(ticketService).verifyTicket(ticketAuthDto);
    }


    @Test
    void testActivateTicket() throws Exception {
        String reqUrl = "/api/v1/ticket?ticket=" + token + "&state=ACTIVE";
        log.info("Activate Ticket: PUT " + reqUrl);

        webTestClient.put()
                .uri(reqUrl)
                .exchange();

        verify(ticketService).activateTicket(ticketAuthDto);
    }

    @Test
    void testRevokeTicket() throws Exception {
        String reqUrl = "/api/v1/ticket?ticket=" + token + "&state=REVOKED";
        log.info("Revoke Ticket: PUT " + reqUrl);

        webTestClient.put()
                .uri(reqUrl)
                .exchange();

        verify(ticketService).revokeTicket(ticketAuthDto);
    }
}
