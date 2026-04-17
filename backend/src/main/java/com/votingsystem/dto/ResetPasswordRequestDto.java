package com.votingsystem.dto;
import lombok.Data;
@Data
public class ResetPasswordRequestDto { private String newPassword; private String confirmPassword; }
