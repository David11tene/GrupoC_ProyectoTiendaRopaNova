package com.tienda.ropa.backend.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// Interceptor para registrar requests
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log =
            LoggerFactory.getLogger(RequestLoggingInterceptor.class);

    // Nombre del atributo de tiempo
    private static final String START_TIME = "requestStartTime";

    // Se ejecuta antes del controlador
    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {

        request.setAttribute(START_TIME, System.currentTimeMillis());

        log.info(
                "[REQUEST] {} {} | IP: {}",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr()
        );

        return true;
    }

    // Se ejecuta después del controlador
    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {

        Long startTime = (Long) request.getAttribute(START_TIME);

        long elapsed = (startTime != null)
                ? System.currentTimeMillis() - startTime
                : -1L;

        if (ex != null) {

            log.error(
                    "[RESPONSE] {} {} | Status: {} | Tiempo: {}ms | Error: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    elapsed,
                    ex.getMessage()
            );

        } else {

            log.info(
                    "[RESPONSE] {} {} | Status: {} | Tiempo: {}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    elapsed
            );
        }
    }
}