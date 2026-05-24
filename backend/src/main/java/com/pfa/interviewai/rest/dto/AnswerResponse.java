package com.pfa.interviewai.rest.dto;

import com.pfa.interviewai.model.Feedback;
import com.pfa.interviewai.model.Question;

public class AnswerResponse {
    private Feedback feedback;
    private Question nextQuestion;

    public AnswerResponse() {}

    public AnswerResponse(Feedback feedback, Question nextQuestion) {
        this.feedback = feedback;
        this.nextQuestion = nextQuestion;
    }

    public Feedback getFeedback()   { return feedback; }
    public void setFeedback(Feedback f) { this.feedback = f; }
    public Question getNextQuestion() { return nextQuestion; }
    public void setNextQuestion(Question q) { this.nextQuestion = q; }
}
