package io.openqueue.common.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Result Code Enum
 *
 * @author chenjing
 */
@Getter
@AllArgsConstructor
public enum ResultCode {
    /**
     * Set up queue success
     */
    SETUP_QUEUE_SUCCESS(20100, "Already set up the queue with your config!"),

    /**
     * Get queue status success
     */
    GET_QUEUE_STATUS_SUCCESS(20001, "Get the queue's runtime status."),

    /**
     * Get queue config success
     */
    GET_QUEUE_CONFIG_SUCCESS(20002, "Get the queue's config."),

    /**
     * Update queue config success
     */
    UPDATE_QUEUE_CONFIG_SUCCESS(20003, "Update the queue's config success!"),

    /**
     * Close queue success
     */
    CLOSE_QUEUE_SUCCESS(20004, "Already closed and removed the queue!");

    final int code;

    final String message;
}
