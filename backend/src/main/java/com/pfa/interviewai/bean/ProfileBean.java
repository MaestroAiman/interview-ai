package com.pfa.interviewai.bean;

import com.pfa.interviewai.service.UserService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

@Named
@ViewScoped
public class ProfileBean implements Serializable {

    @Inject private UserService userService;
    @Inject private AuthBean authBean;

    private String targetPosition;

    public void init() {
        if (authBean.getCurrentUser() != null) {
            targetPosition = authBean.getCurrentUser().getTargetPosition();
        }
    }

    public String save() {
        try {
            authBean.getCurrentUser().setTargetPosition(targetPosition);
            userService.updateProfile(authBean.getCurrentUser());
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Saved", "Profile updated."));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
        return null;
    }

    public String getTargetPosition()        { return targetPosition; }
    public void setTargetPosition(String tp) { this.targetPosition = tp; }
}
