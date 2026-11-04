package com.zozi.shoppingapp.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class ApiKeyInterceptor @Inject constructor(
    private val apiKey: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (apiKey.isBlank()) return chain.proceed(chain.request())

        val authorisedRequest = chain.request().newBuilder()
            .header(API_KEY_HEADER, apiKey)
            .build()
        return chain.proceed(authorisedRequest)
    }

    private companion object {
        const val API_KEY_HEADER = "x-api-key"
    }
}
