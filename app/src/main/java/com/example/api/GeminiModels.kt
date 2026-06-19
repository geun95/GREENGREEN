package com.example.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String // Base64 encoded string
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val responseSchema: ResponseSchema? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class ResponseSchema(
    val type: String, // "OBJECT"
    val properties: Map<String, SchemaProperty>,
    val required: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class SchemaProperty(
    val type: String, // "STRING", "INTEGER"
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

// 식물 분석 결과를 담을 파싱용 데이터 클래스
@JsonClass(generateAdapter = true)
data class PlantAnalysisResult(
    val species: String, // 식물 종류 (예: 해바라기, 몬스테라)
    val nickname_suggestion: String, // 별명 추천 (예: 바라기짱)
    val watering_cycle_days: Int, // 물주기 주기 (며칠 간격)
    val sunlight: String, // 햇빛 조건 (예: 양지 (직사광선))
    val temperature: String, // 적정 키움 온도 (예: 18~25도)
    val ai_care_briefing: String // AI 케어 브리핑 (1. 물... 2. 햇빛... 3. 팁...)
)
