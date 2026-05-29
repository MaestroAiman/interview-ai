package com.pfa.interviewai.bean;

import com.pfa.interviewai.model.InterviewSession;
import com.pfa.interviewai.service.SessionService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.pfa.interviewai.model.enums.SessionStatus;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Named
@ViewScoped
public class ProgressBean implements Serializable {

    @Inject private SessionService sessionService;
    @Inject private AuthBean authBean;

    private List<InterviewSession> sessions = Collections.emptyList();

    public void init() {
        try {
            if (authBean.getCurrentUser() != null) {
                sessions = sessionService.findByUserId(authBean.getCurrentUser().getId())
                        .stream()
                        .filter(s -> s.getStatus() == SessionStatus.COMPLETED)
                        .toList();
            }
        } catch (Exception e) {
        }
    }

    public List<InterviewSession> getSessions() { return sessions; }

    public String getScoresJson() {
        String values = sessions.stream()
            .map(s -> String.valueOf(s.getOverallScore()))
            .collect(Collectors.joining(","));
        return "[" + values + "]";
    }

    public String getLabelsJson() {
        int[] i = {1};
        String labels = sessions.stream()
            .map(s -> "\"Session " + i[0]++ + "\"")
            .collect(Collectors.joining(","));
        return "[" + labels + "]";
    }
}
