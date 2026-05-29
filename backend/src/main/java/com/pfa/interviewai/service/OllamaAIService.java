package com.pfa.interviewai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfa.interviewai.config.AppConfig;
import com.pfa.interviewai.model.Feedback;
import com.pfa.interviewai.model.Question;
import com.pfa.interviewai.rest.dto.CvAnalysisResponse;
import com.pfa.interviewai.rest.dto.SessionSummaryDto;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

@ApplicationScoped
@Named("ollama")
public class OllamaAIService implements AIProvider {

    private static final Logger log = Logger.getLogger(OllamaAIService.class.getName());

    private static final String SYSTEM_PROMPT =
        "You are InterviewAI, a professional interview simulator. " +
        "CRITICAL RULE: You ALWAYS respond with valid JSON only. " +
        "Never add markdown formatting, code blocks, explanations, or any text outside the JSON object. " +
        "Your entire response must be directly parseable by JSON.parse() with no preprocessing.";

    @Inject
    private AppConfig appConfig;

    private final ObjectMapper mapper = new ObjectMapper();
    private HttpClient httpClient;

    @PostConstruct
    private void init() {
        httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(appConfig.getOllamaConnectTimeoutMs()))
            .build();
    }

    @Override
    public List<String> generateQuestions(String type, String position,
                                           String difficulty, int count) {
        String userPrompt = String.format("""
            Generate exactly %d interview questions for a %s-level %s candidate. Interview type: %s.
            Respond ONLY with a JSON object in exactly this format (replace the example questions with real new ones tailored to the position):
            {
              "questions": [
                "Walk me through your design process when starting a new project from scratch.",
                "Describe a time you had to defend a design decision to a skeptical stakeholder.",
                "How do you measure the success of a user experience you have shipped?"
              ]
            }
            Each item must be a complete, non-empty interview question specific to the candidate's position.
            Questions must progress from easier to harder.
            """, count, difficulty, position, type);

        String response = null;
        try {
            response = callOllama(SYSTEM_PROMPT, userPrompt);
            JsonNode node = mapper.readTree(response);
            List<String> questions = new ArrayList<>();
            JsonNode arr = node.get("questions");
            if (arr != null && arr.isArray()) {
                arr.forEach(q -> {
                    String text;
                    if (q.isTextual()) {
                        text = q.asText();
                    } else if (q.isObject() && q.has("question")) {
                        text = q.get("question").asText();
                    } else {
                        text = q.asText();
                    }
                    if (text != null && !text.isBlank()) questions.add(text);
                });
            }
            return questions;
        } catch (Exception e) {
            log.warning("generateQuestions failed: " + e.getMessage() + " | Raw: " + response);
            return List.of(
                "Tell me about yourself and your background.",
                "What are your main technical strengths?",
                "Describe a challenging project you worked on.",
                "How do you handle tight deadlines?",
                "Where do you see yourself in 5 years?"
            );
        }
    }

    @Override
    public Feedback analyzeAnswer(String question, String answerText,
                                   String position, String type,
                                   String sessionId, String questionId) {
        String userPrompt = String.format("""
            Evaluate this interview answer and return a JSON assessment.
            Position: %s | Interview type: %s
            Question: %s
            Candidate answer: %s

            Respond ONLY with a JSON object in exactly this format (replace the example values with your real assessment):
            {
              "relevanceScore": 7.5,
              "clarityScore": 6.0,
              "sentimentScore": 8.0,
              "overallScore": 7.2,
              "strengths": "The candidate demonstrated clear understanding of the topic.",
              "improvements": "The answer lacked specific examples or technical depth.",
              "suggestedAnswer": "A strong answer would include specific examples and demonstrate hands-on experience.",
              "shortComment": "Good start — add concrete examples to strengthen your answer."
            }
            All score fields must be floats between 0.0 and 10.0. All text fields must be non-empty strings.
            """, position, type, question, answerText);

        String response = null;
        try {
            response = callOllama(SYSTEM_PROMPT, userPrompt);
            JsonNode node = mapper.readTree(response);
            return Feedback.builder()
                    .id(UUID.randomUUID().toString())
                    .sessionId(sessionId)
                    .questionId(questionId)
                    .answerText(answerText)
                    .relevanceScore(node.has("relevanceScore") ? (float) node.get("relevanceScore").asDouble() : 5f)
                    .clarityScore(node.has("clarityScore") ? (float) node.get("clarityScore").asDouble() : 5f)
                    .sentimentScore(node.has("sentimentScore") ? (float) node.get("sentimentScore").asDouble() : 5f)
                    .overallScore(node.has("overallScore") ? (float) node.get("overallScore").asDouble() : 5f)
                    .strengths(node.has("strengths") ? node.get("strengths").asText() : "")
                    .improvements(node.has("improvements") ? node.get("improvements").asText() : "")
                    .suggestedAnswer(node.has("suggestedAnswer") ? node.get("suggestedAnswer").asText() : "")
                    .shortComment(node.has("shortComment") ? node.get("shortComment").asText() : "")
                    .build();
        } catch (Exception e) {
            log.warning("analyzeAnswer failed: " + e.getMessage() + " | Raw: " + response);
            return Feedback.builder()
                    .id(UUID.randomUUID().toString())
                    .sessionId(sessionId)
                    .questionId(questionId)
                    .answerText(answerText)
                    .relevanceScore(5f).clarityScore(5f)
                    .sentimentScore(5f).overallScore(5f)
                    .strengths("Answer received.")
                    .improvements("Could not analyze response.")
                    .suggestedAnswer("").shortComment("Keep practicing!")
                    .build();
        }
    }

    @Override
    public Question generateAdaptiveQuestion(String interviewType, String position,
                                              String adaptiveDifficulty,
                                              List<String> askedQuestions) {
        StringBuilder askedList = new StringBuilder();
        for (int i = 0; i < askedQuestions.size(); i++) {
            askedList.append(i + 1).append(". ").append(askedQuestions.get(i)).append("\n");
        }

        String userPrompt = String.format("""
            Generate ONE interview question for this context:
            - Interview type: %s
            - Target position: %s
            - Difficulty level: %s
            - Already asked (DO NOT repeat any of these): %s

            Respond ONLY with a JSON object in exactly this format (replace example values with a real new question for the position):
            {
              "question": "Walk me through how you would design a checkout flow for a mobile e-commerce app.",
              "difficulty": "intermediate",
              "category": "UX Design",
              "estimated_duration_seconds": 180,
              "expected_keywords": ["user research", "wireframes", "usability"]
            }
            The "difficulty" field must be exactly one of: beginner, intermediate, advanced.
            """,
            interviewType, position, adaptiveDifficulty,
            askedList.length() > 0 ? askedList.toString() : "(none yet)");

        String response = null;
        try {
            response = callOllama(SYSTEM_PROMPT, userPrompt);
            JsonNode node = mapper.readTree(response);
            List<String> keywords = new ArrayList<>();
            if (node.has("expected_keywords") && node.get("expected_keywords").isArray()) {
                node.get("expected_keywords").forEach(k -> keywords.add(k.asText()));
            }
            return Question.builder()
                    .id(UUID.randomUUID().toString())
                    .content(node.get("question").asText())
                    .aiDifficulty(node.has("difficulty") ? node.get("difficulty").asText() : adaptiveDifficulty)
                    .category(node.has("category") ? node.get("category").asText() : "General")
                    .estimatedDurationSeconds(node.has("estimated_duration_seconds")
                        ? node.get("estimated_duration_seconds").asInt() : 120)
                    .expectedKeywords(keywords)
                    .build();
        } catch (Exception e) {
            log.warning("generateAdaptiveQuestion failed: " + e.getMessage() + " | Raw: " + response);
            return Question.builder()
                    .id(UUID.randomUUID().toString())
                    .content("Tell me about a challenging project you worked on and how you overcame the difficulties.")
                    .aiDifficulty(adaptiveDifficulty)
                    .category("General")
                    .estimatedDurationSeconds(180)
                    .expectedKeywords(List.of("project", "challenge", "solution"))
                    .build();
        }
    }

    @Override
    public Feedback analyzeAnswerDetailed(String question, String category,
                                           String answerText,
                                           String sessionId, String questionId) {
        String userPrompt = String.format("""
            Analyze this interview answer objectively.
            Question: %s
            Category: %s
            Candidate's answer: %s

            Respond ONLY with a JSON object in exactly this format (replace example values with your real assessment):
            {
              "scores": {
                "relevance": 75,
                "clarity": 68,
                "depth": 60,
                "vocabulary": 70,
                "examples": 55
              },
              "global_score": 67,
              "level_assessment": "mid",
              "key_strengths": ["Clear structure", "Practical examples"],
              "critical_gaps": ["Limited technical depth", "No metrics cited"]
            }
            All score fields must be integers 0-100. The "level_assessment" must be exactly one of: junior, mid, senior.
            """,
            question, category != null ? category : "General", answerText);

        String response = null;
        try {
            response = callOllama(SYSTEM_PROMPT, userPrompt);
            JsonNode node = mapper.readTree(response);
            JsonNode scores = node.get("scores");
            float relevance  = (float) scores.get("relevance").asDouble();
            float clarity    = (float) scores.get("clarity").asDouble();
            float depth      = (float) scores.get("depth").asDouble();
            float vocabulary = (float) scores.get("vocabulary").asDouble();
            float examples   = (float) scores.get("examples").asDouble();
            float globalScore = (float) node.get("global_score").asDouble();

            List<String> keyStrengths = new ArrayList<>();
            if (node.has("key_strengths") && node.get("key_strengths").isArray())
                node.get("key_strengths").forEach(k -> keyStrengths.add(k.asText()));

            List<String> criticalGaps = new ArrayList<>();
            if (node.has("critical_gaps") && node.get("critical_gaps").isArray())
                node.get("critical_gaps").forEach(k -> criticalGaps.add(k.asText()));

            return Feedback.builder()
                    .id(UUID.randomUUID().toString())
                    .sessionId(sessionId)
                    .questionId(questionId)
                    .answerText(answerText)
                    .relevanceScore(relevance / 10f)
                    .clarityScore(clarity / 10f)
                    .depthScore(depth)
                    .vocabularyScore(vocabulary)
                    .examplesScore(examples)
                    .globalScore(globalScore)
                    .overallScore(globalScore / 10f)
                    .levelAssessment(node.has("level_assessment") ? node.get("level_assessment").asText() : "mid")
                    .keyStrengths(keyStrengths)
                    .criticalGaps(criticalGaps)
                    .build();
        } catch (Exception e) {
            log.warning("analyzeAnswerDetailed failed: " + e.getMessage() + " | Raw: " + response);
            return Feedback.builder()
                    .id(UUID.randomUUID().toString())
                    .sessionId(sessionId)
                    .questionId(questionId)
                    .answerText(answerText)
                    .relevanceScore(5f).clarityScore(5f).overallScore(5f)
                    .depthScore(50f).vocabularyScore(50f).examplesScore(50f).globalScore(50f)
                    .levelAssessment("mid")
                    .keyStrengths(List.of("Answer received"))
                    .criticalGaps(List.of("Analysis unavailable"))
                    .build();
        }
    }

    @Override
    public Feedback generateDetailedFeedback(String question, String answerText,
                                              String category, Feedback analysis,
                                              String sessionId, String questionId) {
        String userPrompt = String.format("""
            Generate constructive interview feedback.
            Question: %s
            Candidate's answer: %s
            Scores: Relevance %.0f/100, Clarity %.0f/100, Depth %.0f/100, Vocabulary %.0f/100, Examples %.0f/100

            Respond ONLY with a JSON object in exactly this format (replace example values with your real feedback):
            {
              "positive_points": "The candidate showed strong understanding of user-centered design principles and applied them throughout.",
              "improvement_points": "The answer would be stronger with specific metrics and a concrete example from past work.",
              "concrete_advice": "Practice the STAR method to add structured examples to every answer.",
              "example_answer": "When redesigning the onboarding flow at Acme, I started with user interviews, mapped the journey, prototyped three variants, and A/B tested - the chosen design reduced drop-off by 23 percent.",
              "next_difficulty": "same"
            }
            The "next_difficulty" field must be exactly one of: easier, same, harder.
            """,
            question, answerText,
            analysis.getRelevanceScore() * 10,
            analysis.getClarityScore() * 10,
            analysis.getDepthScore(),
            analysis.getVocabularyScore(),
            analysis.getExamplesScore());

        String response = null;
        try {
            response = callOllama(SYSTEM_PROMPT, userPrompt);
            JsonNode node = mapper.readTree(response);
            String positivePoints    = node.has("positive_points")    ? node.get("positive_points").asText()    : "";
            String improvementPoints = node.has("improvement_points") ? node.get("improvement_points").asText() : "";
            String concreteAdvice    = node.has("concrete_advice")    ? node.get("concrete_advice").asText()    : "";
            String exampleAnswer     = node.has("example_answer")     ? node.get("example_answer").asText()     : "";
            String nextDifficulty    = node.has("next_difficulty")    ? node.get("next_difficulty").asText()    : "same";

            return Feedback.builder()
                    .id(analysis.getId())
                    .sessionId(sessionId)
                    .questionId(questionId)
                    .answerText(answerText)
                    .relevanceScore(analysis.getRelevanceScore())
                    .clarityScore(analysis.getClarityScore())
                    .sentimentScore(0f)
                    .overallScore(analysis.getOverallScore())
                    .strengths(positivePoints)
                    .improvements(improvementPoints)
                    .suggestedAnswer(exampleAnswer)
                    .shortComment(concreteAdvice)
                    .depthScore(analysis.getDepthScore())
                    .vocabularyScore(analysis.getVocabularyScore())
                    .examplesScore(analysis.getExamplesScore())
                    .globalScore(analysis.getGlobalScore())
                    .levelAssessment(analysis.getLevelAssessment())
                    .keyStrengths(analysis.getKeyStrengths())
                    .criticalGaps(analysis.getCriticalGaps())
                    .positivePoints(positivePoints)
                    .improvementPoints(improvementPoints)
                    .concreteAdvice(concreteAdvice)
                    .exampleAnswer(exampleAnswer)
                    .nextDifficulty(nextDifficulty)
                    .build();
        } catch (Exception e) {
            log.warning("generateDetailedFeedback failed: " + e.getMessage() + " | Raw: " + response);
            return Feedback.builder()
                    .id(analysis.getId())
                    .sessionId(sessionId)
                    .questionId(questionId)
                    .answerText(answerText)
                    .relevanceScore(analysis.getRelevanceScore())
                    .clarityScore(analysis.getClarityScore())
                    .overallScore(analysis.getOverallScore())
                    .depthScore(analysis.getDepthScore())
                    .vocabularyScore(analysis.getVocabularyScore())
                    .examplesScore(analysis.getExamplesScore())
                    .globalScore(analysis.getGlobalScore())
                    .levelAssessment(analysis.getLevelAssessment())
                    .keyStrengths(analysis.getKeyStrengths())
                    .criticalGaps(analysis.getCriticalGaps())
                    .positivePoints("Answer received and evaluated.")
                    .improvementPoints("Detailed feedback unavailable.")
                    .concreteAdvice("Continue practicing structured answers.")
                    .exampleAnswer("")
                    .nextDifficulty("same")
                    .strengths("Answer received.")
                    .improvements("Feedback generation failed.")
                    .suggestedAnswer("").shortComment("Keep practicing!")
                    .build();
        }
    }

    @Override
    public SessionSummaryDto generateSessionSummary(List<Map<String, Object>> qaPairs) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < qaPairs.size(); i++) {
            Map<String, Object> pair = qaPairs.get(i);
            context.append("Q").append(i + 1).append(": ").append(pair.get("question")).append("\n");
            context.append("A").append(i + 1).append(": ").append(pair.get("answer")).append("\n");
            context.append("Score: ").append(pair.get("globalScore")).append("/100\n\n");
        }

        String userPrompt = String.format("""
            Generate a complete interview session summary.
            Questions and scores:
            %s

            Respond ONLY with a JSON object in exactly this format (replace example values with your real assessment):
            {
              "overall_score": 72.5,
              "global_assessment": "The candidate demonstrated solid foundational knowledge and good communication, with room to grow in technical depth.",
              "top_strengths": ["Clear communication", "Structured thinking", "Practical examples"],
              "priority_improvements": ["Deepen technical knowledge", "Use specific metrics"],
              "recommended_resources": ["Don't Make Me Think by Steve Krug", "Nielsen Norman Group articles"],
              "readiness_level": "almost_ready"
            }
            The "overall_score" must be a float 0.0-100.0. The "readiness_level" must be exactly one of: not_ready, almost_ready, ready.
            """, context.toString());

        String response = null;
        try {
            response = callOllama(SYSTEM_PROMPT, userPrompt);
            JsonNode node = mapper.readTree(response);

            List<String> topStrengths = new ArrayList<>();
            if (node.has("top_strengths") && node.get("top_strengths").isArray())
                node.get("top_strengths").forEach(k -> topStrengths.add(k.asText()));

            List<String> priorityImprovements = new ArrayList<>();
            if (node.has("priority_improvements") && node.get("priority_improvements").isArray())
                node.get("priority_improvements").forEach(k -> priorityImprovements.add(k.asText()));

            List<String> recommendedResources = new ArrayList<>();
            if (node.has("recommended_resources") && node.get("recommended_resources").isArray())
                node.get("recommended_resources").forEach(k -> recommendedResources.add(k.asText()));

            SessionSummaryDto dto = new SessionSummaryDto();
            dto.setOverallScore((float) node.get("overall_score").asDouble());
            dto.setGlobalAssessment(node.has("global_assessment") ? node.get("global_assessment").asText() : "");
            dto.setTopStrengths(topStrengths);
            dto.setPriorityImprovements(priorityImprovements);
            dto.setRecommendedResources(recommendedResources);
            dto.setReadinessLevel(node.has("readiness_level") ? node.get("readiness_level").asText() : "almost_ready");
            return dto;
        } catch (Exception e) {
            log.warning("generateSessionSummary failed: " + e.getMessage() + " | Raw: " + response);
            SessionSummaryDto dto = new SessionSummaryDto();
            dto.setOverallScore(50f);
            dto.setGlobalAssessment("Session completed. Summary generation encountered an error.");
            dto.setTopStrengths(List.of("Completed the interview session"));
            dto.setPriorityImprovements(List.of("Continue practicing"));
            dto.setRecommendedResources(List.of());
            dto.setReadinessLevel("almost_ready");
            return dto;
        }
    }

    @Override
    public boolean pingApi() {
        try {
            String response = callOllama(SYSTEM_PROMPT, "Respond with: {\"status\":\"ok\"}");
            JsonNode node = mapper.readTree(response);
            return node.has("status") && "ok".equals(node.get("status").asText());
        } catch (Exception e) {
            log.warning("Ollama ping failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public CvAnalysisResponse analyzeCv(String cvText) {
        String truncated = cvText.length() > 4_000
            ? cvText.substring(0, 4_000) + "\n[... truncated ...]"
            : cvText;

        String userPrompt = String.format("""
            You are an expert recruiter. Analyze the following CV and recommend the most appropriate interview configuration.

            CV TEXT:
            %s

            Respond ONLY with a JSON object in exactly this format (replace example values with your real analysis of the CV above):
            {
              "type": "TECHNICAL",
              "position": "Senior Software Engineer",
              "difficulty": "SENIOR",
              "reasoning": "The CV shows 8+ years of backend engineering with leadership of distributed systems projects, indicating a senior technical role."
            }
            The "type" field must be exactly one of: TECHNICAL, HR, DOMAIN.
            The "difficulty" field must be exactly one of: JUNIOR, MID, SENIOR.
            """, truncated);

        String rawResponse = null;
        try {
            rawResponse = callOllama(SYSTEM_PROMPT, userPrompt);
            JsonNode node = mapper.readTree(rawResponse);
            CvAnalysisResponse result = new CvAnalysisResponse();
            result.setType(node.has("type") ? node.get("type").asText() : "HR");
            result.setPosition(node.has("position") ? node.get("position").asText() : "Professional");
            result.setDifficulty(node.has("difficulty") ? node.get("difficulty").asText() : "MID");
            result.setReasoning(node.has("reasoning") ? node.get("reasoning").asText() : "");
            return result;
        } catch (Exception e) {
            log.warning("analyzeCv failed: " + e.getMessage() + " | Raw: " + rawResponse);
            CvAnalysisResponse fallback = new CvAnalysisResponse();
            fallback.setType("HR");
            fallback.setPosition("Professional");
            fallback.setDifficulty("MID");
            fallback.setReasoning("Automatic CV analysis was unavailable. Please configure your interview manually.");
            return fallback;
        }
    }

    private String callOllama(String systemPrompt, String userPrompt) {
        long start = System.currentTimeMillis();
        log.info(String.format("Ollama API call — model=%s, promptLength=%d",
            appConfig.getOllamaModel(), userPrompt.length()));

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("temperature", 0.1);
        options.put("num_predict", 1500);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", appConfig.getOllamaModel());
        body.put("stream", false);
        body.put("format", "json");
        body.put("messages", messages);
        body.put("options", options);

        String rawContent = null;
        try {
            String requestBody = mapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(appConfig.getOllamaApiUrl() + "/api/chat"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMillis(appConfig.getOllamaReadTimeoutMs()))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

            long elapsed = System.currentTimeMillis() - start;
            log.info(String.format("Ollama API response — status=%d, elapsed=%dms",
                response.statusCode(), elapsed));

            if (response.statusCode() != 200) {
                throw new RuntimeException("Ollama returned HTTP " + response.statusCode()
                    + ": " + response.body());
            }

            JsonNode root = mapper.readTree(response.body());
            rawContent = root.get("message").get("content").asText();
            String preview = rawContent.length() > 500
                ? rawContent.substring(0, 500) + "..."
                : rawContent;
            log.info("Ollama raw content: " + preview);
            return extractJson(sanitize(rawContent));

        } catch (java.net.ConnectException | java.net.http.HttpConnectTimeoutException e) {
            throw new RuntimeException(
                "Ollama server is unreachable. Make sure Ollama is running: ollama serve", e);
        } catch (java.net.http.HttpTimeoutException e) {
            throw new RuntimeException(
                "Ollama request timed out after " + appConfig.getOllamaReadTimeoutMs()
                + "ms. Mistral 7B on CPU may be too slow for this prompt — try a shorter input or larger timeout.", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            if (rawContent != null) {
                log.severe("Ollama response parsing failed. Raw response: " + rawContent);
                throw new RuntimeException(
                    "Ollama response parsing failed. Raw: " + rawContent, e);
            }
            throw new RuntimeException(
                "Ollama server is unreachable. Make sure Ollama is running: ollama serve", e);
        }
    }

    private String sanitize(String raw) {
        if (raw == null) return "";
        return raw
            .replaceAll("(?s)```json\\s*", "")
            .replaceAll("(?s)```\\s*", "")
            .trim();
    }

    private String extractJson(String text) {
        if (text == null || text.isEmpty()) return text;
        int start = text.indexOf('{');
        if (start == -1) return text;
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escape) { escape = false; continue; }
            if (c == '\\' && inString) { escape = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (inString) continue;
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) return text.substring(start, i + 1);
            }
        }
        return text.substring(start);
    }
}
