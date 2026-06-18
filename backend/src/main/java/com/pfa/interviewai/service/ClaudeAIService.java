package com.pfa.interviewai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfa.interviewai.config.AppConfig;
import com.pfa.interviewai.metrics.MetricsRegistry;
import com.pfa.interviewai.model.Feedback;
import com.pfa.interviewai.model.Question;
import com.pfa.interviewai.rest.dto.CvAnalysisResponse;
import com.pfa.interviewai.rest.dto.SessionSummaryDto;
import io.prometheus.client.Histogram;
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
@Named("claude")
public class ClaudeAIService implements AIProvider {

    private static final Logger log = Logger.getLogger(ClaudeAIService.class.getName());

    private static final String JSON_SYSTEM_PROMPT =
        "You are an AI interview assistant. Respond ONLY with valid JSON. " +
        "No markdown, no code blocks, no explanation, no preamble. " +
        "The response must be directly parseable as JSON.";

    @Inject
    private AppConfig appConfig;

    @Inject
    private MetricsRegistry metricsRegistry;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public List<String> generateQuestions(String type, String position,
                                           String difficulty, int count) {
        String prompt = String.format("""
            You are an expert interviewer. Generate exactly %d interview questions
            for a %s-level %s candidate. Interview type: %s.
            Rules:
            - Return ONLY a JSON array of strings: ["question1", "question2", ...]
            - No explanations, no numbering, no markdown, no backticks
            - Questions must be progressively more challenging
            - TECHNICAL: algorithms, system design, debugging, code review
            - HR: behavioral (STAR), motivational, situational judgment
            - DOMAIN: domain-specific technical and business knowledge
            """, count, difficulty, position, type);

        String response = callClaude("", prompt, "generate_questions");
        try {
            JsonNode node = mapper.readTree(response);
            List<String> questions = new ArrayList<>();
            node.forEach(q -> questions.add(q.asText()));
            return questions;
        } catch (Exception e) {
            return List.of(
                "Tell me about yourself and your background.",
                "What are your main technical strengths?",
                "Describe a challenging project you worked on.",
                "How do you handle tight deadlines?",
                "Where do you see yourself in 5 years?"
            );
        }
    }

    public Feedback analyzeAnswer(String question, String answerText,
                                   String position, String type,
                                   String sessionId, String questionId) {
        String prompt = String.format("""
            You are an expert interviewer evaluating a job candidate's answer.
            Position: %s | Interview type: %s
            Question: %s
            Candidate's answer: %s

            Respond ONLY with a JSON object (no markdown, no backticks):
            {
              "relevanceScore": <float 0-10>,
              "clarityScore": <float 0-10>,
              "sentimentScore": <float 0-10>,
              "overallScore": <float 0-10>,
              "strengths": "<1-2 sentences on what was good>",
              "improvements": "<1-2 sentences on what was missing>",
              "suggestedAnswer": "<a model answer in 2-3 sentences>",
              "shortComment": "<one concise encouraging sentence>"
            }

            Scoring:
            - relevanceScore: how directly the answer addresses the question (0-10)
            - clarityScore: structure, articulation, vocabulary (0-10)
            - sentimentScore: confidence and professional tone (0=negative, 10=positive)
            - overallScore: weighted (relevance 40%% + clarity 35%% + sentiment 25%%)
            """, position, type, question, answerText);

        String response = callClaude("", prompt, "analyze_answer");
        try {
            JsonNode node = mapper.readTree(response);
            return Feedback.builder()
                    .id(UUID.randomUUID().toString())
                    .sessionId(sessionId)
                    .questionId(questionId)
                    .answerText(answerText)
                    .relevanceScore((float) node.get("relevanceScore").asDouble())
                    .clarityScore((float) node.get("clarityScore").asDouble())
                    .sentimentScore((float) node.get("sentimentScore").asDouble())
                    .overallScore((float) node.get("overallScore").asDouble())
                    .strengths(node.get("strengths").asText())
                    .improvements(node.get("improvements").asText())
                    .suggestedAnswer(node.get("suggestedAnswer").asText())
                    .shortComment(node.get("shortComment").asText())
                    .build();
        } catch (Exception e) {
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

    public Question generateAdaptiveQuestion(String interviewType, String position,
                                              String adaptiveDifficulty,
                                              List<String> askedQuestions) {
        StringBuilder askedList = new StringBuilder();
        for (int i = 0; i < askedQuestions.size(); i++) {
            askedList.append(i + 1).append(". ").append(askedQuestions.get(i)).append("\n");
        }

        String userPrompt = String.format("""
            Generate ONE interview question for the following context.
            Interview type: %s
            Target position: %s
            Difficulty level: %s

            Already asked questions (DO NOT repeat or closely paraphrase any of these):
            %s

            Return ONLY a JSON object matching this exact schema:
            {
              "question": "the full question text",
              "difficulty": "beginner|intermediate|advanced",
              "category": "short category label (e.g. System Design, Behavioral, Java, SQL)",
              "estimated_duration_seconds": <integer, typical 60-300>,
              "expected_keywords": ["keyword1", "keyword2", "keyword3"]
            }
            """,
            interviewType, position, adaptiveDifficulty,
            askedList.length() > 0 ? askedList.toString() : "(none yet)");

        String response = callClaude(JSON_SYSTEM_PROMPT, userPrompt, "adaptive_question");
        try {
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
            log.warning("generateAdaptiveQuestion parse failed: " + e.getMessage());
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

    public Feedback analyzeAnswerDetailed(String question, String category,
                                           String answerText,
                                           String sessionId, String questionId) {
        String userPrompt = String.format("""
            Evaluate the following interview answer across 5 dimensions.
            Question category: %s
            Question: %s
            Candidate answer: %s

            Score each dimension from 0 to 100 (integers). Return ONLY this JSON:
            {
              "scores": {
                "relevance": <0-100>,
                "clarity": <0-100>,
                "depth": <0-100>,
                "vocabulary": <0-100>,
                "examples": <0-100>
              },
              "global_score": <0-100, weighted average: relevance 25%% + clarity 20%% + depth 25%% + vocabulary 15%% + examples 15%%>,
              "level_assessment": "junior|mid|senior",
              "key_strengths": ["strength1", "strength2"],
              "critical_gaps": ["gap1", "gap2"]
            }
            """,
            category != null ? category : "General", question, answerText);

        String response = callClaude(JSON_SYSTEM_PROMPT, userPrompt, "analyze_answer_detailed");
        try {
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
            log.warning("analyzeAnswerDetailed parse failed: " + e.getMessage());
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

    public Feedback generateDetailedFeedback(String question, String answerText,
                                              String category, Feedback analysis,
                                              String sessionId, String questionId) {
        String userPrompt = String.format("""
            Provide actionable feedback for the following interview answer.
            Question category: %s
            Question: %s
            Candidate answer: %s
            Scores — Relevance: %.0f/100, Clarity: %.0f/100, Depth: %.0f/100, Vocabulary: %.0f/100, Examples: %.0f/100

            Return ONLY this JSON:
            {
              "positive_points": "2-3 sentences on what the candidate did well",
              "improvement_points": "2-3 sentences on what was lacking or could be stronger",
              "concrete_advice": "1-2 specific, actionable steps to improve this type of answer",
              "example_answer": "a model answer in 3-4 sentences demonstrating best practice",
              "next_difficulty": "easier|same|harder"
            }
            """,
            category != null ? category : "General",
            question, answerText,
            analysis.getRelevanceScore() * 10,
            analysis.getClarityScore() * 10,
            analysis.getDepthScore(),
            analysis.getVocabularyScore(),
            analysis.getExamplesScore());

        String response = callClaude(JSON_SYSTEM_PROMPT, userPrompt, "detailed_feedback");
        try {
            JsonNode node = mapper.readTree(response);
            String positivePoints   = node.has("positive_points") ? node.get("positive_points").asText() : "";
            String improvementPoints = node.has("improvement_points") ? node.get("improvement_points").asText() : "";
            String concreteAdvice   = node.has("concrete_advice") ? node.get("concrete_advice").asText() : "";
            String exampleAnswer    = node.has("example_answer") ? node.get("example_answer").asText() : "";
            String nextDifficulty   = node.has("next_difficulty") ? node.get("next_difficulty").asText() : "same";

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
            log.warning("generateDetailedFeedback parse failed: " + e.getMessage());
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

    public SessionSummaryDto generateSessionSummary(List<Map<String, Object>> qaPairs) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < qaPairs.size(); i++) {
            Map<String, Object> pair = qaPairs.get(i);
            context.append("Q").append(i + 1).append(": ").append(pair.get("question")).append("\n");
            context.append("A").append(i + 1).append(": ").append(pair.get("answer")).append("\n");
            context.append("Score: ").append(pair.get("globalScore")).append("/100\n\n");
        }

        String userPrompt = String.format("""
            Generate a comprehensive interview session summary based on the following Q&A pairs:

            %s

            Return ONLY this JSON:
            {
              "overall_score": <0-100, float>,
              "global_assessment": "2-3 sentences describing the candidate's overall performance",
              "top_strengths": ["strength1", "strength2", "strength3"],
              "priority_improvements": ["improvement1", "improvement2", "improvement3"],
              "recommended_resources": ["resource1", "resource2"],
              "readiness_level": "not_ready|almost_ready|ready"
            }
            """, context.toString());

        String response = callClaude(JSON_SYSTEM_PROMPT, userPrompt, "session_summary");
        try {
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
            log.warning("generateSessionSummary parse failed: " + e.getMessage());
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

    public boolean pingApi() {
        try {
            String response = callClaude(JSON_SYSTEM_PROMPT, "Respond with: {\"status\":\"ok\"}", "ping");
            JsonNode node = mapper.readTree(response);
            return node.has("status") && "ok".equals(node.get("status").asText());
        } catch (Exception e) {
            log.warning("Claude API ping failed: " + e.getMessage());
            return false;
        }
    }

    public CvAnalysisResponse analyzeCv(String cvText) {
        String truncated = cvText.length() > 12_000
            ? cvText.substring(0, 12_000) + "\n[... truncated ...]"
            : cvText;

        String userPrompt = String.format("""
            You are an expert recruiter and career advisor.
            Analyze the following CV and recommend the most appropriate interview configuration.

            CV TEXT:
            %s

            Return ONLY a JSON object with this exact schema:
            {
              "type": "TECHNICAL" | "HR" | "DOMAIN",
              "position": "<inferred job title, max 50 chars>",
              "difficulty": "JUNIOR" | "MID" | "SENIOR",
              "reasoning": "<2-3 sentences explaining your choices>"
            }

            Rules:
            - type: TECHNICAL if the CV shows programming/engineering skills;
                    HR if it emphasizes soft skills, management, or recruitment;
                    DOMAIN if it shows deep specialization (finance, healthcare, law, etc.)
            - position: extract or infer the most recent or target job title
            - difficulty: JUNIOR for 0-2 yrs experience, MID for 2-6 yrs, SENIOR for 6+ yrs or leadership roles
            - reasoning: briefly justify all three choices
            """, truncated);

        String response = callClaude(JSON_SYSTEM_PROMPT, userPrompt, "analyze_cv");
        try {
            JsonNode node = mapper.readTree(stripCodeFences(response));
            CvAnalysisResponse result = new CvAnalysisResponse();
            result.setType(node.get("type").asText());
            result.setPosition(node.get("position").asText());
            result.setDifficulty(node.get("difficulty").asText());
            result.setReasoning(node.has("reasoning") ? node.get("reasoning").asText() : "");
            return result;
        } catch (Exception e) {
            log.warning("analyzeCv parse failed: " + e.getMessage());
            throw new RuntimeException("CV analysis failed: could not parse AI response", e);
        }
    }

    private String stripCodeFences(String raw) {
        if (raw == null) return "";
        String s = raw.strip();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline != -1) s = s.substring(firstNewline + 1);
            if (s.endsWith("```")) s = s.substring(0, s.lastIndexOf("```"));
        }
        return s.strip();
    }

    private String callClaude(String systemPrompt, String userPrompt, String operation) {
        long start = System.currentTimeMillis();
        log.info(String.format("Claude API call — operation=%s, promptLength=%d",
            operation, userPrompt.length()));

        metricsRegistry.getPromptLength()
            .labels("claude", operation)
            .observe(userPrompt.length());

        Histogram.Timer timer = metricsRegistry.getRequestDuration()
            .labels("claude", operation)
            .startTimer();
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", appConfig.getClaudeModel());
            body.put("max_tokens", appConfig.getClaudeMaxTokens());
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                body.put("system", systemPrompt);
            }
            body.put("messages", List.of(Map.of("role", "user", "content", userPrompt)));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(appConfig.getClaudeApiUrl()))
                .header("x-api-key", appConfig.getClaudeApiKey())
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(appConfig.getClaudeTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

            HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString());

            long elapsed = System.currentTimeMillis() - start;
            log.info(String.format("Claude API response — operation=%s, status=%d, elapsed=%dms",
                operation, response.statusCode(), elapsed));

            if (response.statusCode() != 200) {
                throw new RuntimeException("Anthropic API returned " + response.statusCode()
                    + ": " + response.body());
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode content = root.get("content");
            if (content == null || !content.isArray() || content.isEmpty()) {
                throw new RuntimeException("Unexpected API response: " + response.body());
            }

            metricsRegistry.getRequestsTotal().labels("claude", operation, "success").inc();
            return content.get(0).get("text").asText();

        } catch (Exception e) {
            metricsRegistry.getRequestsTotal().labels("claude", operation, "error").inc();
            log.severe("Claude API error: " + e.getMessage());
            throw new RuntimeException("Claude API call failed: " + e.getMessage(), e);
        } finally {
            timer.observeDuration();
        }
    }
}
