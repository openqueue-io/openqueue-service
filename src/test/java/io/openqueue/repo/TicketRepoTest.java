package io.openqueue.repo;

import io.openqueue.common.constant.LuaScript;
import io.openqueue.model.Ticket;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.*;

import static io.openqueue.common.constant.Keys.ACTIVE_SET_PREFIX;
import static io.openqueue.common.constant.Keys.READY_SET_PREFIX;

@SpringBootTest
@Slf4j
class TicketRepoTest {

    @Autowired
    private ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @Autowired
    private TicketRepo ticketRepo;

    private static String testTicketId;
    private static String testQueueId;
    private static Ticket ticket;
    private static DefaultRedisScript<Object> initQueueScript;
    private static DefaultRedisScript<Object> flushDBScript;

    static {
        testQueueId = "q:unitest";
        testTicketId = "t:" + testQueueId + ":1";
        ticket = Ticket.builder()
                .id(testTicketId)
                .authCode("password")
                .issueTime(Instant.now().getEpochSecond())
                .build();

        initQueueScript = new DefaultRedisScript<>();
        initQueueScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/init_queue.lua")));

        flushDBScript = new DefaultRedisScript<>();
        flushDBScript.setScriptText("redis.call('flushdb')");
    }

    @BeforeEach
    void prepare() {
        // Flush redis.
        reactiveStringRedisTemplate.execute(flushDBScript).blockFirst();
        // Initialize a queue for testing.
        reactiveStringRedisTemplate.execute(initQueueScript).blockFirst();
    }

    @Test
    void testCreateTicket() {
        // Create a new ticket.
        List<String> keys = Collections.singletonList(testQueueId);
        List<String> args = Arrays.asList(
                ticket.getAuthCode(),
                String.valueOf(ticket.getIssueTime()));

        // Expect the current position is 1.
        StepVerifier.create(ticketRepo.invokeLuaScript(LuaScript.TICKET_APPLY, keys, args))
                .expectNext(1L)
                .expectComplete()
                .verify();

        // Expect the ticket has been created.
        StepVerifier.create(reactiveStringRedisTemplate.opsForHash().get(testTicketId, "id"))
                .expectNext(testTicketId)
                .expectComplete()
                .verify();

        // Expect the current position is 2.
        StepVerifier.create(ticketRepo.invokeLuaScript(LuaScript.TICKET_APPLY, keys, args))
                .expectNext(2L)
                .expectComplete()
                .verify();

        // Expect the current position is 2.
        keys = Collections.singletonList("wrongqueueid");
        StepVerifier.create(ticketRepo.invokeLuaScript(LuaScript.TICKET_APPLY, keys, args))
                .expectNext(-1L)
                .expectComplete()
                .verify();
    }

    @Test
    void testRevokeTicket() {
        createTicket();

        // Delete this ticket.
        String queueActiveSetKey = ACTIVE_SET_PREFIX + testQueueId;
        String queueReadySetKey = READY_SET_PREFIX + testQueueId;
        List<String> keys = Arrays.asList(queueActiveSetKey, queueReadySetKey);
        List<String> args = Arrays.asList("",
                testTicketId,
                "wrong password");

        // Expect the response code is 40101.
        StepVerifier.create(ticketRepo.invokeLuaScript(LuaScript.TICKET_REVOKE, keys, args))
                .expectNext(40101L)
                .expectComplete()
                .verify();

        // Expect the response code is 200.
        args = Arrays.asList("",
                testTicketId,
                ticket.getAuthCode());
        StepVerifier.create(ticketRepo.invokeLuaScript(LuaScript.TICKET_REVOKE, keys, args))
                .expectNext(200L)
                .expectComplete()
                .verify();

        // Expect the ticket has been deleted.
        StepVerifier.create(reactiveStringRedisTemplate.opsForHash().hasKey(testTicketId, "id"))
                .expectNext(Boolean.FALSE)
                .expectComplete()
                .verify();
    }

    @Test
    void testActivateTicket0() {
        createTicket();

        String queueActiveSetKey = ACTIVE_SET_PREFIX + testQueueId;
        String queueReadySetKey = READY_SET_PREFIX + testQueueId;
        List<String> keys = Arrays.asList(queueActiveSetKey, queueReadySetKey);
        List<String> args = Arrays.asList("",
                testTicketId,
                testQueueId,
                "wrong password");

        /*
          Wrong password case.
          Expect the response code is 40101.
         */
        StepVerifier.create(ticketRepo.invokeLuaScript(LuaScript.TICKET_ACTIVATE, keys, args))
                .expectNext(40101L)
                .expectComplete()
                .verify();

    }

    @Test
    void testActivateTicket1() {
        createTicket();

        String queueActiveSetKey = ACTIVE_SET_PREFIX + testQueueId;
        String queueReadySetKey = READY_SET_PREFIX + testQueueId;
        List<String> keys = Arrays.asList(queueActiveSetKey, queueReadySetKey);
        List<String> args = Arrays.asList("",
                testTicketId,
                testQueueId,
                ticket.getAuthCode());
        /*
          Ticket not in the ready set, expect the response code is 41202.
         */
        StepVerifier.create(ticketRepo.invokeLuaScript(LuaScript.TICKET_ACTIVATE, keys, args))
                .expectNext(41202L)
                .expectComplete()
                .verify();
    }

    @Test
    void testActivateTicket2() {
        createTicket();

        String queueActiveSetKey = ACTIVE_SET_PREFIX + testQueueId;
        String queueReadySetKey = READY_SET_PREFIX + testQueueId;
        List<String> keys = Arrays.asList(queueActiveSetKey, queueReadySetKey);
        List<String> args = Arrays.asList("testToken",
                testTicketId,
                testQueueId,
                ticket.getAuthCode());

        reactiveStringRedisTemplate.opsForZSet().add(queueActiveSetKey, "testToken", 200).block();
        /*
          Ticket already in the active set, expect the response code is 40004.
         */
        StepVerifier.create(ticketRepo.invokeLuaScript(LuaScript.TICKET_ACTIVATE, keys, args))
                .expectNext(40004L)
                .expectComplete()
                .verify();
    }

    @Test
    void testActivateTicket3() {
        createTicket();

        String queueActiveSetKey = ACTIVE_SET_PREFIX + testQueueId;
        String queueReadySetKey = READY_SET_PREFIX + testQueueId;
        List<String> keys = Arrays.asList(queueActiveSetKey, queueReadySetKey);
        List<String> args = Arrays.asList("testToken",
                testTicketId,
                testQueueId,
                ticket.getAuthCode());

        reactiveStringRedisTemplate.opsForZSet().add(queueReadySetKey, testTicketId, 200).block();
        /*
          Ticket in the ready set, expect the response code is 200.
         */
        StepVerifier.create(ticketRepo.invokeLuaScript(LuaScript.TICKET_ACTIVATE, keys, args))
                .expectNext(200L)
                .verifyComplete();

        StepVerifier.create(reactiveStringRedisTemplate.opsForZSet().score(queueActiveSetKey, "testToken"))
                .expectNextMatches(score -> score > 0)
                .verifyComplete();

        StepVerifier.create(reactiveStringRedisTemplate.opsForZSet().score(queueReadySetKey, "testToken"))
                .expectComplete()
                .verify();

    }

    @Test
    void testVerifyTicket () {
        createTicket();

        String queueActiveSetKey = ACTIVE_SET_PREFIX + testQueueId;
        List<String> keys = Collections.singletonList(queueActiveSetKey);
        List<String> args = Arrays.asList(testTicketId, testTicketId);

        // Ticket not activated, expected response code is 41201.
        StepVerifier.create(ticketRepo.invokeLuaScript(LuaScript.TICKET_VERIFY, keys, args))
                .expectNext(41201L)
                .verifyComplete();

        reactiveStringRedisTemplate.opsForZSet().add(queueActiveSetKey, testTicketId, 200).block();
        // Ticket already activated, expected response code is 200.
        StepVerifier.create(ticketRepo.invokeLuaScript(LuaScript.TICKET_VERIFY, keys, args))
                .expectNext(200L)
                .verifyComplete();

        reactiveStringRedisTemplate.opsForHash().put(testTicketId, "occupied", "true").block();
        // Ticket already marked as occupied, expected response code is 40901.
        StepVerifier.create(ticketRepo.invokeLuaScript(LuaScript.TICKET_VERIFY, keys, args))
                .expectNext(40901L)
                .verifyComplete();
    }

    private void createTicket() {
        DefaultRedisScript<Object> script = new DefaultRedisScript<>();
        String scriptText = String.format("redis.call('hset', '%s', 'id', '%s', 'authCode', '%s', 'issueTime', '%d')", testTicketId, testTicketId, ticket.getAuthCode(), ticket.getIssueTime());
        log.info(scriptText);
        script.setScriptText(scriptText);
        reactiveStringRedisTemplate.execute(script).blockFirst();
    }
}
