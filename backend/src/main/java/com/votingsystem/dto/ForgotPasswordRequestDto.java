package com.votingsystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequestDto {

    @NotBlank(message = "Contact method is required")
    private String contactMethod; 

}
