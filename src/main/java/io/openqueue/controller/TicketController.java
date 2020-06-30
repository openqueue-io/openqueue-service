package io.openqueue.controller;

import io.openqueue.common.api.ResponseBody;
import io.openqueue.common.api.ResultCode;
import io.openqueue.common.constant.Query;
import io.openqueue.common.constant.TicketState;
import io.openqueue.common.exception.TicketServiceException;
import io.openqueue.common.util.AuthUtil;
import io.openqueue.dto.TicketAuthDto;
import io.openqueue.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * @author chenjing
 */
@RestController
@RequestMapping("/api/v1/ticket")
@Validated
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @PostMapping
    public Mono<ResponseEntity<ResponseBody>> applyTicket(@RequestParam String qid){
        return ticketService.applyTicket(qid);
    }

    @GetMapping
    public Mono<ResponseEntity<ResponseBody>> getTicketAuthorization(@RequestParam String ticket, @RequestParam String query){
        TicketAuthDto ticketAuthDto = this.preprocess(ticket);

        switch (query) {
            case Query.TICKET_VERIFY:
                return ticketService.verifyTicket(ticketAuthDto);
            default:
                throw new TicketServiceException(ResultCode.UNDEFINED_TICKET_QUERY_EXCEPTION, HttpStatus.BAD_REQUEST);
        }

    }

    @PutMapping
    public Mono<ResponseEntity<ResponseBody>> updateTicketState(@RequestParam String ticket, @RequestParam String state){
        TicketAuthDto ticketAuthDto = this.preprocess(ticket);

        switch (state) {
            case TicketState.ACTIVE:
                return ticketService.activateTicket(ticketAuthDto);
            case TicketState.REVOKED:
                return ticketService.revokeTicket(ticketAuthDto);
            default:
                throw new TicketServiceException(ResultCode.UNDEFINED_TICKET_STATE_EXCEPTION, HttpStatus.BAD_REQUEST);
        }
    }

    private TicketAuthDto preprocess(String tokenBase64) {
        String token = AuthUtil.decodeUrlBase64(tokenBase64);

        if (!AuthUtil.validateTicketToken(token)) {
            throw new TicketServiceException(ResultCode.ILLEGAL_TICKET_AUTH_FORMAT_EXCEPTION, HttpStatus.BAD_REQUEST);
        }

        String[] ticketParts = token.split(":");
        String queueId = "q:" + ticketParts[2];
        int position = Integer.parseInt(ticketParts[3]);
        String ticketId = token.substring(0, token.lastIndexOf(":"));
        String authCode = ticketParts[4];

        return TicketAuthDto.builder()
                .token(token)
                .ticketId(ticketId)
                .queueId(queueId)
                .position(position)
                .authCode(authCode)
                .build();
    }
}
