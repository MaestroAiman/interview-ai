package com.pfa.interviewai.repository;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.pfa.interviewai.config.FirebaseInitializer;
import com.pfa.interviewai.model.Feedback;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class FeedbackRepository {

    @Inject
    private FirebaseInitializer firebase;

    private static final String COLLECTION = "feedback";

    public Feedback save(Feedback feedback) throws ExecutionException, InterruptedException {
        if (feedback.getId() == null)
            feedback.setId(UUID.randomUUID().toString());
        firebase.getFirestore()
                .collection(COLLECTION)
                .document(feedback.getId())
                .set(toMap(feedback))
                .get();
        return feedback;
    }

    public List<Feedback> findBySessionId(String sessionId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot query = firebase.getFirestore()
                .collection(COLLECTION)
                .whereEqualTo("sessionId", sessionId)
                .get().get();
        return query.getDocuments().stream()
                .map(this::fromDoc)
                .toList();
    }

    private Map<String, Object> toMap(Feedback f) {
        Map<String, Object> map = new HashMap<>();
        map.put("sessionId", f.getSessionId());
        map.put("questionId", f.getQuestionId());
        map.put("answerText", f.getAnswerText());
        map.put("relevanceScore", f.getRelevanceScore());
        map.put("clarityScore", f.getClarityScore());
        map.put("sentimentScore", f.getSentimentScore());
        map.put("overallScore", f.getOverallScore());
        map.put("strengths", f.getStrengths());
        map.put("improvements", f.getImprovements());
        map.put("suggestedAnswer", f.getSuggestedAnswer());
        map.put("shortComment", f.getShortComment());
        map.put("depthScore", f.getDepthScore());
        map.put("vocabularyScore", f.getVocabularyScore());
        map.put("examplesScore", f.getExamplesScore());
        map.put("globalScore", f.getGlobalScore());
        map.put("levelAssessment", f.getLevelAssessment());
        map.put("keyStrengths", f.getKeyStrengths());
        map.put("criticalGaps", f.getCriticalGaps());
        map.put("positivePoints", f.getPositivePoints());
        map.put("improvementPoints", f.getImprovementPoints());
        map.put("concreteAdvice", f.getConcreteAdvice());
        map.put("exampleAnswer", f.getExampleAnswer());
        map.put("nextDifficulty", f.getNextDifficulty());
        return map;
    }

    @SuppressWarnings("unchecked")
    private Feedback fromDoc(DocumentSnapshot doc) {
        return Feedback.builder()
                .id(doc.getId())
                .sessionId(doc.getString("sessionId"))
                .questionId(doc.getString("questionId"))
                .answerText(doc.getString("answerText"))
                .relevanceScore(doc.getDouble("relevanceScore") != null
                    ? doc.getDouble("relevanceScore").floatValue() : 0f)
                .clarityScore(doc.getDouble("clarityScore") != null
                    ? doc.getDouble("clarityScore").floatValue() : 0f)
                .sentimentScore(doc.getDouble("sentimentScore") != null
                    ? doc.getDouble("sentimentScore").floatValue() : 0f)
                .overallScore(doc.getDouble("overallScore") != null
                    ? doc.getDouble("overallScore").floatValue() : 0f)
                .strengths(doc.getString("strengths") != null ? doc.getString("strengths") : "")
                .improvements(doc.getString("improvements") != null ? doc.getString("improvements") : "")
                .suggestedAnswer(doc.getString("suggestedAnswer") != null ? doc.getString("suggestedAnswer") : "")
                .shortComment(doc.getString("shortComment") != null ? doc.getString("shortComment") : "")
                .depthScore(doc.getDouble("depthScore") != null
                    ? doc.getDouble("depthScore").floatValue() : 0f)
                .vocabularyScore(doc.getDouble("vocabularyScore") != null
                    ? doc.getDouble("vocabularyScore").floatValue() : 0f)
                .examplesScore(doc.getDouble("examplesScore") != null
                    ? doc.getDouble("examplesScore").floatValue() : 0f)
                .globalScore(doc.getDouble("globalScore") != null
                    ? doc.getDouble("globalScore").floatValue() : 0f)
                .levelAssessment(doc.getString("levelAssessment") != null ? doc.getString("levelAssessment") : "")
                .keyStrengths(doc.get("keyStrengths") instanceof List
                    ? (List<String>) doc.get("keyStrengths") : new java.util.ArrayList<>())
                .criticalGaps(doc.get("criticalGaps") instanceof List
                    ? (List<String>) doc.get("criticalGaps") : new java.util.ArrayList<>())
                .positivePoints(doc.getString("positivePoints") != null ? doc.getString("positivePoints") : "")
                .improvementPoints(doc.getString("improvementPoints") != null ? doc.getString("improvementPoints") : "")
                .concreteAdvice(doc.getString("concreteAdvice") != null ? doc.getString("concreteAdvice") : "")
                .exampleAnswer(doc.getString("exampleAnswer") != null ? doc.getString("exampleAnswer") : "")
                .nextDifficulty(doc.getString("nextDifficulty") != null ? doc.getString("nextDifficulty") : "same")
                .build();
    }
}
