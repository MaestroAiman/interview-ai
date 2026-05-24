package com.pfa.interviewai.bean;

import com.pfa.interviewai.model.User;
import com.pfa.interviewai.repository.UserRepository;
import com.pfa.interviewai.security.JwtUtil;
import com.pfa.interviewai.service.AuthService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.Serializable;

@Named
@SessionScoped
public class AuthBean implements Serializable {

    @Inject private AuthService authService;
    @Inject private UserRepository userRepository;
    @Inject private JwtUtil jwtUtil;

    private String name;
    private String email;
    private String password;
    private String confirmPassword;
    private String errorMessage;
    private User currentUser;

    public String login() {
        try {
            String token = authService.login(email, password);
            HttpServletResponse response = (HttpServletResponse)
                FacesContext.getCurrentInstance()
                    .getExternalContext().getResponse();
            Cookie cookie = new Cookie("interview_jwt", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(86400);
            response.addCookie(cookie);

            currentUser = userRepository.findByEmail(email).orElse(null);
            errorMessage = null;
            return "dashboard?faces-redirect=true";

        } catch (Exception e) {
            errorMessage = e.getMessage();
            return null;
        }
    }

    public String register() {
        try {
            authService.register(name, email, password, confirmPassword);
            errorMessage = null;
            return "login?faces-redirect=true&registered=true";
        } catch (Exception e) {
            errorMessage = e.getMessage();
            return null;
        }
    }

    public String logout() {
        HttpServletResponse response = (HttpServletResponse)
            FacesContext.getCurrentInstance()
                .getExternalContext().getResponse();
        Cookie cookie = new Cookie("interview_jwt", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        FacesContext.getCurrentInstance()
            .getExternalContext().invalidateSession();
        return "login?faces-redirect=true";
    }

    public String getName()               { return name; }
    public void setName(String n)         { this.name = n; }
    public String getEmail()              { return email; }
    public void setEmail(String e)        { this.email = e; }
    public String getPassword()           { return password; }
    public void setPassword(String p)     { this.password = p; }
    public String getConfirmPassword()    { return confirmPassword; }
    public void setConfirmPassword(String c) { this.confirmPassword = c; }
    public String getErrorMessage()       { return errorMessage; }
    public User getCurrentUser() {
        if (currentUser != null) return currentUser;
        try {
            FacesContext fc = FacesContext.getCurrentInstance();
            if (fc == null) return null;
            Object req = fc.getExternalContext().getRequest();
            if (!(req instanceof HttpServletRequest)) return null;
            Cookie[] cookies = ((HttpServletRequest) req).getCookies();
            if (cookies == null) return null;
            for (Cookie c : cookies) {
                if ("interview_jwt".equals(c.getName()) && jwtUtil.isTokenValid(c.getValue())) {
                    String userId = jwtUtil.extractUserId(c.getValue());
                    currentUser = userRepository.findById(userId).orElse(null);
                    return currentUser;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
    public void setCurrentUser(User u)    { this.currentUser = u; }
}
