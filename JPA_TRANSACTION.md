# JPA & Transaction 생명주기 상세 가이드

> JPA 엔티티 생명주기와 Spring Transaction 관리 완벽 가이드

---

## 📋 목차

1. [JPA 개요](#-jpa-개요)
2. [JPA 엔티티 생명주기](#-jpa-엔티티-생명주기)
3. [영속성 컨텍스트](#-영속성-컨텍스트)
4. [Transaction 개요](#-transaction-개요)
5. [Transaction 생명주기](#-transaction-생명주기)
6. [Transaction 속성](#-transaction-속성)
7. [JPA와 Transaction의 관계](#-jpa와-transaction의-관계)
8. [프로젝트 실전 예시](#-프로젝트-실전-예시)
9. [베스트 프랙티스](#-베스트-프랙티스)
10. [트러블슈팅](#-트러블슈팅)

---

## 🎯 JPA 개요

### JPA (Java Persistence API)란?

**JPA**는 자바 객체와 관계형 데이터베이스 간의 매핑을 관리하는 표준 ORM(Object-Relational Mapping) 명세입니다.

```
Java 객체 (Entity)  ⟷  JPA/Hibernate  ⟷  관계형 데이터베이스 (Oracle)
     User                                      USERS 테이블
```

### 주요 구성 요소

```
┌─────────────────────────────────────────────┐
│         Application (Service Layer)         │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│           EntityManager (JPA API)           │
│         영속성 컨텍스트 (Persistence Context) │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│          Hibernate (JPA 구현체)              │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│            JDBC / Database                  │
└─────────────────────────────────────────────┘
```

---

## 🔄 JPA 엔티티 생명주기

JPA 엔티티는 **4가지 상태**를 가집니다.

```
        new          persist()        flush()         remove()
    ┌────────┐     ┌──────────┐     ┌────────┐     ┌─────────┐
    │  New   │ ──→ │ Managed  │ ──→ │   DB   │ ←── │ Removed │
    │(비영속) │     │  (영속)   │     │        │     │ (삭제)   │
    └────────┘     └──────────┘     └────────┘     └─────────┘
                        │                                │
                        │ detach() / clear()             │
                        ↓                                │
                   ┌──────────┐                         │
                   │ Detached │ ←───────────────────────┘
                   │ (준영속)  │
                   └──────────┘
                        │
                        │ merge()
                        ↓
                   ┌──────────┐
                   │ Managed  │
                   │  (영속)   │
                   └──────────┘
```

---

### 1. New (비영속) 상태

**객체를 생성했지만, 아직 영속성 컨텍스트에 저장되지 않은 상태**

```java
// 새로운 엔티티 객체 생성
User user = new User();
user.setUsername("hyoukjoo");
user.setEmail("hyoukjoo@example.com");
// ← New(비영속) 상태: JPA가 관리하지 않음
```

**특징**:
- ✅ 순수한 자바 객체
- ❌ JPA가 관리하지 않음
- ❌ 변경 감지(Dirty Checking) 동작 안 함
- ❌ 데이터베이스와 연결 없음

---

### 2. Managed (영속) 상태

**영속성 컨텍스트에 저장되어 JPA가 관리하는 상태**

```java
// EntityManager를 통해 영속 상태로 만들기
entityManager.persist(user);  // ← Managed(영속) 상태로 전환
// 또는
User user = entityManager.find(User.class, 1L);  // ← 조회 시 자동으로 영속 상태
```

**특징**:
- ✅ **JPA가 관리**: 엔티티의 모든 변경 사항을 추적
- ✅ **변경 감지(Dirty Checking)**: 필드 변경 시 자동으로 UPDATE 쿼리 생성
- ✅ **1차 캐시**: 동일한 엔티티 조회 시 캐시에서 반환
- ✅ **쓰기 지연(Write-Behind)**: SQL을 모았다가 flush() 시점에 일괄 실행

**영속 상태 진입 방법**:

```java
// 1. persist() - 새로운 엔티티 저장
User newUser = new User("hyoukjoo", "hyoukjoo@example.com");
entityManager.persist(newUser);

// 2. find() - 데이터베이스에서 조회
User foundUser = entityManager.find(User.class, 1L);

// 3. JPQL 쿼리 실행
List<User> users = entityManager
    .createQuery("SELECT u FROM User u", User.class)
    .getResultList();

// 4. merge() - 준영속 엔티티를 영속 상태로 병합
User mergedUser = entityManager.merge(detachedUser);
```

---

### 3. Detached (준영속) 상태

**영속성 컨텍스트에서 분리된 상태**

```java
// 영속 상태의 엔티티
User user = entityManager.find(User.class, 1L);  // Managed

// 준영속 상태로 전환
entityManager.detach(user);  // ← Detached(준영속) 상태
// 또는
entityManager.clear();  // 영속성 컨텍스트 초기화
// 또는
entityManager.close();  // EntityManager 종료
```

**특징**:
- ✅ 식별자(ID) 값을 가지고 있음
- ❌ JPA가 관리하지 않음
- ❌ 변경 감지 동작 안 함
- ❌ 지연 로딩(Lazy Loading) 불가능 → `LazyInitializationException` 발생 가능

**준영속 상태 발생 시나리오**:

```java
@Service
@Transactional
public class UserService {

    public User getUser(Long id) {
        User user = userRepository.findById(id).orElseThrow();
        return user;  // ← 트랜잭션 종료 후 반환 시 Detached 상태
    }
}

// Controller에서
User user = userService.getUser(1L);  // ← Detached 상태
user.getRoles().size();  // ❌ LazyInitializationException 발생 가능!
```

---

### 4. Removed (삭제) 상태

**영속성 컨텍스트에서 삭제가 예약된 상태**

```java
User user = entityManager.find(User.class, 1L);  // Managed
entityManager.remove(user);  // ← Removed(삭제) 상태
// flush() 시점에 DELETE 쿼리 실행
```

**특징**:
- ✅ 영속성 컨텍스트에서 관리됨 (삭제 예약)
- ✅ flush() 호출 시 DELETE 쿼리 실행
- ❌ 커밋 후에는 데이터베이스에서 삭제됨

---

## 💾 영속성 컨텍스트

### 영속성 컨텍스트란?

**엔티티를 영구 저장하는 환경** - JPA의 핵심 개념

```
┌────────────────────────────────────────────────┐
│         Persistence Context (영속성 컨텍스트)    │
│                                                │
│  ┌──────────────┐      ┌──────────────┐       │
│  │  1차 캐시     │      │  쓰기 지연     │       │
│  │ (Identity    │      │  SQL 저장소   │       │
│  │   Map)       │      │              │       │
│  └──────────────┘      └──────────────┘       │
│                                                │
│  ┌──────────────────────────────────┐         │
│  │       변경 감지 (Dirty Checking)   │         │
│  │   스냅샷 비교 → UPDATE 쿼리 생성    │         │
│  └──────────────────────────────────┘         │
└────────────────────────────────────────────────┘
```

---

### 1차 캐시 (First Level Cache)

**영속성 컨텍스트 내부의 Map 구조 캐시**

```java
// 1차 캐시 동작 예시
User user1 = entityManager.find(User.class, 1L);
// SELECT 쿼리 실행 → DB 조회 → 1차 캐시 저장

User user2 = entityManager.find(User.class, 1L);
// DB 조회 없이 1차 캐시에서 반환 (같은 객체)

System.out.println(user1 == user2);  // true (동일성 보장)
```

**1차 캐시 구조**:

```
Key: @Id 값         Value: Entity Instance
─────────────────────────────────────────
1L               →  User@123 (username="hyoukjoo")
2L               →  User@456 (username="john")
3L               →  User@789 (username="jane")
```

**장점**:
- ✅ **성능 향상**: 같은 엔티티 조회 시 DB 접근 없음
- ✅ **동일성 보장**: 같은 트랜잭션 내에서 `==` 비교 가능
- ✅ **변경 감지 기반**: 스냅샷 비교의 기준

**주의사항**:
- ⚠️ 트랜잭션 범위 내에서만 유효
- ⚠️ 애플리케이션 전체 캐시 아님 (2차 캐시와 구분)

---

### 쓰기 지연 (Write-Behind)

**SQL을 모았다가 flush() 시점에 일괄 실행**

```java
// 쓰기 지연 동작 예시
entityManager.persist(user1);  // INSERT SQL을 쓰기 지연 저장소에 저장
entityManager.persist(user2);  // INSERT SQL을 쓰기 지연 저장소에 저장
entityManager.persist(user3);  // INSERT SQL을 쓰기 지연 저장소에 저장

// 아직 DB에 INSERT 쿼리 실행 안 됨!

transaction.commit();  // ← 이 시점에 flush() 자동 호출
// 3개의 INSERT 쿼리를 한 번에 실행 (배치 가능)
```

**쓰기 지연 SQL 저장소**:

```
┌─────────────────────────────────────────┐
│      Write-Behind SQL Storage           │
├─────────────────────────────────────────┤
│ 1. INSERT INTO users VALUES (...)       │
│ 2. INSERT INTO users VALUES (...)       │
│ 3. UPDATE users SET ... WHERE id = 1    │
│ 4. DELETE FROM users WHERE id = 5       │
└─────────────────────────────────────────┘
         ↓ flush() 호출 시
┌─────────────────────────────────────────┐
│          Database                       │
└─────────────────────────────────────────┘
```

**장점**:
- ✅ **성능 최적화**: JDBC Batch 사용 가능
- ✅ **트랜잭션 일관성**: 모든 변경을 한 번에 커밋

---

### 변경 감지 (Dirty Checking)

**JPA가 자동으로 변경된 엔티티를 감지하여 UPDATE 쿼리 생성**

```java
@Transactional
public void updateUser(Long userId, String newEmail) {
    // 1. 엔티티 조회 (영속 상태)
    User user = userRepository.findById(userId).orElseThrow();
    // ← 이 시점에 스냅샷 저장 (최초 조회 상태)

    // 2. 엔티티 수정
    user.setEmail(newEmail);  // setter 호출
    // ← UPDATE 쿼리를 직접 호출하지 않음!

    // 3. 트랜잭션 커밋 시점에 자동으로 UPDATE 실행
    // userRepository.save(user) 불필요!
}
```

**변경 감지 동작 원리**:

```
1. 조회 시점
   ┌──────────────┐
   │ 최초 스냅샷   │  ← user.email = "old@example.com"
   └──────────────┘

2. 수정
   ┌──────────────┐
   │ 현재 엔티티   │  ← user.email = "new@example.com"
   └──────────────┘

3. flush() 시점
   스냅샷과 현재 엔티티 비교
   → 변경 감지!
   → UPDATE 쿼리 자동 생성

4. UPDATE users SET email = 'new@example.com' WHERE user_id = 1;
```

**변경 감지가 동작하는 조건**:
- ✅ 엔티티가 **영속(Managed) 상태**여야 함
- ✅ **트랜잭션 내부**에서 실행되어야 함
- ✅ flush() 시점에 스냅샷과 비교

---

### flush() - 영속성 컨텍스트 동기화

**영속성 컨텍스트의 변경 내용을 데이터베이스에 반영**

```java
entityManager.flush();  // 즉시 DB에 반영
```

**flush() 호출 시점**:

1. **`entityManager.flush()` 직접 호출**
```java
User user = new User("hyoukjoo", "email@example.com");
entityManager.persist(user);
entityManager.flush();  // 즉시 DB에 INSERT
```

2. **트랜잭션 커밋 시 자동 호출**
```java
@Transactional
public void createUser() {
    User user = new User("hyoukjoo", "email@example.com");
    userRepository.save(user);
    // 메서드 종료 시 트랜잭션 커밋 → 자동 flush()
}
```

3. **JPQL 쿼리 실행 직전 자동 호출**
```java
entityManager.persist(user);  // 아직 DB에 없음

// JPQL 실행 직전에 자동 flush()
List<User> users = entityManager
    .createQuery("SELECT u FROM User u", User.class)
    .getResultList();
// ← user가 쿼리 결과에 포함되어야 하므로 자동 flush()
```

**flush()와 commit()의 차이**:

```
flush()  : 영속성 컨텍스트 → DB 반영 (트랜잭션 유지)
commit() : flush() + 트랜잭션 커밋 (최종 확정)
```

---

## 🔐 Transaction 개요

### Transaction이란?

**데이터베이스 작업의 논리적 단위로, ACID 특성을 보장**

```
┌─────────────────────────────────────────┐
│           Transaction                   │
│  ┌──────────────────────────────────┐   │
│  │ 1. 사용자 생성                     │   │
│  │ 2. 역할 할당                       │   │
│  │ 3. 권한 부여                       │   │
│  └──────────────────────────────────┘   │
│                                         │
│  → 모두 성공 시 COMMIT ✅               │
│  → 하나라도 실패 시 ROLLBACK ❌         │
└─────────────────────────────────────────┘
```

### ACID 특성

| 특성 | 설명 | 예시 |
|-----|------|------|
| **Atomicity (원자성)** | All or Nothing | 계좌 이체: 출금과 입금 모두 성공 또는 모두 실패 |
| **Consistency (일관성)** | 정합성 유지 | 계좌 잔액은 항상 0 이상 |
| **Isolation (격리성)** | 동시 실행 트랜잭션 간 독립성 | A의 이체가 B의 조회에 영향 안 줌 |
| **Durability (지속성)** | 커밋 후 영구 저장 | 시스템 장애 후에도 데이터 보존 |

---

## ⏱ Transaction 생명주기

### Spring @Transactional 동작 원리

**AOP 프록시 기반으로 트랜잭션 경계 관리**

```
1. 클라이언트
      ↓
2. @Transactional 메서드 호출
      ↓
3. Spring AOP Proxy 가로채기
      ↓
4. TransactionInterceptor
   ┌──────────────────────────┐
   │ ① 트랜잭션 시작           │
   │    connection.setAutoCommit(false)
   └──────────────────────────┘
      ↓
5. 실제 메서드 실행
   ┌──────────────────────────┐
   │ ② 비즈니스 로직 실행      │
   │    userRepository.save()  │
   └──────────────────────────┘
      ↓
6. 성공 시
   ┌──────────────────────────┐
   │ ③ 트랜잭션 커밋           │
   │    connection.commit()    │
   └──────────────────────────┘
      ↓
   반환

   실패 시
   ┌──────────────────────────┐
   │ ④ 트랜잭션 롤백           │
   │    connection.rollback()  │
   └──────────────────────────┘
      ↓
   예외 던짐
```

---

### Transaction 생명주기 상세

```java
@Service
public class UserService {

    @Transactional  // ← 트랜잭션 시작 지점
    public User createUser(UserDto dto) {
        // ═══════════════════════════════════════
        // ① Transaction Begin
        // - connection.setAutoCommit(false)
        // - EntityManager 생성
        // - 영속성 컨텍스트 생성
        // ═══════════════════════════════════════

        User user = User.builder()
            .username(dto.getUsername())
            .email(dto.getEmail())
            .build();

        // ═══════════════════════════════════════
        // ② 엔티티를 영속 상태로 전환
        // - persist() 호출
        // - 1차 캐시에 저장
        // - INSERT SQL을 쓰기 지연 저장소에 등록
        // ═══════════════════════════════════════
        User savedUser = userRepository.save(user);

        Role role = roleRepository.findByName("USER")
            .orElseThrow();

        savedUser.addRole(role);

        // ═══════════════════════════════════════
        // ③ 변경 감지 (Dirty Checking)
        // - flush() 호출 전 스냅샷과 비교
        // - UPDATE SQL 자동 생성
        // ═══════════════════════════════════════

        return savedUser;

        // ═══════════════════════════════════════
        // ④ Transaction Commit (메서드 정상 종료 시)
        // - flush() 자동 호출
        //   → 쓰기 지연 SQL 저장소의 모든 SQL 실행
        //   → INSERT users ...
        //   → UPDATE users ...
        // - connection.commit()
        // - 영속성 컨텍스트 종료
        // - EntityManager 종료
        // ═══════════════════════════════════════

    }  // ← 트랜잭션 종료 지점
}
```

**예외 발생 시 롤백**:

```java
@Transactional
public User createUser(UserDto dto) {
    User user = new User(dto.getUsername(), dto.getEmail());
    userRepository.save(user);

    if (user.getEmail().contains("invalid")) {
        throw new BusinessException("Invalid email");
        // ═══════════════════════════════════════
        // ⑤ Transaction Rollback (예외 발생 시)
        // - flush() 호출 안 함
        // - connection.rollback()
        // - 모든 변경 사항 취소
        // - 영속성 컨텍스트 종료
        // ═══════════════════════════════════════
    }

    return user;
}
```

---

## ⚙️ Transaction 속성

### 1. Propagation (전파 속성)

**트랜잭션이 이미 진행 중일 때 새로운 트랜잭션을 어떻게 처리할지 정의**

```java
@Transactional(propagation = Propagation.REQUIRED)
```

#### REQUIRED (기본값)

**기존 트랜잭션이 있으면 참여, 없으면 새로 시작**

```java
@Transactional  // propagation = REQUIRED (기본값)
public void outerMethod() {
    // ────── Transaction A 시작 ──────

    innerMethod();  // Transaction A에 참여

    // ────── Transaction A 커밋 ──────
}

@Transactional  // propagation = REQUIRED
public void innerMethod() {
    // 기존 Transaction A 사용
    // 새로운 트랜잭션 생성 안 함
}
```

**동작 흐름**:
```
outerMethod() 호출
  ↓
Transaction A 시작
  ↓
innerMethod() 호출
  ↓
Transaction A 사용 (재사용)
  ↓
innerMethod() 종료
  ↓
outerMethod() 종료
  ↓
Transaction A 커밋
```

---

#### REQUIRES_NEW

**항상 새로운 트랜잭션 시작 (기존 트랜잭션 일시 중단)**

```java
@Transactional
public void outerMethod() {
    // ────── Transaction A 시작 ──────

    userRepository.save(user1);  // Transaction A

    innerMethod();  // Transaction B (새로운 트랜잭션)

    userRepository.save(user2);  // Transaction A

    // ────── Transaction A 커밋 ──────
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void innerMethod() {
    // ────── Transaction B 시작 ──────
    logRepository.save(log);  // Transaction B
    // ────── Transaction B 커밋 (독립적) ──────
}
```

**사용 사례**:
```java
@Transactional
public void processPayment(Payment payment) {
    // Transaction A
    paymentRepository.save(payment);

    // 감사 로그는 결제 실패와 관계없이 저장되어야 함
    auditService.saveAuditLog(payment);  // Transaction B (REQUIRES_NEW)

    if (payment.getAmount() > 1000000) {
        throw new PaymentException("금액 초과");
        // Transaction A만 롤백
        // Transaction B(감사 로그)는 이미 커밋됨
    }
}
```

---

#### MANDATORY

**기존 트랜잭션이 반드시 있어야 함 (없으면 예외)**

```java
@Transactional(propagation = Propagation.MANDATORY)
public void mandatoryMethod() {
    // 트랜잭션 없이 호출 시
    // → IllegalTransactionStateException 발생
}
```

---

#### NEVER

**트랜잭션 없이 실행 (트랜잭션 있으면 예외)**

```java
@Transactional(propagation = Propagation.NEVER)
public void neverMethod() {
    // 트랜잭션 내에서 호출 시
    // → IllegalTransactionStateException 발생
}
```

---

#### SUPPORTS

**트랜잭션이 있으면 참여, 없으면 없이 실행**

```java
@Transactional(propagation = Propagation.SUPPORTS)
public void supportsMethod() {
    // 트랜잭션 있으면 → 참여
    // 트랜잭션 없으면 → 트랜잭션 없이 실행
}
```

---

#### NOT_SUPPORTED

**트랜잭션 없이 실행 (기존 트랜잭션 일시 중단)**

```java
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public void notSupportedMethod() {
    // 기존 트랜잭션 일시 중단
    // 트랜잭션 없이 실행
}
```

---

#### NESTED

**중첩 트랜잭션 (부모 트랜잭션 내에 세이브포인트)**

```java
@Transactional
public void outerMethod() {
    userRepository.save(user);

    try {
        nestedMethod();  // 중첩 트랜잭션
    } catch (Exception e) {
        // nestedMethod()만 롤백
        // outerMethod()는 계속 진행
    }
}

@Transactional(propagation = Propagation.NESTED)
public void nestedMethod() {
    logRepository.save(log);
}
```

---

### 2. Isolation (격리 수준)

**동시에 실행되는 트랜잭션 간의 격리 수준**

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
```

#### Isolation Level 비교

| 격리 수준 | Dirty Read | Non-Repeatable Read | Phantom Read | 설명 |
|---------|-----------|---------------------|--------------|------|
| **READ_UNCOMMITTED** | O | O | O | 커밋 안 된 데이터 읽기 가능 (거의 사용 안 함) |
| **READ_COMMITTED** | X | O | O | 커밋된 데이터만 읽기 (Oracle 기본값) |
| **REPEATABLE_READ** | X | X | O | 같은 데이터 반복 조회 시 동일 결과 (MySQL 기본값) |
| **SERIALIZABLE** | X | X | X | 가장 엄격한 격리 (성능 저하) |

---

#### Dirty Read (더티 리드)

**커밋되지 않은 데이터를 읽는 문제**

```
Time  Transaction A                Transaction B
─────────────────────────────────────────────────
t1    BEGIN
t2    UPDATE users
      SET balance = 1000
      WHERE id = 1
      (아직 커밋 안 함)
t3                                 BEGIN
t4                                 SELECT balance FROM users
                                   WHERE id = 1
                                   → 1000 (커밋 안 된 데이터!)
t5    ROLLBACK
      (balance = 500으로 복구)
t6                                 (잘못된 데이터 사용)
```

---

#### Non-Repeatable Read (반복 불가능한 읽기)

**같은 트랜잭션 내에서 같은 데이터를 다시 읽었을 때 다른 값이 조회되는 문제**

```
Time  Transaction A                Transaction B
─────────────────────────────────────────────────
t1    BEGIN
t2    SELECT balance FROM users
      WHERE id = 1
      → balance = 500
t3                                 BEGIN
t4                                 UPDATE users
                                   SET balance = 1000
                                   WHERE id = 1
t5                                 COMMIT
t6    SELECT balance FROM users
      WHERE id = 1
      → balance = 1000 (다른 값!)
```

---

#### Phantom Read (팬텀 리드)

**같은 조건으로 조회했을 때 행이 추가되거나 삭제되는 문제**

```
Time  Transaction A                Transaction B
─────────────────────────────────────────────────
t1    BEGIN
t2    SELECT COUNT(*) FROM users
      WHERE age >= 20
      → COUNT = 5
t3                                 BEGIN
t4                                 INSERT INTO users
                                   VALUES (6, 'new', 25)
t5                                 COMMIT
t6    SELECT COUNT(*) FROM users
      WHERE age >= 20
      → COUNT = 6 (팬텀 행!)
```

---

#### 실전 예시

```java
// 일반 조회 (READ_COMMITTED)
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public List<User> findAllUsers() {
    return userRepository.findAll();
}

// 중요한 금융 거래 (SERIALIZABLE)
@Transactional(isolation = Isolation.SERIALIZABLE)
public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
    Account from = accountRepository.findById(fromId).orElseThrow();
    Account to = accountRepository.findById(toId).orElseThrow();

    from.withdraw(amount);
    to.deposit(amount);

    accountRepository.save(from);
    accountRepository.save(to);
    // 동시성 문제 방지를 위한 직렬화 격리
}

// 통계 조회 (REPEATABLE_READ)
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public UserStatistics calculateStatistics() {
    int totalUsers = userRepository.count();
    int activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
    // 여러 번 조회해도 동일한 결과 보장
    return new UserStatistics(totalUsers, activeUsers);
}
```

---

### 3. ReadOnly (읽기 전용)

**트랜잭션을 읽기 전용으로 설정**

```java
@Transactional(readOnly = true)
public List<User> findAllUsers() {
    return userRepository.findAll();
}
```

**readOnly = true 효과**:
- ✅ **성능 최적화**: Hibernate가 스냅샷 비교 생략 (변경 감지 안 함)
- ✅ **데이터베이스 최적화**: DB 레벨에서 읽기 전용 최적화
- ✅ **실수 방지**: 의도하지 않은 데이터 수정 방지

```java
@Transactional(readOnly = true)
public void readOnlyMethod() {
    User user = userRepository.findById(1L).orElseThrow();

    user.setEmail("new@example.com");  // 변경 시도

    // readOnly = true이므로 UPDATE 쿼리 실행 안 됨
    // 변경 무시됨 (에러 안 남)
}
```

---

### 4. Timeout (시간 제한)

**트랜잭션 최대 실행 시간 (초 단위)**

```java
@Transactional(timeout = 5)  // 5초
public void longRunningMethod() {
    // 5초 이상 실행 시 TransactionTimedOutException 발생
}
```

---

### 5. RollbackFor (롤백 예외 지정)

**어떤 예외에서 롤백할지 지정**

```java
// Checked Exception에서도 롤백
@Transactional(rollbackFor = {Exception.class, IOException.class})
public void methodWithCheckedException() throws Exception {
    // Exception 발생 시 롤백
}

// 특정 예외는 롤백 안 함
@Transactional(noRollbackFor = {IllegalArgumentException.class})
public void methodWithNoRollback() {
    // IllegalArgumentException 발생 시 롤백 안 함
}
```

**기본 동작**:
- ✅ **RuntimeException (Unchecked)**: 자동 롤백
- ❌ **Exception (Checked)**: 롤백 안 함

```java
@Transactional
public void defaultBehavior() {
    // RuntimeException → 롤백 O
    throw new IllegalArgumentException("Error");
}

@Transactional
public void checkedExceptionBehavior() throws IOException {
    // IOException (Checked Exception) → 롤백 X
    throw new IOException("Error");
}
```

---

## 🔗 JPA와 Transaction의 관계

### JPA는 트랜잭션 없이 동작 불가능

**JPA의 모든 데이터 변경은 반드시 트랜잭션 내에서 수행되어야 함**

```java
// ❌ 트랜잭션 없이 데이터 변경 시도
public void createUserWithoutTransaction() {
    User user = new User("hyoukjoo", "email@example.com");
    userRepository.save(user);
    // → TransactionRequiredException 발생!
}

// ✅ 트랜잭션 내에서 데이터 변경
@Transactional
public void createUserWithTransaction() {
    User user = new User("hyoukjoo", "email@example.com");
    userRepository.save(user);
    // 정상 동작
}
```

---

### Transaction과 영속성 컨텍스트의 생명주기

**Spring에서는 트랜잭션과 영속성 컨텍스트의 생명주기가 같음**

```
Transaction 시작
    ↓
EntityManager 생성
    ↓
Persistence Context 생성
    ↓
비즈니스 로직 실행
    ↓
Transaction 커밋
    ↓
flush() 자동 호출
    ↓
Persistence Context 종료
    ↓
EntityManager 종료
```

**예시**:

```java
@Transactional  // ← Transaction & Persistence Context 시작
public void businessLogic() {
    // 영속성 컨텍스트 활성화
    User user = userRepository.findById(1L).orElseThrow();  // Managed
    user.setEmail("new@example.com");  // 변경 감지 동작

    // 트랜잭션 종료 시 flush() 자동 호출 → UPDATE 실행
}  // ← Transaction & Persistence Context 종료
```

---

### LazyInitializationException 이해

**준영속 상태에서 지연 로딩 시도 시 발생**

```java
// ❌ LazyInitializationException 발생
public User getUserWithRoles(Long id) {
    User user = userRepository.findById(id).orElseThrow();
    // 메서드 종료 → 트랜잭션 종료 → 영속성 컨텍스트 종료
    return user;  // ← Detached 상태
}

// Controller에서
User user = userService.getUserWithRoles(1L);
Set<Role> roles = user.getRoles();  // ❌ LazyInitializationException!
```

**해결 방법**:

#### 1. Fetch Join 사용
```java
@Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id")
Optional<User> findByIdWithRoles(@Param("id") Long id);
```

#### 2. @Transactional 범위 확장
```java
@Transactional(readOnly = true)
public User getUserWithRoles(Long id) {
    User user = userRepository.findById(id).orElseThrow();
    user.getRoles().size();  // 트랜잭션 내에서 강제 초기화
    return user;
}
```

#### 3. DTO 사용
```java
@Transactional(readOnly = true)
public UserDto getUserWithRoles(Long id) {
    User user = userRepository.findByIdWithRoles(id).orElseThrow();
    return UserDto.from(user);  // DTO 변환 (트랜잭션 내)
}
```

---

## 💼 프로젝트 실전 예시

### 1. 사용자 생성 (CRUD)

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * 사용자 생성
     *
     * Transaction 생명주기:
     * 1. @Transactional → Transaction 시작
     * 2. persist() → 영속 상태로 전환
     * 3. 메서드 종료 → flush() + commit()
     */
    @Transactional
    public User createUser(UserDto dto) {
        // ═══ Transaction & Persistence Context 시작 ═══

        // 1. 엔티티 생성 (New 상태)
        User user = User.builder()
            .username(dto.getUsername())
            .email(dto.getEmail())
            .password(passwordEncoder.encode(dto.getPassword()))
            .build();

        // 2. 영속 상태로 전환 (Managed)
        User savedUser = userRepository.save(user);
        // → persist() 호출
        // → INSERT SQL을 쓰기 지연 저장소에 등록

        // 3. 역할 할당
        Role userRole = roleRepository.findByName("USER")
            .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        savedUser.addRole(userRole);
        // → 변경 감지 대상으로 등록

        // ═══ Transaction 커밋 시점 ═══
        // - flush() 자동 호출
        // - INSERT INTO users ...
        // - INSERT INTO user_roles ...
        // - commit()
        // ═══ Persistence Context 종료 ═══

        return savedUser;
    }
}
```

---

### 2. 사용자 조회 (읽기 전용)

```java
/**
 * 사용자 조회 (읽기 전용)
 *
 * readOnly = true:
 * - 스냅샷 비교 생략 (성능 향상)
 * - 변경 감지 동작 안 함
 */
@Transactional(readOnly = true)
public User findById(Long id) {
    // ═══ Transaction (ReadOnly) 시작 ═══

    User user = userRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    // → SELECT 쿼리 실행
    // → 1차 캐시에 저장 (Managed 상태)

    // readOnly = true이므로 스냅샷 저장 안 함

    // ═══ Transaction 종료 (flush() 생략) ═══

    return user;
}

/**
 * 사용자 목록 조회 (Fetch Join)
 *
 * Fetch Join:
 * - N+1 문제 해결
 * - 한 번의 쿼리로 연관 엔티티까지 조회
 */
@Transactional(readOnly = true)
public List<User> findAllWithRoles() {
    // ═══ Transaction (ReadOnly) 시작 ═══

    List<User> users = userRepository.findAllWithRoles();
    // → SELECT u.*, r.* FROM users u
    //    LEFT JOIN user_roles ur ON u.user_id = ur.user_id
    //    LEFT JOIN roles r ON ur.role_id = r.role_id

    // N+1 문제 발생 안 함

    // ═══ Transaction 종료 ═══

    return users;
}
```

---

### 3. 사용자 수정 (변경 감지)

```java
/**
 * 사용자 정보 수정
 *
 * Dirty Checking:
 * 1. 엔티티 조회 (Managed 상태) → 스냅샷 저장
 * 2. setter 호출 → 엔티티 변경
 * 3. flush() 시점에 스냅샷과 비교
 * 4. 변경 감지 → UPDATE 쿼리 자동 생성
 */
@Transactional
public User updateUser(Long id, UserUpdateDto dto) {
    // ═══ Transaction 시작 ═══

    // 1. 조회 (Managed 상태)
    User user = userRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    // → SELECT 쿼리 실행
    // → 스냅샷 저장 (최초 상태)

    // 2. 엔티티 수정
    user.setEmail(dto.getEmail());
    user.setPhone(dto.getPhone());
    // → userRepository.save(user) 불필요!
    // → 변경 감지가 자동으로 UPDATE 생성

    // ═══ Transaction 커밋 시점 ═══
    // - flush() 자동 호출
    // - 스냅샷과 현재 엔티티 비교
    // - 변경 감지 → UPDATE users SET email = ?, phone = ? WHERE user_id = ?
    // - commit()
    // ═══ Persistence Context 종료 ═══

    return user;
}
```

---

### 4. 사용자 삭제 (Soft Delete)

```java
/**
 * 사용자 삭제 (Soft Delete)
 *
 * Soft Delete:
 * - 실제 DELETE 쿼리 실행 안 함
 * - is_deleted 플래그만 true로 변경
 * - 변경 감지를 통한 UPDATE 실행
 */
@Transactional
public void deleteUser(Long id) {
    // ═══ Transaction 시작 ═══

    User user = userRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    // Soft Delete
    user.softDelete();  // is_deleted = true 설정
    // → 변경 감지 대상으로 등록

    // ═══ Transaction 커밋 시점 ═══
    // - flush() 자동 호출
    // - UPDATE users SET is_deleted = 1, updated_at = ? WHERE user_id = ?
    // - commit()
    // ═══ Persistence Context 종료 ═══
}

/**
 * 사용자 영구 삭제 (Hard Delete)
 */
@Transactional
public void permanentlyDeleteUser(Long id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    // Hard Delete
    userRepository.delete(user);
    // → DELETE FROM users WHERE user_id = ?
}
```

---

### 5. 복잡한 비즈니스 로직 (여러 엔티티 수정)

```java
/**
 * 결재 승인 처리
 *
 * 여러 엔티티 수정:
 * 1. Approval 상태 변경
 * 2. ApprovalHistory 생성
 * 3. User lastApprovedAt 업데이트
 *
 * 모두 하나의 트랜잭션에서 처리
 */
@Transactional
public void approveApproval(Long approvalId, Long approverId) {
    // ═══ Transaction 시작 ═══

    // 1. 결재 조회
    Approval approval = approvalRepository.findById(approvalId)
        .orElseThrow(() -> new BusinessException(ErrorCode.APPROVAL_NOT_FOUND));

    // 2. 승인자 조회
    User approver = userRepository.findById(approverId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    // 3. 권한 체크
    if (!approval.canApprove(approver)) {
        throw new BusinessException(ErrorCode.NO_PERMISSION);
    }

    // 4. 결재 승인
    approval.approve(approver);
    // → 변경 감지: UPDATE approvals SET status = 'APPROVED', ...

    // 5. 히스토리 생성
    ApprovalHistory history = ApprovalHistory.builder()
        .approval(approval)
        .approver(approver)
        .action(ApprovalAction.APPROVE)
        .build();
    approvalHistoryRepository.save(history);
    // → INSERT INTO approval_history ...

    // 6. 승인자 정보 업데이트
    approver.updateLastApprovedAt(LocalDateTime.now());
    // → 변경 감지: UPDATE users SET last_approved_at = ...

    // ═══ Transaction 커밋 시점 ═══
    // - flush() 자동 호출
    // - UPDATE approvals ...
    // - INSERT INTO approval_history ...
    // - UPDATE users ...
    // - commit() (모든 변경 사항 일괄 커밋)
    // ═══ Persistence Context 종료 ═══
}
```

---

### 6. 트랜잭션 전파 (REQUIRES_NEW)

```java
/**
 * 감사 로그 저장
 *
 * REQUIRES_NEW:
 * - 항상 새로운 트랜잭션 시작
 * - 부모 트랜잭션 롤백과 무관하게 커밋
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAuditLog(String action, String username, String details) {
        // ═══ Transaction B 시작 (새로운 트랜잭션) ═══

        AuditLog log = AuditLog.builder()
            .action(action)
            .username(username)
            .details(details)
            .timestamp(LocalDateTime.now())
            .build();

        auditLogRepository.save(log);

        // ═══ Transaction B 커밋 (독립적) ═══
    }
}

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public User createUser(UserDto dto) {
        // ═══ Transaction A 시작 ═══

        User user = new User(dto.getUsername(), dto.getEmail());
        userRepository.save(user);

        // 감사 로그 저장 (별도 트랜잭션)
        auditService.saveAuditLog("CREATE_USER", dto.getUsername(), "User created");
        // → Transaction B 시작 및 커밋 (독립적)

        if (dto.getEmail().contains("invalid")) {
            throw new BusinessException("Invalid email");
            // Transaction A만 롤백
            // Transaction B(감사 로그)는 이미 커밋됨
        }

        // ═══ Transaction A 커밋 ═══

        return user;
    }
}
```

---

## 🎓 베스트 프랙티스

### 1. 트랜잭션 범위는 최소화

**❌ 나쁜 예**:
```java
@Transactional
public void processLargeData() {
    List<User> users = userRepository.findAll();  // 1만 건

    for (User user : users) {
        // 외부 API 호출 (느림)
        externalService.sendEmail(user.getEmail());  // 각 1초

        user.setLastEmailSent(LocalDateTime.now());
    }
    // 트랜잭션이 10,000초(약 3시간) 동안 유지됨!
}
```

**✅ 좋은 예**:
```java
public void processLargeData() {
    List<User> users = userRepository.findAll();

    for (User user : users) {
        // 외부 API 호출 (트랜잭션 밖)
        externalService.sendEmail(user.getEmail());

        // 짧은 트랜잭션으로 업데이트
        updateLastEmailSent(user.getId());
    }
}

@Transactional
public void updateLastEmailSent(Long userId) {
    User user = userRepository.findById(userId).orElseThrow();
    user.setLastEmailSent(LocalDateTime.now());
}
```

---

### 2. 읽기 전용에는 readOnly = true

**✅ 항상 읽기 전용 트랜잭션 명시**:
```java
@Transactional(readOnly = true)
public List<User> findAllUsers() {
    return userRepository.findAll();
}

@Transactional(readOnly = true)
public User findById(Long id) {
    return userRepository.findById(id).orElseThrow();
}
```

---

### 3. LazyInitializationException 방지

**❌ 나쁜 예**:
```java
public UserDto getUser(Long id) {
    User user = userRepository.findById(id).orElseThrow();
    return UserDto.from(user);  // user.getRoles() 접근 시 예외!
}
```

**✅ 좋은 예 1 - Fetch Join**:
```java
@Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id = :id")
Optional<User> findByIdWithRoles(@Param("id") Long id);

@Transactional(readOnly = true)
public UserDto getUser(Long id) {
    User user = userRepository.findByIdWithRoles(id).orElseThrow();
    return UserDto.from(user);
}
```

**✅ 좋은 예 2 - DTO 변환을 트랜잭션 내부에서**:
```java
@Transactional(readOnly = true)
public UserDto getUser(Long id) {
    User user = userRepository.findById(id).orElseThrow();
    return UserDto.from(user);  // 트랜잭션 내에서 DTO 변환
}
```

---

### 4. 적절한 격리 수준 사용

```java
// 일반 조회
@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
public List<User> findAll() { ... }

// 중요한 금융 거래
@Transactional(isolation = Isolation.SERIALIZABLE)
public void transferMoney() { ... }

// 통계 조회 (일관된 결과 필요)
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public Statistics getStatistics() { ... }
```

---

### 5. Checked Exception은 명시적 롤백

```java
// ✅ Checked Exception에서도 롤백
@Transactional(rollbackFor = Exception.class)
public void methodWithCheckedException() throws IOException {
    // IOException 발생 시에도 롤백됨
}
```

---

### 6. 변경 감지 활용 (save() 불필요)

**❌ 불필요한 save() 호출**:
```java
@Transactional
public void updateUser(Long id, String newEmail) {
    User user = userRepository.findById(id).orElseThrow();
    user.setEmail(newEmail);
    userRepository.save(user);  // ❌ 불필요!
}
```

**✅ 변경 감지 활용**:
```java
@Transactional
public void updateUser(Long id, String newEmail) {
    User user = userRepository.findById(id).orElseThrow();
    user.setEmail(newEmail);
    // save() 호출 불필요 - 변경 감지가 자동으로 UPDATE
}
```

---

## 🐛 트러블슈팅

### 1. LazyInitializationException

**증상**:
```
org.hibernate.LazyInitializationException:
could not initialize proxy - no Session
```

**원인**: 영속성 컨텍스트가 종료된 후 지연 로딩 시도

**해결**:
1. Fetch Join 사용
2. @Transactional 범위 확장
3. DTO 변환을 트랜잭션 내에서

---

### 2. N+1 문제

**증상**:
```
SELECT * FROM users;           -- 1번
SELECT * FROM roles WHERE ...  -- N번 (각 User마다)
```

**원인**: 지연 로딩으로 인한 추가 쿼리 발생

**해결**:
```java
@Query("SELECT u FROM User u LEFT JOIN FETCH u.roles")
List<User> findAllWithRoles();
```

---

### 3. 트랜잭션이 적용 안 됨

**증상**: 데이터가 저장/수정되지 않음

**원인**:
1. `@Transactional`이 private 메서드에 적용됨
2. 같은 클래스 내부에서 호출 (프록시 우회)
3. 예외가 발생했지만 Checked Exception

**해결**:
```java
// ❌ private 메서드
@Transactional
private void createUser() { ... }  // 동작 안 함

// ✅ public 메서드
@Transactional
public void createUser() { ... }

// ❌ 내부 호출
public void outerMethod() {
    innerMethod();  // 프록시 우회
}
@Transactional
public void innerMethod() { ... }

// ✅ 외부에서 호출
@Autowired
private UserService userService;

public void outerMethod() {
    userService.innerMethod();  // 프록시 적용됨
}
```

---

### 4. 트랜잭션이 너무 길어짐

**증상**: 데드락, 성능 저하, 타임아웃

**원인**: 외부 API 호출, 파일 I/O 등이 트랜잭션 내부에 있음

**해결**: 트랜잭션 범위 최소화 (위 베스트 프랙티스 참고)

---

## 🔗 관련 문서

- **[README.md](./README.md)** - 프로젝트 전체 개요
- **[JWT.md](./JWT.md)** - JWT 인증 흐름 상세
- **[AOP.md](./AOP.md)** - AOP 문법 및 구현
- **Spring Data JPA 공식 문서**: https://spring.io/projects/spring-data-jpa
- **Spring Transaction 공식 문서**: https://docs.spring.io/spring-framework/reference/data-access/transaction.html
- **Hibernate 공식 문서**: https://hibernate.org/orm/documentation/

---

**작성일**: 2025-10-26
**작성자**: Hyoukjoo Lee
**프로젝트**: SmartWork - Enterprise Intranet System
