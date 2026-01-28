package com.review.agent.entity.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePasswordRequest {
    @NotNull(message = "oldPassword is required")
    private String oldPassword;
    @NotNull(message = "newPassword is required")
    private String newPassword;
}