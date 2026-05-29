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

    public String getId()                          { return id; }
    public void setId(String id)                   { this.id = id; }
    public String getName()                        { return name; }
    public void setName(String name)               { this.name = name; }
    public String getEmail()                       { return email; }
    public void setEmail(String email)             { this.email = email; }
    public String getRole()                        { return role; }
    public void setRole(String role)               { this.role = role; }
    public String getTargetPosition()              { return targetPosition; }
    public void setTargetPosition(String targetPosition) { this.targetPosition = targetPosition; }
    public String getCreatedAt()                   { return createdAt; }
    public void setCreatedAt(String createdAt)     { this.createdAt = createdAt; }
}
