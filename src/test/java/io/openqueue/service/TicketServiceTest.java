package io.openqueue.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.openqueue.common.api.ResultCode;
import io.openqueue.common.exception.TicketServiceException;
import io.openqueue.dto.TicketAuthDto;
import io.openqueue.dto.TicketUsageStatDto;
import io.openqueue.model.Queue;
import io.openqueue.model.Ticket;
import io.openqueue.repo.QueueRepo;
import io.openqueue.repo.TicketRepo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static io.openqueue.common.constant.Keys.ACTIVE_SET_PREFIX;
import static io.openqueue.common.constant.Keys.READY_SET_PREFIX;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class TicketServiceTest {

    @Autowired
    private TicketService ticketService;

    @MockBean
    private QueueRepo queueRepo;

    @MockBean
    private TicketRepo ticketRepo;

    private static String testQueueId;
    private static Queue queue;
    private static Ticket ticket;

    @BeforeAll
    static void createQueue() {
        testQueueId = "q:3nHFKa";
        queue = Queue.builder()
                .availableSecondPerUser(300)
                .callbackURL("xxx")
                .capacity(10000)
                .maxActiveUsers(500)
                .name("test_queue")
                .id(testQueueId)
                .build();

        ticket = Ticket.builder()
                .id("t:q:3nHFKa:1000")
                .countOfUsage(0)
                .issueTime(123L)
                .authCode("DRFGasjdm1")
                .activateTime(123L)
                .build();

    }

    @Test
    void testApplyTicket(){
        when(queueRepo.findById(anyString())).thenReturn(Mono.empty());
        when(queueRepo.incAndGetTail(anyString())).thenReturn(Mono.just(1L));

        StepVerifier.create(ticketService.applyTicket(testQueueId))
                .expectErrorSatisfies(error -> {
                    assert error instanceof TicketServiceException;
                    assertThat(((TicketServiceException) error).getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(((TicketServiceException) error).getResultCode()).isEqualTo(ResultCode.QUEUE_NOT_EXIST_EXCEPTION);
                })
                .verify();

        when(queueRepo.findById(anyString())).thenReturn(Mono.just(queue));
        when(queueRepo.incAndGetTail(anyString())).thenReturn(Mono.just(1L));
        when(ticketRepo.create(any(Ticket.class))).thenReturn(Mono.just(ticket));

        StepVerifier.create(ticketService.applyTicket(testQueueId))
                .assertNext(responseBodyResponseEntity -> {
                    JSONObject jsonRes = (JSONObject)JSON.toJSON(responseBodyResponseEntity.getBody());
                    Ticket ticket = jsonRes.getJSONObject("data").toJavaObject(Ticket.class);
                    System.out.println(ticket);
                    assertThat(ticket.getId()).isNotNull();
                    assertThat(ticket.getAuthCode()).isNotNull();
                    assertThat(ticket.getIssueTime()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    void testTicketUsageStat(){
        TicketAuthDto ticketAuthDto = applyTicket();
        when(ticketRepo.isTicketInSet(anyString(), anyString())).thenReturn(Mono.just(Boolean.FALSE));
        when(ticketRepo.findById(anyString())).thenReturn(Mono.just(ticket));

        StepVerifier.create(ticketService.getTicketUsageStat(ticketAuthDto))
                .expectErrorSatisfies(error -> {
                    assert error instanceof TicketServiceException;
                    assertThat(((TicketServiceException) error).getHttpStatus()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
                    assertThat(((TicketServiceException) error).getResultCode()).isEqualTo(ResultCode.TICKET_NOT_ACTIVE_EXCEPTION);
                })
                .verify();

        when(ticketRepo.isTicketInSet(anyString(), anyString())).thenReturn(Mono.just(Boolean.TRUE));
        StepVerifier.create(ticketService.getTicketUsageStat(ticketAuthDto))
                .assertNext(responseBodyResponseEntity -> {
                    JSONObject jsonRes = (JSONObject)JSON.toJSON(responseBodyResponseEntity.getBody());
                    TicketUsageStatDto ticketUsageStatDto = jsonRes.getJSONObject("data").toJavaObject(TicketUsageStatDto.class);
                    assertThat(ticketUsageStatDto.getCountOfUsage()).isEqualTo(0);
                    assertThat(ticketUsageStatDto.getActivateTime()).isEqualTo(123L);
                })
                .verifyComplete();
    }

    @Test
    void testGetTicketAuthorization(){
        TicketAuthDto ticketAuthDto = applyTicket();

        try {
            ticketService.getTicketAuthorization(ticketAuthDto, "wrong_qid").block();
        } catch (TicketServiceException e) {
            assertThat(e).isInstanceOf(TicketServiceException.class);
            assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(e.getResultCode()).isEqualTo(ResultCode.MISMATCH_QUEUE_ID_EXCEPTION);
        }

        when(ticketRepo.isTicketInSet(anyString(), anyString())).thenReturn(Mono.just(Boolean.FALSE));
        when(ticketRepo.findById(anyString())).thenReturn(Mono.just(ticket));
        when(ticketRepo.incUsage(anyString())).thenReturn(Mono.just(1L));

        StepVerifier.create(ticketService.getTicketAuthorization(ticketAuthDto, ticketAuthDto.getQueueId()))
                .expectErrorSatisfies(error -> {
                    assert error instanceof TicketServiceException;
                    assertThat(((TicketServiceException) error).getHttpStatus()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
                    assertThat(((TicketServiceException) error).getResultCode()).isEqualTo(ResultCode.TICKET_NOT_ACTIVE_EXCEPTION);
                })
                .verify();

        when(ticketRepo.isTicketInSet(anyString(), anyString())).thenReturn(Mono.just(Boolean.TRUE));
        ticket.setOccupied(true);

        StepVerifier.create(ticketService.getTicketAuthorization(ticketAuthDto, ticketAuthDto.getQueueId()))
                .expectErrorSatisfies(error -> {
                    assert error instanceof TicketServiceException;
                    assertThat(((TicketServiceException) error).getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(((TicketServiceException) error).getResultCode()).isEqualTo(ResultCode.TICKET_OCCUPIED_EXCEPTION);
                })
                .verify();

        ticket.setOccupied(false);
        StepVerifier.create(ticketService.getTicketAuthorization(ticketAuthDto, ticketAuthDto.getQueueId()))
                .assertNext(responseBodyResponseEntity -> {
                    JSONObject jsonRes = (JSONObject)JSON.toJSON(responseBodyResponseEntity.getBody());
                    assertThat(jsonRes.getIntValue("code")).isEqualTo(ResultCode.TICKET_AUTHORIZED_SUCCESS.getCode());
                    assertThat(jsonRes.getString("message")).isEqualTo(ResultCode.TICKET_AUTHORIZED_SUCCESS.getMessage());
                })
                .verifyComplete();
    }

    @Test
    void testSetTicketOccupied(){
        TicketAuthDto ticketAuthDto = applyTicket();

        when(ticketRepo.isTicketInSet(anyString(), anyString())).thenReturn(Mono.just(Boolean.FALSE));
        when(ticketRepo.setOccupied(anyString())).thenReturn(Mono.empty());
        StepVerifier.create(ticketService.setTicketOccupied(ticketAuthDto))
                .expectErrorSatisfies(error -> {
                    assert error instanceof TicketServiceException;
                    assertThat(((TicketServiceException) error).getHttpStatus()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
                    assertThat(((TicketServiceException) error).getResultCode()).isEqualTo(ResultCode.TICKET_NOT_ACTIVE_EXCEPTION);
                })
                .verify();
    }

    @Test
    void testActivateTicket(){
        TicketAuthDto ticketAuthDto = applyTicket();
        ticketAuthDto.setAuthCode("123");
        when(ticketRepo.findById(anyString())).thenReturn(Mono.just(ticket));

        try {
            ticketService.activateTicket(ticketAuthDto).block();
        } catch (TicketServiceException e) {
            assertThat(e).isInstanceOf(TicketServiceException.class);
            assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(e.getResultCode()).isEqualTo(ResultCode.MISMATCH_TICKET_AUTH_CODE_EXCEPTION);
        }

        ticketAuthDto.setAuthCode(ticket.getAuthCode());

        when(ticketRepo.isTicketInSet(startsWith(ACTIVE_SET_PREFIX), anyString())).thenReturn(Mono.just(Boolean.FALSE));
        when(ticketRepo.isTicketInSet(startsWith(READY_SET_PREFIX), anyString())).thenReturn(Mono.just(Boolean.FALSE));

        try {
            ticketService.activateTicket(ticketAuthDto).block();
        } catch (TicketServiceException e) {
            assertThat(e).isInstanceOf(TicketServiceException.class);
            assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
            assertThat(e.getResultCode()).isEqualTo(ResultCode.TICKET_NOT_READY_FOR_ACTIVATE_EXCEPTION);
        }

    }

    @Test
    void testRevokeTicket(){
        TicketAuthDto ticketAuthDto = applyTicket();
        when(ticketRepo.findById(anyString())).thenReturn(Mono.empty());

        try {
            ticketService.revokeTicket(ticketAuthDto).block();
        } catch (TicketServiceException e) {
            assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(e.getResultCode()).isEqualTo(ResultCode.MISMATCH_TICKET_AUTH_CODE_EXCEPTION);
        }

        ticketAuthDto.setAuthCode("123");
        when(ticketRepo.findById(anyString())).thenReturn(Mono.just(ticket));
        try {
            ticketService.revokeTicket(ticketAuthDto).block();
        } catch (TicketServiceException e) {
            assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(e.getResultCode()).isEqualTo(ResultCode.MISMATCH_TICKET_AUTH_CODE_EXCEPTION);
        }
    }

    private TicketAuthDto applyTicket() {

        return TicketAuthDto.builder()
                .token(ticket.getId() + ":" + ticket.getAuthCode())
                .queueId("q:" + ticket.getId().split(":")[2])
                .position(Integer.parseInt(ticket.getId().split(":")[3]))
                .ticketId(ticket.getId())
                .authCode(ticket.getAuthCode())
                .build();
    }
}
