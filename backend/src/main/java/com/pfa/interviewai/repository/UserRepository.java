package com.pfa.interviewai.repository;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.pfa.interviewai.config.FirebaseInitializer;
import com.pfa.interviewai.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class UserRepository {

    @Inject
    private FirebaseInitializer firebase;

    private static final String COLLECTION = "users";

    public User save(User user) throws ExecutionException, InterruptedException {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID().toString());
        }
        firebase.getFirestore()
                .collection(COLLECTION)
                .document(user.getId())
                .set(toMap(user))
                .get();
        return user;
    }

    public Optional<User> findById(String id)
            throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = firebase.getFirestore()
                .collection(COLLECTION)
                .document(id)
                .get().get();
        return doc.exists() ? Optional.of(fromDoc(doc)) : Optional.empty();
    }

    public Optional<User> findByEmail(String email)
            throws ExecutionException, InterruptedException {
        QuerySnapshot query = firebase.getFirestore()
                .collection(COLLECTION)
                .whereEqualTo("email", email)
                .get().get();
        if (query.isEmpty()) return Optional.empty();
        return Optional.of(fromDoc(query.getDocuments().get(0)));
    }

    public boolean existsByEmail(String email)
            throws ExecutionException, InterruptedException {
        return findByEmail(email).isPresent();
    }

    public List<User> findAll() throws ExecutionException, InterruptedException {
        QuerySnapshot query = firebase.getFirestore().collection(COLLECTION).get().get();
        return query.getDocuments().stream().map(this::fromDoc).toList();
    }

    public void update(User user) throws ExecutionException, InterruptedException {
        firebase.getFirestore()
                .collection(COLLECTION)
                .document(user.getId())
                .set(toMap(user))
                .get();
    }

    private Map<String, Object> toMap(User u) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", u.getName());
        map.put("email", u.getEmail());
        map.put("passwordHash", u.getPasswordHash());
        map.put("role", u.getRole() != null ? u.getRole() : "USER");
        map.put("targetPosition", u.getTargetPosition());
        map.put("createdAt", u.getCreatedAt());
        return map;
    }

    private User fromDoc(DocumentSnapshot doc) {
        return User.builder()
                .id(doc.getId())
                .name(doc.getString("name"))
                .email(doc.getString("email"))
                .passwordHash(doc.getString("passwordHash"))
                .role(normalizeRole(doc.getString("role")))
                .targetPosition(doc.getString("targetPosition"))
                .createdAt(doc.getString("createdAt"))
                .build();
    }

    private String normalizeRole(String raw) {
        if (raw == null || raw.isBlank()) return "USER";
        return raw.trim().toUpperCase();
    }
}
