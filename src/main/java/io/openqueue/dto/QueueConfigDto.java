package io.openqueue.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author chenjing
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QueueConfigDto {
    private String name;
    private int capacity;
    private int maxActiveUsers;
    private int availableMinutePerUser;
    private String callbackWebSite;
}
