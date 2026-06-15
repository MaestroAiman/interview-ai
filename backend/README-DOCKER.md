# Interview-AI — Guide de lancement Docker

Cette archive contient une application **Jakarta EE 10** (JSF + JAX-RS + CDI) déployée sur **Tomcat 10**, avec **Firestore (Firebase)** comme base de données cloud et **Ollama** comme moteur d'IA local.

Tout est packagé dans Docker : **aucune installation de Java, Maven, Tomcat ou Ollama n'est requise** sur votre machine.

---

## Prérequis

Un seul logiciel à installer :

- **Docker Desktop** (Windows / Mac) ou **Docker Engine** (Linux)
  - Téléchargement : <https://www.docker.com/products/docker-desktop/>
  - Vérification de l'installation : ouvrir un terminal et taper `docker --version` puis `docker compose version`

**Ressources recommandées** :
- RAM : 8 GB minimum (16 GB recommandé — Ollama charge le modèle en mémoire)
- Espace disque : ~6 GB libres (image Tomcat ~300 Mo + image Ollama ~500 Mo + modèle mistral:7b ~4 Go)
- Connexion Internet active au premier lancement

---

## Contenu de l'archive

```
interview-ai/
├── src/
│   └── main/
│       └── resources/
│           └── firebase-service-account.json  # Credentials Firebase (NE PAS PARTAGER)
├── pom.xml                           # Configuration Maven
├── Dockerfile                        # Build multi-stage du WAR + image Tomcat
├── docker-compose.yml                # Orchestration des 3 services
├── .dockerignore                     # Exclusions du contexte de build
├── README.md                         # README original du projet
└── README-DOCKER.md                  # Ce fichier
```

---

## Lancement en 3 étapes

### 1. Décompresser l'archive

Extraire le ZIP dans un dossier de votre choix, puis ouvrir un terminal dans ce dossier :

**PowerShell (Windows)** :
```powershell
cd C:\chemin\vers\interview-ai
```

**Bash (Mac / Linux)** :
```bash
cd /chemin/vers/interview-ai
```

### 2. Lancer la stack Docker

Une seule commande lance toute l'application :

```bash
docker compose up --build
```

**Que se passe-t-il au premier lancement ?**

1. Docker télécharge les images de base (Tomcat, Maven, Ollama) — quelques minutes
2. Maven compile l'application et produit le fichier WAR — ~1 minute
3. Le service **ollama** démarre (serveur LLM)
4. Le service **ollama-pull** télécharge automatiquement le modèle `mistral:7b` (**~4 GB, environ 5–10 minutes selon la connexion**)
5. Une fois le modèle prêt, le service **app** démarre Tomcat avec l'application déployée

> **Durée totale du premier démarrage : 10–15 minutes**
> Les lancements suivants prennent **~30 secondes** (images et modèle déjà en cache).

> **Note normale** : Une fois le téléchargement terminé, le container `interview-ai-ollama-pull` affichera le statut **"Exited (0)"**. C'est le comportement attendu — ce service est volontairement éphémère.

### 3. Ouvrir l'application dans le navigateur

Lorsque les logs affichent `Server startup in [X] milliseconds`, l'application est prête.

👉 **Ouvrir : <http://localhost:8080/interview-ai/>**

---

## URLs disponibles

| Endpoint | URL | Description |
|----------|-----|-------------|
| Application Web | <http://localhost:8080/interview-ai/> | Page d'accueil (landing.xhtml) |
| API REST | <http://localhost:8080/interview-ai/api/> | Endpoints JAX-RS |
| Health check AI | <http://localhost:8080/interview-ai/api/health/ai> | État du provider AI (Ollama) |
| Ollama API | <http://localhost:11434> | Serveur LLM (utilisable directement) |

**Vérification rapide de l'AI** :
```bash
curl http://localhost:8080/interview-ai/api/health/ai
```
Doit retourner quelque chose comme :
```json
{"status":"ok","provider":"ollama","model":"mistral:7b","latencyMs":123}
```

---

## Commandes utiles

```bash
# Voir les logs en temps réel
docker compose logs -f app          # Application Tomcat
docker compose logs -f ollama       # Serveur LLM
docker compose logs -f              # Tous les services

# Arrêter la stack (préserve les modèles Ollama et l'image)
docker compose down

# Redémarrer après un arrêt (rapide, pas de re-téléchargement)
docker compose up

# Arrêt complet avec suppression des volumes (RE-télécharge le modèle au prochain run)
docker compose down -v

# Reconstruire l'image après une modification du code
docker compose up --build

# Ouvrir un shell dans le container de l'application
docker compose exec app bash

# Voir l'état des services
docker compose ps
```

---

## Architecture

```
┌──────────────────────────────────────────────────────┐
│   Docker Compose (réseau interne "ai-net")           │
│                                                       │
│    ┌──────────────┐        ┌─────────────────┐       │
│    │  app         │───────▶│  ollama         │       │
│    │  Tomcat 10   │  HTTP  │  mistral:7b     │       │
│    │  Port 8080   │        │  Port 11434     │       │
│    └──────┬───────┘        └────────┬────────┘       │
│           │                         │                │
│           ▼                         ▼                │
│   firebase-sa.json          ollama_models            │
│   (bind mount lecture       (volume Docker           │
│    seule depuis l'hôte)     persisté)                │
│                                                       │
└───────────┬──────────────────────────────────────────┘
            │ HTTPS
            ▼
    ┌──────────────────────┐
    │ Firebase / Firestore │
    │  (cloud Google)      │
    └──────────────────────┘
```

- **app** : container Tomcat 10 contenant l'application WAR. Lit `firebase-service-account.json` depuis un bind-mount (le fichier reste sur votre machine, jamais dans l'image Docker).
- **ollama** : serveur LLM local exposant l'API Ollama. Les modèles sont persistés dans un volume nommé pour éviter les re-téléchargements.
- **ollama-pull** : container éphémère qui télécharge `mistral:7b` au premier démarrage puis s'arrête.

---

## Dépannage

### Le port 8080 est déjà utilisé

Un autre service utilise déjà ce port. Deux options :

**Option A** — Arrêter le service concurrent (ex : un autre Tomcat).

**Option B** — Changer le port dans `docker-compose.yml`, section `app` :
```yaml
ports:
  - "9090:8080"   # 9090 sur l'hôte → 8080 dans le container
```
L'application sera alors accessible sur <http://localhost:9090/interview-ai/>.

### Erreur "firebase-service-account.json not found"

Vérifier que le fichier `firebase-service-account.json` est bien présent dans **`src/main/resources/`** (à côté des autres ressources du projet).

### Ollama est lent à répondre

Le **premier appel** au modèle `mistral:7b` nécessite son chargement en mémoire (~5 GB) — comptez **30–60 secondes**. Les appels suivants sont instantanés tant que le container Ollama tourne.

### Utiliser un modèle plus léger

Si votre machine a peu de RAM, vous pouvez remplacer `mistral:7b` par un modèle plus petit. Dans `docker-compose.yml`, modifier deux endroits :

**Service `ollama-pull`** :
```yaml
ollama pull phi3:mini    # au lieu de mistral:7b
```

**Service `app`** :
```yaml
OLLAMA_MODEL: "phi3:mini"   # au lieu de "mistral:7b"
```

Alternatives populaires :
- `phi3:mini` (~2 GB, rapide)
- `llama3.2:3b` (~2 GB, équilibré)
- `gemma2:2b` (~1.5 GB, très léger)

Puis relancer : `docker compose down && docker compose up --build`

### Erreur de connexion Firestore

Vérifier que la machine a bien accès à Internet — Firestore est un service cloud Google et nécessite une connexion sortante vers `*.googleapis.com`.

### Build Maven échoue

Si le build échoue avec une erreur réseau, c'est probablement le téléchargement des dépendances Maven qui a été interrompu. Relancer simplement :
```bash
docker compose build --no-cache
```

---

## Arrêt complet du projet

Pour arrêter et nettoyer entièrement (libérer ~5 GB d'espace disque) :

```bash
docker compose down -v
docker rmi interview-ai-app ollama/ollama:latest tomcat:10.1-jdk17-temurin maven:3.9-eclipse-temurin-17
```

---

## Informations techniques

| Composant | Version |
|-----------|---------|
| Java | 17 (Eclipse Temurin) |
| Jakarta EE | 10.0.0 |
| Tomcat | 10.1 |
| JSF (Mojarra) | 4.0.6 |
| CDI (Weld) | 5.1.2 |
| Jersey (JAX-RS) | 3.1.5 |
| Firebase Admin SDK | 9.3.0 |
| Ollama | dernière (`latest`) |
| Modèle LLM par défaut | mistral:7b |

---

**Bon test ! 🎓**

En cas de problème, consulter les logs avec `docker compose logs -f` puis contacter l'étudiant.
