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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class SessionService {

    @Inject private SessionRepository sessionRepository;
    @Inject private FeedbackRepository feedbackRepository;
    @Inject private AnswerRepository answerRepository;
    @Inject private AIProviderFactory aiProviderFactory;

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

        List<String> questions = aiProviderFactory.getProvider().generateQuestions(
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

        Feedback feedback = aiProviderFactory.getProvider().analyzeAnswer(
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
            double relevanceSum = 0, claritySum = 0, sentimentSum = 0, overallSum = 0;
            for (Feedback f : feedbacks) {
                relevanceSum  += f.getRelevanceScore();
                claritySum    += f.getClarityScore();
                sentimentSum  += f.getSentimentScore();
                overallSum    += f.getOverallScore();
            }
            int n = feedbacks.size();
            session.setRelevanceAvg((float) (relevanceSum / n));
            session.setClarityAvg((float) (claritySum / n));
            session.setSentimentAvg((float) (sentimentSum / n));
            session.setOverallScore((float) (overallSum / n));
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

    public Question getFirstUnansweredQuestion(String sessionId)
            throws ExecutionException, InterruptedException {
        List<Question> questions = sessionRepository.findAllQuestions(sessionId);
        List<Feedback> feedbacks = feedbackRepository.findBySessionId(sessionId);
        java.util.Set<String> answeredIds = feedbacks.stream()
                .map(Feedback::getQuestionId)
                .collect(java.util.stream.Collectors.toSet());
        return questions.stream()
                .filter(q -> !answeredIds.contains(q.getId()))
                .findFirst()
                .orElse(null);
    }

    public int getAnsweredCount(String sessionId)
            throws ExecutionException, InterruptedException {
        return feedbackRepository.findBySessionId(sessionId).size();
    }

    public List<InterviewSession> findAll() throws ExecutionException, InterruptedException {
        return sessionRepository.findAll();
    }

    public void deleteSessionById(String sessionId)
            throws ExecutionException, InterruptedException {
        feedbackRepository.deleteBySessionId(sessionId);
        answerRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteSession(sessionId);  // also removes questions subcollection
    }

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

        Question firstQuestion = aiProviderFactory.getProvider().generateAdaptiveQuestion(
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

        // Idempotency check: if this question was already answered, return existing result
        List<Question> questions = sessionRepository.findAllQuestions(sessionId);
        List<Feedback> existingFeedbacks = feedbackRepository.findBySessionId(sessionId);
        Feedback existingFeedback = existingFeedbacks.stream()
                .filter(f -> questionId.equals(f.getQuestionId()))
                .findFirst().orElse(null);

        if (existingFeedback != null) {
            Question currentQExisting = questions.stream()
                    .filter(q -> q.getId().equals(questionId)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));
            boolean isLastExisting = currentQExisting.getOrder() >= session.getQuestionCount();
            if (isLastExisting || session.getStatus() == SessionStatus.COMPLETED) {
                return new AnswerFeedbackResponse(existingFeedback, null,
                        buildExistingSummary(session, existingFeedbacks));
            }
            Question nextQExisting = questions.stream()
                    .filter(q -> q.getOrder() == currentQExisting.getOrder() + 1)
                    .findFirst().orElse(null);
            return new AnswerFeedbackResponse(existingFeedback, nextQExisting, null);
        }

        if (session.getStatus() != SessionStatus.IN_PROGRESS)
            throw new IllegalArgumentException("Session is not in progress: " + sessionId);
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

        // Step 1: Analyze answer (sequential — needed before generating feedback)
        Feedback analysis = aiProviderFactory.getProvider().analyzeAnswerDetailed(
            currentQ.getContent(), currentQ.getCategory(), answerText, sessionId, questionId);

        // Step 2: Compute adaptive difficulty from previous feedbacks (fast DB call)
        List<Feedback> previousFeedbacks = feedbackRepository.findBySessionId(sessionId);
        double avgGlobal = previousFeedbacks.stream()
                .mapToDouble(Feedback::getGlobalScore)
                .average()
                .orElse(50.0);
        String adaptiveDifficulty = avgGlobal >= 75 ? "advanced" : avgGlobal >= 45 ? "intermediate" : "beginner";

        boolean isLastQuestion = currentQ.getOrder() >= session.getQuestionCount();
        List<String> askedContents = questions.stream().map(Question::getContent).toList();

        // Step 3: Run feedback generation and next-question generation in parallel
        CompletableFuture<Feedback> feedbackFuture = CompletableFuture.supplyAsync(() ->
            aiProviderFactory.getProvider().generateDetailedFeedback(
                currentQ.getContent(), answerText, currentQ.getCategory(), analysis, sessionId, questionId));

        CompletableFuture<Question> nextQuestionFuture = isLastQuestion
            ? CompletableFuture.completedFuture(null)
            : CompletableFuture.supplyAsync(() ->
                aiProviderFactory.getProvider().generateAdaptiveQuestion(
                    session.getType().name(), session.getPosition(), adaptiveDifficulty, askedContents));

        Feedback feedback = feedbackFuture.get();
        feedbackRepository.save(feedback);

        if (isLastQuestion) {
            List<Feedback> allFeedbacks = feedbackRepository.findBySessionId(sessionId);
            SessionSummaryDto summary = endSessionWithSummary(sessionId, allFeedbacks, questions);
            return new AnswerFeedbackResponse(feedback, null, summary);
        }

        Question nextQuestion = nextQuestionFuture.get();
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

    private SessionSummaryDto buildExistingSummary(InterviewSession session, List<Feedback> feedbacks) {
        SessionSummaryDto summary = new SessionSummaryDto();
        summary.setOverallScore(session.getOverallScore() * 10);
        summary.setGlobalAssessment(session.getGlobalAssessment());
        summary.setTopStrengths(session.getTopStrengths());
        summary.setPriorityImprovements(session.getPriorityImprovements());
        summary.setRecommendedResources(session.getRecommendedResources());
        summary.setReadinessLevel(session.getReadinessLevel());
        return summary;
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

        SessionSummaryDto summary = aiProviderFactory.getProvider().generateSessionSummary(qaPairs);

        if (!feedbacks.isEmpty()) {
            double overallSum = 0, relevanceSum = 0, claritySum = 0;
            for (Feedback f : feedbacks) {
                overallSum   += f.getOverallScore();
                relevanceSum += f.getRelevanceScore();
                claritySum   += f.getClarityScore();
            }
            int n = feedbacks.size();
            session.setOverallScore((float) (overallSum / n));
            session.setRelevanceAvg((float) (relevanceSum / n));
            session.setClarityAvg((float) (claritySum / n));
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
