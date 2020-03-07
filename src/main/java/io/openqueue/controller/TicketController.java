package io.openqueue.controller;

import com.alibaba.fastjson.JSONObject;
import io.openqueue.common.api.ResultCode;
import io.openqueue.common.constant.TicketState;
import io.openqueue.common.exception.TicketServiceException;
import io.openqueue.common.util.AuthUtil;
import io.openqueue.dto.TicketAuthDto;
import io.openqueue.dto.TicketStateDto;
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
    public ResponseEntity<JSONObject> applyTicket(@RequestParam String qid){
        return ticketService.applyTicket("q:" + qid);
    }

    @GetMapping(value = "/stat")
    public ResponseEntity<JSONObject> getTicketUsageStat(@RequestParam String ticket){
        TicketAuthDto ticketAuthDto = this.preprocess(ticket);
        return ticketService.getTicketUsageStat(ticketAuthDto);
    }

    @GetMapping(value = "/authorization")
    public ResponseEntity<JSONObject> getTicketAuthorization(@RequestParam String ticket,
                                                 @RequestParam String qid){
        TicketAuthDto ticketAuthDto = this.preprocess(ticket);
        return ticketService.getTicketAuthorization(ticketAuthDto, "q:" + qid);
    }

    @PutMapping(value = "/state")
    public ResponseEntity<JSONObject> updateTicketState(@RequestBody TicketStateDto ticketStateDto){
        TicketAuthDto ticketAuthDto = this.preprocess(ticketStateDto.getTicketToken());

        switch (ticketStateDto.getState()) {
            case TicketState.ACTIVE:
                return ticketService.activateTicket(ticketAuthDto);
            case TicketState.OCCUPIED:
                return ticketService.setTicketOccupied(ticketAuthDto);
            case TicketState.REVOKED:
                return ticketService.revokeTicket(ticketAuthDto);
            default:
                throw new TicketServiceException(ResultCode.UNDEFINED_TICKET_STATE_EXCEPTION, HttpStatus.NOT_ACCEPTABLE);
        }
    }

    private TicketAuthDto preprocess(String tokenBase64) {
        String token = AuthUtil.decodeUrlBase64(tokenBase64);

        if (!AuthUtil.ticketTokenValidate(token)) {
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
