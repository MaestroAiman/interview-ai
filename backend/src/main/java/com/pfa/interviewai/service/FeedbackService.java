package com.pfa.interviewai.service;

import com.pfa.interviewai.model.Feedback;
import com.pfa.interviewai.repository.FeedbackRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class FeedbackService {

    @Inject
    private FeedbackRepository feedbackRepository;

    public List<Feedback> getFeedbackForSession(String sessionId)
            throws ExecutionException, InterruptedException {
        return feedbackRepository.findBySessionId(sessionId);
    }
}
