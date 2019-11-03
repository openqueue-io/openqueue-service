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
public class QueueStatusDto {
    private int head;
    private int tail;
}
