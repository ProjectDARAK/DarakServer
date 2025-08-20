package camp.cultr.darakserver.util.handler

import camp.cultr.darakserver.dto.CommonResponse
import camp.cultr.darakserver.util.annotation.SkipCommonWrap
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.coyote.Response
import org.springframework.core.MethodParameter
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpResponse
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

@Order(0)
@RestControllerAdvice
class CommonWrapHandler: ResponseBodyAdvice<Any> {
    override fun supports(
        returnType: MethodParameter,
        converterType: Class<out HttpMessageConverter<*>?>
    ): Boolean {
        if (returnType.containingClass.isAnnotationPresent(SkipCommonWrap::class.java)) return false
        if (returnType.hasMethodAnnotation(SkipCommonWrap::class.java)) return false
        if (returnType.containingClass.javaClass.equals(StreamingResponseBody::class.java)) return false
        if (returnType.containingClass.javaClass.equals(ResponseEntity::class.java)) return false
        return true
    }

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>?>,
        request: ServerHttpRequest,
        response: ServerHttpResponse
    ): Any? {
        // 이미 래핑/에러/바이너리/리소스/스트리밍류는 패스
        when (body) {
            null -> return CommonResponse(data = null)
            is CommonResponse<*> -> return body
            is ResponseEntity<*> -> return body
            is StreamingResponseBody -> return body
            is org.springframework.web.servlet.mvc.method.annotation.SseEmitter -> return body
        }

        // StringHttpMessageConverter 특수 처리
        if (StringHttpMessageConverter::class.java.isAssignableFrom(selectedConverterType)) {
            return ObjectMapper().writeValueAsString(CommonResponse(data = body))
        }

        // 일반 객체는 래핑
        return CommonResponse(data = body)
    }
}