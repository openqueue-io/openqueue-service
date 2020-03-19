package io.openqueue.controller;

import io.openqueue.dto.TicketAuthDto;
import io.openqueue.dto.TicketStateDto;
import io.openqueue.service.TicketService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.mockito.Mockito.verify;

@RunWith(SpringRunner.class)
@WebFluxTest(controllers = TicketController.class)
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
    void testApplyTicket() throws Exception {
        webTestClient.post()
                .uri("/v1/ticket/apply?qid=1234")
                .exchange();

        verify(ticketService).applyTicket("q:1234");
    }

    @Test
    void testGetTicketUsage() throws Exception {
        String reqUrl = "/v1/ticket/stat?ticket=" + token;

        webTestClient.get()
                .uri(reqUrl)
                .exchange();

        verify(ticketService).getTicketUsageStat(ticketAuthDto);
    }

    @Test
    void testGetTicketAuthorization() throws Exception {
        String reqUrl = "/v1/ticket/authorization?qid=1234&ticket=" + token;

        webTestClient.get()
                .uri(reqUrl)
                .exchange();

        verify(ticketService).getTicketAuthorization(ticketAuthDto, "q:1234");
    }

    @Test
    void testMarkTicketInUse() throws Exception {
        String reqUrl = "/v1/ticket/state";
        System.out.println(reqUrl);

        TicketStateDto ticketStateDto = TicketStateDto.builder()
                .state("OCCUPIED")
                .ticketToken(token)
                .build();

        webTestClient.put()
                .uri(reqUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(ticketStateDto))
                .exchange();

        verify(ticketService).setTicketOccupied(ticketAuthDto);
    }

    @Test
    void testActivateTicket() throws Exception {
        String reqUrl = "/v1/ticket/state";
        System.out.println(reqUrl);

        TicketStateDto ticketStateDto = TicketStateDto.builder()
                .state("ACTIVE")
                .ticketToken(token)
                .build();

        webTestClient.put()
                .uri(reqUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(ticketStateDto))
                .exchange();

        verify(ticketService).activateTicket(ticketAuthDto);
    }

    @Test
    void testRevokeTicket() throws Exception {
        String reqUrl = "/v1/ticket/state";
        System.out.println(reqUrl);

        TicketStateDto ticketStateDto = TicketStateDto.builder()
                .state("REVOKED")
                .ticketToken(token)
                .build();

        webTestClient.put()
                .uri(reqUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(ticketStateDto))
                .exchange();

        verify(ticketService).revokeTicket(ticketAuthDto);
    }
}
