package com.pfa.interview.di

import com.pfa.interview.core.utils.AuthEventBus
import com.pfa.interview.core.utils.Constants
import com.pfa.interview.data.local.AiProviderPreference
import com.pfa.interview.data.local.TokenDataStore
import com.pfa.interview.data.remote.api.AdminApi
import com.pfa.interview.data.remote.api.AuthApi
import com.pfa.interview.data.remote.api.CvApi
import com.pfa.interview.data.remote.api.FeedbackApi
import com.pfa.interview.data.remote.api.ProfileApi
import com.pfa.interview.data.remote.api.InterviewApi
import com.pfa.interview.data.remote.api.SessionApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

class AuthInterceptor @Inject constructor(
    private val tokenDataStore: TokenDataStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenDataStore.getToken() }
        val request = chain.request().newBuilder().apply {
            if (token != null) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()
        return chain.proceed(request)
    }
}

class AiProviderInterceptor @Inject constructor(
    private val aiProviderPreference: AiProviderPreference
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val provider = runBlocking { aiProviderPreference.aiProvider.first() }
        val request = chain.request().newBuilder()
            .header("X-AI-Provider", provider)
            .build()
        return chain.proceed(request)
    }
}

class UnauthorizedInterceptor @Inject constructor(
    private val tokenDataStore: TokenDataStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            runBlocking { tokenDataStore.clearAll() }
            AuthEventBus.emitUnauthorized()
        }
        return response
    }
}

class AdaptiveTimeoutInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val path = chain.request().url.encodedPath
        val needsLongTimeout = (path.contains("/sessions/") && path.endsWith("/answer"))
            || path.endsWith("/cv/analyze")
        return if (needsLongTimeout) {
            chain.withReadTimeout(120, TimeUnit.SECONDS)
                .withWriteTimeout(120, TimeUnit.SECONDS)
                .proceed(chain.request())
        } else {
            chain.proceed(chain.request())
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

    @Provides
    @Singleton
    fun provideAuthInterceptor(tokenDataStore: TokenDataStore): AuthInterceptor =
        AuthInterceptor(tokenDataStore)

    @Provides
    @Singleton
    fun provideAiProviderInterceptor(aiProviderPreference: AiProviderPreference): AiProviderInterceptor =
        AiProviderInterceptor(aiProviderPreference)

    @Provides
    @Singleton
    fun provideUnauthorizedInterceptor(tokenDataStore: TokenDataStore): UnauthorizedInterceptor =
        UnauthorizedInterceptor(tokenDataStore)

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor,
        aiProviderInterceptor: AiProviderInterceptor,
        unauthorizedInterceptor: UnauthorizedInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(aiProviderInterceptor)
        .addInterceptor(unauthorizedInterceptor)
        .addInterceptor(AdaptiveTimeoutInterceptor())
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(Constants.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideSessionApi(retrofit: Retrofit): SessionApi = retrofit.create(SessionApi::class.java)

    @Provides
    @Singleton
    fun provideFeedbackApi(retrofit: Retrofit): FeedbackApi = retrofit.create(FeedbackApi::class.java)

    @Provides
    @Singleton
    fun provideAdminApi(retrofit: Retrofit): AdminApi = retrofit.create(AdminApi::class.java)

    @Provides
    @Singleton
    fun provideInterviewApi(retrofit: Retrofit): InterviewApi = retrofit.create(InterviewApi::class.java)

    @Provides
    @Singleton
    fun provideCvApi(retrofit: Retrofit): CvApi = retrofit.create(CvApi::class.java)

    @Provides
    @Singleton
    fun provideProfileApi(retrofit: Retrofit): ProfileApi = retrofit.create(ProfileApi::class.java)
}
