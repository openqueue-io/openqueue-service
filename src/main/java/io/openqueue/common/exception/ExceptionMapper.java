package io.openqueue.common.exception;

import io.openqueue.common.api.ResultCode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class ExceptionMapper {

    @Bean(name = "ExceptionMapper")
    public Map<Integer, TicketServiceException> get() {
        Map<Integer, TicketServiceException> map = new HashMap<>();

        map.put(ResultCode.TICKET_NOT_ACTIVE_EXCEPTION.getCode(),
                new TicketServiceException(ResultCode.TICKET_NOT_ACTIVE_EXCEPTION, HttpStatus.PRECONDITION_FAILED));

        map.put(ResultCode.TICKET_OCCUPIED_EXCEPTION.getCode(),
                new TicketServiceException(ResultCode.TICKET_OCCUPIED_EXCEPTION, HttpStatus.CONFLICT));

        map.put(ResultCode.MISMATCH_TICKET_AUTH_CODE_EXCEPTION.getCode(),
                new TicketServiceException(ResultCode.MISMATCH_TICKET_AUTH_CODE_EXCEPTION, HttpStatus.UNAUTHORIZED));

        map.put(ResultCode.TICKET_ALREADY_ACTIVATED_EXCEPTION.getCode(),
                new TicketServiceException(ResultCode.TICKET_ALREADY_ACTIVATED_EXCEPTION, HttpStatus.BAD_REQUEST));

        map.put(ResultCode.TICKET_NOT_READY_FOR_ACTIVATE_EXCEPTION.getCode(),
                new TicketServiceException(ResultCode.TICKET_NOT_READY_FOR_ACTIVATE_EXCEPTION, HttpStatus.PRECONDITION_FAILED));

        map.put(ResultCode.MISMATCH_TICKET_AUTH_CODE_EXCEPTION.getCode(),
                new TicketServiceException(ResultCode.MISMATCH_TICKET_AUTH_CODE_EXCEPTION, HttpStatus.UNAUTHORIZED));

        return map;
    }

}
