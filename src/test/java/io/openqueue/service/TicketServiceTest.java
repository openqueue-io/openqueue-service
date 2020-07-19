package io.openqueue.service;

import io.openqueue.common.api.ResultCode;
import io.openqueue.common.exception.TicketServiceException;
import io.openqueue.common.util.TypeConverter;
import io.openqueue.dto.TicketAuthDto;
import io.openqueue.model.Queue;
import io.openqueue.model.Ticket;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.scripting.support.ResourceScriptSource;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;

import static io.openqueue.common.constant.Keys.ACTIVE_SET_PREFIX;
import static io.openqueue.common.constant.Keys.READY_SET_PREFIX;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Slf4j
class TicketServiceTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    private static Queue queue;
    private static Ticket ticket;

    static  {
        queue = Queue.builder()
                .permissionExpirationSeconds(300)
                .callbackURL("xxx")
                .capacity(10000)
                .maxActiveUsers(500)
                .name("test_queue")
                .id("q:unitest")
                .build();


        ticket = Ticket.builder()
                .id("t:" + queue.getId() + ":1")
                .authCode("password")
                .issueTime(Instant.now().getEpochSecond())
                .build();

    }

    @BeforeEach
    void prepare() {
        // Flush redis.
        DefaultRedisScript<Object> flushDBScript = new DefaultRedisScript<>();
        flushDBScript.setScriptText("redis.call('flushdb')");
        reactiveStringRedisTemplate.execute(flushDBScript).blockFirst();
    }

    @Test
    void testApplyTicket(){
        // Queue not exist, expected throw QUEUE_NOT_EXIST_EXCEPTION
        StepVerifier.create(ticketService.applyTicket(queue.getId()))
                .expectErrorSatisfies(error -> {
                    assert error instanceof TicketServiceException;
                    assertThat(((TicketServiceException) error).getResultCode()).isEqualTo(ResultCode.QUEUE_NOT_EXIST_EXCEPTION);
                })
                .verify();

        DefaultRedisScript<Object> initQueueScript = new DefaultRedisScript<>();
        initQueueScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/init_queue.lua")));
        reactiveStringRedisTemplate.execute(initQueueScript).blockFirst();

        // Queue exists, expect get a ticket.
        StepVerifier.create(ticketService.applyTicket(queue.getId()))
                .assertNext(responseBodyResponseEntity -> {
                    Map<String, Object> map = TypeConverter.pojo2Map(responseBodyResponseEntity.getBody());
                    Ticket ticket = TypeConverter.cast(map.get("data"), Ticket.class);
                            assertThat(ticket.getId()).isNotNull();
                    assertThat(ticket.getAuthCode()).isNotNull();
                    assertThat(ticket.getIssueTime()).isNotNull();
                })
                .verifyComplete();

    }

    @Test
    void testActivateTicket(){
        TicketAuthDto ticketAuthDto = applyTicket();

        // Wrong password.
        ticketAuthDto.setAuthCode("wrongpassword");
        StepVerifier.create(ticketService.activateTicket(ticketAuthDto))
                .expectErrorSatisfies(error -> {
                    assert error instanceof TicketServiceException;
                    assertThat(((TicketServiceException) error).getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(((TicketServiceException) error).getResultCode()).isEqualTo(ResultCode.MISMATCH_TICKET_AUTH_CODE_EXCEPTION);
                })
                .verify();

        // Not ready for activate.
        ticketAuthDto.setAuthCode(ticket.getAuthCode());
        StepVerifier.create(ticketService.activateTicket(ticketAuthDto))
                .expectErrorSatisfies(error -> {
                    assert error instanceof TicketServiceException;
                    assertThat(((TicketServiceException) error).getResultCode()).isEqualTo(ResultCode.TICKET_NOT_READY_FOR_ACTIVATE_EXCEPTION);
                })
                .verify();

        // Already activated.
        String queueActiveSetKey = ACTIVE_SET_PREFIX + queue.getId();
        reactiveStringRedisTemplate.opsForZSet().add(queueActiveSetKey, ticketAuthDto.getToken(), 200).block();

        StepVerifier.create(ticketService.activateTicket(ticketAuthDto))
                .expectErrorSatisfies(error -> {
                    assert error instanceof TicketServiceException;
                    assertThat(((TicketServiceException) error).getResultCode()).isEqualTo(ResultCode.TICKET_ALREADY_ACTIVATED_EXCEPTION);
                })
                .verify();

        String queueReadySetKey = READY_SET_PREFIX + queue.getId();
        reactiveStringRedisTemplate.opsForZSet().delete(queueActiveSetKey).block();
        reactiveStringRedisTemplate.opsForZSet().add(queueReadySetKey, ticketAuthDto.getTicketId(), 200).block();
        DefaultRedisScript<Object> initQueueScript = new DefaultRedisScript<>();
        initQueueScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/init_queue.lua")));
        reactiveStringRedisTemplate.execute(initQueueScript).blockFirst();

        StepVerifier.create(ticketService.activateTicket(ticketAuthDto))
                .assertNext(responseBodyResponseEntity -> {
                    Map<String, Object> map = TypeConverter.pojo2Map(responseBodyResponseEntity.getBody());
                    assertThat(map.get("code")).isEqualTo(ResultCode.ACTIVATE_TICKET_SUCCESS.getCode());
                })
                .verifyComplete();
    }

    @Test
    void testVerifyTicket() {
        TicketAuthDto ticketAuthDto = applyTicket();
        StepVerifier.create(ticketService.verifyTicket(ticketAuthDto))
                .expectErrorSatisfies(error -> {
                    assert error instanceof TicketServiceException;
                    assertThat(((TicketServiceException) error).getResultCode()).isEqualTo(ResultCode.TICKET_NOT_ACTIVE_EXCEPTION);
                })
                .verify();

        String queueActiveSetKey = ACTIVE_SET_PREFIX + queue.getId();
        reactiveStringRedisTemplate.opsForZSet().add(queueActiveSetKey, ticketAuthDto.getToken(), 200).block();
        StepVerifier.create(ticketService.verifyTicket(ticketAuthDto))
                .assertNext(responseBodyResponseEntity -> {
                    Map<String, Object> map = TypeConverter.pojo2Map(responseBodyResponseEntity.getBody());
                    assertThat(map.get("code")).isEqualTo(ResultCode.TICKET_AUTHORIZED_SUCCESS.getCode());
                })
                .verifyComplete();

        reactiveStringRedisTemplate.opsForHash().put(ticket.getId(), "occupied", "true").block();
        StepVerifier.create(ticketService.verifyTicket(ticketAuthDto))
                .expectErrorSatisfies(error -> {
                    assert error instanceof TicketServiceException;
                    assertThat(((TicketServiceException) error).getResultCode()).isEqualTo(ResultCode.TICKET_OCCUPIED_EXCEPTION);
                })
                .verify();
    }

    @Test
    void testRevokeTicket(){
        TicketAuthDto ticketAuthDto = applyTicket();

        // Wrong auth code
        ticketAuthDto.setAuthCode("wrongpassword");
        StepVerifier.create(ticketService.revokeTicket(ticketAuthDto))
                .expectErrorSatisfies(error -> {
                    assert error instanceof TicketServiceException;
                    assertThat(((TicketServiceException) error).getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(((TicketServiceException) error).getResultCode()).isEqualTo(ResultCode.MISMATCH_TICKET_AUTH_CODE_EXCEPTION);
                })
                .verify();

        ticketAuthDto.setAuthCode(ticket.getAuthCode());
        StepVerifier.create(ticketService.revokeTicket(ticketAuthDto))
                .assertNext(responseBodyResponseEntity -> {
                    Map<String, Object> map = TypeConverter.pojo2Map(responseBodyResponseEntity.getBody());
                    assertThat(map.get("code")).isEqualTo(ResultCode.REVOKE_TICKET_SUCCESS.getCode());
                })
                .verifyComplete();
    }

    private TicketAuthDto applyTicket() {
        DefaultRedisScript<Object> script = new DefaultRedisScript<>();
        String scriptText = String.format("redis.call('hset', '%s', 'id', '%s', 'authCode', '%s', 'issueTime', '%d')", ticket.getId(), ticket.getId(), ticket.getAuthCode(), ticket.getIssueTime());
        log.info(scriptText);
        script.setScriptText(scriptText);
        reactiveStringRedisTemplate.execute(script).blockFirst();

        return TicketAuthDto.builder()
                .token(ticket.getId() + ":" + ticket.getAuthCode())
                .queueId(queue.getId())
                .position(1)
                .ticketId(ticket.getId())
                .authCode(ticket.getAuthCode())
                .build();
    }
}
