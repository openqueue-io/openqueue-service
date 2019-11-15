package io.openqueue.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * @author chenjing
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {
    private String id;
    private String authCode;
    private Instant issueTime;
    @Builder.Default
    private boolean used = false;
    @Builder.Default
    private int countOfUsage = 0;
}
