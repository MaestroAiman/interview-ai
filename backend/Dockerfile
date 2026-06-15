# =========================================================
#  Interview-AI — Dockerfile multi-stage
#  Stage 1 : Build du WAR avec Maven + JDK 17
#  Stage 2 : Runtime Tomcat 10 + JDK 17 (WAR pré-explosé)
# =========================================================

# ---------- Stage 1 : Build ----------
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 1) Copier uniquement pom.xml pour profiter du cache Docker
#    des dépendances Maven (ne se réinstalle que si pom.xml change)
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

# 2) Copier les sources et builder le WAR
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# Résultat : /build/target/interview-ai.war


# ---------- Stage 2 : Runtime ----------
FROM tomcat:10.1-jdk17-temurin

# Supprimer les webapps par défaut de Tomcat (ROOT, examples, docs, manager…)
RUN rm -rf /usr/local/tomcat/webapps/* /usr/local/tomcat/webapps.dist || true

# Copier le WAR depuis le stage build vers un emplacement temporaire
COPY --from=builder /build/target/interview-ai.war /tmp/interview-ai.war

# Pré-exploser le WAR dans webapps/interview-ai/
# Indispensable pour permettre le bind-mount de firebase-service-account.json
# directement dans WEB-INF/classes/ au démarrage du container.
RUN mkdir -p /usr/local/tomcat/webapps/interview-ai \
    && cd /usr/local/tomcat/webapps/interview-ai \
    && jar -xf /tmp/interview-ai.war \
    && rm /tmp/interview-ai.war

# Tomcat écoute par défaut sur 8080 ; aucun reverse-proxy à ce stade.
EXPOSE 8080

# Démarrage standard de Tomcat (foreground, logs sur stdout)
CMD ["catalina.sh", "run"]
