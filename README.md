# InterviewAI

Simulateur d'entretiens techniques assisté par IA : une application web Jakarta EE et son client mobile Android.

| Dossier | Contenu | Stack |
|---|---|---|
| [`backend/`](backend/) | API REST + interface web JSF, génération de questions et feedback par IA, monitoring | Jakarta EE 10, Tomcat 10, Firestore, Claude / Ollama, Prometheus + Grafana, Docker |
| [`android/`](android/) | Application mobile (sessions d'entretien, enregistrement vocal, historique, progression, admin) | Kotlin, Jetpack Compose, MVVM |

## Lancer le projet

```bash
cd backend
cp .env.example .env        # renseigner ANTHROPIC_API_KEY et JWT_SECRET
# placer firebase-service-account.json dans backend/src/main/resources/
docker compose up --build
```

Application Android : ouvrir `android/` dans Android Studio, ajouter votre `app/google-services.json` (Firebase), puis lancer sur un émulateur (l'API est attendue sur `http://10.0.2.2:8080/interview-ai/api/`).
