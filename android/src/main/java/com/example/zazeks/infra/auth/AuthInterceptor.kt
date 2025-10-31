package com.example.zazeks.infra.auth

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStorage: AuthTokenStorage,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header(AUTHORIZATION_HEADER) != null) {
            return chain.proceed(request)
        }
        val path = request.url.encodedPath
        if (path.endsWith("/auth/login") || path.endsWith("/auth/register")) {
            return chain.proceed(request)
        }
        val session = runBlocking { tokenStorage.currentSession() }
        val token = session?.accessToken
        if (token.isNullOrBlank()) {
            return chain.proceed(request)
        }
        val tokenType = session.tokenType.ifBlank { DEFAULT_TOKEN_TYPE }
        val headerValue = "${tokenType.capitalizeFirst()} $token"
        val authenticatedRequest = request.newBuilder()
            .addHeader(AUTHORIZATION_HEADER, headerValue)
            .build()
        return chain.proceed(authenticatedRequest)
    }

    private fun String.capitalizeFirst(): String = replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase() else char.toString()
    }

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val DEFAULT_TOKEN_TYPE = "Bearer"
    }
}
