package io.openqueue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * @author chenjing
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TicketDetailDto {
    private String ticketId;
    private Instant issueTime;
    private String authCode;
}
