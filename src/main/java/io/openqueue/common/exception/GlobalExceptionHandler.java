package io.openqueue.common.exception;

import io.openqueue.common.api.ResponseBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

/**
 * @author chenjing
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketServiceException.class)
    public Mono<ResponseEntity<ResponseBody>> handleTicketException(TicketServiceException e) {
        ResponseBody responseBody = new ResponseBody(e.getResultCode());
        return Mono.just(ResponseEntity.status(e.getHttpStatus()).body(responseBody));
    }

}
