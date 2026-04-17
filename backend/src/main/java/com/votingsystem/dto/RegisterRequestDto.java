package com.votingsystem.dto;
import lombok.Data;
import java.time.LocalDate;
@Data
public class RegisterRequestDto { private String studentId; private String password; private String firstName; private String lastName; private LocalDate birthday; private String gmail; }
