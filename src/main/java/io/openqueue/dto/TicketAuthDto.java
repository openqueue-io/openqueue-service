package io.openqueue.dto;

import lombok.Builder;
import lombok.Data;

/**
 * @author chenjing
 */
@Data
@Builder
public class TicketAuthDto {
    private String ticketId;
    private String queueId;
    private String authCode;
    private String token;
    private int position;
}
