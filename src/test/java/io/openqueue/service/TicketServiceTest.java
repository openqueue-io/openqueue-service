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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.Serializable;
import java.util.Set;

import static io.openqueue.common.constant.Keys.ACTIVE_SET_PREFIX;
import static io.openqueue.common.constant.Keys.READY_SET_PREFIX;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class TicketServiceTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private QueueRepo queueRepo;

    @Autowired
    private RedisTemplate<String, Serializable> redisTemplate;

    private String testQueueId;
    @BeforeEach
    void createQueue() {
        testQueueId = "q:3nHFKa";
        Queue queue = Queue.builder()
                .availableSecondPerUser(300)
                .callbackWebSite("xxx")
                .capacity(10000)
                .maxActiveUsers(500)
                .name("test_queue")
                .id(testQueueId)
                .build();

        queueRepo.setupQueue(queue);
    }

    @AfterEach
    void cleanup() {
        Set<String> keys = redisTemplate.keys("*");
        redisTemplate.delete(keys);
    }

    @Test
    void testApplyTicket(){
        boolean success = true;
        try {
            ticketService.applyTicket("q:3nHFKs");
        } catch (TicketServiceException e) {
            success = false;
            assertThat(e).isInstanceOf(TicketServiceException.class);
            assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(e.getResultCode()).isEqualTo(ResultCode.QUEUE_NOT_EXIST_EXCEPTION);
        }
        assert !success;

        ResponseEntity responseEntity = ticketService.applyTicket(testQueueId);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Ticket ticket = ((JSONObject)responseEntity.getBody()).getJSONObject("data").toJavaObject(Ticket.class);

        System.out.println(JSON.toJSONString(ticket));

    }

    @Test
    void testTicketUsageStat(){
        TicketAuthDto ticketAuthDto = applyTicket();

        boolean success = true;
        try {
            ticketService.getTicketUsageStat(ticketAuthDto);
        } catch (TicketServiceException e) {
            success = false;
            assertThat(e).isInstanceOf(TicketServiceException.class);
            assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
            assertThat(e.getResultCode()).isEqualTo(ResultCode.TICKET_NOT_ACTIVE_EXCEPTION);
        }
        assert  !success;

        redisTemplate.opsForZSet().add(ACTIVE_SET_PREFIX + ticketAuthDto.getQueueId(), ticketAuthDto.getToken(), 100);

        ResponseEntity responseEntity = ticketService.getTicketUsageStat(ticketAuthDto);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        TicketUsageStatDto ticketUsageStatDto = ((JSONObject)responseEntity.getBody()).getJSONObject("data").toJavaObject(TicketUsageStatDto.class);
        System.out.println(JSON.toJSONString(ticketUsageStatDto));
    }

    @Test
    void testGetTicketAuthorization(){
        TicketAuthDto ticketAuthDto = applyTicket();

        boolean success = true;
        try {
            ticketService.getTicketAuthorization(ticketAuthDto, "wrong_qid");
        } catch (TicketServiceException e) {
            success = false;
            assertThat(e).isInstanceOf(TicketServiceException.class);
            assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(e.getResultCode()).isEqualTo(ResultCode.MISMATCH_QUEUE_ID_EXCEPTION);
        }
        assert !success;

        success = true;
        try {
            ticketService.getTicketAuthorization(ticketAuthDto, ticketAuthDto.getQueueId());
        } catch (TicketServiceException e) {
            success = false;
            assertThat(e).isInstanceOf(TicketServiceException.class);
            assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
            assertThat(e.getResultCode()).isEqualTo(ResultCode.TICKET_NOT_ACTIVE_EXCEPTION);
        }
        assert !success;

        redisTemplate.opsForZSet().add(ACTIVE_SET_PREFIX + ticketAuthDto.getQueueId(), ticketAuthDto.getToken(), 100);
        redisTemplate.opsForHash().put(ticketAuthDto.getTicketId(), "occupied", Boolean.TRUE);
        success = true;
        try {
            ticketService.getTicketAuthorization(ticketAuthDto, ticketAuthDto.getQueueId());
        } catch (TicketServiceException e) {
            success = false;
            assertThat(e).isInstanceOf(TicketServiceException.class);
            assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(e.getResultCode()).isEqualTo(ResultCode.TICKET_OCCUPIED_EXCEPTION);
        }
        assert !success;

        redisTemplate.opsForHash().put(ticketAuthDto.getTicketId(), "occupied", Boolean.FALSE);
        ResponseEntity responseEntity = ticketService.getTicketAuthorization(ticketAuthDto, ticketAuthDto.getQueueId());
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

    }

    @Test
    void testSetTicketOccupied(){
        TicketAuthDto ticketAuthDto = applyTicket();
        redisTemplate.opsForZSet().add(ACTIVE_SET_PREFIX + ticketAuthDto.getQueueId(), ticketAuthDto.getToken(), 100);

        assertThat(redisTemplate.opsForHash().get(ticketAuthDto.getTicketId(), "occupied")).isEqualTo(Boolean.FALSE);
        ticketService.setTicketOccupied(ticketAuthDto);
        assertThat(redisTemplate.opsForHash().get(ticketAuthDto.getTicketId(), "occupied")).isEqualTo(Boolean.TRUE);
    }

    @Test
    void testActivateTicket(){
        TicketAuthDto ticketAuthDto = applyTicket();
        ticketAuthDto.setAuthCode("123");

        boolean success = true;
        try {
            ticketService.activateTicket(ticketAuthDto);
        } catch (TicketServiceException e) {
            success = false;
            assertThat(e).isInstanceOf(TicketServiceException.class);
            assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(e.getResultCode()).isEqualTo(ResultCode.MISMATCH_TICKET_AUTH_CODE_EXCEPTION);
        }
        assert !success;

        ticketAuthDto = applyTicket();
        success = true;
        try {
            ticketService.activateTicket(ticketAuthDto);
        } catch (TicketServiceException e) {
            success = false;
            assertThat(e).isInstanceOf(TicketServiceException.class);
            assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
            assertThat(e.getResultCode()).isEqualTo(ResultCode.TICKET_NOT_READY_FOR_ACTIVATE_EXCEPTION);
        }
        assert !success;

        redisTemplate.opsForZSet().add(READY_SET_PREFIX + ticketAuthDto.getQueueId(), ticketAuthDto.getTicketId(), 100);
        ticketService.activateTicket(ticketAuthDto);
        assertThat(redisTemplate.opsForZSet()
                .score(READY_SET_PREFIX + ticketAuthDto.getQueueId(), ticketAuthDto.getTicketId())).isNull();
        assertThat(redisTemplate.opsForZSet()
                .score(ACTIVE_SET_PREFIX + ticketAuthDto.getQueueId(), ticketAuthDto.getToken())).isNotNull();
    }

    @Test
    void testRevokeTicket(){
        TicketAuthDto ticketAuthDto = applyTicket();
        ticketAuthDto.setAuthCode("123");

        boolean success = true;
        try {
            ticketService.revokeTicket(ticketAuthDto);
        } catch (TicketServiceException e) {
            success = false;
            assertThat(e).isInstanceOf(TicketServiceException.class);
            assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(e.getResultCode()).isEqualTo(ResultCode.MISMATCH_TICKET_AUTH_CODE_EXCEPTION);
        }
        assert !success;

        ticketAuthDto = applyTicket();
        redisTemplate.opsForZSet().add(READY_SET_PREFIX + ticketAuthDto.getQueueId(), ticketAuthDto.getTicketId(), 100);
        redisTemplate.opsForZSet().add(ACTIVE_SET_PREFIX + ticketAuthDto.getQueueId(), ticketAuthDto.getToken(), 100);
        ticketService.revokeTicket(ticketAuthDto);

        assertThat(redisTemplate.opsForZSet()
                .score(READY_SET_PREFIX + ticketAuthDto.getQueueId(), ticketAuthDto.getTicketId())).isNull();
        assertThat(redisTemplate.opsForZSet()
                .score(ACTIVE_SET_PREFIX + ticketAuthDto.getQueueId(), ticketAuthDto.getToken())).isNull();
        assertThat(redisTemplate.opsForHash().entries(ticketAuthDto.getTicketId()).size()).isEqualTo(0);
    }

    private TicketAuthDto applyTicket() {
        ResponseEntity responseEntity = ticketService.applyTicket(testQueueId);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Ticket ticket = ((JSONObject)responseEntity.getBody()).getJSONObject("data").toJavaObject(Ticket.class);

        TicketAuthDto ticketAuthDto = TicketAuthDto.builder()
                .token(ticket.getId() + ":" + ticket.getAuthCode())
                .queueId("q:" + ticket.getId().split(":")[2])
                .position(Integer.parseInt(ticket.getId().split(":")[3]))
                .ticketId(ticket.getId())
                .authCode(ticket.getAuthCode())
                .build();
        return ticketAuthDto;
    }
}
