package com.pfa.interviewai.service;

import com.pfa.interviewai.model.Feedback;
import com.pfa.interviewai.model.Question;
import com.pfa.interviewai.rest.dto.CvAnalysisResponse;
import com.pfa.interviewai.rest.dto.SessionSummaryDto;

import java.util.List;
import java.util.Map;

public interface AIProvider {

    List<String> generateQuestions(String type, String position, String difficulty, int count);

    Feedback analyzeAnswer(String question, String answerText, String position, String type,
                           String sessionId, String questionId);

    Question generateAdaptiveQuestion(String interviewType, String position,
                                      String adaptiveDifficulty, List<String> askedQuestions);

    Feedback analyzeAnswerDetailed(String question, String category, String answerText,
                                   String sessionId, String questionId);

    Feedback generateDetailedFeedback(String question, String answerText, String category,
                                      Feedback analysis, String sessionId, String questionId);

    SessionSummaryDto generateSessionSummary(List<Map<String, Object>> qaPairs);

    boolean pingApi();

    CvAnalysisResponse analyzeCv(String cvText);
}
