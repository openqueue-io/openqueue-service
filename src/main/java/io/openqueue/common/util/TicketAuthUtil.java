package io.openqueue.common.util;

import java.util.Arrays;
import java.util.Base64;


/**
 * @author chenjing
 */
public class TicketAuthUtil {

    public static String decodeUrlBase64(String base64) {
        Base64.Decoder decoder = Base64.getUrlDecoder();
        return Arrays.toString(decoder.decode(base64));
    }

    public static boolean formatValidate(String authCode) {
        return authCode.matches("[a-zA-Z0-9]+\\.[1-9][0-9]{0,8}\\.[a-zA-Z0-9]+");
    }

}
