package com.jobtracker.controller

import com.jobtracker.service.ApplicationNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

data class ErrorResponse(val status: Int, val message: String, val errors: List<String> = emptyList())

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationNotFoundException::class)
    fun notFound(e: ApplicationNotFoundException) = error(HttpStatus.NOT_FOUND, e.message ?: "Ikke funnet")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun invalid(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = e.bindingResult.fieldErrors
            .map { it.defaultMessage ?: "${it.field} er ugyldig" }
            .sorted()
        return error(HttpStatus.BAD_REQUEST, "Valideringen feilet", errors)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun unreadable(e: HttpMessageNotReadableException) = error(
        HttpStatus.BAD_REQUEST,
        "Ugyldig eller manglende innhold i forespørselen. " +
            "Sjekk JSON-formatet, datoer (yyyy-MM-dd), status og at company og position er satt",
    )

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun badParameter(e: MethodArgumentTypeMismatchException) =
        error(HttpStatus.BAD_REQUEST, "Ugyldig verdi '${e.value}' for parameteren '${e.name}'")

    @ExceptionHandler(IllegalArgumentException::class)
    fun badArgument(e: IllegalArgumentException) = error(HttpStatus.BAD_REQUEST, e.message ?: "Ugyldig forespørsel")

    private fun error(status: HttpStatus, message: String, errors: List<String> = emptyList()) =
        ResponseEntity.status(status).body(ErrorResponse(status.value(), message, errors))
}
