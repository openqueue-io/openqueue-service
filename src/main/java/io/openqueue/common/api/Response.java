package io.openqueue.common.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author chenjing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Response<T> {
    @Builder.Default
    private int code = ResultCode.SUCCESS.code;
    @Builder.Default
    private String message = ResultCode.SUCCESS.msg;
    private T data;


}
