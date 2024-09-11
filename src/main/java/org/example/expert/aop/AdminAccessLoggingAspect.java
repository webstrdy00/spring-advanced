package org.example.expert.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Aspect
public class AdminAccessLoggingAspect {

    @Pointcut("@annotation(org.example.expert.domain.common.annotation.LogAdminAccess)")
    private void adminAccess(){}

    @Around("adminAccess()")
    public Object logAdminAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null){
            HttpServletRequest request = attributes.getRequest();

            Long userId = (Long) request.getAttribute("userId");
            String accessTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String requestUrl = request.getRequestURI();

            log.info("Admin Access - User ID : {}, Access Time : {}, Request URL: {}", userId, accessTime, requestUrl);
        }

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            return result;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("프로그램 실행 시간 : {}", executionTime);
        }
    }
}
