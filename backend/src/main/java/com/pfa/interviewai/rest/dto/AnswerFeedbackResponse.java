package com.pfa.interviewai.rest.dto;

import com.pfa.interviewai.model.Feedback;
import com.pfa.interviewai.model.Question;

public class AnswerFeedbackResponse {
    private Feedback feedback;
    private Question nextQuestion;
    private SessionSummaryDto sessionSummary;

    public AnswerFeedbackResponse() {}

    public AnswerFeedbackResponse(Feedback feedback, Question nextQuestion, SessionSummaryDto sessionSummary) {
        this.feedback = feedback;
        this.nextQuestion = nextQuestion;
        this.sessionSummary = sessionSummary;
    }

    public Feedback getFeedback() { return feedback; }
    public void setFeedback(Feedback feedback) { this.feedback = feedback; }

    public Question getNextQuestion() { return nextQuestion; }
    public void setNextQuestion(Question nextQuestion) { this.nextQuestion = nextQuestion; }

    public SessionSummaryDto getSessionSummary() { return sessionSummary; }
    public void setSessionSummary(SessionSummaryDto sessionSummary) { this.sessionSummary = sessionSummary; }
}
