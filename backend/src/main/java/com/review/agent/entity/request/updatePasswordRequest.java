package com.review.agent.entity.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class updatePasswordRequest {
    @NotNull(message = "oldPassword is required")
    private String oldPassword;
    @NotNull(message = "newPassword is required")
    private String newPassword;
}