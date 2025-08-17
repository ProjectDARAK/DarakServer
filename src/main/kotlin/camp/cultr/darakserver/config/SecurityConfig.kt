package camp.cultr.darakserver.config

import camp.cultr.darakserver.util.Logger
import camp.cultr.darakserver.util.errorhandler.AuthEntryPointJwt
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

@Configuration
class SecurityConfig(
    private val userDetailsServiceImpl: UserDetailsService,
    private val authTokenFilter: AuthTokenFilter,
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
            // 폼 로그인 비활성화
            .formLogin { it.disable() }
            // CSRF 보호 비활성화 (REST API에서는 일반적으로 비활성화)
            .csrf { it.disable() }
            // 인증되지 않은 요청에 대한 예외 처리
            .exceptionHandling { it.authenticationEntryPoint(unauthorizedHandler) }
            // 세션 관리 정책 설정 (상태 비저장 - REST API에 적합)
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            // 인증 제공자 설정
            .authenticationProvider(authenticationProvider())
            // JWT 토큰 필터 추가
            .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter::class.java)
            // 프레임 옵션 비활성화 (X-Frame-Options 헤더)
            .headers { headersConfigurer -> headersConfigurer.frameOptions { it.disable() } }
            // HTTP 요청 인가 규칙 설정
            .authorizeHttpRequests {
                it.dispatcherTypeMatchers(DispatcherType.FORWARD)
                    .permitAll()
                    // OPTIONS 메서드는 모든 경로에서 허용 (CORS 프리플라이트 요청용)
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/**")
                    .permitAll()
                // 그 외 모든 요청은 인증 필요
                //                    .anyRequest()
                //                    .authenticated()
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
