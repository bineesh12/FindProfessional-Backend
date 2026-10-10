package com.findprofessional.marketplace.legal

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class PublicLegalController {
    @GetMapping("/terms", produces = [MediaType.TEXT_HTML_VALUE])
    fun termsOfService(): ResponseEntity<Resource> = legalDocument("legal/terms.html", MediaType.TEXT_HTML)

    @GetMapping("/privacy", produces = [MediaType.TEXT_HTML_VALUE])
    fun privacyPolicy(): ResponseEntity<Resource> = legalDocument("legal/privacy.html", MediaType.TEXT_HTML)

    @GetMapping("/account-deletion", produces = [MediaType.TEXT_HTML_VALUE])
    fun accountDeletion(): ResponseEntity<Resource> =
        legalDocument("legal/account-deletion.html", MediaType.TEXT_HTML)

    @GetMapping("/legal/legal.css", produces = ["text/css"])
    fun stylesheet(): ResponseEntity<Resource> =
        legalDocument("legal/legal.css", MediaType.parseMediaType("text/css"))

    private fun legalDocument(path: String, mediaType: MediaType): ResponseEntity<Resource> =
        ResponseEntity.ok()
            .contentType(mediaType)
            .body(ClassPathResource(path))
}
