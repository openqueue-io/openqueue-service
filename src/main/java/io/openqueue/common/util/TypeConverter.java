package io.openqueue.common.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.Map;

public class TypeConverter {

    public static Map<String, Object> pojo2Map(Object pojo) {
        return (Map<String, Object>) (JSONObject) JSON.toJSON(pojo);
    }

    public static <T> T map2Pojo(Map map, Class<T> clazz) {
        JSON map2Json = (JSON) JSON.toJSON(map);
        return map2Json.toJavaObject(clazz);
    }

    public static <T> T cast(Object from, Class<T> toClazz) {
        Map<String, Object> attrMap = pojo2Map(from);
        return map2Pojo(attrMap, toClazz);
    }
}
