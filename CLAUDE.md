# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**SmartWork** is an enterprise-grade intranet system built with Spring Boot and Java 21, demonstrating production-ready patterns for workflow management, authentication, and business process automation.

**Purpose**: Portfolio project showcasing enterprise Java development skills including Spring Security, JWT authentication, AOP, transactional processing, and Oracle database integration.

## Technology Stack

- **Java**: 21 (with preview features enabled)
- **Framework**: Spring Boot 3.2.1
- **Database**: Oracle 19c
- **Security**: Spring Security + JWT (JJWT 0.12.3)
- **ORM**: JPA (Hibernate)
- **Build Tool**: Gradle 8.5
- **API Documentation**: SpringDoc OpenAPI 3.0

## Project Structure

```
smartwork/
├── build.gradle                # Gradle build configuration
├── settings.gradle             # Gradle settings
├── src/main/java/com/smartwork/
│   ├── SmartWorkApplication.java       # Main application
│   ├── domain/                         # JPA Entities
│   │   ├── BaseEntity.java
│   │   ├── User.java
│   │   ├── Role.java
│   │   └── Permission.java
│   ├── repository/                     # Spring Data JPA Repositories
│   │   └── UserRepository.java
│   ├── service/                        # Business Logic (planned)
│   ├── controller/                     # REST Controllers (planned)
│   ├── security/                       # Security & JWT
│   │   ├── jwt/JwtTokenProvider.java
│   │   ├── filter/JwtAuthenticationFilter.java
│   │   ├── service/UserDetailsServiceImpl.java
│   │   └── config/                     # SecurityConfig (planned)
│   ├── config/                         # App Configuration (planned)
│   ├── exception/                      # Exception Handling
│   │   ├── ErrorCode.java
│   │   ├── BusinessException.java
│   │   └── GlobalExceptionHandler.java
│   ├── dto/                            # Data Transfer Objects
│   │   ├── ApiResponse.java
│   │   └── ErrorResponse.java
│   └── aop/                            # AOP Aspects
│       ├── LoggingAspect.java
│       └── TransactionAspect.java
└── src/main/resources/
    └── application.yml                 # Application configuration
```

## Build Commands

### Build Project
```bash
./gradlew clean build
```

### Run Application
```bash
./gradlew bootRun
```

### Run Tests
```bash
./gradlew test
```

### Skip Tests
```bash
./gradlew build -x test
```

### Generate Dependency Report
```bash
./gradlew dependencies
```

### Check for Dependency Updates
```bash
./gradlew dependencyUpdates
```

## Database Setup

### Oracle Database Configuration

**Required Setup**:
1. Create Oracle user and tablespace:
```sql
CREATE USER smartwork IDENTIFIED BY smartwork123;
GRANT CONNECT, RESOURCE, DBA TO smartwork;
ALTER USER smartwork QUOTA UNLIMITED ON USERS;
```

2. Create sequences:
```sql
CREATE SEQUENCE USER_SEQ START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE ROLE_SEQ START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE PERMISSION_SEQ START WITH 1 INCREMENT BY 1;
```

3. Database connection configured in `src/main/resources/application.yml`

### JPA/Hibernate Configuration

- **DDL Mode**: `validate` (production-safe)
- **Dialect**: Oracle12cDialect
- **Batch Processing**: Enabled with batch_size=20
- **SQL Logging**: Enabled in DEBUG mode

## Key Architectural Patterns

**1. AOP-Based Cross-Cutting Concerns**
- **Logging**: `aop/LoggingAspect.java`
  - Automatic method entry/exit logging
  - Performance monitoring with execution time
  - Slow method detection (>3 seconds)
- **Transaction Monitoring**: `aop/TransactionAspect.java`
  - Transaction boundary logging
  - Rollback scenario tracking

**2. Exception Handling Strategy**
- **Global Handler**: `exception/GlobalExceptionHandler.java`
- **Custom Exceptions**: `BusinessException` with `ErrorCode` enum
- **Structured Responses**: `ErrorResponse` DTO

**3. JWT Authentication Flow**
- **Token Generation**: `security/jwt/JwtTokenProvider.java`
- **Token Validation**: `security/filter/JwtAuthenticationFilter.java`
- **User Loading**: `security/service/UserDetailsServiceImpl.java`

**4. Audit Trail System**
- **BaseEntity**: Automatic audit fields (created/updated timestamps and users)
- **Soft Delete**: isDeleted flag
- **JPA Auditing**: @EntityListeners

**5. RBAC (Role-Based Access Control)**
- **Entities**: User → Role → Permission
- **Many-to-Many**: Users have multiple roles, roles have multiple permissions

## Development Guidelines

### Adding New Features

**1. Add New Domain Entity**:
```java
@Entity
@Table(name = "table_name")
public class EntityName extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_name")
    @SequenceGenerator(name = "seq_name", sequenceName = "SEQ_NAME", allocationSize = 1)
    private Long id;
    // ... fields
}
```

**2. Create Repository**:
```java
@Repository
public interface EntityRepository extends JpaRepository<EntityName, Long> {
    // Custom query methods
}
```

**3. Implement Service Layer**:
```java
@Service
@RequiredArgsConstructor
public class EntityService {
    private final EntityRepository repository;

    @Transactional(readOnly = true)
    public Entity findById(Long id) {
        // ...
    }

    @Transactional
    public Entity save(Entity entity) {
        // ...
    }
}
```

**4. Create REST Controller**:
```java
@RestController
@RequestMapping("/api/entities")
@RequiredArgsConstructor
public class EntityController {
    private final EntityService service;

    @GetMapping("/{id}")
    public ApiResponse<EntityDto> getEntity(@PathVariable Long id) {
        // ...
    }
}
```

### Transaction Management

```java
@Transactional(readOnly = true)  // For queries
public List<Entity> findAll() { }

@Transactional(isolation = Isolation.SERIALIZABLE)  // For critical operations
public void criticalOperation() { }
```

### Error Handling

```java
// Throw business exception
throw new BusinessException(ErrorCode.USER_NOT_FOUND);

// Add new error code in ErrorCode.java
USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "User not found")
```

## API Documentation

### Swagger/OpenAPI

- **UI URL**: http://localhost:8080/swagger-ui.html
- **API Docs JSON**: http://localhost:8080/api-docs

### API Response Structure

**Success**:
```json
{
  "success": true,
  "message": "Success",
  "data": {...},
  "timestamp": "2025-10-25T20:30:00"
}
```

**Error**:
```json
{
  "code": "U001",
  "message": "User not found",
  "status": 404,
  "timestamp": "2025-10-25T20:30:00",
  "errors": []
}
```

## Code Conventions

### Package Structure
- `domain/` - JPA entities
- `repository/` - Spring Data repositories
- `service/` - Business logic
- `controller/` - REST controllers
- `security/` - Security & JWT
- `config/` - Configuration classes
- `exception/` - Custom exceptions
- `dto/` - Data transfer objects
- `aop/` - Aspect classes

### Naming Conventions
- **Entities**: PascalCase (e.g., `User`, `BoardPost`)
- **Tables**: lowercase_underscore (e.g., `users`, `board_posts`)
- **Columns**: lowercase_underscore (e.g., `user_id`, `created_at`)
- **Sequences**: UPPERCASE_SEQ (e.g., `USER_SEQ`)
- **Methods**: camelCase (e.g., `findUserById`)
- **Constants**: UPPERCASE_UNDERSCORE (e.g., `MAX_LOGIN_ATTEMPTS`)

## Testing Strategy

### Unit Tests
```bash
./gradlew test --tests UserServiceTest
```

### Integration Tests
```bash
./gradlew integrationTest
```

## Logging Guidelines

**Logging Levels**:
- **ERROR**: System errors, exceptions
- **WARN**: Unexpected scenarios, slow operations
- **INFO**: Important business events
- **DEBUG**: Detailed execution flow
- **TRACE**: Very detailed debugging

## Performance Optimization

- Add indexes on foreign keys and frequently queried columns
- Use `@Transactional(readOnly=true)` for queries
- Enable Hibernate batch processing
- Use `@EntityGraph` to avoid N+1 queries

## Deployment

### Environment Variables
```bash
DB_PASSWORD=your_oracle_password
JWT_SECRET=your_base64_encoded_secret
SPRING_PROFILES_ACTIVE=prod
```

### Build for Production
```bash
./gradlew clean build -x test
java -jar build/libs/smartwork-1.0.0-SNAPSHOT.jar
```

## Troubleshooting

**Issue**: Cannot connect to Oracle database
**Solution**: Verify Oracle service, check connection string

**Issue**: JWT token validation fails
**Solution**: Check JWT secret matches, verify token hasn't expired

**Issue**: N+1 query problem
**Solution**: Use `@EntityGraph` or `JOIN FETCH`

## Project Roadmap

**Completed**:
- ✅ Spring Boot 3.2 with Java 21
- ✅ Spring Security + JWT authentication
- ✅ AOP-based logging and transaction monitoring
- ✅ Global exception handling
- ✅ User/Role/Permission RBAC model
- ✅ Base entity with audit trail
- ✅ Oracle database integration
- ✅ Gradle build configuration

**Planned**:
- ⏳ Board management module
- ⏳ Approval workflow module
- ⏳ Attendance management module
- ⏳ File upload/download (Oracle BLOB)
- ⏳ Comprehensive test coverage
- ⏳ CI/CD with Jenkins/GitHub Actions
- ⏳ Docker containerization
