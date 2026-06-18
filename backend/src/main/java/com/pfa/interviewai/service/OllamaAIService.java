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

    private static final double TEMP_QUESTION_GEN = 0.75;
    private static final double TEMP_EVALUATION   = 0.35;
    private static final double TEMP_FEEDBACK     = 0.50;
    private static final double TEMP_SUMMARY      = 0.45;

    @Inject
    private AppConfig appConfig;

    @Inject
    private MetricsRegistry metricsRegistry;

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
            response = callOllama(SYSTEM_PROMPT, userPrompt, TEMP_QUESTION_GEN, "generate_questions");
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
            Evaluate this interview answer and return an accurate JSON assessment.
            Position: %s | Interview type: %s
            Question: %s
            Candidate answer: %s

            Scoring rubric for floats 0.0-10.0 (apply strictly):
            - 0.0-1.5: No answer, "I don't know", completely off-topic, or blank
            - 1.6-3.5: Only keywords repeated, no real understanding demonstrated
            - 3.6-5.5: Partial answer with basic understanding, lacks depth or examples
            - 5.6-7.5: Solid answer with relevant content and some structure
            - 7.6-9.0: Strong answer with concrete examples and clear structure
            - 9.1-10.0: Exceptional answer with metrics, nuanced insight, and originality

            CRITICAL: If the candidate says "I don't know" or gives an empty/irrelevant answer, relevanceScore MUST be below 2.0 and overallScore MUST be below 2.5.
            Do NOT use the placeholder values in the format below as your scores — assign based solely on actual answer quality.

            Respond ONLY with a JSON object in exactly this format (replace ALL placeholder values with your real assessment):
            {
              "relevanceScore": <float 0.0-10.0>,
              "clarityScore": <float 0.0-10.0>,
              "sentimentScore": <float 0.0-10.0>,
              "overallScore": <float 0.0-10.0>,
              "strengths": "<specific strength observed in this answer>",
              "improvements": "<specific improvement needed for this answer>",
              "suggestedAnswer": "<a model answer for this specific question>",
              "shortComment": "<one sentence of direct, honest feedback>"
            }
            All score fields must be floats between 0.0 and 10.0. All text fields must be non-empty strings.
            """, position, type, question, answerText);

        String response = null;
        try {
            response = callOllama(SYSTEM_PROMPT, userPrompt, TEMP_EVALUATION, "analyze_answer");
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
            - Already asked (DO NOT repeat or paraphrase any of these, and avoid their categories): %s

            IMPORTANT: Generate a question on a COMPLETELY DIFFERENT topic from those already asked.
            The question must be specifically relevant to the position and interview type provided.
            Do NOT use generic filler questions.

            Respond ONLY with a JSON object in exactly this format (the values below are placeholders — replace ALL of them with your actual output):
            {
              "question": "<Your generated question text goes here>",
              "difficulty": "<beginner|intermediate|advanced>",
              "category": "<topic category>",
              "estimated_duration_seconds": <90|120|180|240>,
              "expected_keywords": ["<keyword1>", "<keyword2>", "<keyword3>"]
            }
            The "difficulty" field must be exactly one of: beginner, intermediate, advanced.
            """,
            interviewType, position, adaptiveDifficulty,
            askedList.length() > 0 ? askedList.toString() : "(none yet)");

        String response = null;
        try {
            response = callOllama(SYSTEM_PROMPT, userPrompt, TEMP_QUESTION_GEN, "adaptive_question");
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
            Analyze this interview answer objectively and assign accurate scores.
            Question: %s
            Category: %s
            Candidate's answer: %s

            Scoring rubric (apply strictly based on the actual answer above):
            - 0-15: No answer, "I don't know", completely off-topic, or blank
            - 16-35: Only keywords repeated from the question, no real understanding demonstrated
            - 36-55: Partial answer with basic understanding, lacks depth or concrete examples
            - 56-75: Solid answer with relevant content and some structure
            - 76-90: Strong answer with concrete examples, clear structure, and domain insight
            - 91-100: Exceptional answer with metrics, nuanced understanding, and originality

            CRITICAL RULES:
            - If the candidate's answer is empty, says "I don't know", or simply repeats words from the question without adding substance, relevance MUST be below 20 and global_score MUST be below 25.
            - Do NOT assign scores based on the placeholder values shown in the format below — assign scores based solely on the actual answer quality.
            - key_strengths and critical_gaps must reference specific content from the candidate's answer, not generic observations.

            Respond ONLY with a JSON object in exactly this format (the numeric values below are placeholders — replace ALL of them with your real assessment):
            {
              "scores": {
                "relevance": <integer 0-100>,
                "clarity": <integer 0-100>,
                "depth": <integer 0-100>,
                "vocabulary": <integer 0-100>,
                "examples": <integer 0-100>
              },
              "global_score": <integer 0-100, weighted average: relevance 30%% + clarity 20%% + depth 25%% + vocabulary 10%% + examples 15%%>,
              "level_assessment": "<junior|mid|senior>",
              "key_strengths": ["<specific strength observed in this answer>"],
              "critical_gaps": ["<specific gap observed in this answer>"]
            }
            All score fields must be integers 0-100. The "level_assessment" must be exactly one of: junior, mid, senior.
            """,
            question, category != null ? category : "General", answerText);

        String response = null;
        try {
            response = callOllama(SYSTEM_PROMPT, userPrompt, TEMP_EVALUATION, "analyze_answer_detailed");
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
            Generate specific, constructive interview feedback based strictly on the actual answer provided.
            Question: %s
            Candidate's answer: %s
            Scores: Relevance %.0f/100, Clarity %.0f/100, Depth %.0f/100, Vocabulary %.0f/100, Examples %.0f/100

            Base your feedback ONLY on the candidate's actual answer above — not on generic interview advice.
            If the answer was weak or empty, be honest about it. If it was strong, acknowledge specifically what made it strong.

            Respond ONLY with a JSON object in exactly this format (replace ALL placeholder values with your real feedback):
            {
              "positive_points": "<2-3 sentences specifically about what this candidate did well in their actual answer, or 'No significant strengths were demonstrated in this answer' if the answer was poor>",
              "improvement_points": "<2-3 sentences on what was specifically lacking or unclear in this answer>",
              "concrete_advice": "<1-2 actionable steps this specific candidate should practice based on their weaknesses>",
              "example_answer": "<a model answer in 3-4 sentences for this specific question>",
              "next_difficulty": "<easier if Depth or Relevance score below 40, harder if both above 75, same otherwise>"
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
            response = callOllama(SYSTEM_PROMPT, userPrompt, TEMP_FEEDBACK, "detailed_feedback");
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
            Generate an honest and accurate interview session summary based on the actual Q&A and scores below.
            Questions, answers, and scores:
            %s

            IMPORTANT scoring rules:
            - The overall_score must reflect the actual average of the scores shown above. Do NOT invent a score.
            - If most scores are below 40, readiness_level must be "not_ready".
            - If most scores are above 75, readiness_level must be "ready".
            - Otherwise, readiness_level must be "almost_ready".
            - top_strengths and priority_improvements must reference specific patterns observed across the actual answers above.
            - recommended_resources must be real, specific books, websites, or courses relevant to the weaknesses identified.

            Respond ONLY with a JSON object in exactly this format (replace ALL placeholder values with your real assessment):
            {
              "overall_score": <float 0.0-100.0, weighted average of the scores shown above>,
              "global_assessment": "<honest 2-3 sentence assessment summarizing the candidate's actual performance across all answers>",
              "top_strengths": ["<specific strength observed across answers>", "<another strength>"],
              "priority_improvements": ["<specific area needing improvement based on answers>", "<another area>"],
              "recommended_resources": ["<specific book, course, or website relevant to the gaps>", "<another resource>"],
              "readiness_level": "<not_ready|almost_ready|ready based on the rules above>"
            }
            The "overall_score" must be a float 0.0-100.0. The "readiness_level" must be exactly one of: not_ready, almost_ready, ready.
            """, context.toString());

        String response = null;
        try {
            response = callOllama(SYSTEM_PROMPT, userPrompt, TEMP_SUMMARY, "session_summary");
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
            String response = callOllama(SYSTEM_PROMPT, "Respond with: {\"status\":\"ok\"}", 0.1, "ping");
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
            rawResponse = callOllama(SYSTEM_PROMPT, userPrompt, 0.1, "analyze_cv");
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

    private String callOllama(String systemPrompt, String userPrompt,
                               double temperature, String operation) {
        long start = System.currentTimeMillis();
        log.info(String.format("Ollama API call — model=%s, operation=%s, promptLength=%d",
            appConfig.getOllamaModel(), operation, userPrompt.length()));

        metricsRegistry.getPromptLength()
            .labels("ollama", operation)
            .observe(userPrompt.length());

        Histogram.Timer timer = metricsRegistry.getRequestDuration()
            .labels("ollama", operation)
            .startTimer();

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("temperature", temperature);
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
            log.info(String.format("Ollama API response — operation=%s, status=%d, elapsed=%dms",
                operation, response.statusCode(), elapsed));

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

            metricsRegistry.getRequestsTotal().labels("ollama", operation, "success").inc();
            return extractJson(sanitize(rawContent));

        } catch (java.net.ConnectException | java.net.http.HttpConnectTimeoutException e) {
            metricsRegistry.getRequestsTotal().labels("ollama", operation, "error").inc();
            throw new RuntimeException(
                "Ollama server is unreachable. Make sure Ollama is running: ollama serve", e);
        } catch (java.net.http.HttpTimeoutException e) {
            metricsRegistry.getRequestsTotal().labels("ollama", operation, "error").inc();
            throw new RuntimeException(
                "Ollama request timed out after " + appConfig.getOllamaReadTimeoutMs()
                + "ms. Mistral 7B on CPU may be too slow for this prompt — try a shorter input or larger timeout.", e);
        } catch (RuntimeException e) {
            metricsRegistry.getRequestsTotal().labels("ollama", operation, "error").inc();
            throw e;
        } catch (Exception e) {
            metricsRegistry.getRequestsTotal().labels("ollama", operation, "error").inc();
            if (rawContent != null) {
                log.severe("Ollama response parsing failed. Raw response: " + rawContent);
                throw new RuntimeException(
                    "Ollama response parsing failed. Raw: " + rawContent, e);
            }
            throw new RuntimeException(
                "Ollama server is unreachable. Make sure Ollama is running: ollama serve", e);
        } finally {
            timer.observeDuration();
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
