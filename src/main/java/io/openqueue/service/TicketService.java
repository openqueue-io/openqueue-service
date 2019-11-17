package io.openqueue.service;

import io.openqueue.common.api.ResponseBody;
import io.openqueue.common.api.ResultCode;
import io.openqueue.common.exception.TicketServiceException;
import io.openqueue.dto.TicketUsageStatDto;
import io.openqueue.model.Ticket;
import io.openqueue.repo.TicketRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * @author chenjing
 */
@Service
public class TicketService {

    @Autowired
    private TicketRepo ticketRepo;

    public ResponseEntity applyTicket(String queueId){
        Ticket ticket = ticketRepo.createTicket(queueId);

        ResponseBody responseBody = ResponseBody.builder()
                .resultCode(ResultCode.APPLY_TICKET_SUCCESS)
                .data(ticket)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody.toJSON());
    }

    public ResponseEntity getTicketUsageStat(String ticketAuthCode){
        String[] ticketParts = ticketAuthCode.split(".");
        String queueId = ticketParts[0];
        String ticketId = ticketParts[0] + ticketParts[1];

        boolean active = ticketRepo.isTicketActive(ticketAuthCode, queueId);

        if (!active) {
            throw new TicketServiceException(ResultCode.TICKET_NOT_ACTIVE_EXCEPTION, HttpStatus.PRECONDITION_FAILED);
        }

        Ticket ticket = ticketRepo.findTicket(ticketId);

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

    public ResponseEntity getTicketAuthorization(String ticketAuthCode, String qid){
        String queueId = ticketAuthCode.split(".")[0];
        if (!queueId.equals(qid)) {
            throw new TicketServiceException(ResultCode.MISMATCH_QUEUE_ID_EXCEPTION, HttpStatus.CONFLICT);
        }

        boolean active = ticketRepo.isTicketActive(ticketAuthCode, queueId);
        if (!active) {
            throw new TicketServiceException(ResultCode.TICKET_NOT_ACTIVE_EXCEPTION, HttpStatus.PRECONDITION_FAILED);
        }

        String ticketId = queueId + ticketAuthCode.split(".")[1];
        Ticket ticket = ticketRepo.findTicket(ticketId);
        if (ticket.isOccupied()) {
            throw new TicketServiceException(ResultCode.TICKET_OCCUPIED_EXCEPTION, HttpStatus.CONFLICT);
        }

        ResponseBody responseBody = ResponseBody.builder()
                .resultCode(ResultCode.TICKET_AUTHORIZED_SUCCESS)
                .build();
        return ResponseEntity.ok(responseBody.toJSON());
    }

    public ResponseEntity setTicketOccupied(String ticketAuthCode){
        String queueId = ticketAuthCode.split(".")[0];

        boolean active = ticketRepo.isTicketActive(ticketAuthCode, queueId);
        if (!active) {
            throw new TicketServiceException(ResultCode.TICKET_NOT_ACTIVE_EXCEPTION, HttpStatus.PRECONDITION_FAILED);
        }

        ticketRepo.setTicketOccupied(ticketAuthCode);

        ResponseBody responseBody = ResponseBody.builder()
                .resultCode(ResultCode.SET_TICKET_OCCUPIED_SUCCESS)
                .build();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(responseBody.toJSON());
    }

    public ResponseEntity activateTicket(String ticketAuthCode){
        String[] ticketParts = ticketAuthCode.split(".");
        String queueId = ticketParts[0];
        int position = Integer.parseInt(ticketParts[1]);
        String ticketId = ticketParts[0] + ticketParts[1];
        String authCode = ticketParts[2];

        Ticket ticket = ticketRepo.findTicket(ticketId);
        if (!authCode.equals(ticket.getAuthCode())) {
            throw new TicketServiceException(ResultCode.MISMATCH_TICKET_AUTH_CODE_EXCEPTION, HttpStatus.UNAUTHORIZED);
        }

        boolean ready = ticketRepo.isTicketReadyForActivate(queueId, position);
        if (!ready) {
            throw new TicketServiceException(ResultCode.TICKET_NOT_READY_FOR_ACTIVATE_EXCEPTION, HttpStatus.PRECONDITION_FAILED);
        }

        ticketRepo.activateTicket(ticket);

        ResponseBody responseBody = ResponseBody.builder()
                .resultCode(ResultCode.ACTIVATE_TICKET_SUCCESS)
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(responseBody.toJSON());
    }

    public ResponseEntity revokeTicket(String ticketAuthCode){
        String[] ticketParts = ticketAuthCode.split(".");
        String ticketId = ticketParts[0] + ticketParts[1];
        String authCode = ticketParts[2];

        Ticket ticket = ticketRepo.findTicket(ticketId);
        if (!authCode.equals(ticket.getAuthCode())) {
            throw new TicketServiceException(ResultCode.MISMATCH_TICKET_AUTH_CODE_EXCEPTION, HttpStatus.UNAUTHORIZED);
        }

        ticketRepo.revokeTicket(ticket);

        ResponseBody responseBody = ResponseBody.builder()
                .resultCode(ResultCode.REVOKE_TICKET_SUCCESS)
                .build();

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(responseBody.toJSON());
    }
}
