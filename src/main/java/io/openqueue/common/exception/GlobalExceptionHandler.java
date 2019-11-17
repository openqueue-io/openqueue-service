package io.openqueue.common.exception;

import io.openqueue.common.api.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author chenjing
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TicketServiceException.class)
    public ResponseEntity handleTicketException(TicketServiceException e) {
        logger.error("Ticket Exception", e);
        ResponseBody responseBody = ResponseBody.builder()
                .resultCode(e.getResultCode())
                .build();

        return ResponseEntity.status(e.getHttpStatus()).body(responseBody.toJSON());
    }

}
