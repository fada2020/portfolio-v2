# Spring Boot 핵심 개념 완벽 가이드

> Spring Bean, DI/IoC, 생명주기, 스코프 등 Spring Boot의 핵심 개념 총정리

---

## 📋 목차

1. [Spring Boot 개요](#-spring-boot-개요)
2. [IoC Container & DI](#-ioc-container--di)
3. [Spring Bean](#-spring-bean)
4. [Bean 생명주기](#-bean-생명주기)
5. [Bean Scope](#-bean-scope)
6. [Component Scan](#-component-scan)
7. [Configuration](#-configuration)
8. [Profile](#-profile)
9. [Properties & YAML](#-properties--yaml)
10. [Auto Configuration](#-auto-configuration)

---

## 🎯 Spring Boot 개요

### Spring Framework vs Spring Boot

```
Spring Framework (2003~)
    ↓
복잡한 설정 (XML, Java Config)
    ↓
Spring Boot (2014~)
    ↓
Convention over Configuration
설정 자동화, 빠른 개발
```

### Spring Boot의 핵심 특징

| 특징 | 설명 |
|-----|------|
| **Auto Configuration** | 자동 설정 - 필요한 Bean을 자동으로 등록 |
| **Starter Dependencies** | 의존성 묶음 - spring-boot-starter-web 등 |
| **Embedded Server** | 내장 서버 - Tomcat, Jetty, Undertow |
| **Actuator** | 운영 도구 - 헬스체크, 메트릭 수집 |
| **Opinionated Defaults** | 관례 기반 설정 - 최소 설정으로 시작 |

---

## 🏗 IoC Container & DI

### IoC (Inversion of Control) - 제어의 역전

**객체의 생성과 관리를 개발자가 아닌 Spring Container가 담당**

```java
// ❌ 전통적인 방식 (개발자가 직접 제어)
public class UserService {
    private UserRepository userRepository = new UserRepository();
    // 강한 결합 - UserRepository 변경 시 UserService도 변경 필요
}

// ✅ IoC 방식 (Spring이 제어)
@Service
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        // 느슨한 결합 - Spring이 UserRepository 주입
    }
}
```

**IoC의 장점**:
- ✅ **느슨한 결합**: 인터페이스 기반 프로그래밍
- ✅ **테스트 용이성**: Mock 객체 주입 가능
- ✅ **유연성**: 구현체 변경 용이
- ✅ **생명주기 관리**: Spring이 객체 생성부터 소멸까지 관리

---

### DI (Dependency Injection) - 의존성 주입

**객체 간의 의존 관계를 외부에서 주입**

#### 1. 생성자 주입 (Constructor Injection) ⭐ 권장

```java
@Service
@RequiredArgsConstructor  // Lombok - final 필드 생성자 자동 생성
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Lombok이 자동 생성:
    // public UserService(UserRepository userRepository,
    //                    PasswordEncoder passwordEncoder) {
    //     this.userRepository = userRepository;
    //     this.passwordEncoder = passwordEncoder;
    // }
}
```

**생성자 주입의 장점**:
- ✅ **불변성(Immutability)**: final 키워드 사용 가능
- ✅ **필수 의존성 명확**: 생성자 파라미터로 명시
- ✅ **순환 참조 방지**: 컴파일 시점에 순환 참조 감지
- ✅ **테스트 용이**: new로 객체 생성 시 의존성 명확

#### 2. Setter 주입 (Setter Injection)

```java
@Service
public class UserService {

    private UserRepository userRepository;

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

**Setter 주입의 특징**:
- ✅ 선택적 의존성 (Optional)
- ❌ 불변성 보장 안 됨
- ❌ 런타임에 의존성 변경 가능 (위험)

#### 3. 필드 주입 (Field Injection) ❌ 권장하지 않음

```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;  // ❌ 권장하지 않음
}
```

**필드 주입의 문제점**:
- ❌ **불변성 없음**: final 사용 불가
- ❌ **테스트 어려움**: Mock 주입이 복잡
- ❌ **순환 참조 감지 늦음**: 런타임에만 감지
- ❌ **의존성 숨김**: 외부에서 의존성 파악 어려움

---

### IoC Container 종류

```
ApplicationContext (Interface)
    ↓
AnnotationConfigApplicationContext
    ↓
Spring Boot의 ApplicationContext
    ↓
- Bean 생성 및 관리
- DI 수행
- AOP 적용
- 이벤트 처리
- 리소스 로딩
```

**ApplicationContext 접근**:

```java
@SpringBootApplication
public class SmartWorkApplication {

    public static void main(String[] args) {
        ApplicationContext context =
            SpringApplication.run(SmartWorkApplication.class, args);

        // Bean 조회
        UserService userService = context.getBean(UserService.class);

        // Bean 이름으로 조회
        UserService userService2 =
            context.getBean("userService", UserService.class);

        // 특정 타입의 모든 Bean 조회
        Map<String, UserService> beans =
            context.getBeansOfType(UserService.class);
    }
}
```

---

## 🫘 Spring Bean

### Bean이란?

**Spring IoC Container가 관리하는 객체**

```
일반 Java 객체 (POJO)
    ↓
Spring Container에 등록
    ↓
Spring Bean
    ↓
- 생명주기 관리
- 의존성 주입
- AOP 적용 가능
```

### Bean 등록 방법

#### 1. Component Scan 기반 (@Component 및 파생 애노테이션)

```java
// @Component - 범용 컴포넌트
@Component
public class EmailValidator {
    public boolean validate(String email) {
        return email.contains("@");
    }
}

// @Service - 비즈니스 로직 계층
@Service
public class UserService {
    // ...
}

// @Repository - 데이터 액세스 계층
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // ...
}

// @Controller - 프레젠테이션 계층 (View 반환)
@Controller
public class HomeController {
    @GetMapping("/")
    public String home() {
        return "index";
    }
}

// @RestController - REST API (@Controller + @ResponseBody)
@RestController
@RequestMapping("/api/users")
public class UserController {
    @GetMapping
    public List<User> getUsers() {
        return userService.findAll();
    }
}
```

**Component Scan 계층 구조**:

```
@Component (최상위)
    ↓
┌───────────┬───────────┬────────────┐
│           │           │            │
@Service  @Repository @Controller  @Configuration
                          ↓
                    @RestController
```

#### 2. Java Config 기반 (@Configuration + @Bean)

```java
@Configuration
public class AppConfig {

    // Bean 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // Bean 이름 지정
    @Bean(name = "customObjectMapper")
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    // 다른 Bean 주입 받기
    @Bean
    public UserService userService(UserRepository userRepository,
                                    PasswordEncoder passwordEncoder) {
        return new UserService(userRepository, passwordEncoder);
    }

    // 초기화 및 소멸 메서드 지정
    @Bean(initMethod = "init", destroyMethod = "cleanup")
    public DatabaseConnection databaseConnection() {
        return new DatabaseConnection();
    }
}
```

**@Configuration의 특징**:
- ✅ CGLIB 프록시를 통한 싱글톤 보장
- ✅ @Bean 메서드 간 호출 시 싱글톤 유지

```java
@Configuration
public class AppConfig {

    @Bean
    public UserRepository userRepository() {
        return new UserRepository();
    }

    @Bean
    public UserService userService() {
        // userRepository() 호출 시 싱글톤 보장
        return new UserService(userRepository());  // 같은 인스턴스
    }

    @Bean
    public NotificationService notificationService() {
        return new NotificationService(userRepository());  // 같은 인스턴스
    }
}
```

---

### Bean 이름 규칙

```java
// 1. 기본 이름: 클래스명의 첫 글자를 소문자로
@Service
public class UserService { }  // Bean 이름: "userService"

// 2. 명시적 이름 지정
@Service("customUserService")
public class UserService { }  // Bean 이름: "customUserService"

// 3. @Bean의 경우 메서드명이 Bean 이름
@Bean
public PasswordEncoder passwordEncoder() { }  // Bean 이름: "passwordEncoder"

// 4. @Bean name 속성
@Bean(name = "customEncoder")
public PasswordEncoder passwordEncoder() { }  // Bean 이름: "customEncoder"
```

---

### 같은 타입의 Bean이 여러 개일 때

```java
// 문제 상황
public interface MessageSender {
    void send(String message);
}

@Component
public class EmailSender implements MessageSender { }

@Component
public class SmsSender implements MessageSender { }

@Service
public class NotificationService {
    @Autowired
    private MessageSender messageSender;  // ❌ 어떤 Bean을 주입?
    // NoUniqueBeanDefinitionException 발생!
}
```

**해결 방법**:

#### 1. @Primary - 기본 Bean 지정

```java
@Component
@Primary  // 기본으로 사용할 Bean
public class EmailSender implements MessageSender { }

@Component
public class SmsSender implements MessageSender { }

@Service
public class NotificationService {
    @Autowired
    private MessageSender messageSender;  // EmailSender 주입됨
}
```

#### 2. @Qualifier - Bean 이름 지정

```java
@Service
public class NotificationService {

    @Autowired
    @Qualifier("emailSender")
    private MessageSender emailSender;

    @Autowired
    @Qualifier("smsSender")
    private MessageSender smsSender;
}
```

#### 3. 파라미터 이름으로 매칭

```java
@Service
public class NotificationService {

    @Autowired
    public NotificationService(MessageSender emailSender) {
        // 파라미터명 "emailSender"로 Bean 이름 매칭
    }
}
```

#### 4. List/Map으로 모든 Bean 주입

```java
@Service
public class NotificationService {

    private final List<MessageSender> messageSenders;

    @Autowired
    public NotificationService(List<MessageSender> messageSenders) {
        this.messageSenders = messageSenders;  // 모든 MessageSender Bean
    }

    public void sendAll(String message) {
        messageSenders.forEach(sender -> sender.send(message));
    }
}

// Map으로 주입 (Bean 이름이 Key)
@Service
public class NotificationService {

    private final Map<String, MessageSender> messageSenderMap;

    @Autowired
    public NotificationService(Map<String, MessageSender> messageSenderMap) {
        this.messageSenderMap = messageSenderMap;
        // {"emailSender": EmailSender 인스턴스,
        //  "smsSender": SmsSender 인스턴스}
    }
}
```

---

## ⏱ Bean 생명주기

### Bean 생명주기 흐름

```
1. Spring Container 시작
    ↓
2. Bean 인스턴스 생성
    ↓
3. 의존성 주입 (DI)
    ↓
4. 초기화 콜백 메서드 호출
    ↓
5. Bean 사용
    ↓
6. 소멸 전 콜백 메서드 호출
    ↓
7. Spring Container 종료
```

### Bean 초기화 및 소멸 콜백

#### 1. @PostConstruct / @PreDestroy (권장) ⭐

```java
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class DatabaseConnection {

    private Connection connection;

    // 의존성 주입 완료 후 실행
    @PostConstruct
    public void init() {
        System.out.println("DatabaseConnection 초기화 시작");
        this.connection = createConnection();
        System.out.println("DatabaseConnection 초기화 완료");
    }

    // Bean 소멸 전 실행
    @PreDestroy
    public void cleanup() {
        System.out.println("DatabaseConnection 정리 시작");
        if (connection != null) {
            connection.close();
        }
        System.out.println("DatabaseConnection 정리 완료");
    }

    private Connection createConnection() {
        // 실제 연결 생성 로직
        return new Connection();
    }
}
```

**실행 순서**:
```
1. DatabaseConnection 생성자 호출
2. 의존성 주입 (있다면)
3. @PostConstruct init() 호출
4. Bean 사용 가능
   ...
5. Container 종료 시작
6. @PreDestroy cleanup() 호출
7. Bean 소멸
```

#### 2. InitializingBean / DisposableBean 인터페이스

```java
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;

@Component
public class CacheManager implements InitializingBean, DisposableBean {

    private Map<String, Object> cache;

    @Override
    public void afterPropertiesSet() throws Exception {
        // 초기화 로직
        System.out.println("CacheManager 초기화");
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public void destroy() throws Exception {
        // 소멸 로직
        System.out.println("CacheManager 정리");
        this.cache.clear();
    }
}
```

#### 3. @Bean의 initMethod / destroyMethod

```java
@Configuration
public class AppConfig {

    @Bean(initMethod = "init", destroyMethod = "cleanup")
    public ExternalService externalService() {
        return new ExternalService();
    }
}

public class ExternalService {

    public void init() {
        System.out.println("ExternalService 초기화");
    }

    public void cleanup() {
        System.out.println("ExternalService 정리");
    }
}
```

---

### Bean 생명주기 전체 흐름 상세

```java
@Component
public class LifecycleBean implements InitializingBean, DisposableBean {

    private String name;

    // ① 생성자 호출
    public LifecycleBean() {
        System.out.println("1. 생성자 호출");
    }

    // ② 의존성 주입 (Setter 주입 예시)
    @Autowired
    public void setName(@Value("${bean.name}") String name) {
        System.out.println("2. 의존성 주입 (Setter)");
        this.name = name;
    }

    // ③ BeanNameAware
    @Override
    public void setBeanName(String beanName) {
        System.out.println("3. setBeanName: " + beanName);
    }

    // ④ BeanFactoryAware
    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        System.out.println("4. setBeanFactory");
    }

    // ⑤ ApplicationContextAware
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        System.out.println("5. setApplicationContext");
    }

    // ⑥ @PostConstruct
    @PostConstruct
    public void postConstruct() {
        System.out.println("6. @PostConstruct");
    }

    // ⑦ InitializingBean.afterPropertiesSet()
    @Override
    public void afterPropertiesSet() {
        System.out.println("7. afterPropertiesSet()");
    }

    // ⑧ @Bean(initMethod)
    public void customInit() {
        System.out.println("8. @Bean initMethod");
    }

    // ═══════════════════════════════════════
    // Bean 사용 가능
    // ═══════════════════════════════════════

    // ⑨ @PreDestroy
    @PreDestroy
    public void preDestroy() {
        System.out.println("9. @PreDestroy");
    }

    // ⑩ DisposableBean.destroy()
    @Override
    public void destroy() {
        System.out.println("10. destroy()");
    }

    // ⑪ @Bean(destroyMethod)
    public void customDestroy() {
        System.out.println("11. @Bean destroyMethod");
    }
}
```

**출력 결과**:
```
1. 생성자 호출
2. 의존성 주입 (Setter)
3. setBeanName: lifecycleBean
4. setBeanFactory
5. setApplicationContext
6. @PostConstruct
7. afterPropertiesSet()
8. @Bean initMethod
═══ Bean 사용 ═══
9. @PreDestroy
10. destroy()
11. @Bean destroyMethod
```

---

### ApplicationContext 이벤트 활용

```java
// 이벤트 정의
public class UserCreatedEvent extends ApplicationEvent {
    private final User user;

    public UserCreatedEvent(Object source, User user) {
        super(source);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}

// 이벤트 발행
@Service
@RequiredArgsConstructor
public class UserService {

    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public User createUser(UserDto dto) {
        User user = userRepository.save(new User(dto));

        // 이벤트 발행
        eventPublisher.publishEvent(new UserCreatedEvent(this, user));

        return user;
    }
}

// 이벤트 리스너
@Component
public class UserEventListener {

    @EventListener
    public void handleUserCreated(UserCreatedEvent event) {
        User user = event.getUser();
        System.out.println("사용자 생성됨: " + user.getUsername());
        // 환영 이메일 발송 등
    }

    // 비동기 처리
    @Async
    @EventListener
    public void handleUserCreatedAsync(UserCreatedEvent event) {
        // 비동기로 처리
    }

    // 조건부 실행
    @EventListener(condition = "#event.user.email.contains('admin')")
    public void handleAdminUserCreated(UserCreatedEvent event) {
        // 관리자 계정 생성 시에만 실행
    }
}
```

---

## 📦 Bean Scope

### Scope란?

**Bean이 존재할 수 있는 범위**

| Scope | 설명 | 생명주기 | 사용 예시 |
|-------|------|---------|----------|
| **singleton** | 기본값, 단일 인스턴스 | 컨테이너와 동일 | Service, Repository |
| **prototype** | 요청마다 새 인스턴스 | 요청 시마다 생성/소멸 | 상태를 가진 객체 |
| **request** | HTTP 요청마다 | HTTP 요청-응답 | 요청별 데이터 |
| **session** | HTTP 세션마다 | 세션 생성-소멸 | 사용자 세션 데이터 |
| **application** | ServletContext 생애 | 애플리케이션 전체 | 전역 설정 |
| **websocket** | WebSocket 생애 | WebSocket 연결-종료 | 채팅방 데이터 |

---

### 1. Singleton Scope (기본값) ⭐

**Spring Container당 하나의 인스턴스만 생성**

```java
@Component  // 기본 scope = singleton
public class UserService {
    // Spring Container에 단 하나만 존재
}

// 명시적 지정
@Component
@Scope("singleton")  // 또는 @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class UserService {
}
```

**Singleton Bean 동작 방식**:

```java
@SpringBootTest
class SingletonTest {

    @Autowired
    private UserService userService1;

    @Autowired
    private UserService userService2;

    @Test
    void singletonTest() {
        System.out.println("userService1: " + userService1);
        System.out.println("userService2: " + userService2);

        assertSame(userService1, userService2);  // ✅ 같은 인스턴스
    }
}
```

**출력**:
```
userService1: com.smartwork.service.UserService@1a2b3c4d
userService2: com.smartwork.service.UserService@1a2b3c4d
```

**Singleton의 특징**:
- ✅ **메모리 효율적**: 인스턴스 재사용
- ✅ **성능 우수**: 객체 생성 비용 절감
- ⚠️ **상태 공유**: 멀티스레드 환경에서 필드 변수 주의

**주의사항 - Thread Safety**:

```java
// ❌ Singleton Bean에서 상태를 가지면 위험
@Service
public class UserService {
    private int requestCount = 0;  // ❌ 여러 스레드가 공유!

    public void incrementCount() {
        requestCount++;  // 동시성 문제 발생 가능
    }
}

// ✅ Stateless로 설계
@Service
public class UserService {
    // 필드 변수 없음 (또는 final 필드만)

    public int calculateSomething(int input) {
        int result = input * 2;  // 지역 변수 사용
        return result;
    }
}

// ✅ 상태가 필요하면 ThreadLocal 사용
@Service
public class UserService {
    private ThreadLocal<Integer> requestCount = ThreadLocal.withInitial(() -> 0);

    public void incrementCount() {
        requestCount.set(requestCount.get() + 1);
    }
}
```

---

### 2. Prototype Scope

**요청할 때마다 새로운 인스턴스 생성**

```java
@Component
@Scope("prototype")  // 또는 @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PrototypeBean {

    @PostConstruct
    public void init() {
        System.out.println("PrototypeBean 생성: " + this);
    }

    @PreDestroy
    public void destroy() {
        System.out.println("PrototypeBean 소멸: " + this);
        // ⚠️ Prototype은 @PreDestroy가 호출되지 않음!
    }
}
```

**Prototype Bean 동작 방식**:

```java
@SpringBootTest
class PrototypeTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void prototypeTest() {
        PrototypeBean bean1 = context.getBean(PrototypeBean.class);
        PrototypeBean bean2 = context.getBean(PrototypeBean.class);

        System.out.println("bean1: " + bean1);
        System.out.println("bean2: " + bean2);

        assertNotSame(bean1, bean2);  // ✅ 다른 인스턴스
    }
}
```

**출력**:
```
PrototypeBean 생성: com.smartwork.PrototypeBean@1a2b3c4d
bean1: com.smartwork.PrototypeBean@1a2b3c4d
PrototypeBean 생성: com.smartwork.PrototypeBean@5e6f7g8h
bean2: com.smartwork.PrototypeBean@5e6f7g8h
```

**Prototype의 특징**:
- ✅ 요청마다 새 인스턴스
- ⚠️ **소멸 콜백 없음**: Spring이 소멸 관리 안 함
- ⚠️ **메모리 주의**: 많이 생성 시 메모리 부담

**Prototype Bean 소멸 관리**:

```java
@Component
@Scope("prototype")
public class PrototypeBean implements DisposableBean {

    @Override
    public void destroy() {
        // ❌ 자동 호출 안 됨!
    }
}

// 직접 소멸 관리 필요
public void cleanup() {
    PrototypeBean bean = context.getBean(PrototypeBean.class);
    // 사용
    ((DisposableBean) bean).destroy();  // 수동 호출
}
```

---

### 3. Singleton Bean에서 Prototype Bean 주입 문제

**문제 상황**:

```java
@Component
@Scope("prototype")
public class PrototypeBean {
    private int count = 0;

    public void increment() {
        count++;
        System.out.println("Count: " + count);
    }
}

@Service  // Singleton
public class SingletonService {

    @Autowired
    private PrototypeBean prototypeBean;  // ❌ 한 번만 주입됨!

    public void doSomething() {
        prototypeBean.increment();
        // Singleton이 생성될 때 단 한 번만 주입
        // 이후 계속 같은 인스턴스 사용 → Prototype 의미 없음
    }
}
```

**해결 방법**:

#### 방법 1: ApplicationContext에서 직접 조회

```java
@Service
@RequiredArgsConstructor
public class SingletonService {

    private final ApplicationContext context;

    public void doSomething() {
        // 호출마다 새 인스턴스 받기
        PrototypeBean prototypeBean = context.getBean(PrototypeBean.class);
        prototypeBean.increment();
    }
}
```

#### 방법 2: ObjectProvider 사용 (권장)

```java
@Service
@RequiredArgsConstructor
public class SingletonService {

    private final ObjectProvider<PrototypeBean> prototypeBeanProvider;

    public void doSomething() {
        // 호출마다 새 인스턴스 받기
        PrototypeBean prototypeBean = prototypeBeanProvider.getObject();
        prototypeBean.increment();
    }
}
```

#### 방법 3: @Lookup (프록시 방식)

```java
@Service
public abstract class SingletonService {

    public void doSomething() {
        PrototypeBean prototypeBean = getPrototypeBean();
        prototypeBean.increment();
    }

    @Lookup
    protected abstract PrototypeBean getPrototypeBean();
    // Spring이 CGLIB으로 구현체 생성
}
```

---

### 4. Request Scope

**HTTP 요청마다 새로운 Bean 인스턴스**

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestScopedBean {

    private String requestId;

    @PostConstruct
    public void init() {
        this.requestId = UUID.randomUUID().toString();
        System.out.println("Request Scoped Bean 생성: " + requestId);
    }

    public String getRequestId() {
        return requestId;
    }

    @PreDestroy
    public void destroy() {
        System.out.println("Request Scoped Bean 소멸: " + requestId);
    }
}
```

**proxyMode 필요 이유**:

```java
@Service  // Singleton
public class UserService {

    @Autowired
    private RequestScopedBean requestScopedBean;
    // Singleton 생성 시점에 Request Scope Bean이 없음!
    // proxyMode로 프록시 객체 주입 → 실제 사용 시점에 Bean 조회
}
```

**Request Scope 사용 예시**:

```java
@RestController
@RequiredArgsConstructor
public class UserController {

    private final RequestScopedBean requestScopedBean;

    @GetMapping("/api/users")
    public List<User> getUsers() {
        System.out.println("Request ID: " + requestScopedBean.getRequestId());
        // 각 HTTP 요청마다 다른 requestId 출력
        return userService.findAll();
    }
}
```

**실행 결과**:
```
요청 1:
Request Scoped Bean 생성: a1b2c3d4-e5f6-7890-abcd-ef1234567890
Request ID: a1b2c3d4-e5f6-7890-abcd-ef1234567890
Request Scoped Bean 소멸: a1b2c3d4-e5f6-7890-abcd-ef1234567890

요청 2:
Request Scoped Bean 생성: 9876fedc-ba09-8765-4321-0fedcba98765
Request ID: 9876fedc-ba09-8765-4321-0fedcba98765
Request Scoped Bean 소멸: 9876fedc-ba09-8765-4321-0fedcba98765
```

---

### 5. Session Scope

**HTTP 세션마다 새로운 Bean 인스턴스**

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ShoppingCart {

    private List<Product> items = new ArrayList<>();

    public void addItem(Product product) {
        items.add(product);
    }

    public List<Product> getItems() {
        return items;
    }
}
```

**Session Scope 사용 예시**:

```java
@RestController
@RequiredArgsConstructor
public class CartController {

    private final ShoppingCart shoppingCart;  // 세션별로 다른 인스턴스

    @PostMapping("/api/cart")
    public void addToCart(@RequestBody Product product) {
        shoppingCart.addItem(product);
        // 같은 세션의 사용자는 같은 ShoppingCart 인스턴스 사용
    }

    @GetMapping("/api/cart")
    public List<Product> getCart() {
        return shoppingCart.getItems();
    }
}
```

---

## 🔍 Component Scan

### Component Scan이란?

**@Component 및 파생 애노테이션이 붙은 클래스를 자동으로 Bean으로 등록**

```java
@SpringBootApplication
// ↓ 내부에 @ComponentScan 포함
public class SmartWorkApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartWorkApplication.class, args);
    }
}

// @SpringBootApplication 내부
@ComponentScan(
    basePackages = "com.smartwork",  // 기본: 현재 패키지 및 하위
    excludeFilters = {
        @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class)
    }
)
```

### Component Scan 동작 원리

```
1. @SpringBootApplication 시작
    ↓
2. @ComponentScan 실행
    ↓
3. 지정된 패키지 스캔
    ↓
4. @Component, @Service, @Repository, @Controller 찾기
    ↓
5. Bean으로 등록
    ↓
6. 의존성 주입 (DI)
```

### basePackages 지정

```java
@Configuration
@ComponentScan(
    basePackages = {"com.smartwork.service", "com.smartwork.repository"}
)
public class AppConfig {
}

// 여러 패키지 스캔
@ComponentScan(
    basePackages = {
        "com.smartwork.domain",
        "com.smartwork.service",
        "com.smartwork.repository"
    }
)

// 타입 안전한 방식
@ComponentScan(
    basePackageClasses = {UserService.class, UserRepository.class}
    // 해당 클래스가 있는 패키지를 스캔
)
```

### Filter 사용

```java
@Configuration
@ComponentScan(
    basePackages = "com.smartwork",

    // 특정 애노테이션 포함
    includeFilters = {
        @Filter(type = FilterType.ANNOTATION, classes = CustomComponent.class)
    },

    // 특정 클래스 제외
    excludeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = LegacyService.class),
        @Filter(type = FilterType.REGEX, pattern = "com\\.smartwork\\.temp\\..*")
    }
)
public class AppConfig {
}
```

---

## ⚙️ Configuration

### @Configuration이란?

**Bean 정의를 담고 있는 설정 클래스**

```java
@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
```

### @Configuration vs @Component

```java
// @Configuration - CGLIB 프록시로 싱글톤 보장
@Configuration
public class ConfigA {

    @Bean
    public BeanA beanA() {
        return new BeanA();
    }

    @Bean
    public BeanB beanB() {
        return new BeanB(beanA());  // ✅ 같은 beanA 인스턴스
    }

    @Bean
    public BeanC beanC() {
        return new BeanC(beanA());  // ✅ 같은 beanA 인스턴스
    }
}

// @Component - 싱글톤 보장 안 됨
@Component
public class ConfigB {

    @Bean
    public BeanA beanA() {
        return new BeanA();
    }

    @Bean
    public BeanB beanB() {
        return new BeanB(beanA());  // ❌ 새로운 beanA 인스턴스
    }

    @Bean
    public BeanC beanC() {
        return new BeanC(beanA());  // ❌ 또 다른 beanA 인스턴스
    }
}
```

### @Import로 설정 분리

```java
// 보안 설정
@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

// 데이터베이스 설정
@Configuration
public class DatabaseConfig {
    @Bean
    public DataSource dataSource() {
        return new HikariDataSource();
    }
}

// 메인 설정
@Configuration
@Import({SecurityConfig.class, DatabaseConfig.class})
public class AppConfig {
    // SecurityConfig, DatabaseConfig의 Bean 사용 가능
}
```

---

## 🌍 Profile

### Profile이란?

**환경별로 다른 Bean 설정**

```
개발 환경 (dev)    → H2 Database
테스트 환경 (test)  → H2 Database
운영 환경 (prod)    → Oracle Database
```

### Profile 설정

```java
// 개발 환경 설정
@Configuration
@Profile("dev")
public class DevConfig {

    @Bean
    public DataSource dataSource() {
        return new H2DataSource();  // H2 인메모리 DB
    }
}

// 운영 환경 설정
@Configuration
@Profile("prod")
public class ProdConfig {

    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:oracle:thin:@prod-db:1521:ORCL");
        return dataSource;
    }
}

// 여러 Profile
@Configuration
@Profile({"dev", "test"})
public class NonProdConfig {
}

// Profile 제외
@Configuration
@Profile("!prod")  // prod가 아닐 때
public class NonProdConfig {
}
```

### Profile 활성화 방법

#### 1. application.yml

```yaml
spring:
  profiles:
    active: dev
```

#### 2. 프로그램 인수

```bash
java -jar smartwork.jar --spring.profiles.active=prod
```

#### 3. 환경 변수

```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar smartwork.jar
```

#### 4. IDE 설정

```
IntelliJ IDEA:
Run → Edit Configurations → Active profiles: dev
```

### @Profile을 Bean 메서드에 적용

```java
@Configuration
public class DataSourceConfig {

    @Bean
    @Profile("dev")
    public DataSource devDataSource() {
        return new H2DataSource();
    }

    @Bean
    @Profile("prod")
    public DataSource prodDataSource() {
        return new OracleDataSource();
    }
}
```

---

## 📝 Properties & YAML

### application.yml 구조

```yaml
spring:
  application:
    name: smartwork

  # 데이터베이스 설정
  datasource:
    url: jdbc:oracle:thin:@localhost:1521:ORCL
    username: smartwork
    password: ${DB_PASSWORD}  # 환경 변수
    driver-class-name: oracle.jdbc.OracleDriver

  # JPA 설정
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.Oracle12cDialect
        format_sql: true
        show_sql: false

# 커스텀 프로퍼티
smartwork:
  jwt:
    secret: ${JWT_SECRET}
    access-token-validity: 3600000   # 1시간
    refresh-token-validity: 86400000  # 24시간

  upload:
    path: /var/uploads
    max-file-size: 10MB
```

### Profile별 설정 파일

```
application.yml          # 공통 설정
application-dev.yml      # 개발 환경
application-test.yml     # 테스트 환경
application-prod.yml     # 운영 환경
```

**application-dev.yml**:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb

  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        show_sql: true

logging:
  level:
    com.smartwork: DEBUG
```

**application-prod.yml**:
```yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@prod-db:1521:ORCL
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        show_sql: false

logging:
  level:
    com.smartwork: INFO
```

### @Value로 프로퍼티 주입

```java
@Service
public class JwtTokenProvider {

    @Value("${smartwork.jwt.secret}")
    private String jwtSecret;

    @Value("${smartwork.jwt.access-token-validity}")
    private long accessTokenValidity;

    @Value("${smartwork.jwt.refresh-token-validity:86400000}")  // 기본값
    private long refreshTokenValidity;

    // SpEL (Spring Expression Language)
    @Value("#{systemProperties['user.home']}")
    private String userHome;

    @Value("#{${smartwork.upload.max-file-size} * 1024}")
    private long maxFileSizeInBytes;
}
```

### @ConfigurationProperties (권장)

```java
@Component
@ConfigurationProperties(prefix = "smartwork.jwt")
@Getter
@Setter
public class JwtProperties {

    private String secret;
    private long accessTokenValidity;
    private long refreshTokenValidity;
}

// 사용
@Service
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    public String generateToken(Authentication authentication) {
        String secret = jwtProperties.getSecret();
        long validity = jwtProperties.getAccessTokenValidity();
        // ...
    }
}
```

**@ConfigurationProperties 장점**:
- ✅ 타입 안전
- ✅ 검증 가능 (@Validated + Bean Validation)
- ✅ 중복 제거
- ✅ IDE 자동완성

```java
@Component
@ConfigurationProperties(prefix = "smartwork.jwt")
@Validated
@Getter
@Setter
public class JwtProperties {

    @NotBlank(message = "JWT secret은 필수입니다")
    private String secret;

    @Min(value = 1000, message = "최소 1초 이상이어야 합니다")
    private long accessTokenValidity;

    @Min(value = 3600000, message = "최소 1시간 이상이어야 합니다")
    private long refreshTokenValidity;
}
```

---

## 🤖 Auto Configuration

### Auto Configuration이란?

**조건에 따라 자동으로 Bean을 등록하는 Spring Boot의 핵심 기능**

```
spring-boot-autoconfigure.jar
    ↓
META-INF/spring.factories
    ↓
수백 개의 자동 설정 클래스
    ↓
조건 평가 (@Conditional)
    ↓
Bean 자동 등록
```

### 대표적인 Auto Configuration

```java
// DataSourceAutoConfiguration
@Configuration
@ConditionalOnClass({DataSource.class, EmbeddedDatabaseType.class})
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }
}
```

### @Conditional 애노테이션

| 애노테이션 | 설명 | 예시 |
|----------|------|------|
| `@ConditionalOnClass` | 클래스패스에 특정 클래스 존재 | H2Driver 있을 때 |
| `@ConditionalOnMissingClass` | 클래스패스에 특정 클래스 없음 | |
| `@ConditionalOnBean` | 특정 Bean 존재 | DataSource Bean 있을 때 |
| `@ConditionalOnMissingBean` | 특정 Bean 없음 | 사용자 정의 Bean 우선 |
| `@ConditionalOnProperty` | 특정 프로퍼티 값 | `spring.datasource.url` 설정됨 |
| `@ConditionalOnResource` | 특정 리소스 존재 | `schema.sql` 파일 있음 |
| `@ConditionalOnWebApplication` | 웹 애플리케이션 | Spring MVC/WebFlux |
| `@ConditionalOnNotWebApplication` | 비웹 애플리케이션 | 배치, 스케줄러 |

### 커스텀 Auto Configuration

```java
// 1. 자동 설정 클래스 작성
@Configuration
@ConditionalOnClass(RedisTemplate.class)
@ConditionalOnProperty(prefix = "smartwork.redis", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(RedisProperties.class)
public class RedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        return template;
    }
}

// 2. resources/META-INF/spring.factories 파일 생성
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.smartwork.config.RedisAutoConfiguration
```

### Auto Configuration 디버깅

```yaml
# application.yml
logging:
  level:
    org.springframework.boot.autoconfigure: DEBUG

# 또는
debug: true
```

**실행 시 출력**:
```
============================
CONDITIONS EVALUATION REPORT
============================

Positive matches:
-----------------
DataSourceAutoConfiguration matched:
   - @ConditionalOnClass found required classes (DataSource, EmbeddedDatabaseType)

Negative matches:
-----------------
RedisAutoConfiguration:
   - @ConditionalOnClass did not find required class 'RedisTemplate'
```

---

## 🔗 관련 문서

- **[README.md](./README.md)** - 프로젝트 전체 개요
- **[JWT.md](./JWT.md)** - JWT 인증 흐름 상세
- **[AOP.md](./AOP.md)** - AOP 문법 및 구현
- **[JPA_TRANSACTION.md](./JPA_TRANSACTION.md)** - JPA & Transaction 생명주기
- **Spring Boot 공식 문서**: https://spring.io/projects/spring-boot
- **Spring Framework Core 문서**: https://docs.spring.io/spring-framework/reference/core.html

---

**작성일**: 2025-10-26
**작성자**: Hyoukjoo Lee
**프로젝트**: SmartWork - Enterprise Intranet System
