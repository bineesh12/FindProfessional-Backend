package com.findprofessional.marketplace

import com.findprofessional.marketplace.auth.AuthProperties
import com.findprofessional.marketplace.ai.OpenAiProperties
import com.findprofessional.marketplace.service.CatalogProperties
import com.findprofessional.marketplace.request.PostcodeResolverProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(
    AuthProperties::class,
    CatalogProperties::class,
    OpenAiProperties::class,
    PostcodeResolverProperties::class
)
class FindProfessionalApplication

fun main(args: Array<String>) {
    runApplication<FindProfessionalApplication>(*args)
}
