# AOP (Aspect-Oriented Programming) 상세 가이드

> Spring AOP를 활용한 관심사 분리와 횡단 관심사 처리

---

## 📋 목차

1. [AOP 개요](#-aop-개요)
2. [AOP 핵심 개념](#-aop-핵심-개념)
3. [AOP 용어 정리](#-aop-용어-정리)
4. [AOP 문법](#-aop-문법)
5. [Pointcut 표현식](#-pointcut-표현식)
6. [프로젝트 AOP 구현](#-프로젝트-aop-구현)
7. [AOP 베스트 프랙티스](#-aop-베스트-프랙티스)

---

## 🎯 AOP 개요

### AOP란?

**AOP (Aspect-Oriented Programming, 관점 지향 프로그래밍)**는 프로그램의 횡단 관심사(Cross-Cutting Concerns)를 분리하여 모듈성을 높이는 프로그래밍 패러다임입니다.

### 왜 AOP를 사용하는가?

**문제점**: 비즈니스 로직과 부가 기능이 뒤섞여 있음

```java
// ❌ AOP 적용 전 - 코드 중복과 관심사 혼재
public User createUser(UserDto dto) {
    // 로깅 코드 (횡단 관심사)
    log.info("createUser() - START");
    StopWatch sw = new StopWatch();
    sw.start();

    // 트랜잭션 로깅 (횡단 관심사)
    log.debug("Transaction started");

    try {
        // 실제 비즈니스 로직 (핵심 관심사)
        User user = userMapper.toEntity(dto);
        User saved = userRepository.save(user);

        // 트랜잭션 로깅 (횡단 관심사)
        log.debug("Transaction committed");

        return saved;
    } catch (Exception e) {
        // 예외 로깅 (횡단 관심사)
        log.error("Error in createUser: {}", e.getMessage());
        throw e;
    } finally {
        // 성능 측정 (횡단 관심사)
        sw.stop();
        log.info("createUser() - END ({}ms)", sw.getTotalTimeMillis());
    }
}
```

**해결책**: AOP를 통한 관심사 분리

```java
// ✅ AOP 적용 후 - 비즈니스 로직만 집중
public User createUser(UserDto dto) {
    // 비즈니스 로직만 존재
    User user = userMapper.toEntity(dto);
    return userRepository.save(user);
}

// 횡단 관심사는 Aspect로 분리
@Aspect
@Component
public class LoggingAspect {
    @Around("serviceMethods()")
    public Object logServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        // 로깅, 성능 측정, 예외 처리 등 자동 적용
    }
}
```

### AOP 적용 효과

- ✅ **코드 중복 제거**: 동일한 부가 기능을 여러 곳에 작성할 필요 없음
- ✅ **유지보수성 향상**: 부가 기능 수정 시 한 곳만 변경
- ✅ **비즈니스 로직 집중**: 핵심 로직에만 집중 가능
- ✅ **일관성 보장**: 모든 메서드에 동일한 정책 자동 적용

---

## 💡 AOP 핵심 개념

### 횡단 관심사 (Cross-Cutting Concerns)

**핵심 관심사 (Core Concern)**와 **횡단 관심사 (Cross-Cutting Concern)**의 분리

```
┌─────────────────────────────────────────────────┐
│          횡단 관심사 (Cross-Cutting)              │
│  Logging | Security | Transaction | Caching     │
└─────────────────────────────────────────────────┘
         ↓           ↓           ↓           ↓
┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│ Service1 │  │ Service2 │  │ Service3 │  │ Service4 │
│ (핵심)    │  │ (핵심)    │  │ (핵심)    │  │ (핵심)    │
└──────────┘  └──────────┘  └──────────┘  └──────────┘
```

**횡단 관심사 예시**:
- **로깅 (Logging)**: 메서드 실행 로그, 성능 측정
- **보안 (Security)**: 인증/인가 체크
- **트랜잭션 (Transaction)**: 트랜잭션 경계 관리
- **예외 처리 (Exception Handling)**: 전역 예외 처리
- **캐싱 (Caching)**: 메서드 결과 캐싱
- **모니터링 (Monitoring)**: 성능 메트릭 수집

---

## 📚 AOP 용어 정리

### 1. Aspect (애스펙트)

**횡단 관심사를 모듈화한 클래스**

```java
@Aspect          // 이 클래스가 Aspect임을 선언
@Component       // Spring Bean으로 등록
public class LoggingAspect {
    // Advice 메서드들...
}
```

### 2. Join Point (조인 포인트)

**Aspect가 적용될 수 있는 실행 지점**

Spring AOP에서는 **메서드 실행** 시점만 지원합니다.

```
가능한 Join Point:
- 메서드 호출 (Method Call) ✅ Spring AOP 지원
- 메서드 실행 (Method Execution) ✅ Spring AOP 지원
- 객체 생성 (Object Instantiation) ❌ AspectJ만 지원
- 필드 접근 (Field Access) ❌ AspectJ만 지원
```

### 3. Pointcut (포인트컷)

**Join Point 중에서 Advice를 적용할 위치를 선별하는 표현식**

```java
@Pointcut("execution(* com.smartwork.service.*.*(..))")
public void serviceMethods() {}
//         └─────────────────────────────────┘
//              Pointcut 표현식
```

### 4. Advice (어드바이스)

**Join Point에서 실행되는 실제 코드 (부가 기능)**

| Advice 타입 | 실행 시점 | 설명 |
|------------|---------|------|
| `@Before` | 메서드 실행 전 | 메서드 시작 전에 실행 |
| `@After` | 메서드 실행 후 | 성공/실패 관계없이 실행 (finally) |
| `@AfterReturning` | 정상 종료 후 | 성공적으로 반환된 후 실행 |
| `@AfterThrowing` | 예외 발생 후 | 예외가 던져진 후 실행 |
| `@Around` | 메서드 실행 전후 | 가장 강력, 메서드 실행 제어 가능 |

### 5. Target (타겟)

**Aspect가 적용되는 대상 객체**

```java
@Service
public class UserService {  // ← Target
    public User createUser(UserDto dto) {
        // ...
    }
}
```

### 6. Weaving (위빙)

**Aspect를 Target에 적용하여 프록시 객체를 생성하는 과정**

```
원본 객체 (Target) + Aspect → Weaving → 프록시 객체
```

**Weaving 시점**:
- **컴파일 타임**: AspectJ 컴파일러 사용
- **클래스 로드 타임**: 클래스 로더가 클래스 로딩 시
- **런타임**: Spring AOP (프록시 패턴 사용) ✅

---

## 🔧 AOP 문법

### 1. @Before - 메서드 실행 전

**메서드 실행 전에 실행되는 Advice**

```java
@Before("execution(* com.smartwork.service.*.*(..))")
public void beforeServiceMethod(JoinPoint joinPoint) {
    String methodName = joinPoint.getSignature().getName();
    String className = joinPoint.getSignature().getDeclaringType().getSimpleName();

    log.info("[BEFORE] {}.{}() 실행 시작", className, methodName);
}
```

**실행 흐름**:
```
1. @Before Advice 실행
2. Target 메서드 실행
```

**사용 사례**:
- 파라미터 검증
- 사전 조건 체크
- 로깅

---

### 2. @After - 메서드 실행 후 (finally)

**메서드 실행 후 항상 실행 (성공/실패 관계없이)**

```java
@After("execution(* com.smartwork.service.*.*(..))")
public void afterServiceMethod(JoinPoint joinPoint) {
    log.info("[AFTER] {}.{}() 실행 완료",
        joinPoint.getSignature().getDeclaringType().getSimpleName(),
        joinPoint.getSignature().getName());
}
```

**실행 흐름**:
```
1. Target 메서드 실행
2. 성공 또는 예외 발생
3. @After Advice 실행 (항상)
```

**사용 사례**:
- 리소스 정리
- 로그 기록
- 메트릭 수집

---

### 3. @AfterReturning - 정상 종료 후

**메서드가 정상적으로 반환된 후에만 실행**

```java
@AfterReturning(
    pointcut = "execution(* com.smartwork.service.*.*(..))",
    returning = "result"  // 반환값을 매개변수로 받음
)
public void afterReturningServiceMethod(JoinPoint joinPoint, Object result) {
    log.info("[AFTER-RETURNING] {}.{}() 반환값: {}",
        joinPoint.getSignature().getDeclaringType().getSimpleName(),
        joinPoint.getSignature().getName(),
        result);
}
```

**실행 흐름**:
```
1. Target 메서드 실행
2. 정상 반환 ✅
3. @AfterReturning Advice 실행
```

**사용 사례**:
- 반환값 로깅
- 반환값 후처리
- 성공 메트릭 수집

---

### 4. @AfterThrowing - 예외 발생 후

**메서드에서 예외가 발생한 후에만 실행**

```java
@AfterThrowing(
    pointcut = "execution(* com.smartwork.service.*.*(..))",
    throwing = "ex"  // 예외 객체를 매개변수로 받음
)
public void afterThrowingServiceMethod(JoinPoint joinPoint, Exception ex) {
    log.error("[AFTER-THROWING] {}.{}() 예외 발생: {}",
        joinPoint.getSignature().getDeclaringType().getSimpleName(),
        joinPoint.getSignature().getName(),
        ex.getMessage(), ex);
}
```

**실행 흐름**:
```
1. Target 메서드 실행
2. 예외 발생 ❌
3. @AfterThrowing Advice 실행
4. 예외 재발생 (propagate)
```

**사용 사례**:
- 예외 로깅
- 에러 알림
- 실패 메트릭 수집

---

### 5. @Around - 메서드 실행 전후 (가장 강력)

**메서드 실행 전후를 완전히 제어 가능**

```java
@Around("execution(* com.smartwork.service.*.*(..))")
public Object aroundServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
    String methodName = joinPoint.getSignature().getName();

    // ===== 메서드 실행 전 =====
    log.info("[AROUND-BEFORE] {}() 실행 시작", methodName);
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    try {
        // ===== 실제 메서드 실행 =====
        Object result = joinPoint.proceed();  // ⭐ 중요!

        stopWatch.stop();

        // ===== 메서드 정상 종료 후 =====
        log.info("[AROUND-AFTER-RETURNING] {}() 성공 ({}ms)",
            methodName, stopWatch.getTotalTimeMillis());

        return result;  // 반환값 전달

    } catch (Exception e) {
        stopWatch.stop();

        // ===== 메서드 예외 발생 후 =====
        log.error("[AROUND-AFTER-THROWING] {}() 실패 ({}ms): {}",
            methodName, stopWatch.getTotalTimeMillis(), e.getMessage());

        throw e;  // 예외 재발생
    }
}
```

**실행 흐름**:
```
1. @Around Advice 시작
2. joinPoint.proceed() 호출 → Target 메서드 실행
3. Target 메서드 완료
4. @Around Advice 종료
```

**주의사항**:
- ⚠️ **반드시 `joinPoint.proceed()` 호출**: 호출하지 않으면 실제 메서드가 실행되지 않음
- ⚠️ **반환값 전달 필수**: `proceed()`의 반환값을 그대로 반환해야 함
- ⚠️ **예외 처리**: 예외를 잡은 경우 다시 던져야 함 (또는 의도적으로 처리)

**사용 사례**:
- 성능 측정
- 트랜잭션 관리
- 캐싱
- 재시도 로직

---

## 🎯 Pointcut 표현식

### Pointcut 표현식 구조

```java
@Pointcut("execution(modifiers? return-type declaring-type? method-name(params) throws?)")
           └────────┘ └─────────┘ └──────────────┘ └────────────┘ └────┘ └──────┘
              ①           ②              ③               ④          ⑤       ⑥
```

#### ① modifiers (접근 제어자) - 생략 가능
- `public`, `protected`, `private`
- 생략 시 모든 접근 제어자

#### ② return-type (반환 타입) - 필수
- `void`, `String`, `User` 등 구체적 타입
- `*` : 모든 타입
- `!void` : void가 아닌 모든 타입

#### ③ declaring-type (선언 타입) - 생략 가능
- 패키지 + 클래스명
- `com.smartwork.service.UserService`
- `com.smartwork.service.*` : service 패키지의 모든 클래스
- `com.smartwork.service..*` : service 패키지 및 하위 패키지의 모든 클래스

#### ④ method-name (메서드 이름) - 필수
- `createUser`, `findById` 등 구체적 메서드명
- `find*` : find로 시작하는 모든 메서드
- `*` : 모든 메서드

#### ⑤ params (파라미터) - 필수
- `()` : 파라미터 없음
- `(..)` : 모든 파라미터 (0개 이상)
- `(*)` : 파라미터 1개 (타입 무관)
- `(String)` : String 타입 파라미터 1개
- `(String, *)` : String 파라미터 1개 + 아무 타입 1개
- `(String, ..)` : String 파라미터 1개 + 추가 파라미터 0개 이상

#### ⑥ throws (예외) - 생략 가능
- 던지는 예외 타입 지정 (거의 사용 안 함)

---

### Pointcut 표현식 예시

#### 1. 모든 public 메서드

```java
@Pointcut("execution(public * *(..))")
```

#### 2. 특정 패키지의 모든 메서드

```java
// service 패키지의 모든 메서드
@Pointcut("execution(* com.smartwork.service.*.*(..))")

// service 패키지 및 하위 패키지의 모든 메서드
@Pointcut("execution(* com.smartwork.service..*.*(..))")
```

#### 3. 특정 클래스의 모든 메서드

```java
@Pointcut("execution(* com.smartwork.service.UserService.*(..))")
```

#### 4. 특정 메서드명 패턴

```java
// find로 시작하는 모든 메서드
@Pointcut("execution(* com.smartwork.service.*.find*(..))")

// create 또는 update로 시작하는 메서드
@Pointcut("execution(* com.smartwork.service.*.*(..))" +
          " && (execution(* create*(..)) || execution(* update*(..)))")
```

#### 5. 특정 반환 타입

```java
// User 타입을 반환하는 모든 메서드
@Pointcut("execution(com.smartwork.domain.User *(..))")

// void가 아닌 모든 메서드
@Pointcut("execution(!void *(..))")
```

#### 6. 특정 파라미터

```java
// 파라미터가 없는 메서드
@Pointcut("execution(* com.smartwork.service.*.*())")

// Long 타입 파라미터 1개를 받는 메서드
@Pointcut("execution(* com.smartwork.service.*.*(Long))")

// String으로 시작하고 추가 파라미터가 있을 수 있는 메서드
@Pointcut("execution(* com.smartwork.service.*.*(String, ..))")
```

---

### Pointcut 조합

**논리 연산자**로 Pointcut을 조합할 수 있습니다.

```java
// && (AND): 두 조건 모두 만족
@Pointcut("execution(* com.smartwork.service.*.*(..)) " +
          "&& !execution(* com.smartwork.service.*.find*(..))")
public void nonReadServiceMethods() {}
// service 패키지의 메서드 중 find로 시작하지 않는 메서드

// || (OR): 둘 중 하나 만족
@Pointcut("execution(* com.smartwork.service.*.create*(..)) " +
          "|| execution(* com.smartwork.service.*.update*(..))")
public void createOrUpdateMethods() {}

// ! (NOT): 조건을 만족하지 않는 경우
@Pointcut("execution(* com.smartwork.service.*.*(..)) " +
          "&& !execution(* com.smartwork.service.*.find*(..))")
public void modifyingServiceMethods() {}
```

---

### 애노테이션 기반 Pointcut

**특정 애노테이션이 붙은 메서드에만 적용**

```java
// @Transactional이 붙은 모든 메서드
@Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
public void transactionalMethods() {}

// @Async가 붙은 모든 메서드
@Pointcut("@annotation(org.springframework.scheduling.annotation.Async)")
public void asyncMethods() {}

// 커스텀 애노테이션
@Pointcut("@annotation(com.smartwork.aop.annotation.LogExecutionTime)")
public void logExecutionTime() {}
```

**특정 애노테이션이 붙은 클래스의 모든 메서드**

```java
// @Service가 붙은 클래스의 모든 메서드
@Pointcut("@within(org.springframework.stereotype.Service)")
public void serviceBeans() {}

// @RestController가 붙은 클래스의 모든 메서드
@Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
public void restControllers() {}
```

---

## 🏗 프로젝트 AOP 구현

### 1. LoggingAspect - 로깅 및 성능 측정

**파일 위치**: `src/main/java/com/smartwork/aop/LoggingAspect.java`

```java
package com.smartwork.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

/**
 * 로깅 및 성능 측정 Aspect
 *
 * 모든 계층(Controller, Service, Repository)의 메서드 실행을 자동으로 로깅하고,
 * 성능 측정을 수행하여 느린 메서드를 탐지합니다.
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    // ==================== Pointcut 정의 ====================

    /**
     * Controller 계층의 모든 public 메서드
     */
    @Pointcut("execution(public * com.smartwork.controller..*.*(..))")
    public void controllerMethods() {}

    /**
     * Service 계층의 모든 public 메서드
     */
    @Pointcut("execution(public * com.smartwork.service..*.*(..))")
    public void serviceMethods() {}

    /**
     * Repository 계층의 모든 메서드
     */
    @Pointcut("execution(* com.smartwork.repository..*.*(..))")
    public void repositoryMethods() {}

    // ==================== Advice 구현 ====================

    /**
     * Controller 메서드 로깅
     *
     * - 요청 시작/종료 로그
     * - 파라미터 로깅
     * - 실행 시간 측정
     */
    @Around("controllerMethods()")
    public Object logControllerExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("[CONTROLLER] {}.{}() - REQUEST with args: {}",
                 className, methodName, args);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();

            log.info("[CONTROLLER] {}.{}() - SUCCESS in {}ms",
                     className, methodName, stopWatch.getTotalTimeMillis());

            return result;

        } catch (Exception e) {
            stopWatch.stop();
            log.error("[CONTROLLER] {}.{}() - FAILED in {}ms: {}",
                      className, methodName, stopWatch.getTotalTimeMillis(),
                      e.getMessage());
            throw e;
        }
    }

    /**
     * Service 메서드 로깅 및 성능 측정
     *
     * - 비즈니스 로직 실행 추적
     * - 느린 메서드 경고 (3초 이상)
     */
    @Around("serviceMethods()")
    public Object logServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.debug("[SERVICE] {}.{}() - ENTER", className, methodName);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();

            log.info("[SERVICE] {}.{}() - SUCCESS in {}ms",
                     className, methodName, stopWatch.getTotalTimeMillis());

            // 느린 메서드 경고 (3초 이상)
            if (stopWatch.getTotalTimeMillis() > 3000) {
                log.warn("⚠️ SLOW METHOD DETECTED: {}.{}() took {}ms",
                         className, methodName, stopWatch.getTotalTimeMillis());
            }

            return result;

        } catch (Exception e) {
            stopWatch.stop();
            log.error("[SERVICE] {}.{}() - FAILED in {}ms: {}",
                      className, methodName, stopWatch.getTotalTimeMillis(),
                      e.getMessage());
            throw e;
        }
    }

    /**
     * Repository 메서드 로깅
     *
     * - 데이터베이스 접근 로깅
     * - 쿼리 성능 추적
     */
    @Around("repositoryMethods()")
    public Object logRepositoryExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.debug("[REPOSITORY] {}.{}() - DB ACCESS", className, methodName);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            Object result = joinPoint.proceed();
            stopWatch.stop();

            log.debug("[REPOSITORY] {}.{}() - COMPLETED in {}ms",
                      className, methodName, stopWatch.getTotalTimeMillis());

            // 느린 쿼리 경고 (1초 이상)
            if (stopWatch.getTotalTimeMillis() > 1000) {
                log.warn("⚠️ SLOW QUERY: {}.{}() took {}ms",
                         className, methodName, stopWatch.getTotalTimeMillis());
            }

            return result;

        } catch (Exception e) {
            stopWatch.stop();
            log.error("[REPOSITORY] {}.{}() - FAILED in {}ms: {}",
                      className, methodName, stopWatch.getTotalTimeMillis(),
                      e.getMessage());
            throw e;
        }
    }
}
```

---

### 2. TransactionAspect - 트랜잭션 모니터링

**파일 위치**: `src/main/java/com/smartwork/aop/TransactionAspect.java`

```java
package com.smartwork.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 트랜잭션 모니터링 Aspect
 *
 * @Transactional이 붙은 메서드의 트랜잭션 시작/커밋/롤백을 로깅하여
 * 디버깅 및 모니터링을 지원합니다.
 */
@Slf4j
@Aspect
@Component
public class TransactionAspect {

    /**
     * @Transactional이 붙은 모든 메서드
     */
    @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void transactionalMethods() {}

    /**
     * 트랜잭션 시작/종료 로깅
     *
     * - 트랜잭션 시작 로그
     * - 커밋/롤백 로그
     * - 트랜잭션 설정 정보 (ReadOnly, Isolation, Propagation)
     */
    @Around("transactionalMethods() && @annotation(transactional)")
    public Object logTransaction(ProceedingJoinPoint joinPoint,
                                  Transactional transactional) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        // 트랜잭션 설정 정보
        boolean readOnly = transactional.readOnly();
        String isolation = transactional.isolation().name();
        String propagation = transactional.propagation().name();

        log.info("┌─────────────────────────────────────────────────────");
        log.info("│ [TRANSACTION START] {}.{}()", className, methodName);
        log.info("│ - ReadOnly: {}", readOnly);
        log.info("│ - Isolation: {}", isolation);
        log.info("│ - Propagation: {}", propagation);
        log.info("└─────────────────────────────────────────────────────");

        try {
            Object result = joinPoint.proceed();

            log.info("✅ [TRANSACTION COMMIT] {}.{}() - SUCCESS",
                     className, methodName);

            return result;

        } catch (Exception e) {
            log.error("❌ [TRANSACTION ROLLBACK] {}.{}() - FAILED: {}",
                      className, methodName, e.getMessage());
            throw e;
        }
    }
}
```

---

### 실행 예시 로그

**Service 메서드 실행 시**:

```
2025-10-26 14:30:00.123 DEBUG [SERVICE] UserService.createUser() - ENTER
2025-10-26 14:30:00.125 DEBUG [REPOSITORY] UserRepository.save() - DB ACCESS
2025-10-26 14:30:00.156 DEBUG [REPOSITORY] UserRepository.save() - COMPLETED in 31ms
2025-10-26 14:30:00.157 INFO  [SERVICE] UserService.createUser() - SUCCESS in 34ms
```

**트랜잭션 메서드 실행 시**:

```
2025-10-26 14:30:00.100 INFO  ┌─────────────────────────────────────────────────────
2025-10-26 14:30:00.101 INFO  │ [TRANSACTION START] UserService.createUser()
2025-10-26 14:30:00.102 INFO  │ - ReadOnly: false
2025-10-26 14:30:00.103 INFO  │ - Isolation: DEFAULT
2025-10-26 14:30:00.104 INFO  │ - Propagation: REQUIRED
2025-10-26 14:30:00.105 INFO  └─────────────────────────────────────────────────────
2025-10-26 14:30:00.250 INFO  ✅ [TRANSACTION COMMIT] UserService.createUser() - SUCCESS
```

**느린 메서드 경고**:

```
2025-10-26 14:35:12.456 WARN  ⚠️ SLOW METHOD DETECTED: ApprovalService.processApproval() took 3245ms
```

---

## 🎓 AOP 베스트 프랙티스

### 1. Pointcut 재사용

**❌ 나쁜 예**:
```java
@Around("execution(* com.smartwork.service.*.*(..))")
public Object log1(ProceedingJoinPoint joinPoint) { ... }

@Around("execution(* com.smartwork.service.*.*(..))")
public Object log2(ProceedingJoinPoint joinPoint) { ... }
```

**✅ 좋은 예**:
```java
@Pointcut("execution(* com.smartwork.service.*.*(..))")
public void serviceMethods() {}

@Around("serviceMethods()")
public Object log1(ProceedingJoinPoint joinPoint) { ... }

@Around("serviceMethods()")
public Object log2(ProceedingJoinPoint joinPoint) { ... }
```

---

### 2. @Around 사용 시 주의사항

**❌ 위험**:
```java
@Around("serviceMethods()")
public Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
    log.info("Before");
    // joinPoint.proceed() 호출 누락! ← 실제 메서드가 실행 안 됨
    log.info("After");
    return null;  // 항상 null 반환
}
```

**✅ 올바른 사용**:
```java
@Around("serviceMethods()")
public Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
    log.info("Before");
    Object result = joinPoint.proceed();  // ✅ 반드시 호출
    log.info("After");
    return result;  // ✅ 반환값 전달
}
```

---

### 3. 예외 처리

**❌ 예외 삼키기**:
```java
@Around("serviceMethods()")
public Object aroundAdvice(ProceedingJoinPoint joinPoint) {
    try {
        return joinPoint.proceed();
    } catch (Throwable e) {
        log.error("Error", e);
        return null;  // ❌ 예외를 숨김
    }
}
```

**✅ 예외 재발생**:
```java
@Around("serviceMethods()")
public Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
    try {
        return joinPoint.proceed();
    } catch (Throwable e) {
        log.error("Error", e);
        throw e;  // ✅ 예외를 다시 던짐
    }
}
```

---

### 4. 성능 고려

**과도한 Pointcut 범위는 성능 저하**:

```java
// ❌ 너무 광범위
@Pointcut("execution(* *.*(..))")  // 모든 메서드!

// ✅ 적절한 범위
@Pointcut("execution(* com.smartwork.service.*.*(..))")
```

---

### 5. 로깅 레벨 적절히 사용

```java
@Around("serviceMethods()")
public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {
    // ✅ 상세 로그는 DEBUG
    log.debug("[SERVICE] {} - ENTER", joinPoint.getSignature().getName());

    Object result = joinPoint.proceed();

    // ✅ 중요 정보는 INFO
    log.info("[SERVICE] {} - SUCCESS", joinPoint.getSignature().getName());

    return result;
}
```

---

## 📊 AOP vs 기존 방식 비교

### 코드 중복 제거 효과

**Before AOP**:
- 10개 Service 메서드 × 10줄 로깅 코드 = **100줄**
- 1개 Service 추가 시 → 10줄 추가

**After AOP**:
- 1개 Aspect × 30줄 = **30줄**
- 1개 Service 추가 시 → 0줄 추가 (자동 적용)

**코드 감소율**: **70% 감소**

---

### 유지보수성 향상

**Before AOP** - 로깅 정책 변경 시:
```
10개 Service 파일을 모두 수정
→ 누락 가능성 높음
→ 일관성 보장 어려움
```

**After AOP** - 로깅 정책 변경 시:
```
1개 Aspect 파일만 수정
→ 누락 불가능
→ 일관성 자동 보장
```

---

## 🔗 관련 문서

- **[README.md](./README.md)** - 프로젝트 전체 개요
- **[JWT.md](./JWT.md)** - JWT 인증 흐름 상세
- **Spring AOP 공식 문서**: https://docs.spring.io/spring-framework/reference/core/aop.html
- **AspectJ 공식 문서**: https://www.eclipse.org/aspectj/doc/released/progguide/index.html

---

**작성일**: 2025-10-26
**작성자**: Hyoukjoo Lee
**프로젝트**: SmartWork - Enterprise Intranet System
