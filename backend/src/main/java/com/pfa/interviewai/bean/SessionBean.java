package com.pfa.interviewai.bean;

import com.pfa.interviewai.model.InterviewSession;
import com.pfa.interviewai.model.Question;
import com.pfa.interviewai.model.enums.SessionStatus;
import com.pfa.interviewai.service.SessionService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Map;

@Named
@ViewScoped
public class SessionBean implements Serializable {

    @Inject private SessionService sessionService;
    @Inject private AuthBean authBean;

    private String sessionId;
    private InterviewSession session;
    private Question currentQuestion;
    private int currentQuestionIndex = 1;
    private boolean sessionComplete = false;

    public void init() {
        Map<String, String> params = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap();
        sessionId = params.get("sessionId");
        try {
            session = sessionService.findById(sessionId);
            if (session.getStatus() == SessionStatus.IN_PROGRESS) {
                Question unanswered = sessionService.getFirstUnansweredQuestion(sessionId);
                if (unanswered != null) {
                    currentQuestion = unanswered;
                    currentQuestionIndex = sessionService.getAnsweredCount(sessionId) + 1;
                } else {
                    currentQuestion = sessionService.getFirstQuestion(sessionId);
                }
            } else {
                currentQuestion = sessionService.getFirstQuestion(sessionId);
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", "Could not load session: " + e.getMessage()));
        }
    }

    public String getSessionId()            { return sessionId; }
    public InterviewSession getSession()    { return session; }
    public Question getCurrentQuestion()    { return currentQuestion; }
    public int getCurrentQuestionIndex()    { return currentQuestionIndex; }
    public boolean isSessionComplete()      { return sessionComplete; }
}
