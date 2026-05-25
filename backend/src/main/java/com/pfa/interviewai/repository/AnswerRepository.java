package com.pfa.interviewai.repository;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.pfa.interviewai.config.FirebaseInitializer;
import com.pfa.interviewai.model.Answer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class AnswerRepository {

    @Inject
    private FirebaseInitializer firebase;

    private static final String COLLECTION = "answers";

    public Answer save(Answer answer) throws ExecutionException, InterruptedException {
        if (answer.getId() == null)
            answer.setId(UUID.randomUUID().toString());
        firebase.getFirestore()
                .collection(COLLECTION)
                .document(answer.getId())
                .set(toMap(answer))
                .get();
        return answer;
    }

    public List<Answer> findBySessionId(String sessionId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot query = firebase.getFirestore()
                .collection(COLLECTION)
                .whereEqualTo("sessionId", sessionId)
                .get().get();
        return query.getDocuments().stream()
                .map(this::fromDoc)
                .toList();
    }

    public void deleteBySessionId(String sessionId)
            throws ExecutionException, InterruptedException {
        QuerySnapshot docs = firebase.getFirestore()
                .collection(COLLECTION)
                .whereEqualTo("sessionId", sessionId)
                .get().get();
        if (!docs.isEmpty()) {
            WriteBatch batch = firebase.getFirestore().batch();
            docs.getDocuments().forEach(doc -> batch.delete(doc.getReference()));
            batch.commit().get();
        }
    }

    private Map<String, Object> toMap(Answer a) {
        Map<String, Object> map = new HashMap<>();
        map.put("sessionId", a.getSessionId());
        map.put("questionId", a.getQuestionId());
        map.put("text", a.getText());
        map.put("submittedAt", a.getSubmittedAt());
        return map;
    }

    private Answer fromDoc(DocumentSnapshot doc) {
        return Answer.builder()
                .id(doc.getId())
                .sessionId(doc.getString("sessionId"))
                .questionId(doc.getString("questionId"))
                .text(doc.getString("text"))
                .submittedAt(doc.getString("submittedAt"))
                .build();
    }
}
