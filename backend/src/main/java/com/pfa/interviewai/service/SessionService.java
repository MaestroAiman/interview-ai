package com.pfa.interviewai.service;

import com.pfa.interviewai.model.Answer;
import com.pfa.interviewai.model.Feedback;
import com.pfa.interviewai.model.InterviewSession;
import com.pfa.interviewai.model.Question;
import com.pfa.interviewai.model.enums.Difficulty;
import com.pfa.interviewai.model.enums.InterviewType;
import com.pfa.interviewai.model.enums.SessionStatus;
import com.pfa.interviewai.repository.AnswerRepository;
import com.pfa.interviewai.repository.FeedbackRepository;
import com.pfa.interviewai.repository.SessionRepository;
import com.pfa.interviewai.rest.dto.AnswerFeedbackResponse;
import com.pfa.interviewai.rest.dto.AnswerResponse;
import com.pfa.interviewai.rest.dto.HistoryDetailsDto;
import com.pfa.interviewai.rest.dto.HistoryPageDto;
import com.pfa.interviewai.rest.dto.SessionStateDto;
import com.pfa.interviewai.rest.dto.SessionSummaryDto;
import com.pfa.interviewai.rest.dto.StartSessionResponse;
import com.pfa.interviewai.rest.dto.SubmitAnswerRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class SessionService {

    @Inject private SessionRepository sessionRepository;
    @Inject private FeedbackRepository feedbackRepository;
    @Inject private AnswerRepository answerRepository;
    @Inject private ClaudeAIService claudeAIService;

    public String startSession(InterviewType type, String position,
                                Difficulty difficulty, int questionCount,
                                String userId)
            throws ExecutionException, InterruptedException {

        InterviewSession session = InterviewSession.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .type(type)
                .position(position)
                .difficulty(difficulty)
                .status(SessionStatus.IN_PROGRESS)
                .questionCount(questionCount)
                .startedAt(Instant.now().toString())
                .build();

        sessionRepository.save(session);

        List<String> questions = claudeAIService.generateQuestions(
            type.name(), position, difficulty.name(), questionCount);

        for (int i = 0; i < questions.size(); i++) {
            Question q = Question.builder()
                    .id(UUID.randomUUID().toString())
                    .sessionId(session.getId())
                    .content(questions.get(i))
                    .order(i + 1)
                    .build();
            sessionRepository.saveQuestion(session.getId(), q);
        }

        return session.getId();
    }

    public InterviewSession findById(String sessionId)
            throws ExecutionException, InterruptedException {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
    }

    public Question getFirstQuestion(String sessionId)
            throws ExecutionException, InterruptedException {
        List<Question> questions = sessionRepository.findAllQuestions(sessionId);
        return questions.isEmpty() ? null : questions.get(0);
    }

    public AnswerResponse submitAnswer(SubmitAnswerRequest req)
            throws ExecutionException, InterruptedException {

        InterviewSession session = findById(req.getSessionId());
        List<Question> questions = sessionRepository.findAllQuestions(req.getSessionId());

        Question currentQ = questions.stream()
                .filter(q -> q.getId().equals(req.getQuestionId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));

        Feedback feedback = claudeAIService.analyzeAnswer(
            currentQ.getContent(),
            req.getAnswerText(),
            session.getPosition(),
            session.getType().name(),
            req.getSessionId(),
            req.getQuestionId()
        );
        feedbackRepository.save(feedback);

        Question nextQuestion = sessionRepository.findNextQuestion(
            req.getSessionId(), currentQ.getOrder());

        if (nextQuestion == null) {
            completeSession(req.getSessionId());
        }

        return new AnswerResponse(feedback, nextQuestion);
    }

    public InterviewSession completeSession(String sessionId)
            throws ExecutionException, InterruptedException {

        InterviewSession session = findById(sessionId);
        List<Feedback> feedbacks = feedbackRepository.findBySessionId(sessionId);

        if (!feedbacks.isEmpty()) {
            float relevanceAvg  = avg(feedbacks.stream().mapToDouble(f -> f.getRelevanceScore()).toArray());
            float clarityAvg    = avg(feedbacks.stream().mapToDouble(f -> f.getClarityScore()).toArray());
            float sentimentAvg  = avg(feedbacks.stream().mapToDouble(f -> f.getSentimentScore()).toArray());
            float overallScore  = avg(feedbacks.stream().mapToDouble(f -> f.getOverallScore()).toArray());

            session.setRelevanceAvg(relevanceAvg);
            session.setClarityAvg(clarityAvg);
            session.setSentimentAvg(sentimentAvg);
            session.setOverallScore(overallScore);
        }

        session.setStatus(SessionStatus.COMPLETED);
        session.setEndedAt(Instant.now().toString());
        sessionRepository.update(session);
        return session;
    }

    public List<InterviewSession> findByUserId(String userId)
            throws ExecutionException, InterruptedException {
        return sessionRepository.findByUserId(userId);
    }

    public List<InterviewSession> findAll() throws ExecutionException, InterruptedException {
        return sessionRepository.findAll();
    }

    private float avg(double[] values) {
        if (values.length == 0) return 0f;
        double sum = 0;
        for (double v : values) sum += v;
        return (float) (sum / values.length);
    }

    // =========================================================================
    // New adaptive interview methods
    // =========================================================================

    public StartSessionResponse startAdaptiveSession(InterviewType type, String position,
                                                      Difficulty difficulty, int questionCount,
                                                      String userId)
            throws ExecutionException, InterruptedException {

        InterviewSession session = InterviewSession.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .type(type)
                .position(position)
                .difficulty(difficulty)
                .status(SessionStatus.IN_PROGRESS)
                .questionCount(questionCount)
                .startedAt(Instant.now().toString())
                .build();
        sessionRepository.save(session);

        Question firstQuestion = claudeAIService.generateAdaptiveQuestion(
            type.name(), position, "intermediate", List.of());
        firstQuestion.setSessionId(session.getId());
        firstQuestion.setOrder(1);
        sessionRepository.saveQuestion(session.getId(), firstQuestion);

        return new StartSessionResponse(session.getId(), firstQuestion);
    }

    public AnswerFeedbackResponse submitAdaptiveAnswer(String sessionId, String questionId,
                                                        String answerText, String userId)
            throws ExecutionException, InterruptedException {

        InterviewSession session = findById(sessionId);
        if (!session.getUserId().equals(userId))
            throw new IllegalArgumentException("Access denied to session: " + sessionId);
        if (session.getStatus() != SessionStatus.IN_PROGRESS)
            throw new IllegalArgumentException("Session is not in progress: " + sessionId);

        List<Question> questions = sessionRepository.findAllQuestions(sessionId);
        Question currentQ = questions.stream()
                .filter(q -> q.getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));

        answerRepository.save(Answer.builder()
                .id(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .questionId(questionId)
                .text(answerText)
                .submittedAt(Instant.now().toString())
                .build());

        Feedback analysis = claudeAIService.analyzeAnswerDetailed(
            currentQ.getContent(), currentQ.getCategory(), answerText, sessionId, questionId);

        Feedback feedback = claudeAIService.generateDetailedFeedback(
            currentQ.getContent(), answerText, currentQ.getCategory(), analysis, sessionId, questionId);

        feedbackRepository.save(feedback);

        List<Feedback> allFeedbacks = feedbackRepository.findBySessionId(sessionId);
        double avgGlobal = allFeedbacks.stream()
                .mapToDouble(Feedback::getGlobalScore)
                .average()
                .orElse(50.0);
        String adaptiveDifficulty = avgGlobal >= 75 ? "advanced" : avgGlobal >= 45 ? "intermediate" : "beginner";

        boolean isLastQuestion = currentQ.getOrder() >= session.getQuestionCount();
        if (isLastQuestion) {
            SessionSummaryDto summary = endSessionWithSummary(sessionId, allFeedbacks, questions);
            return new AnswerFeedbackResponse(feedback, null, summary);
        }

        List<String> askedContents = questions.stream().map(Question::getContent).toList();
        Question nextQuestion = claudeAIService.generateAdaptiveQuestion(
            session.getType().name(), session.getPosition(), adaptiveDifficulty, askedContents);
        nextQuestion.setSessionId(sessionId);
        nextQuestion.setOrder(currentQ.getOrder() + 1);
        sessionRepository.saveQuestion(sessionId, nextQuestion);

        return new AnswerFeedbackResponse(feedback, nextQuestion, null);
    }

    public SessionStateDto getSessionState(String sessionId, String userId)
            throws ExecutionException, InterruptedException {
        InterviewSession session = findById(sessionId);
        if (!session.getUserId().equals(userId))
            throw new IllegalArgumentException("Access denied to session: " + sessionId);
        List<Question> questions = sessionRepository.findAllQuestions(sessionId);
        List<Answer> answers = answerRepository.findBySessionId(sessionId);
        List<Feedback> feedbacks = feedbackRepository.findBySessionId(sessionId);
        return new SessionStateDto(session, questions, answers, feedbacks);
    }

    public SessionSummaryDto forceEndSession(String sessionId, String userId)
            throws ExecutionException, InterruptedException {
        InterviewSession session = findById(sessionId);
        if (!session.getUserId().equals(userId))
            throw new IllegalArgumentException("Access denied to session: " + sessionId);
        if (session.getStatus() != SessionStatus.IN_PROGRESS)
            throw new IllegalArgumentException("Session is not in progress: " + sessionId);
        List<Feedback> feedbacks = feedbackRepository.findBySessionId(sessionId);
        List<Question> questions = sessionRepository.findAllQuestions(sessionId);
        return endSessionWithSummary(sessionId, feedbacks, questions);
    }

    public HistoryPageDto getSessionHistory(String userId, int page, int size)
            throws ExecutionException, InterruptedException {
        List<InterviewSession> all = sessionRepository.findByUserId(userId);
        long total = all.size();
        int fromIndex = Math.min(page * size, all.size());
        int toIndex = Math.min(fromIndex + size, all.size());
        List<InterviewSession> pageContent = all.subList(fromIndex, toIndex);
        return new HistoryPageDto(page, size, total, pageContent);
    }

    public HistoryDetailsDto getHistoryDetails(String sessionId, String userId)
            throws ExecutionException, InterruptedException {
        InterviewSession session = findById(sessionId);
        if (!session.getUserId().equals(userId))
            throw new IllegalArgumentException("Access denied to session: " + sessionId);
        List<Question> questions = sessionRepository.findAllQuestions(sessionId);
        List<Answer> answers = answerRepository.findBySessionId(sessionId);
        List<Feedback> feedbacks = feedbackRepository.findBySessionId(sessionId);
        return new HistoryDetailsDto(session, questions, answers, feedbacks);
    }

    private SessionSummaryDto endSessionWithSummary(String sessionId,
                                                     List<Feedback> feedbacks,
                                                     List<Question> questions)
            throws ExecutionException, InterruptedException {

        InterviewSession session = findById(sessionId);
        List<Answer> answers = answerRepository.findBySessionId(sessionId);

        List<Map<String, Object>> qaPairs = new ArrayList<>();
        for (Question q : questions) {
            Answer answer = answers.stream()
                    .filter(a -> a.getQuestionId().equals(q.getId()))
                    .findFirst().orElse(null);
            Feedback fb = feedbacks.stream()
                    .filter(f -> f.getQuestionId().equals(q.getId()))
                    .findFirst().orElse(null);
            Map<String, Object> pair = new HashMap<>();
            pair.put("question", q.getContent());
            pair.put("answer", answer != null ? answer.getText() : "(no answer)");
            pair.put("globalScore", fb != null ? fb.getGlobalScore() : 0f);
            qaPairs.add(pair);
        }

        SessionSummaryDto summary = claudeAIService.generateSessionSummary(qaPairs);

        if (!feedbacks.isEmpty()) {
            float overallScore = avg(feedbacks.stream().mapToDouble(f -> f.getOverallScore()).toArray());
            session.setOverallScore(overallScore);
            session.setRelevanceAvg(avg(feedbacks.stream().mapToDouble(f -> f.getRelevanceScore()).toArray()));
            session.setClarityAvg(avg(feedbacks.stream().mapToDouble(f -> f.getClarityScore()).toArray()));
        }

        session.setStatus(SessionStatus.COMPLETED);
        session.setEndedAt(Instant.now().toString());
        session.setGlobalAssessment(summary.getGlobalAssessment());
        session.setTopStrengths(summary.getTopStrengths());
        session.setPriorityImprovements(summary.getPriorityImprovements());
        session.setRecommendedResources(summary.getRecommendedResources());
        session.setReadinessLevel(summary.getReadinessLevel());
        sessionRepository.update(session);

        summary.setOverallScore(session.getOverallScore() * 10);
        return summary;
    }
}
