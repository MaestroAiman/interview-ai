package com.pfa.interviewai.bean;

import com.pfa.interviewai.model.enums.Difficulty;
import com.pfa.interviewai.model.enums.InterviewType;
import com.pfa.interviewai.service.SessionService;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

@Named
@ViewScoped
public class SetupBean implements Serializable {

    @Inject private SessionService sessionService;
    @Inject private AuthBean authBean;

    private InterviewType selectedType = InterviewType.TECHNICAL;
    private String position = "";
    private Difficulty difficulty = Difficulty.MID;
    private int questionCount = 10;
    private int currentStep = 1;
    private boolean loading = false;
    private String errorMessage;

    public InterviewType[] getInterviewTypes() { return InterviewType.values(); }
    public Difficulty[]    getDifficulties()   { return Difficulty.values(); }

    public String getDurationEstimate() {
        return "~" + (questionCount * 2) + " minutes";
    }

    public void nextStep() {
        if (currentStep < 5) currentStep++;
    }

    public void previousStep() {
        if (currentStep > 1) currentStep--;
    }

    public String launch() {
        try {
            loading = true;
            String sessionId = sessionService.startSession(
                selectedType, position, difficulty, questionCount,
                authBean.getCurrentUser().getId()
            );
            return "session?faces-redirect=true&sessionId=" + sessionId;
        } catch (Exception e) {
            errorMessage = "Failed to start session: " + e.getMessage();
            loading = false;
            return null;
        }
    }

    public InterviewType getSelectedType()       { return selectedType; }
    public void setSelectedType(InterviewType t) { this.selectedType = t; }
    public String getPosition()                  { return position; }
    public void setPosition(String p)            { this.position = p; }
    public Difficulty getDifficulty()            { return difficulty; }
    public void setDifficulty(Difficulty d)      { this.difficulty = d; }
    public int getQuestionCount()                { return questionCount; }
    public void setQuestionCount(int q)          { this.questionCount = q; }
    public int getCurrentStep()                  { return currentStep; }
    public boolean isLoading()                   { return loading; }
    public String getErrorMessage()              { return errorMessage; }
}
