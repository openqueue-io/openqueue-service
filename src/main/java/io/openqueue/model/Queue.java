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
    private int capacity;
    @Builder.Default
    private int head = 0;
    @Builder.Default
    private int tail = 0;
    private int maxActiveUsers;
    private int permissionExpirationSeconds;
    private int timeoutForActivateSeconds;
    private String callbackURL;
}
