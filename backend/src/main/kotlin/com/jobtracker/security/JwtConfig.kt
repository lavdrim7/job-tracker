package com.jobtracker.security

import com.nimbusds.jose.jwk.source.ImmutableSecret
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtClaimNames
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtIssuerValidator
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.time.Duration
import java.time.Instant
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class JwtConfig {

    private fun key(properties: JwtProperties) =
        SecretKeySpec(properties.secret.toByteArray(Charsets.UTF_8), "HmacSHA256")

    @Bean
    fun jwtEncoder(properties: JwtProperties): JwtEncoder = NimbusJwtEncoder(ImmutableSecret(key(properties)))

    /** Kun HS256 aksepteres. Tokens uten `exp`, med feil utsteder eller utløpt tid avvises (ingen klokketoleranse). */
    @Bean
    fun jwtDecoder(properties: JwtProperties): JwtDecoder {
        val decoder = NimbusJwtDecoder.withSecretKey(key(properties)).macAlgorithm(MacAlgorithm.HS256).build()
        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                JwtTimestampValidator(Duration.ZERO),
                JwtIssuerValidator(properties.issuer),
                JwtClaimValidator<Instant?>(JwtClaimNames.EXP) { it != null },
                JwtClaimValidator<String?>(JwtClaimNames.SUB) { !it.isNullOrBlank() },
            ),
        )
        return decoder
    }
}
