# JWT 인증 흐름 상세 가이드

SmartWork 프로젝트의 JWT(JSON Web Token) 기반 인증/인가 시스템의 전체 흐름을 설명합니다.

---

## 📋 목차

1. [JWT 개요](#jwt-개요)
2. [전체 인증 흐름](#전체-인증-흐름)
3. [주요 컴포넌트](#주요-컴포넌트)
4. [코드 레벨 상세 분석](#코드-레벨-상세-분석)
5. [시퀀스 다이어그램](#시퀀스-다이어그램)
6. [보안 특징](#보안-특징)
7. [에러 처리](#에러-처리)
8. [Refresh Token](#refresh-token)

---

## 🎯 JWT 개요

### JWT란?

JSON Web Token은 당사자 간에 정보를 JSON 객체로 안전하게 전송하기 위한 간결하고 자체 포함된 방법입니다.

### JWT 구조

```
Header.Payload.Signature
```

**Header (알고리즘 및 토큰 타입)**:
```json
{
  "alg": "HS512",
  "typ": "JWT"
}
```

**Payload (클레임)**:
```json
{
  "sub": "admin",
  "auth": "USER_READ,USER_WRITE,BOARD_READ",
  "iat": 1706000000,
  "exp": 1706003600
}
```

**Signature (서명)**:
```
HMACSHA512(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secretKey
)
```

### SmartWork JWT 설정

**파일**: `src/main/resources/application.yml:42-46`

```yaml
jwt:
  secret: ${JWT_SECRET:c21hcnR3b3JrLWp3dC1zZWNyZXQta2V5...}
  access-token-validity: 3600000   # 1시간 (3,600,000ms)
  refresh-token-validity: 86400000 # 24시간 (86,400,000ms)
```

**특징**:
- **Access Token**: 1시간 (짧은 수명으로 보안 강화)
- **Refresh Token**: 24시간 (재로그인 빈도 감소)
- **알고리즘**: HMAC-SHA512 (512비트 보안)
- **환경 변수**: `JWT_SECRET`으로 외부 주입 가능

---

## 🔄 전체 인증 흐름

### Step 1: 로그인 요청

```http
POST /api/auth/login HTTP/1.1
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```

### Step 2: 사용자 인증

**UserDetailsServiceImpl이 수행하는 작업**:

1. **사용자 조회**: `UserRepository.findByUsername(username)`
2. **계정 상태 검증**:
   - 계정 존재 여부
   - 계정 잠금 상태 (`isLocked()`)
   - 계정 활성화 상태 (`isActive()`)
3. **권한 수집**: User → Role → Permission 변환
4. **UserDetails 반환**: Spring Security가 인식하는 형식

### Step 3: 비밀번호 검증

- **BCrypt 암호화** 사용
- Salt 자동 생성
- 레인보우 테이블 공격 방지

### Step 4: JWT 토큰 생성

**JwtTokenProvider가 생성하는 토큰**:

```java
// Access Token (1시간)
String accessToken = generateAccessToken(authentication);

// Refresh Token (24시간)
String refreshToken = generateRefreshToken(authentication);
```

### Step 5: 토큰 응답

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImF1dGgiOiJVU0VSX1JFQUQsVVNFUl9XUklURSIsImlhdCI6MTcwNjAwMDAwMCwiZXhwIjoxNzA2MDAzNjAwfQ.signature...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  },
  "timestamp": "2025-01-26T12:00:00"
}
```

### Step 6: 인증된 API 요청

```http
GET /api/users/profile HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

### Step 7: JWT 필터에서 자동 검증

**JwtAuthenticationFilter가 수행하는 작업**:

1. `Authorization` 헤더에서 토큰 추출
2. `Bearer ` 접두사 제거
3. 토큰 유효성 검증
4. 사용자명 추출
5. UserDetails 로드
6. Authentication 객체 생성
7. SecurityContext에 인증 정보 설정

### Step 8: 컨트롤러에서 인증 정보 사용

```java
@GetMapping("/profile")
@PreAuthorize("hasPermission('USER_READ')")
public UserResponse getProfile() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String username = auth.getName();
    return userService.getUserProfile(username);
}
```

---

## 🧩 주요 컴포넌트

### 1. JwtTokenProvider

**역할**: JWT 토큰 생성 및 검증

**파일**: `src/main/java/com/smartwork/security/jwt/JwtTokenProvider.java`

**주요 메서드**:

| 메서드 | 설명 |
|--------|------|
| `generateAccessToken(Authentication)` | Access Token 생성 (1시간) |
| `generateRefreshToken(Authentication)` | Refresh Token 생성 (24시간) |
| `validateToken(String)` | 토큰 유효성 검증 |
| `getUsernameFromToken(String)` | 토큰에서 사용자명 추출 |
| `getAuthoritiesFromToken(String)` | 토큰에서 권한 추출 |

### 2. JwtAuthenticationFilter

**역할**: 모든 HTTP 요청에서 JWT 검증

**파일**: `src/main/java/com/smartwork/security/filter/JwtAuthenticationFilter.java`

**상속**: `OncePerRequestFilter` (요청당 한 번만 실행)

**처리 순서**:
```
Request → extractJwtFromRequest()
       → validateToken()
       → getUsernameFromToken()
       → loadUserByUsername()
       → setAuthentication()
       → filterChain.doFilter()
```

### 3. UserDetailsServiceImpl

**역할**: 사용자 정보 로드 및 권한 변환

**파일**: `src/main/java/com/smartwork/security/service/UserDetailsServiceImpl.java`

**책임**:
- DB에서 사용자 조회
- 계정 상태 검증 (잠금, 비활성화)
- User → Role → Permission 권한 변환
- Spring Security UserDetails 반환

---

## 💻 코드 레벨 상세 분석

### 1️⃣ 토큰 생성 과정

**파일**: `src/main/java/com/smartwork/security/jwt/JwtTokenProvider.java:44-59`

```java
private String generateToken(Authentication authentication, long validityMs) {
    // 1. 사용자명 추출
    String username = authentication.getName();

    // 2. 권한 목록을 쉼표로 구분된 문자열로 변환
    // 예: "USER_READ,USER_WRITE,BOARD_READ"
    String authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));

    // 3. 발급 시간과 만료 시간 계산
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + validityMs);

    // 4. JWT 빌더로 토큰 생성
    return Jwts.builder()
            .subject(username)              // JWT 주체 (subject)
            .claim("auth", authorities)     // 커스텀 클레임 (권한)
            .issuedAt(now)                  // 발급 시간 (issued at)
            .expiration(expiryDate)         // 만료 시간 (expiration)
            .signWith(secretKey, Jwts.SIG.HS512) // HMAC-SHA512 서명
            .compact();                     // 문자열로 변환
}
```

**생성 과정**:
```
Authentication → username + authorities
              → Date(now, expiry)
              → Jwts.builder()
              → Sign with HMAC-SHA512
              → Compact to String
              → "eyJhbGciOiJIUzUxMiJ9..."
```

### 2️⃣ 토큰 검증 과정

**파일**: `src/main/java/com/smartwork/security/jwt/JwtTokenProvider.java:70-95`

```java
public boolean validateToken(String token) {
    try {
        parseClaims(token); // 파싱 성공 = 유효한 토큰
        return true;
    } catch (SecurityException | MalformedJwtException e) {
        // 잘못된 서명 또는 형식
        log.error("Invalid JWT token: {}", e.getMessage());
        throw new BusinessException(ErrorCode.INVALID_TOKEN, e);
    } catch (ExpiredJwtException e) {
        // 만료된 토큰
        log.error("Expired JWT token: {}", e.getMessage());
        throw new BusinessException(ErrorCode.TOKEN_EXPIRED, e);
    } catch (UnsupportedJwtException e) {
        // 지원되지 않는 JWT
        log.error("Unsupported JWT token: {}", e.getMessage());
        throw new BusinessException(ErrorCode.INVALID_TOKEN, e);
    } catch (IllegalArgumentException e) {
        // 빈 클레임
        log.error("JWT claims string is empty: {}", e.getMessage());
        throw new BusinessException(ErrorCode.INVALID_TOKEN, e);
    }
}

private Claims parseClaims(String token) {
    return Jwts.parser()
            .verifyWith(secretKey)  // 서명 검증 (중요!)
            .build()
            .parseSignedClaims(token)
            .getPayload();
}
```

**검증 항목**:
1. ✅ **서명 검증**: `secretKey`로 HMAC-SHA512 서명 확인
2. ✅ **만료 시간**: `exp` 클레임과 현재 시간 비교
3. ✅ **형식 검증**: JWT 구조 (Header.Payload.Signature) 확인
4. ✅ **클레임 존재**: Payload에 필수 클레임 존재 여부

### 3️⃣ 필터에서 인증 처리

**파일**: `src/main/java/com/smartwork/security/filter/JwtAuthenticationFilter.java:29-62`

```java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain) throws ServletException, IOException {
    try {
        // 1. Authorization 헤더에서 JWT 추출
        String jwt = extractJwtFromRequest(request);

        // 2. JWT가 있고 유효한지 검증
        if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {

            // 3. JWT에서 사용자명 추출
            String username = tokenProvider.getUsernameFromToken(jwt);

            // 4. UserDetailsService로 사용자 정보 로드
            // (DB 조회 + 권한 수집)
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 5. Authentication 객체 생성
            // Spring Security가 인식하는 인증 객체
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,           // Principal (주체)
                        null,                  // Credentials (자격증명 - 이미 검증됨)
                        userDetails.getAuthorities() // Authorities (권한)
                    );

            // 6. 요청 세부정보 추가 (IP, 세션 ID 등)
            authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
            );

            // 7. SecurityContext에 인증 정보 설정
            // 이후 모든 코드에서 SecurityContextHolder로 접근 가능
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("Set authentication for user: {}", username);
        }
    } catch (Exception e) {
        // 인증 실패 시 로그만 남기고 계속 진행
        // (Spring Security가 나중에 처리)
        log.error("Could not set user authentication: {}", e.getMessage());
    }

    // 8. 다음 필터로 진행
    // SecurityContext에 인증 정보가 있으면 인증된 요청
    // 없으면 익명(Anonymous) 요청으로 처리
    filterChain.doFilter(request, response);
}

private String extractJwtFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    // "Bearer eyJhbGc..." → "eyJhbGc..."
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
        return bearerToken.substring(7); // "Bearer " 제거
    }
    return null;
}
```

### 4️⃣ 사용자 정보 로드 및 권한 변환

**파일**: `src/main/java/com/smartwork/security/service/UserDetailsServiceImpl.java:25-55`

```java
@Override
@Transactional(readOnly = true)
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    // 1. DB에서 사용자 조회
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

    // 2. 계정 잠금 체크 (5회 로그인 실패 시)
    if (user.isLocked()) {
        throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
    }

    // 3. 계정 활성화 체크
    if (!user.isActive()) {
        throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
    }

    // 4. Spring Security UserDetails 객체 반환
    return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),  // BCrypt 해시값
            true,                // enabled
            true,                // accountNonExpired
            true,                // credentialsNonExpired
            !user.isLocked(),    // accountNonLocked
            getAuthorities(user) // 권한 목록
    );
}

private Collection<? extends GrantedAuthority> getAuthorities(User user) {
    // User → Set<Role> → Set<Permission> → Set<GrantedAuthority>
    return user.getRoles().stream()
            .flatMap(role -> role.getPermissions().stream()) // 모든 Role의 Permission 수집
            .map(permission -> new SimpleGrantedAuthority(permission.getPermissionName())) // Permission → GrantedAuthority 변환
            .collect(Collectors.toSet()); // 중복 제거 (Set)
}
```

**권한 변환 과정**:
```
User {
    roles: [
        Role(ADMIN) {
            permissions: [USER_READ, USER_WRITE, USER_DELETE]
        },
        Role(MANAGER) {
            permissions: [BOARD_READ, BOARD_WRITE]
        }
    ]
}
↓
flatMap으로 모든 Permission 수집
↓
Set<Permission> {
    USER_READ, USER_WRITE, USER_DELETE,
    BOARD_READ, BOARD_WRITE
}
↓
map으로 GrantedAuthority 변환
↓
Set<GrantedAuthority> {
    SimpleGrantedAuthority("USER_READ"),
    SimpleGrantedAuthority("USER_WRITE"),
    SimpleGrantedAuthority("USER_DELETE"),
    SimpleGrantedAuthority("BOARD_READ"),
    SimpleGrantedAuthority("BOARD_WRITE")
}
```

---

## 📊 시퀀스 다이어그램

### 로그인 및 토큰 발급

```
클라이언트          AuthController    UserDetailsService    JwtTokenProvider    Database
    │                     │                    │                     │             │
    │  POST /login        │                    │                     │             │
    ├────────────────────>│                    │                     │             │
    │  {username,pwd}     │                    │                     │             │
    │                     │                    │                     │             │
    │                     │ loadUserByUsername()                     │             │
    │                     ├───────────────────>│                     │             │
    │                     │                    │                     │             │
    │                     │                    │  SELECT * FROM users             │
    │                     │                    │  WHERE username=?                │
    │                     │                    ├────────────────────────────────>│
    │                     │                    │                     │             │
    │                     │                    │  User + Roles + Permissions      │
    │                     │                    │<────────────────────────────────┤
    │                     │                    │                     │             │
    │                     │                    │  계정 상태 검증      │             │
    │                     │                    │  (잠금, 비활성화)    │             │
    │                     │                    │                     │             │
    │                     │  UserDetails       │                     │             │
    │                     │<───────────────────┤                     │             │
    │                     │                    │                     │             │
    │                     │  authenticate()    │                     │             │
    │                     │  (BCrypt 검증)     │                     │             │
    │                     │                    │                     │             │
    │                     │  generateAccessToken()                   │             │
    │                     ├─────────────────────────────────────────>│             │
    │                     │                    │                     │             │
    │                     │                    │  JWT 생성            │             │
    │                     │                    │  (HMAC-SHA512)      │             │
    │                     │                    │                     │             │
    │                     │  accessToken       │                     │             │
    │                     │<─────────────────────────────────────────┤             │
    │                     │                    │                     │             │
    │                     │  generateRefreshToken()                  │             │
    │                     ├─────────────────────────────────────────>│             │
    │                     │                    │                     │             │
    │                     │  refreshToken      │                     │             │
    │                     │<─────────────────────────────────────────┤             │
    │                     │                    │                     │             │
    │  {accessToken,      │                    │                     │             │
    │   refreshToken}     │                    │                     │             │
    │<────────────────────┤                    │                     │             │
```

### 인증된 API 요청

```
클라이언트          Filter              JwtTokenProvider    UserDetailsService   Controller
    │                 │                        │                    │               │
    │  GET /api/users │                        │                    │               │
    │  Authorization: │                        │                    │               │
    │  Bearer {token} │                        │                    │               │
    ├────────────────>│                        │                    │               │
    │                 │                        │                    │               │
    │                 │  extractJwtFromRequest()                    │               │
    │                 │  → "eyJhbGc..."        │                    │               │
    │                 │                        │                    │               │
    │                 │  validateToken(jwt)    │                    │               │
    │                 ├───────────────────────>│                    │               │
    │                 │                        │                    │               │
    │                 │                        │  parseClaims()     │               │
    │                 │                        │  (서명 검증)        │               │
    │                 │                        │  (만료 확인)        │               │
    │                 │                        │                    │               │
    │                 │  valid = true          │                    │               │
    │                 │<───────────────────────┤                    │               │
    │                 │                        │                    │               │
    │                 │  getUsernameFromToken()│                    │               │
    │                 ├───────────────────────>│                    │               │
    │                 │                        │                    │               │
    │                 │  username = "admin"    │                    │               │
    │                 │<───────────────────────┤                    │               │
    │                 │                        │                    │               │
    │                 │  loadUserByUsername("admin")                │               │
    │                 ├────────────────────────────────────────────>│               │
    │                 │                        │                    │               │
    │                 │  UserDetails (+ authorities)                │               │
    │                 │<────────────────────────────────────────────┤               │
    │                 │                        │                    │               │
    │                 │  setAuthentication()   │                    │               │
    │                 │  (SecurityContext)     │                    │               │
    │                 │                        │                    │               │
    │                 │  filterChain.doFilter()────────────────────────────────────>│
    │                 │                        │                    │               │
    │                 │                        │                    │  @PreAuthorize│
    │                 │                        │                    │  권한 체크     │
    │                 │                        │                    │               │
    │                 │                        │                    │  비즈니스 로직 │
    │                 │                        │                    │               │
    │  Response       │                        │                    │               │
    │<─────────────────────────────────────────────────────────────────────────────┤
```

---

## 🛡️ 보안 특징

### 1. Stateless 아키텍처

**장점**:
- ✅ 서버에 세션 저장 불필요
- ✅ 수평 확장 용이 (로드 밸런서)
- ✅ CORS 문제 최소화
- ✅ 마이크로서비스 친화적

**단점 및 대응**:
- ❌ 토큰 무효화 어려움 → Refresh Token으로 보완
- ❌ 토큰 크기 → 필요한 클레임만 포함

### 2. 강력한 암호화

```java
// HMAC-SHA512 사용
private final SecretKey secretKey;

public JwtTokenProvider(@Value("${jwt.secret}") String secret, ...) {
    this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
}
```

**특징**:
- **알고리즘**: HMAC-SHA512 (대칭키 암호화)
- **키 길이**: 512비트 (매우 안전)
- **Base64 인코딩**: 안전한 전송

### 3. 토큰 만료 전략

```
Access Token:  1시간   → 짧은 수명으로 탈취 위험 감소
Refresh Token: 24시간  → 재로그인 빈도 감소
```

**시나리오**:
```
시간 00:00 → 로그인 → Access Token (01:00 만료), Refresh Token (익일 00:00 만료)
시간 01:00 → Access Token 만료 → Refresh Token으로 재발급
시간 01:01 → 새 Access Token (02:01 만료), 새 Refresh Token (익일 01:01 만료)
```

### 4. 계정 보안 메커니즘

**로그인 실패 카운터**:

```java
public void incrementFailedAttempts() {
    this.failedLoginAttempts++;
    if (this.failedLoginAttempts >= 5) {
        this.status = UserStatus.LOCKED;
        this.lockedUntil = LocalDateTime.now().plusHours(1);
    }
}

public void resetFailedAttempts() {
    this.failedLoginAttempts = 0;
}
```

**보호 기능**:
- 5회 실패 → 1시간 계정 잠금
- 무차별 대입 공격(Brute Force) 방지
- 시간 기반 자동 해제

### 5. BCrypt 비밀번호 암호화

```java
// 비밀번호 암호화
String hashedPassword = passwordEncoder.encode("password123");
// → $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhW

// 비밀번호 검증
boolean matches = passwordEncoder.matches("password123", hashedPassword);
// → true
```

**특징**:
- Salt 자동 생성 (레인보우 테이블 공격 방지)
- 느린 해싱 (무차별 대입 공격 방지)
- 같은 비밀번호도 매번 다른 해시값

### 6. 권한 기반 접근 제어

```java
@PreAuthorize("hasPermission('USER_WRITE')")
public UserResponse createUser(@Valid UserCreateRequest request) {
    // USER_WRITE 권한이 있어야만 실행 가능
}

@PreAuthorize("hasRole('ADMIN')")
public List<UserResponse> getAllUsers() {
    // ADMIN 역할이 있어야만 실행 가능
}

@PreAuthorize("hasPermission('USER_DELETE') and #userId != principal.id")
public void deleteUser(@PathVariable Long userId) {
    // USER_DELETE 권한 + 자기 자신은 삭제 불가
}
```

---

## ❌ 에러 처리

### 에러 코드 체계

**파일**: `src/main/java/com/smartwork/exception/ErrorCode.java`

| 에러 코드 | HTTP 상태 | 메시지 | 설명 |
|----------|----------|--------|------|
| `A001` | 401 UNAUTHORIZED | "Authentication required" | 인증 필요 |
| `A002` | 401 UNAUTHORIZED | "Invalid token" | 잘못된 토큰 |
| `A003` | 401 UNAUTHORIZED | "Token has expired" | 토큰 만료 |
| `A004` | 403 FORBIDDEN | "Access denied" | 권한 부족 |
| `A006` | 403 FORBIDDEN | "Account is locked" | 계정 잠금 |
| `U001` | 404 NOT_FOUND | "User not found" | 사용자 없음 |

### 에러 응답 형식

**토큰 만료**:
```json
{
  "success": false,
  "code": "A003",
  "message": "Token has expired",
  "timestamp": "2025-01-26T12:00:00",
  "errors": []
}
```

**잘못된 토큰**:
```json
{
  "success": false,
  "code": "A002",
  "message": "Invalid token",
  "timestamp": "2025-01-26T12:00:00",
  "errors": []
}
```

**계정 잠금**:
```json
{
  "success": false,
  "code": "A006",
  "message": "Account is locked due to multiple failed login attempts",
  "timestamp": "2025-01-26T12:00:00",
  "errors": []
}
```

**권한 부족**:
```json
{
  "success": false,
  "code": "A004",
  "message": "Access denied - Insufficient permissions",
  "timestamp": "2025-01-26T12:00:00",
  "errors": []
}
```

### 에러 처리 흐름

```
JwtAuthenticationFilter
    ↓ (Exception 발생)
JwtTokenProvider.validateToken()
    ↓ (BusinessException throw)
GlobalExceptionHandler
    ↓ (@ExceptionHandler)
ErrorResponse 생성
    ↓
JSON 응답
```

---

## 🔄 Refresh Token

### Refresh Token 사용 시나리오

```
1. Access Token 만료 (1시간 경과)
   ↓
2. 클라이언트가 API 요청 시 401 Unauthorized 수신
   ↓
3. 저장된 Refresh Token으로 재발급 요청
   POST /api/auth/refresh
   Headers: Authorization: Bearer {refreshToken}
   ↓
4. 서버가 Refresh Token 검증
   - 서명 검증
   - 만료 확인 (24시간 이내)
   - 사용자 존재 확인
   ↓
5. 새로운 토큰 쌍 발급
   {
     "accessToken": "new_access_token",
     "refreshToken": "new_refresh_token"
   }
   ↓
6. 클라이언트가 새 토큰 저장 및 재요청
```

### Refresh Token 구현 예시

```java
@PostMapping("/refresh")
public ResponseEntity<TokenResponse> refresh(
        @RequestHeader("Authorization") String refreshToken) {

    // 1. "Bearer " 제거
    String token = refreshToken.substring(7);

    // 2. Refresh Token 검증
    if (!tokenProvider.validateToken(token)) {
        throw new BusinessException(ErrorCode.INVALID_TOKEN);
    }

    // 3. 사용자명 추출
    String username = tokenProvider.getUsernameFromToken(token);

    // 4. 사용자 정보 로드
    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

    // 5. 새로운 Authentication 객체 생성
    Authentication authentication = new UsernamePasswordAuthenticationToken(
        userDetails, null, userDetails.getAuthorities()
    );

    // 6. 새 토큰 쌍 생성
    String newAccessToken = tokenProvider.generateAccessToken(authentication);
    String newRefreshToken = tokenProvider.generateRefreshToken(authentication);

    // 7. 응답
    return ResponseEntity.ok(new TokenResponse(
        newAccessToken,
        newRefreshToken,
        "Bearer",
        3600
    ));
}
```

### Refresh Token 보안 고려사항

1. **저장 위치**:
   - ✅ HttpOnly 쿠키 (XSS 공격 방지)
   - ✅ Secure Storage (모바일)
   - ❌ LocalStorage (XSS 취약)

2. **만료 정책**:
   - Access Token보다 긴 수명
   - 하지만 무제한은 위험
   - 사용 시마다 갱신 (Sliding Window)

3. **취소 메커니즘**:
   - DB에 Refresh Token 저장 (선택)
   - 로그아웃 시 무효화
   - 의심스러운 활동 시 강제 만료

---

## 💡 베스트 프랙티스

### 1. 토큰 저장

**클라이언트 사이드**:
```javascript
// ✅ Good: HttpOnly 쿠키 (서버에서 설정)
// 브라우저가 자동으로 전송, JavaScript 접근 불가

// ✅ Good: 메모리 (SPA)
const token = loginResponse.accessToken;
// 페이지 새로고침 시 사라짐 (보안 강화)

// ⚠️ Caution: SessionStorage
sessionStorage.setItem('token', token);
// 탭 닫으면 사라짐

// ❌ Bad: LocalStorage
localStorage.setItem('token', token);
// XSS 공격에 취약
```

### 2. 토큰 갱신 자동화

```javascript
// Axios Interceptor 예시
axios.interceptors.response.use(
  response => response,
  async error => {
    if (error.response.status === 401) {
      // Access Token 만료
      const refreshToken = getRefreshToken();
      const newTokens = await refreshAccessToken(refreshToken);
      setAccessToken(newTokens.accessToken);

      // 실패한 요청 재시도
      return axios(error.config);
    }
    return Promise.reject(error);
  }
);
```

### 3. 로그아웃

```java
@PostMapping("/logout")
public ResponseEntity<ApiResponse> logout(
        @RequestHeader("Authorization") String token) {

    // 1. 토큰 추출
    String jwt = token.substring(7);

    // 2. (선택) Refresh Token DB에서 제거
    // refreshTokenRepository.deleteByToken(jwt);

    // 3. (선택) Blacklist에 추가 (Redis)
    // redisTemplate.opsForValue().set(
    //     "blacklist:" + jwt,
    //     "true",
    //     tokenProvider.getExpirationTime(jwt),
    //     TimeUnit.MILLISECONDS
    // );

    // 4. 클라이언트에 응답
    // (클라이언트는 토큰 삭제)
    return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
}
```

### 4. 권한 체크

```java
// ✅ Good: 선언적 권한 체크
@PreAuthorize("hasPermission('USER_WRITE')")
public void updateUser() { }

// ✅ Good: 복잡한 조건
@PreAuthorize("hasPermission('USER_DELETE') and #userId != principal.id")
public void deleteUser(@PathVariable Long userId) { }

// ⚠️ Caution: 프로그래매틱 체크 (필요한 경우만)
public void updateUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (!auth.getAuthorities().contains(new SimpleGrantedAuthority("USER_WRITE"))) {
        throw new AccessDeniedException("Access denied");
    }
    // ...
}
```

---

## 📚 참고 자료

- [JWT 공식 사이트](https://jwt.io/)
- [JJWT 라이브러리](https://github.com/jwtk/jjwt)
- [Spring Security 문서](https://docs.spring.io/spring-security/reference/)
- [OWASP JWT Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)

---

## 🔗 관련 문서

- [README.md](./README.md) - 프로젝트 전체 개요
- [EC2_DEPLOYMENT_GUIDE.md](./EC2_DEPLOYMENT_GUIDE.md) - 배포 가이드
- [DEPLOYMENT.md](./DEPLOYMENT.md) - 배포 요약

---

**작성일**: 2025-01-26
**프로젝트**: SmartWork - Enterprise Intranet System
**Spring Boot**: 3.2.1 | **Java**: 21 | **JJWT**: 0.12.3
