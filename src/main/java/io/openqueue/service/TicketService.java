package io.openqueue.service;

import io.openqueue.common.api.ResponseBody;
import io.openqueue.common.api.ResultCode;
import io.openqueue.common.exception.TicketServiceException;
import io.openqueue.common.util.RandomCodeGenerator;
import io.openqueue.dto.TicketAuthDto;
import io.openqueue.model.Ticket;
import io.openqueue.repo.QueueRepo;
import io.openqueue.repo.TicketRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

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

    public Mono<ResponseEntity<ResponseBody>> applyTicket(String queueId) {
        String authCode = RandomCodeGenerator.getCode();
        Ticket ticket = Ticket.builder()
                .queueId(queueId)
                .issueTime(Instant.now().getEpochSecond())
                .authCode(authCode)
                .build();

        return ticketRepo.applyOne(ticket)
                .flatMap(position -> {
                    if (position == -1) {
                        throw new TicketServiceException(ResultCode.QUEUE_NOT_EXIST_EXCEPTION, HttpStatus.NOT_FOUND);
                    }
                    ticket.setId("t:" + queueId + ":" + position);
                    ResponseBody responseBody = new ResponseBody(ResultCode.APPLY_TICKET_SUCCESS, ticket);
                    return Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(responseBody));
                });
    }

    public Mono<ResponseEntity<ResponseBody>> verifyTicket(TicketAuthDto ticketAuthDto) {

        String queueActiveSetKey = ACTIVE_SET_PREFIX + ticketAuthDto.getQueueId();

        return ticketRepo.isTicketInSet(queueActiveSetKey, ticketAuthDto.getToken())
                .flatMap(active -> {
                    if (!active) {
                        throw new TicketServiceException(ResultCode.TICKET_NOT_ACTIVE_EXCEPTION, HttpStatus.PRECONDITION_FAILED);
                    }
                    return Mono.empty();
                })
                .then(ticketRepo.findById(ticketAuthDto.getTicketId()))
                .flatMap(ticket -> {
//                    if (ticket.isOccupied()) {
//                        throw new TicketServiceException(ResultCode.TICKET_OCCUPIED_EXCEPTION, HttpStatus.CONFLICT);
//                    }
                    return Mono.empty();
                })
                .then(ticketRepo.incUsage(ticketAuthDto.getTicketId()))
                .flatMap(usage -> {
                    return Mono.just(ResponseEntity.ok(new ResponseBody(ResultCode.TICKET_AUTHORIZED_SUCCESS)));
                });
    }

    public Mono<ResponseEntity<ResponseBody>> activateTicket(TicketAuthDto ticketAuthDto) {
        String queueActiveSetKey = ACTIVE_SET_PREFIX + ticketAuthDto.getQueueId();
        return validateTicket(ticketAuthDto)
                .then(ticketRepo.isTicketInSet(queueActiveSetKey, ticketAuthDto.getToken()))
                .flatMap(activated -> {
                    if (activated) {
                        throw new TicketServiceException(ResultCode.TICKET_ALREADY_ACTIVATED_EXCEPTION, HttpStatus.BAD_REQUEST);
                    }
                    String queueReadySetKey = READY_SET_PREFIX + ticketAuthDto.getQueueId();
                    return ticketRepo.isTicketInSet(queueReadySetKey, ticketAuthDto.getTicketId());
                })
                .flatMap(readyToActivate -> {
                    if (!readyToActivate) {
                        throw new TicketServiceException(ResultCode.TICKET_NOT_READY_FOR_ACTIVATE_EXCEPTION, HttpStatus.PRECONDITION_FAILED);
                    }
                    return doActivateTicket(ticketAuthDto);
                })
                .thenReturn(ResponseEntity.ok(new ResponseBody(ResultCode.ACTIVATE_TICKET_SUCCESS)));
    }

    private Mono<Ticket> validateTicket(TicketAuthDto ticketAuthDto) {
        TicketServiceException ticketServiceException = new TicketServiceException(ResultCode.MISMATCH_TICKET_AUTH_CODE_EXCEPTION, HttpStatus.UNAUTHORIZED);
        return ticketRepo.findById(ticketAuthDto.getTicketId())
                .flatMap(ticket -> {
                    if (!ticketAuthDto.getAuthCode().equals(ticket.getAuthCode())) {
                        return Mono.error(ticketServiceException);
                    }
                    return Mono.just(ticket);
                })
                .switchIfEmpty(Mono.error(ticketServiceException))
                .onErrorResume(throwable -> {
                    throw ticketServiceException;
                });
    }

    private Mono<Void> doActivateTicket(TicketAuthDto ticketAuthDto) {
        String queueActiveSetKey = ACTIVE_SET_PREFIX + ticketAuthDto.getQueueId();
        String queueReadySetKey = READY_SET_PREFIX + ticketAuthDto.getQueueId();

        return queueRepo.findById(ticketAuthDto.getQueueId())
                .flatMap(queue -> {
                    long expirationTime = Instant.now().getEpochSecond() + queue.getAvailableSecondPerUser();
                    return ticketRepo.addToSet(queueActiveSetKey, ticketAuthDto.getToken(), expirationTime);
                })
                .then(ticketRepo.incUsage(ticketAuthDto.getTicketId()))
                .then(ticketRepo.setActivateTime(ticketAuthDto.getTicketId(), Instant.now().getEpochSecond()))
                .then(ticketRepo.removeOutOfSetById(queueReadySetKey, ticketAuthDto.getTicketId()))
                .then();
    }

    public Mono<ResponseEntity<ResponseBody>> revokeTicket(TicketAuthDto ticketAuthDto) {
        Ticket ticket = ticketRepo.findById(ticketAuthDto.getTicketId()).block();
        if (ticket == null || !ticketAuthDto.getAuthCode().equals(ticket.getAuthCode())) {
            throw new TicketServiceException(ResultCode.MISMATCH_TICKET_AUTH_CODE_EXCEPTION, HttpStatus.UNAUTHORIZED);
        }

        String queueActiveSetKey = ACTIVE_SET_PREFIX + ticketAuthDto.getQueueId();
        String queueReadySetKey = READY_SET_PREFIX + ticketAuthDto.getQueueId();

        return ticketRepo.removeOutOfSetById(queueActiveSetKey, ticketAuthDto.getToken())
                .then(ticketRepo.removeOutOfSetById(queueReadySetKey, ticketAuthDto.getToken()))
                .then(ticketRepo.revoke(ticketAuthDto.getTicketId()))
                .thenReturn(ResponseEntity.accepted().body(new ResponseBody(ResultCode.REVOKE_TICKET_SUCCESS)));
    }
}
