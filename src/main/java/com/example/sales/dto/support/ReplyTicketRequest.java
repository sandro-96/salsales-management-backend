package com.example.sales.dto.support;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReplyTicketRequest {

    @NotBlank(message = "Message is required")
    private String message;
}
