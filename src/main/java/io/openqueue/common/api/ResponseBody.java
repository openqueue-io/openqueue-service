package io.openqueue.common.api;

import lombok.*;

/**
 * @author chenjing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseBody {
    @Builder.Default
    private int businessCode = ResultCode.SUCCESS.code;
    @Builder.Default
    private String message = ResultCode.SUCCESS.msg;
    private Object data;
}
