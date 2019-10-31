package io.openqueue.model;

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
public class Queue {
    private String id;
    private String name;
    private String ownerId;
    private int maxCapacity;
    private int head;
    private int tail;
    private int maxActiveUsers;
    private int availableMinutePerUser;
    private String callbackWebSite;
}
