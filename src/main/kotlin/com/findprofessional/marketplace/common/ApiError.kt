package com.findprofessional.marketplace.common

import com.findprofessional.marketplace.auth.AuthException
import com.findprofessional.marketplace.request.RequestException
import com.findprofessional.marketplace.professional.ProfessionalProfileException
import com.findprofessional.marketplace.matching.ProfessionalOpportunityException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ApiError(
    val code: String,
    val message: String
)

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(AuthException::class)
    fun handleAuthException(exception: AuthException): ResponseEntity<ApiError> =
        ResponseEntity.status(exception.status)
            .body(ApiError(code = exception.code, message = exception.message ?: "Authentication failed"))

    @ExceptionHandler(RequestException::class)
    fun handleRequestException(exception: RequestException): ResponseEntity<ApiError> =
        ResponseEntity.status(exception.status)
            .body(ApiError(code = exception.code, message = exception.message ?: "Request operation failed"))

    @ExceptionHandler(ProfessionalProfileException::class)
    fun handleProfessionalProfileException(exception: ProfessionalProfileException): ResponseEntity<ApiError> =
        ResponseEntity.status(exception.status)
            .body(ApiError(code = exception.code, message = exception.message ?: "Professional profile operation failed"))

    @ExceptionHandler(ProfessionalOpportunityException::class)
    fun handleProfessionalOpportunityException(
        exception: ProfessionalOpportunityException
    ): ResponseEntity<ApiError> = ResponseEntity.status(exception.status)
        .body(ApiError(code = exception.code, message = exception.message ?: "Opportunity operation failed"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(exception: MethodArgumentNotValidException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError(code = "VALIDATION_ERROR", message = "Request validation failed"))
}
