package com.pfa.interviewai.rest.dto;

public class AdminUserCreateRequest {
    private String name;
    private String email;
    private String password;
    private String role;
    private String targetPosition;

    public AdminUserCreateRequest() {}

    public String getName()                  { return name; }
    public void setName(String v)            { this.name = v; }
    public String getEmail()                 { return email; }
    public void setEmail(String v)           { this.email = v; }
    public String getPassword()              { return password; }
    public void setPassword(String v)        { this.password = v; }
    public String getRole()                  { return role; }
    public void setRole(String v)            { this.role = v; }
    public String getTargetPosition()        { return targetPosition; }
    public void setTargetPosition(String v)  { this.targetPosition = v; }
}
