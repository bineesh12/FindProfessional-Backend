package com.findprofessional.marketplace.professional

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.portfolio.storage")
data class PortfolioStorageProperties(
    val localDirectory: String = defaultPortfolioUploadDirectory(),
    val publicPath: String = "/uploads"
)

private fun defaultPortfolioUploadDirectory(): String =
    "${System.getProperty("user.home")}/.findprofessional/uploads"
