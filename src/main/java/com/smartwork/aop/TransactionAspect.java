package com.smartwork.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Aspect
@Component
public class TransactionAspect {

    @Around("@annotation(transactional)")
    public Object monitorTransaction(ProceedingJoinPoint joinPoint, Transactional transactional) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.debug("Transaction started: {}.{}() - ReadOnly: {}, Propagation: {}, Isolation: {}",
                  className, methodName,
                  transactional.readOnly(),
                  transactional.propagation(),
                  transactional.isolation());

        try {
            Object result = joinPoint.proceed();
            log.debug("Transaction committed: {}.{}()", className, methodName);
            return result;
        } catch (Exception e) {
            log.error("Transaction rolled back: {}.{}() - Reason: {}", className, methodName, e.getMessage());
            throw e;
        }
    }
}
