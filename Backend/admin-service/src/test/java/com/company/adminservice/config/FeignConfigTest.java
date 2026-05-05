package com.company.adminservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FeignConfigTest {

    @InjectMocks
    private FeignConfig feignConfig;

    // ════════════════════════════════════════════════════════
    // RequestInterceptor — forwards Authorization header
    // ════════════════════════════════════════════════════════

    @Test
    void requestInterceptor_whenAuthHeaderPresent_shouldForwardIt() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader("Authorization", "Bearer test-jwt-token");

        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(mockRequest));

        RequestInterceptor interceptor = feignConfig.requestInterceptor();
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers())
                .containsKey("Authorization");
        assertThat(template.headers().get("Authorization"))
                .contains("Bearer test-jwt-token");

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void requestInterceptor_whenNoAuthHeader_shouldNotAddAuthHeader() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        // No Authorization header

        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(mockRequest));

        RequestInterceptor interceptor = feignConfig.requestInterceptor();
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey("Authorization");

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void requestInterceptor_whenNoRequestContext_shouldNotThrow() {
        // No request context set
        RequestContextHolder.resetRequestAttributes();

        RequestInterceptor interceptor = feignConfig.requestInterceptor();
        RequestTemplate template = new RequestTemplate();

        // Should not throw any exception
        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey("Authorization");
    }

    @Test
    void requestInterceptor_whenEmptyAuthHeader_shouldNotForwardIt() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader("Authorization", ""); // empty header

        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(mockRequest));

        RequestInterceptor interceptor = feignConfig.requestInterceptor();
        RequestTemplate template = new RequestTemplate();

        interceptor.apply(template);

        // Your code checks: authHeader != null && !authHeader.isEmpty()
        // Empty string → not forwarded
        assertThat(template.headers()).doesNotContainKey("Authorization");

        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void requestInterceptor_shouldReturnNonNullBean() {
        RequestInterceptor interceptor = feignConfig.requestInterceptor();
        assertThat(interceptor).isNotNull();
    }
}
