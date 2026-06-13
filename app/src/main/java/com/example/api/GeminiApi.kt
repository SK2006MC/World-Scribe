package com.example.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini REST API Request & Response Models ---

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val role: String? = null, // "user" or "model"
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

// --- Game Story Turn JSON Schema (Parsed from the response text) ---

@JsonClass(generateAdapter = true)
data class IntroducedCharacter(
    val name: String,
    val role: String, // e.g. "villain", "heroine", "friend", "neutral"
    val description: String
)

@JsonClass(generateAdapter = true)
data class IntroducedPlace(
    val name: String,
    val description: String
)

@JsonClass(generateAdapter = true)
data class IntroducedLore(
    val keyword: String,
    val description: String
)

@JsonClass(generateAdapter = true)
data class CharacterStatusUpdate(
    val name: String,
    val status: String, // "ALIVE" or "DEAD"
    val historySummary: String? = null
)

@JsonClass(generateAdapter = true)
data class SimulationTurnResponse(
    val narrative: String,
    val suggestions: List<String>,
    val activeCharacterName: String? = null,
    val introducedCharacters: List<IntroducedCharacter>? = null,
    val introducedPlaces: List<IntroducedPlace>? = null,
    val introducedLores: List<IntroducedLore>? = null,
    val characterStatusUpdates: List<CharacterStatusUpdate>? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    val simulationTurnAdapter by lazy {
        moshi.adapter(SimulationTurnResponse::class.java)
    }
}
