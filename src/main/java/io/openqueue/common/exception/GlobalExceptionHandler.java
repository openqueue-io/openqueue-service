package io.openqueue.common.exception;

import io.openqueue.common.api.ResponseBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author chenjing
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketServiceException.class)
    public ResponseEntity<Object> handleTicketException(TicketServiceException e) {
        ResponseBody responseBody = new ResponseBody(e.getResultCode());
        return ResponseEntity.status(e.getHttpStatus()).body(responseBody);
    }

}
