package com.findprofessional.marketplace.ai

import org.springframework.boot.web.client.RestClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import java.net.http.HttpClient
import java.time.Duration

@Configuration
class OpenAiRestClientConfiguration {
    @Bean
    fun openAiRestClientCustomizer(properties: OpenAiProperties) = RestClientCustomizer { builder ->
        val timeout = Duration.ofSeconds(properties.timeoutSeconds)
        val httpClient = HttpClient.newBuilder().connectTimeout(timeout).build()
        builder.requestFactory(
            JdkClientHttpRequestFactory(httpClient).apply { setReadTimeout(timeout) }
        )
    }
}
