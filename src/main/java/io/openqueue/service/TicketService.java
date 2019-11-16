package io.openqueue.service;

import io.openqueue.common.api.ResponseBody;
import io.openqueue.common.api.ResultCode;
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

    public ResponseEntity getTicketUsageStat(String ticketId){
        Ticket ticket = ticketRepo.findTicket(ticketId);

        if (ticket == null) {
            // TODO
            // throw ticket not exist exception
        }

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

    public ResponseEntity getTicketAuthorization(String ticketId, String qid){
        boolean active = ticketRepo.isTicketActive(ticketId);
        if (!active) {
            // TODO
            // throw ticket not active exception
        }

        ResponseBody responseBody = ResponseBody.builder()
                .resultCode(ResultCode.TICKET_AUTHORIZED_SUCCESS)
                .build();
        return ResponseEntity.ok(responseBody.toJSON());
    }

    public ResponseEntity markTicketInUse(String ticketId){
        boolean active = ticketRepo.isTicketActive(ticketId);
        if (!active) {
            // TODO
            // throw ticket not active exception
        }
        ticketRepo.markTicketInUse(ticketId);
        ResponseBody responseBody = ResponseBody.builder()
                .resultCode(ResultCode.MARK_TICKET_USED_SUCCESS)
                .build();
        return ResponseEntity.ok(responseBody.toJSON());
    }

    public ResponseEntity activateTicket(String ticketId){
        boolean ready = ticketRepo.isTicketReadyForActivate(ticketId);
        if (!ready) {

        }
        return null;
    }

    public ResponseEntity revokeTicket(String ticketId){
        return null;
    }
}
