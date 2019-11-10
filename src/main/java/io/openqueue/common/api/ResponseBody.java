package io.openqueue.common.api;

import com.alibaba.fastjson.JSONObject;
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
public class ResponseBody {
    private ResultCode resultCode;
    private Object data;

    public JSONObject toJSON() {
        JSONObject flattedJsonObject = new JSONObject();
        flattedJsonObject.put("code", resultCode.code);
        flattedJsonObject.put("message", resultCode.message);
        flattedJsonObject.put("data", data);
        return flattedJsonObject;
    }
}
