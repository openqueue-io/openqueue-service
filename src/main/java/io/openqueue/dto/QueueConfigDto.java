package io.openqueue.dto;

import io.openqueue.common.validation.SetupQueue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @author chenjing
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QueueConfigDto {
    @NotEmpty(groups = SetupQueue.class, message = "'name' can't be empty.")
    private String name;

    @Min(value = 100, message = "'capacity' must greater than 100.")
    private int capacity;

    @Min(value = 1, message = "'maxActiveUsers' must greater than 0.")
    private int maxActiveUsers;

    @Min(value = 1, message = "'permissionExpirationSeconds' must greater than 0.")
    private int permissionExpirationSeconds;

    @Min(value = 1, message = "'timeoutForActivateSeconds' must greater than 0.")
    private int timeoutForActivateSeconds;

    @Length(max = 255, message = "'callbackURL' too long.")
    @NotNull(groups = SetupQueue.class, message = "'callbackURL' can't be null.")
    private String callbackURL;
}
