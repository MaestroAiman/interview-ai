package com.pfa.interviewai.rest.dto;

import com.pfa.interviewai.model.Answer;
import com.pfa.interviewai.model.Feedback;
import com.pfa.interviewai.model.InterviewSession;
import com.pfa.interviewai.model.Question;

import java.util.List;

public class HistoryDetailsDto {
    private InterviewSession session;
    private List<Question> questions;
    private List<Answer> answers;
    private List<Feedback> feedbacks;

    public HistoryDetailsDto() {}

    public HistoryDetailsDto(InterviewSession session, List<Question> questions,
                              List<Answer> answers, List<Feedback> feedbacks) {
        this.session = session;
        this.questions = questions;
        this.answers = answers;
        this.feedbacks = feedbacks;
    }

    public InterviewSession getSession() { return session; }
    public void setSession(InterviewSession session) { this.session = session; }

    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }

    public List<Answer> getAnswers() { return answers; }
    public void setAnswers(List<Answer> answers) { this.answers = answers; }

    public List<Feedback> getFeedbacks() { return feedbacks; }
    public void setFeedbacks(List<Feedback> feedbacks) { this.feedbacks = feedbacks; }
}
