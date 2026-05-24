package com.pfa.interviewai.bean;

import com.pfa.interviewai.model.Feedback;
import com.pfa.interviewai.model.InterviewSession;
import com.pfa.interviewai.service.FeedbackService;
import com.pfa.interviewai.service.SessionService;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Named
@ViewScoped
public class ResultsBean implements Serializable {

    @Inject private SessionService sessionService;
    @Inject private FeedbackService feedbackService;

    private InterviewSession session;
    private List<Feedback> feedbacks = Collections.emptyList();

    public void init() {
        Map<String, String> params = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap();
        String sessionId = params.get("sessionId");
        try {
            session = sessionService.findById(sessionId);
            feedbacks = feedbackService.getFeedbackForSession(sessionId);
        } catch (Exception e) {
            // handle gracefully
        }
    }

    public InterviewSession getSession()  { return session; }
    public List<Feedback> getFeedbacks()  { return feedbacks; }

    public String getOverallScoreFormatted() {
        return session != null ? String.format("%.1f", session.getOverallScore()) : "0.0";
    }
}
