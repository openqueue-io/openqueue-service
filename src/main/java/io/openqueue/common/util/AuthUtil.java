package io.openqueue.common.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;


/**
 * @author chenjing
 */
public class AuthUtil {

    public static String decodeUrlBase64(String base64) {
        Base64.Decoder decoder = Base64.getUrlDecoder();
        return new String(decoder.decode(base64), StandardCharsets.UTF_8);
    }

    public static boolean validateTicketToken(String ticketToken) {
        return ticketToken.matches("t:q:[a-zA-Z0-9]+:[1-9][0-9]{0,8}:[a-zA-Z0-9]+");
    }

    public static boolean queueTokenValidate(String ticketToken) {
        return ticketToken.matches("q:[a-zA-Z0-9]+");
    }

}
