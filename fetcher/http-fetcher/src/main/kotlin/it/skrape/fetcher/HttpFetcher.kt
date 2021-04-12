package it.skrape.fetcher

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.runBlocking

public object HttpFetcher : BlockingFetcher<Request> {

    override val requestBuilder: Request get() = Request()

    override fun fetch(request: Request): Result = configuredClient(request).toResult()

    @Suppress("MagicNumber")
    private fun configuredClient(request: Request): HttpResponse {

        val client = HttpClient(Apache) {
            expectSuccess = false
            followRedirects = request.followRedirects
            install(HttpTimeout)
            request.authentication?.let { authentication: Authentication ->
                if (authentication is BasicAuth) {
                    installBasicAuth()
                }
            }
            HttpResponseValidator {

                handleResponseException { cause: Throwable ->
                    when (cause) {
                        is SocketTimeoutException -> {
                            throw cause
                        }
                    }
                }
            }
            engine {
                request.proxy?.toProxy()?.toHttpHost()?.let {
                    customizeClient {
                        setProxy(it)
                    }
                }
                connectionRequestTimeout = request.timeout
                socketTimeout = request.timeout
                followRedirects = request.followRedirects
            }
            if (request.sslRelaxed) {
                trustSelfSignedClient()
            }
        }
        return runBlocking { client.request(request.toHttpRequest()) }
    }
}
