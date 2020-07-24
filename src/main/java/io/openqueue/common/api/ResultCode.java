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
    CLOSE_QUEUE_SUCCESS(20004, "Already closed and removed the queue!"),

    /**
     * Get ticket usage stat success
     */
    GET_TICKET_USAGE_STAT_SUCCESS(20005, "Get the ticket usage stat."),

    /**
     * Ticket is authorized
     */
    TICKET_AUTHORIZED_SUCCESS(20006, "This ticket was authorized."),

    /**
     * Set ticket occupied success
     */
    SET_TICKET_OCCUPIED_SUCCESS(20201, "The ticket has been set as occupied."),

    /**
     * Activate ticket success
     */
    ACTIVATE_TICKET_SUCCESS(20202, "The ticket has been activated."),

    /**
     * Revoke ticket success
     */
    REVOKE_TICKET_SUCCESS(20203, "The ticket has been revoked."),

    /**
     * Set up queue success
     */
    SETUP_QUEUE_SUCCESS(20100, "Already set up the queue with your config!"),

    /**
     * Apply ticket success
     */
    APPLY_TICKET_SUCCESS(20101, "Apply ticket success!"),

    /**
     * Invalid request parameter
     */
    GENERAL_ARGUMENT_VALIDATION_ERROR(40000, "Invalid request parameter"),
    /**
     * Illegal ticket auth code format
     */
    ILLEGAL_TICKET_AUTH_FORMAT_EXCEPTION(40001, "Illegal ticket auth code format."),

    /**
     * Exception when close queue.
     */
    CLOSE_QUEUE_FAILED(40002, "Failed to close queue."),

    /**
     * Queue already exist
     */
    QUEUE_ALREADY_EXIST_EXCEPTION(40003, "Queue already exist."),

    /**
     * Queue already exist
     */
    TICKET_ALREADY_ACTIVATED_EXCEPTION(40004, "Ticket already activated."),

    /**
     * Exception when close queue.
     */
    SETUP_QUEUE_Validation_FAILED(40005, "Setup queue failed due to invalid parameters."),

    /**
     * Mismatch ticket auth code
     */
    MISMATCH_TICKET_AUTH_CODE_EXCEPTION(40101, "Mismatch ticket auth code."),

    /**
     * Queue not exist
     */
    QUEUE_NOT_EXIST_EXCEPTION(40401, "Queue not exist."),

    /**
     * Undefined ticket state
     */
    UNDEFINED_TICKET_STATE_EXCEPTION(40601, "New state is not acceptable because it is undefined."),

    /**
     * Undefined ticket state
     */
    UNDEFINED_TICKET_QUERY_EXCEPTION(40602, "Undefined query value."),

    /**
     * Ticket has been occupied
     */
    TICKET_OCCUPIED_EXCEPTION(40901, "This ticket has been occupied."),

    /**
     * Ticket is not belong to the queue.
     */
    MISMATCH_QUEUE_ID_EXCEPTION(40902, "This ticket is not belong to the queue."),

    /**
     * Ticket is not active
     */
    TICKET_NOT_ACTIVE_EXCEPTION(41201, "This ticket is not active."),

    /**
     * Ticket is not ready for activate
     */
    TICKET_NOT_READY_FOR_ACTIVATE_EXCEPTION(41202, "This ticket is not ready for activate.");



    final int code;

    final String message;
}
