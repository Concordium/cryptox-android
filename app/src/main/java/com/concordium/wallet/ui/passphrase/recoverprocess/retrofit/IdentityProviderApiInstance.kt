package com.concordium.wallet.ui.passphrase.recoverprocess.retrofit

import android.net.Uri
import com.concordium.wallet.BuildConfig
import com.concordium.wallet.data.model.RecoverErrorResponse
import com.concordium.wallet.data.model.RecoverResponse
import com.concordium.wallet.util.Log
import com.google.gson.Gson
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

object IdentityProviderApiInstance {
    private val httpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(
            if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        )
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://some.api.url/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient)
            .build()
    }

    private val api: IdentityProviderApi by lazy(retrofit::create)

    suspend fun safeRecoverCall(url: String?): Pair<Boolean, RecoverResponse?> {
        try {
            val response = api.recover(url)
            if (response.isSuccessful) {
                response.body()?.let {
                    if (it.value != null)
                        return Pair(true, it)
                    else
                        return Pair(true, null)
                }
            } else {
                return if (response.errorBody() != null && response.code() == 404) {
                    val errorResponse: RecoverErrorResponse = Gson().fromJson(
                        response.errorBody()!!.charStream(),
                        RecoverErrorResponse::class.java
                    )
                    Log.d("${errorResponse.code} ${errorResponse.message} on $url")
                    Pair(true, null)
                } else {
                    Pair(false, null)
                }
            }
        } catch (t: Throwable) {
            Log.d(Log.toString(t))
        }
        return Pair(false, null)
    }

    /**
     * When the ID verification start URL is built, it must be requested
     * internally before passing to a browser. The final URI will be obtained
     * after following all the redirects. It will help to detect errors
     * such as idCredPub duplication faster.
     *
     * @param verificationStartUrl a URL crafted to start the verification process
     * @param redirectUriScheme a scheme which the app expects for result redirect
     *
     * @return a URL to proceed with the verification or a URI with [redirectUriScheme]
     * if the result is available instantly.
     */
    fun getVerificationRedirectUri(
        verificationStartUrl: String,
        redirectUriScheme: String,
    ): String =
        httpClient
            .newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
            .newCall(
                Request.Builder()
                    .url(verificationStartUrl)
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .build()
            )
            .execute()
            .use { response ->
                check(response.isRedirect) {
                    "The identity provider did not redirect as expected"
                }

                val redirectLocation = response.header("Location")
                    ?: error("Can't find the location in the identity provider response")

                return@use if (Uri.parse(redirectLocation).scheme in setOf(
                        "http",
                        "https",
                        redirectUriScheme,
                    )
                ) {
                    // Return the location if it is a URI with an allowed scheme:
                    // HTTP or HTTPS to proceed with the verification on a web page,
                    // or the app redirect URI scheme to get the instant result.
                    redirectLocation
                } else {
                    // Otherwise try to resolve the location against the base URL
                    // in case it is a relative path.
                    response.request.url.resolve(redirectLocation)?.toString()
                        ?: error("Can't resolve the redirect: $redirectLocation")
                }
            }
}
