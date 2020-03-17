package io.openqueue.common.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @author chenjing
 */
@Data
@AllArgsConstructor
@Builder
public class ResponseBody {
    private String message;
    private int code;
    private Object data;

    public ResponseBody(ResultCode resultCode, Object data) {
        this.code = resultCode.code;
        this.message = resultCode.message;
        this.data = data;
    }

    public ResponseBody (ResultCode resultCode) {
        this.code = resultCode.code;
        this.message = resultCode.message;
    }

}
