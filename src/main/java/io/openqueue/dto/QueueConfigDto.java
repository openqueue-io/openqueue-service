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
    @Builder.Default
    private int capacity = 1000000;
    private int maxActiveUsers;
    private int availableSecondPerUser;
    private String callbackWebSite;
}
