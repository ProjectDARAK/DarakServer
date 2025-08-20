package camp.cultr.darakserver.config

import camp.cultr.darakserver.util.Logger
import camp.cultr.darakserver.util.errorhandler.AuthEntryPointJwt
import camp.cultr.darakserver.util.filter.ApiKeyFilter
import camp.cultr.darakserver.util.filter.AuthTokenFilter
import jakarta.servlet.DispatcherType
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * Security Configuration Class
 *
 * Configures Spring Security for the application, including authentication, authorization, and security filter chains.
 * Provides beans for password encoding and authentication management, integrates custom JWT filters, and establishes
 * security rules for HTTP requests.
 *
 * Dependencies:
 * - UserDetailsService: Custom implementation for loading user-specific data.
 * - AuthTokenFilter: JWT-based authentication filter.
 * - AuthEntryPointJwt: Handles unauthorized access attempts.
 *
 * Configuration Highlights:
 * - Disabling HTTP Basic and Form Login authentication methods.
 * - CSRF protection is disabled.
 * - Stateless session management.
 * - Integration of custom authentication provider and JWT filter.
 * - Authorization rules allowing all incoming requests by default.
 * - Configurations for WebAuthn, setting the relying party name, domain, and allowed origins.
 */
@Configuration
class SecurityConfig(
    private val userDetailsServiceImpl: UserDetailsService,
    private val authTokenFilter: AuthTokenFilter,
    private val apiKeyFilter: ApiKeyFilter,
    private val unauthorizedHandler: AuthEntryPointJwt,
    @Value("\${darak.instance-name}") private val instanceName: String,
    @Value("\${darak.domain}") private val domain: String,
) : Logger {

    @Bean
    fun authenticationProvider(): DaoAuthenticationProvider {
        val authProvider = DaoAuthenticationProvider(userDetailsServiceImpl)
        authProvider.setPasswordEncoder(passwordEncoder())
        return authProvider
    }

    @Bean
    fun authenticationManager(authConfig: AuthenticationConfiguration): AuthenticationManager? {
        return authConfig.authenticationManager
    }

    @Bean fun passwordEncoder() = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            // HTTP 기본 인증 비활성화
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .csrf { it.disable() }
            .exceptionHandling { it.authenticationEntryPoint(unauthorizedHandler) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterAfter(authTokenFilter, ApiKeyFilter::class.java)
            .headers { headersConfigurer -> headersConfigurer.frameOptions { it.disable() } }
            .authorizeHttpRequests {
                it.dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/api/account/auth/checkLoginType").permitAll()
                    .requestMatchers("/api/account/auth/login/password").permitAll()
                    .requestMatchers("/api/account/auth/login/otp").permitAll()
                    .requestMatchers("/api/file/f/**").permitAll()
                    .anyRequest()
                    .authenticated()
            }
            .webAuthn{
                it.rpName(instanceName)
                it.rpId(domain)
                it.allowedOrigins("https://$domain", "http://localhost:8080")
//                it.creationOptionsRepository()
//                it.messageConverter()
            }
            .build()
}
