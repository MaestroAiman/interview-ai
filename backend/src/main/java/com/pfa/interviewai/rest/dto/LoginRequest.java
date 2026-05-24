package com.pfa.interviewai.rest.dto;

public class LoginRequest {
    private String email;
    private String password;

    public String getEmail()    { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getPassword() { return password; }
    public void setPassword(String v) { this.password = v; }
}
