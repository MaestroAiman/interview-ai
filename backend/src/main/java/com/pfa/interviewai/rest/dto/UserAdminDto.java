package com.pfa.interviewai.rest.dto;

public class UserAdminDto {
    private String id;
    private String name;
    private String email;
    private String role;
    private String targetPosition;
    private String createdAt;

    public UserAdminDto() {}

    public UserAdminDto(String id, String name, String email, String role,
                        String targetPosition, String createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.targetPosition = targetPosition;
        this.createdAt = createdAt;
    }

    public String getId()             { return id; }
    public void setId(String v)       { this.id = v; }
    public String getName()           { return name; }
    public void setName(String v)     { this.name = v; }
    public String getEmail()          { return email; }
    public void setEmail(String v)    { this.email = v; }
    public String getRole()           { return role; }
    public void setRole(String v)     { this.role = v; }
    public String getTargetPosition() { return targetPosition; }
    public void setTargetPosition(String v) { this.targetPosition = v; }
    public String getCreatedAt()      { return createdAt; }
    public void setCreatedAt(String v){ this.createdAt = v; }
}
