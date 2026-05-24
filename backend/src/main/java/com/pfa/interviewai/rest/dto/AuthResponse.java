package com.pfa.interviewai.rest.dto;

public class AuthResponse {
    private String token;
    private String userId;
    private String name;
    private String role;

    public AuthResponse() {}

    public AuthResponse(String token, String userId, String name, String role) {
        this.token = token;
        this.userId = userId;
        this.name = name;
        this.role = role;
    }

    public String getToken()       { return token; }
    public void setToken(String v) { this.token = v; }
    public String getUserId()      { return userId; }
    public void setUserId(String v){ this.userId = v; }
    public String getName()        { return name; }
    public void setName(String v)  { this.name = v; }
    public String getRole()        { return role; }
    public void setRole(String v)  { this.role = v; }
}
