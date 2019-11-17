package io.openqueue.common.exception;

import io.openqueue.common.api.ResultCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * @author chenjing
 */
@Getter
public class TicketServiceException extends RuntimeException {
    private ResultCode resultCode;
    private HttpStatus httpStatus;

    public TicketServiceException(ResultCode resultCode, HttpStatus httpStatus) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
        this.httpStatus = httpStatus;
    }

}
