package io.openqueue.common.exception;

import io.openqueue.common.api.ResponseBody;
import io.openqueue.common.api.ResultCode;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author chenjing
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TicketServiceException.class)
    public Mono<ResponseEntity<ResponseBody>> handleTicketException(TicketServiceException e) {
        ResponseBody responseBody = new ResponseBody(e.getResultCode());
        return Mono.just(ResponseEntity.status(e.getHttpStatus()).body(responseBody));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ResponseBody>> handleBindException(WebExchangeBindException exception) {
        Map<String, List<String>> body = new HashMap<>();
        body.put("errors", exception.getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.toList()));
        String errorMethod = exception.getMethodParameter().getMethod().getName();
        ResultCode resultCode = null;
        switch (errorMethod) {
            case "setupQueue":
                resultCode = ResultCode.SETUP_QUEUE_Validation_FAILED;
                break;
            default:
                resultCode = ResultCode.GENERAL_ARGUMENT_VALIDATION_ERROR;
        }
        ResponseBody responseBody = new ResponseBody(resultCode, body);

        return Mono.just(ResponseEntity.badRequest().body(responseBody));
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ResponseBody>> handleArgumentViolationException(ServerWebInputException exception) {

        Map<String, String> body = new HashMap<>();
        body.put("error", exception.getReason());

        ResponseBody responseBody = new ResponseBody(ResultCode.GENERAL_ARGUMENT_VALIDATION_ERROR, body);

        return Mono.just(ResponseEntity.badRequest().body(responseBody));
    }
}
