package com.pfa.interviewai.bean;

import com.pfa.interviewai.model.InterviewSession;
import com.pfa.interviewai.service.SessionService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Named
@ViewScoped
public class HistoryBean implements Serializable {

    @Inject private SessionService sessionService;
    @Inject private AuthBean authBean;

    private List<InterviewSession> sessions = Collections.emptyList();

    public void init() {
        try {
            if (authBean.getCurrentUser() != null) {
                sessions = sessionService.findByUserId(authBean.getCurrentUser().getId());
            }
        } catch (Exception e) {
            // keep empty list
        }
    }

    public List<InterviewSession> getSessions() { return sessions; }
}
