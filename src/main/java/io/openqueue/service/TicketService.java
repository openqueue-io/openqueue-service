package io.openqueue.service;

import io.openqueue.common.api.ResponseBody;
import io.openqueue.common.api.ResultCode;
import io.openqueue.common.exception.TicketServiceException;
import io.openqueue.common.util.RandomCodeGenerator;
import io.openqueue.dto.TicketAuthDto;
import io.openqueue.dto.TicketUsageStatDto;
import io.openqueue.model.Ticket;
import io.openqueue.repo.QueueRepo;
import io.openqueue.repo.TicketRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;

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
        return queueRepo.findById(queueId)
                .hasElement()
                .flatMap(hasElement -> {
                    if (!hasElement) {
                        throw new TicketServiceException(ResultCode.QUEUE_NOT_EXIST_EXCEPTION, HttpStatus.NOT_FOUND);
                    }
                    return Mono.empty();
                })
                .then(queueRepo.incAndGetTail(queueId))
                .flatMap(position -> {
                    String authCode = RandomCodeGenerator.getCode();
                    String ticketId = "t:" + queueId + ":" + position;
                    Ticket ticket = Ticket.builder()
                            .id(ticketId)
                            .issueTime(Instant.now().getEpochSecond())
                            .authCode(authCode)
                            .build();
                    return ticketRepo.create(ticket);
                })
                .flatMap(ticket -> {
                            ResponseBody responseBody = new ResponseBody(ResultCode.APPLY_TICKET_SUCCESS, ticket);
                            return Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(responseBody));
                        }
                );
    }

    public Mono<ResponseEntity<ResponseBody>> getTicketUsageStat(TicketAuthDto ticketAuthDto) {
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
                    TicketUsageStatDto ticketUsageStatDto = TicketUsageStatDto.builder()
                            .countOfUsage(ticket.getCountOfUsage())
                            .activateTime(ticket.getActivateTime())
                            .build();
                    return Mono.just(ResponseEntity.ok(new ResponseBody(ResultCode.GET_TICKET_USAGE_STAT_SUCCESS, ticketUsageStatDto)));
                });
    }

    public Mono<ResponseEntity<ResponseBody>> getTicketAuthorization(TicketAuthDto ticketAuthDto, String qid) {
        if (!qid.equals(ticketAuthDto.getQueueId())) {
            throw new TicketServiceException(ResultCode.MISMATCH_QUEUE_ID_EXCEPTION, HttpStatus.CONFLICT);
        }

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
                    if (ticket.isOccupied()) {
                        throw new TicketServiceException(ResultCode.TICKET_OCCUPIED_EXCEPTION, HttpStatus.CONFLICT);
                    }
                    return Mono.empty();
                })
                .then(ticketRepo.incUsage(ticketAuthDto.getTicketId()))
                .flatMap(usage -> {
                    return Mono.just(ResponseEntity.ok(new ResponseBody(ResultCode.TICKET_AUTHORIZED_SUCCESS)));
                });
    }

    public Mono<ResponseEntity<ResponseBody>> setTicketOccupied(TicketAuthDto ticketAuthDto) {
        String queueActiveSetKey = ACTIVE_SET_PREFIX + ticketAuthDto.getQueueId();

        return ticketRepo.isTicketInSet(queueActiveSetKey, ticketAuthDto.getToken())
                .flatMap(active -> {
                    if (!active) {
                        throw new TicketServiceException(ResultCode.TICKET_NOT_ACTIVE_EXCEPTION, HttpStatus.PRECONDITION_FAILED);
                    }
                    return Mono.empty();
                })
                .then(ticketRepo.setOccupied(ticketAuthDto.getTicketId()))
                .thenReturn(ResponseEntity.ok(new ResponseBody(ResultCode.SET_TICKET_OCCUPIED_SUCCESS)));
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
                    return doActivatedTicket(ticketAuthDto);
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

    private Mono<Void> doActivatedTicket(TicketAuthDto ticketAuthDto) {
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
