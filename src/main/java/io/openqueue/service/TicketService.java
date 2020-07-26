package io.openqueue.service;

import io.openqueue.common.api.ResponseBody;
import io.openqueue.common.api.ResultCode;
import io.openqueue.common.constant.LuaScript;
import io.openqueue.common.exception.TicketServiceException;
import io.openqueue.common.util.RandomCodeGenerator;
import io.openqueue.dto.TicketAuthDto;
import io.openqueue.model.Ticket;
import io.openqueue.repo.QueueRepo;
import io.openqueue.repo.TicketRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.openqueue.common.constant.Keys.ACTIVE_SET_PREFIX;
import static io.openqueue.common.constant.Keys.READY_SET_PREFIX;

/**
 * @author chenjing
 */
@Service
public class TicketService {

    @Autowired
    private TicketRepo ticketRepo;

    @Autowired
    private QueueRepo queueRepo;

    @Autowired
    @Qualifier("ExceptionMapper")
    Map<Integer, TicketServiceException> exceptionMap;

    public Mono<ResponseEntity<ResponseBody>> applyTicket(String queueId) {
        String authCode = RandomCodeGenerator.getCode();
        long issueTime = Instant.now().getEpochSecond();

        List<String> keys = Collections.singletonList(queueId);
        List<String> args = Arrays.asList(
                authCode,
                String.valueOf(issueTime));

        return ticketRepo
                .invokeLuaScript(LuaScript.TICKET_APPLY, keys, args)
                .flatMap(position -> {
                    if (position == -1) {
                        throw new TicketServiceException(ResultCode.QUEUE_NOT_EXIST_EXCEPTION, HttpStatus.NOT_FOUND);
                    }

                    Ticket ticket = Ticket.builder()
                            .id("t:" + queueId + ":" + position)
                            .authCode(authCode)
                            .issueTime(issueTime)
                            .build();

                    ResponseBody responseBody = new ResponseBody(ResultCode.APPLY_TICKET_SUCCESS, ticket);
                    return Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(responseBody));
                });
    }

    public Mono<ResponseEntity<ResponseBody>> verifyTicket(TicketAuthDto ticketAuthDto) {

        String queueActiveSetKey = ACTIVE_SET_PREFIX + ticketAuthDto.getQueueId();
        List<String> keys = Collections.singletonList(queueActiveSetKey);
        List<String> args = Arrays.asList(ticketAuthDto.getToken(), ticketAuthDto.getTicketId());

        return ticketRepo
                .invokeLuaScript(LuaScript.TICKET_VERIFY, keys, args)
                .flatMap(response -> {
                    checkResponse(response.intValue());
                    return Mono.just(ResponseEntity.ok(new ResponseBody(ResultCode.TICKET_AUTHORIZED_SUCCESS)));
                })
                .single();
    }

    public Mono<ResponseEntity<ResponseBody>> activateTicket(TicketAuthDto ticketAuthDto) {
        String queueActiveSetKey = ACTIVE_SET_PREFIX + ticketAuthDto.getQueueId();
        String queueReadySetKey = READY_SET_PREFIX + ticketAuthDto.getQueueId();
        List<String> keys = Arrays.asList(queueActiveSetKey, queueReadySetKey);
        List<String> args = Arrays.asList(ticketAuthDto.getToken(),
                ticketAuthDto.getTicketId(),
                ticketAuthDto.getQueueId(),
                ticketAuthDto.getAuthCode(),
                String.valueOf(Instant.now().getEpochSecond()));

        return ticketRepo
                .invokeLuaScript(LuaScript.TICKET_ACTIVATE, keys, args)
                .flatMap(response -> {
                    checkResponse(response.intValue());
                    return Mono.just(ResponseEntity.ok(new ResponseBody(ResultCode.ACTIVATE_TICKET_SUCCESS)));
                })
                .single();
    }

    public Mono<ResponseEntity<ResponseBody>> revokeTicket(TicketAuthDto ticketAuthDto) {
        String queueActiveSetKey = ACTIVE_SET_PREFIX + ticketAuthDto.getQueueId();
        String queueReadySetKey = READY_SET_PREFIX + ticketAuthDto.getQueueId();
        List<String> keys = Arrays.asList(queueActiveSetKey, queueReadySetKey);
        List<String> args = Arrays.asList(ticketAuthDto.getToken(),
                ticketAuthDto.getTicketId(),
                ticketAuthDto.getAuthCode());

        return ticketRepo
                .invokeLuaScript(LuaScript.TICKET_REVOKE, keys, args)
                .flatMap(response -> {
                    checkResponse(response.intValue());
                    return Mono.just(ResponseEntity.accepted().body(new ResponseBody(ResultCode.REVOKE_TICKET_SUCCESS)));
                })
                .single();
    }

    private void checkResponse(int responseCode) {
        TicketServiceException ticketServiceException = exceptionMap.getOrDefault(responseCode, null);
        if (ticketServiceException != null) {
            throw ticketServiceException;
        }
    }
}
