package com.findprofessional.marketplace

import com.findprofessional.marketplace.auth.AuthProperties
import com.findprofessional.marketplace.service.CatalogProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AuthProperties::class, CatalogProperties::class)
class FindProfessionalApplication

fun main(args: Array<String>) {
    runApplication<FindProfessionalApplication>(*args)
}
