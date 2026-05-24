package com.pfa.interviewai.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.io.InputStream;

@ApplicationScoped
public class FirebaseInitializer {

    @Inject
    private AppConfig appConfig;

    private Firestore firestore;

    @PostConstruct
    public void init() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = getClass()
                    .getClassLoader()
                    .getResourceAsStream("firebase-service-account.json");

                if (serviceAccount == null) {
                    throw new RuntimeException(
                        "firebase-service-account.json not found in resources");
                }

                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(appConfig.getFirebaseProjectId())
                    .build();

                FirebaseApp.initializeApp(options);
            }
            this.firestore = FirestoreClient.getFirestore();

        } catch (IOException e) {
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }

    public Firestore getFirestore() {
        return firestore;
    }
}
