package io.openqueue.controller;

import io.openqueue.common.api.ResultCode;
import io.openqueue.common.constant.TicketState;
import io.openqueue.common.exception.TicketServiceException;
import io.openqueue.common.util.TicketAuthUtil;
import io.openqueue.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author chenjing
 */
@RestController
@RequestMapping("/v1/ticket")
@Validated
public class TicketController {

    @Autowired
    private TicketService ticketService;

    static final Logger logger = LoggerFactory.getLogger(TicketController.class);

    @PostMapping(value = "/apply")
    public ResponseEntity applyTicket(@RequestParam String qid){
        return ticketService.applyTicket(qid);
    }

    @GetMapping(value = "/{ticketBase64}/stat")
    public ResponseEntity getTicketUsageStat(@PathVariable("ticketBase64") String ticketBase64){
        String ticketAuthCode = this.preprocess(ticketBase64);
        return ticketService.getTicketUsageStat(ticketAuthCode);
    }

    @GetMapping(value = "/{ticketBase64}/authorization")
    public ResponseEntity getTicketAuthorization(@PathVariable("{ticketBase64}") String ticketBase64,
                                                 @RequestParam String qid){
        String ticketAuthCode = this.preprocess(ticketBase64);
        return ticketService.getTicketAuthorization(ticketAuthCode, qid);
    }

    @PutMapping(value = "/{ticketBase64}/state")
    public ResponseEntity updateTicketState(@PathVariable("ticketBase64") String ticketBase64, @RequestParam String state){
        String ticketAuthCode = this.preprocess(ticketBase64);

        switch (state) {
            case TicketState.ACTIVE:
                return ticketService.activateTicket(ticketAuthCode);
            case TicketState.OCCUPIED:
                return ticketService.setTicketOccupied(ticketAuthCode);
            case TicketState.REVOKED:
                return ticketService.revokeTicket(ticketAuthCode);
            default:
                throw new TicketServiceException(ResultCode.UNDEFINED_TICKET_STATE_EXCEPTION, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    private String preprocess(String ticketAuthCodeBase64) {
        String ticketAuthCode = TicketAuthUtil.decodeUrlBase64(ticketAuthCodeBase64);

        if (!TicketAuthUtil.formatValidate(ticketAuthCode)) {
            throw new TicketServiceException(ResultCode.ILLEGAL_TICKET_AUTH_FORMAT_EXCEPTION, HttpStatus.BAD_REQUEST);
        }

        return ticketAuthCode;
    }
}
