package com.votingsystem.dto;
import lombok.Data;
@Data
public class AuthResponseDto { 
    private String token; 
    private String studentId; 
    private String role; 

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
