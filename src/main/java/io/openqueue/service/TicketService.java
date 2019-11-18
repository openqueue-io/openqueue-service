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

import java.time.Instant;

/**
 * @author chenjing
 */
@Service
public class TicketService {

    @Autowired
    private TicketRepo ticketRepo;

    @Autowired
    private QueueRepo queueRepo;

    public ResponseEntity applyTicket(String queueId){
        int position = queueRepo.getAndPlusQueueTail(queueId);
        String authCode = RandomCodeGenerator.get();
        String ticketId = "t:" + queueId + ":" + position;

        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .issueTime(Instant.now().getEpochSecond())
                .authCode(authCode)
                .build();

        ticketRepo.createTicket(ticket);

        ResponseBody responseBody = ResponseBody.builder()
                .resultCode(ResultCode.APPLY_TICKET_SUCCESS)
                .data(ticket)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody.toJSON());
    }

    public ResponseEntity getTicketUsageStat( TicketAuthDto ticketAuthDto){
        String queueActiveSetKey = "set:active:" + ticketAuthDto.getQueueId();
        boolean active = ticketRepo.isElementInSet(ticketAuthDto.getToken(), queueActiveSetKey);

        if (!active) {
            throw new TicketServiceException(ResultCode.TICKET_NOT_ACTIVE_EXCEPTION, HttpStatus.PRECONDITION_FAILED);
        }

        Ticket ticket = ticketRepo.findTicket(ticketAuthDto.getTicketId());

        TicketUsageStatDto ticketUsageStatDto = TicketUsageStatDto.builder()
                .countOfUsage(ticket.getCountOfUsage())
                .activateTime(ticket.getActivateTime())
                .build();

        ResponseBody responseBody = ResponseBody.builder()
                .resultCode(ResultCode.GET_TICKET_USAGE_STAT_SUCCESS)
                .data(ticketUsageStatDto)
                .build();

        return ResponseEntity.ok(responseBody.toJSON());

    }

    public ResponseEntity getTicketAuthorization(TicketAuthDto ticketAuthDto, String qid){
        if (!qid.equals(ticketAuthDto.getQueueId())) {
            throw new TicketServiceException(ResultCode.MISMATCH_QUEUE_ID_EXCEPTION, HttpStatus.CONFLICT);
        }

        String queueActiveSetKey = "set:active:" + ticketAuthDto.getQueueId();
        boolean active = ticketRepo.isElementInSet(ticketAuthDto.getToken(), queueActiveSetKey);
        if (!active) {
            throw new TicketServiceException(ResultCode.TICKET_NOT_ACTIVE_EXCEPTION, HttpStatus.PRECONDITION_FAILED);
        }

        Ticket ticket = ticketRepo.findTicket(ticketAuthDto.getTicketId());
        if (ticket.isOccupied()) {
            throw new TicketServiceException(ResultCode.TICKET_OCCUPIED_EXCEPTION, HttpStatus.CONFLICT);
        }

        ResponseBody responseBody = ResponseBody.builder()
                .resultCode(ResultCode.TICKET_AUTHORIZED_SUCCESS)
                .build();
        return ResponseEntity.ok(responseBody.toJSON());
    }

    public ResponseEntity setTicketOccupied(TicketAuthDto ticketAuthDto){
        String queueActiveSetKey = "set:active:" + ticketAuthDto.getQueueId();
        boolean active = ticketRepo.isElementInSet(ticketAuthDto.getToken(), queueActiveSetKey);
        if (!active) {
            throw new TicketServiceException(ResultCode.TICKET_NOT_ACTIVE_EXCEPTION, HttpStatus.PRECONDITION_FAILED);
        }

        ticketRepo.setTicketOccupied(ticketAuthDto.getTicketId());

        ResponseBody responseBody = ResponseBody.builder()
                .resultCode(ResultCode.SET_TICKET_OCCUPIED_SUCCESS)
                .build();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(responseBody.toJSON());
    }

    public ResponseEntity activateTicket(TicketAuthDto ticketAuthDto){
        Ticket ticket = ticketRepo.findTicket(ticketAuthDto.getTicketId());
        if (!ticketAuthDto.getAuthCode().equals(ticket.getAuthCode())) {
            throw new TicketServiceException(ResultCode.MISMATCH_TICKET_AUTH_CODE_EXCEPTION, HttpStatus.UNAUTHORIZED);
        }

        String queueReadySetKey = "set:ready:" + ticketAuthDto.getQueueId();
        boolean ready = ticketRepo.isElementInSet(ticketAuthDto.getTicketId(), queueReadySetKey);
        if (!ready) {
            throw new TicketServiceException(ResultCode.TICKET_NOT_READY_FOR_ACTIVATE_EXCEPTION, HttpStatus.PRECONDITION_FAILED);
        }

        String queueActiveSetKey = "set:active:" + ticketAuthDto.getQueueId();

        long expirationTime = Instant.now().getEpochSecond() +
                queueRepo.getQueue(ticketAuthDto.getQueueId()).getAvailableSecondPerUser();

        ticketRepo.addElementToSet(ticketAuthDto.getToken(), queueActiveSetKey, expirationTime);
        ticketRepo.removeElementOutOfSet(ticketAuthDto.getTicketId(), queueReadySetKey);

        ResponseBody responseBody = ResponseBody.builder()
                .resultCode(ResultCode.ACTIVATE_TICKET_SUCCESS)
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(responseBody.toJSON());
    }

    public ResponseEntity revokeTicket(TicketAuthDto ticketAuthDto){
        Ticket ticket = ticketRepo.findTicket(ticketAuthDto.getTicketId());
        if (!ticketAuthDto.getAuthCode().equals(ticket.getAuthCode())) {
            throw new TicketServiceException(ResultCode.MISMATCH_TICKET_AUTH_CODE_EXCEPTION, HttpStatus.UNAUTHORIZED);
        }

        String queueActiveSetKey = "set:active:" + ticketAuthDto.getQueueId();
        String queueReadySetKey = "set:ready:" + ticketAuthDto.getQueueId();
        ticketRepo.removeElementOutOfSet(ticketAuthDto.getToken(), queueActiveSetKey);
        ticketRepo.removeElementOutOfSet(ticketAuthDto.getTicketId(), queueReadySetKey);
        ticketRepo.revokeTicket(ticketAuthDto.getTicketId());

        ResponseBody responseBody = ResponseBody.builder()
                .resultCode(ResultCode.REVOKE_TICKET_SUCCESS)
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(responseBody.toJSON());
    }
}
