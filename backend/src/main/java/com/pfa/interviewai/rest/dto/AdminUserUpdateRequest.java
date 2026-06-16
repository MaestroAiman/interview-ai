package com.pfa.interviewai.rest.dto;

public class AdminUserUpdateRequest {
    private String name;
    private String email;
    private String targetPosition;
    private String role;

    public AdminUserUpdateRequest() {}

    public String getName()                  { return name; }
    public void setName(String v)            { this.name = v; }
    public String getEmail()                 { return email; }
    public void setEmail(String v)           { this.email = v; }
    public String getTargetPosition()        { return targetPosition; }
    public void setTargetPosition(String v)  { this.targetPosition = v; }
    public String getRole()                  { return role; }
    public void setRole(String v)            { this.role = v; }
}
