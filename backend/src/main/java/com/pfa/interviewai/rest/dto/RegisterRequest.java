package com.pfa.interviewai.rest.dto;

public class RegisterRequest {
    private String name;
    private String email;
    private String password;

    public String getName()     { return name; }
    public void setName(String v) { this.name = v; }
    public String getEmail()    { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getPassword() { return password; }
    public void setPassword(String v) { this.password = v; }
}
