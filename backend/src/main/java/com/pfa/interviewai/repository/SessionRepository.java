package com.pfa.interviewai.repository;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.pfa.interviewai.config.FirebaseInitializer;
import com.pfa.interviewai.model.InterviewSession;
import com.pfa.interviewai.model.Question;
import com.pfa.interviewai.model.enums.Difficulty;
import com.pfa.interviewai.model.enums.InterviewType;
import com.pfa.interviewai.model.enums.SessionStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class SessionRepository {

    @Inject
    private FirebaseInitializer firebase;

    private static final String COLLECTION = "sessions";

    public InterviewSession save(InterviewSession session)
            throws ExecutionException, InterruptedException {
        if (session.getId() == null)
            session.setId(UUID.randomUUID().toString());
        firebase.getFirestore()
                .collection(COLLECTION)
                .document(session.getId())
                .set(toMap(session))
                .get();
        return session;
    }

    public void saveQuestion(String sessionId, Question question)
            throws ExecutionException, InterruptedException {
        Map<String, Object> qMap = new HashMap<>();
        qMap.put("content", question.getContent());
        qMap.put("order", question.getOrder());
        qMap.put("sessionId", sessionId);
        qMap.put("category", question.getCategory());
        qMap.put("aiDifficulty", question.getAiDifficulty());
        qMap.put("estimatedDurationSeconds", question.getEstimatedDurationSeconds());
        qMap.put("expectedKeywords", question.getExpectedKeywords());
        firebase.getFirestore()
                .collection(COLLECTION)
                .document(sessionId)
                .collection("questions")
                .document(question.getId())
                .set(qMap).get();
    }

    public Optional<InterviewSession> findById(String id)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = firebase.getFirestore()
                .collection(COLLECTION).document(id).get().get();
        return doc.exists() ? Optional.of(fromDoc(doc)) : Optional.empty();
    }

    public List<InterviewSession> findByUserId(String userId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot query = firebase.getFirestore()
                .collection(COLLECTION)
                .whereEqualTo("userId", userId)
                .get().get();
        return query.getDocuments().stream()
                .map(this::fromDoc)
                .sorted(java.util.Comparator.comparing(
                    s -> s.getStartedAt() != null ? s.getStartedAt() : "",
                    java.util.Comparator.reverseOrder()))
                .toList();
    }

    public List<Question> findAllQuestions(String sessionId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot query = firebase.getFirestore()
                .collection(COLLECTION)
                .document(sessionId)
                .collection("questions")
                .orderBy("order")
                .get().get();
        return query.getDocuments().stream().map(doc ->
            Question.builder()
                .id(doc.getId())
                .sessionId(sessionId)
                .content(doc.getString("content"))
                .order(doc.getLong("order") != null ? doc.getLong("order").intValue() : 0)
                .category(doc.getString("category"))
                .aiDifficulty(doc.getString("aiDifficulty"))
                .estimatedDurationSeconds(doc.getLong("estimatedDurationSeconds") != null
                    ? doc.getLong("estimatedDurationSeconds").intValue() : 120)
                .expectedKeywords(doc.get("expectedKeywords") instanceof List
                    ? (List<String>) doc.get("expectedKeywords") : new java.util.ArrayList<>())
                .build()
        ).toList();
    }

    public Question findNextQuestion(String sessionId, int currentOrder)
            throws ExecutionException, InterruptedException {
        QuerySnapshot query = firebase.getFirestore()
                .collection(COLLECTION)
                .document(sessionId)
                .collection("questions")
                .whereGreaterThan("order", currentOrder)
                .orderBy("order")
                .limit(1)
                .get().get();
        if (query.isEmpty()) return null;
        DocumentSnapshot doc = query.getDocuments().get(0);
        return Question.builder()
                .id(doc.getId())
                .sessionId(sessionId)
                .content(doc.getString("content"))
                .order(doc.getLong("order") != null ? doc.getLong("order").intValue() : 0)
                .category(doc.getString("category"))
                .aiDifficulty(doc.getString("aiDifficulty"))
                .estimatedDurationSeconds(doc.getLong("estimatedDurationSeconds") != null
                    ? doc.getLong("estimatedDurationSeconds").intValue() : 120)
                .expectedKeywords(doc.get("expectedKeywords") instanceof List
                    ? (List<String>) doc.get("expectedKeywords") : new java.util.ArrayList<>())
                .build();
    }

    public List<InterviewSession> findAll() throws ExecutionException, InterruptedException {
        QuerySnapshot query = firebase.getFirestore()
                .collection(COLLECTION)
                .orderBy("startedAt", Query.Direction.DESCENDING)
                .get().get();
        return query.getDocuments().stream().map(this::fromDoc).toList();
    }

    public void update(InterviewSession session)
            throws ExecutionException, InterruptedException {
        firebase.getFirestore()
                .collection(COLLECTION)
                .document(session.getId())
                .set(toMap(session))
                .get();
    }

    private Map<String, Object> toMap(InterviewSession s) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", s.getUserId());
        map.put("type", s.getType() != null ? s.getType().name() : null);
        map.put("position", s.getPosition());
        map.put("difficulty", s.getDifficulty() != null ? s.getDifficulty().name() : null);
        map.put("status", s.getStatus() != null ? s.getStatus().name() : null);
        map.put("questionCount", s.getQuestionCount());
        map.put("overallScore", s.getOverallScore());
        map.put("relevanceAvg", s.getRelevanceAvg());
        map.put("clarityAvg", s.getClarityAvg());
        map.put("sentimentAvg", s.getSentimentAvg());
        map.put("startedAt", s.getStartedAt());
        map.put("endedAt", s.getEndedAt());
        map.put("globalAssessment", s.getGlobalAssessment());
        map.put("topStrengths", s.getTopStrengths());
        map.put("priorityImprovements", s.getPriorityImprovements());
        map.put("recommendedResources", s.getRecommendedResources());
        map.put("readinessLevel", s.getReadinessLevel());
        return map;
    }

    @SuppressWarnings("unchecked")
    private InterviewSession fromDoc(DocumentSnapshot doc) {
        String typeStr   = doc.getString("type");
        String diffStr   = doc.getString("difficulty");
        String statusStr = doc.getString("status");
        return InterviewSession.builder()
                .id(doc.getId())
                .userId(doc.getString("userId"))
                .type(typeStr != null ? InterviewType.valueOf(typeStr) : null)
                .position(doc.getString("position"))
                .difficulty(diffStr != null ? Difficulty.valueOf(diffStr) : null)
                .status(statusStr != null ? SessionStatus.valueOf(statusStr) : null)
                .questionCount(doc.getLong("questionCount") != null
                    ? doc.getLong("questionCount").intValue() : 0)
                .overallScore(doc.getDouble("overallScore") != null
                    ? doc.getDouble("overallScore").floatValue() : 0f)
                .relevanceAvg(doc.getDouble("relevanceAvg") != null
                    ? doc.getDouble("relevanceAvg").floatValue() : 0f)
                .clarityAvg(doc.getDouble("clarityAvg") != null
                    ? doc.getDouble("clarityAvg").floatValue() : 0f)
                .sentimentAvg(doc.getDouble("sentimentAvg") != null
                    ? doc.getDouble("sentimentAvg").floatValue() : 0f)
                .startedAt(doc.getString("startedAt"))
                .endedAt(doc.getString("endedAt"))
                .globalAssessment(doc.getString("globalAssessment"))
                .topStrengths(doc.get("topStrengths") instanceof List
                    ? (List<String>) doc.get("topStrengths") : new java.util.ArrayList<>())
                .priorityImprovements(doc.get("priorityImprovements") instanceof List
                    ? (List<String>) doc.get("priorityImprovements") : new java.util.ArrayList<>())
                .recommendedResources(doc.get("recommendedResources") instanceof List
                    ? (List<String>) doc.get("recommendedResources") : new java.util.ArrayList<>())
                .readinessLevel(doc.getString("readinessLevel"))
                .build();
    }
}
