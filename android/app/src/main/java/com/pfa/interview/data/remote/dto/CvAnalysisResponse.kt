package com.pfa.interview.data.remote.dto

data class CvAnalysisResponse(
    val type: String,       // "TECHNICAL", "HR", "DOMAIN"
    val position: String,   // ex: "Senior Software Engineer"
    val difficulty: String, // "JUNIOR", "MID", "SENIOR"
    val reasoning: String   // explication de l'IA
)
