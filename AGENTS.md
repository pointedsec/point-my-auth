# AGENTS.md — point-my-auth

## Project Context

- **Type**: Spring Boot Starter Library (AOP-based authorization)
- **Java**: 21 (LTS) and 25 (EA) — compile with 21, test with both via Maven profiles
- **Spring Boot**: 3.2.5
- **Build**: Maven multi-module
- **Group**: com.pointmyauth
- **Version**: 0.1.0-SNAPSHOT
- **Repo**: https://github.com/pointedsec/point-my-auth

## JDK Setup

```bash
# JDK 21 (default)
export JAVA_HOME=/usr/local/jdk-21
export PATH=$JAVA_HOME/bin:$PATH

# JDK 25 (EA)
export JAVA_HOME=/usr/local/jdk-25
export PATH=$JAVA_HOME/bin:$PATH

# Build with profile
mvn clean install -P java-21    # default, explicit
mvn clean install -P java-25    # test with JDK 25
mvn clean verify -P coverage    # with Jacoco report
```

## Branch Strategy

- `main` — stable releases
- `develop` — active development (default)
- Feature branches from `develop`
- Tags for releases: `v0.1.0`, `v0.2.0`, etc.

## Maven Profiles

| Profile | Purpose |
|---------|---------|
| `java-21` | Compile with Java 21 (default) |
| `java-25` | Compile with Java 25 |
| `coverage` | Jacoco coverage report |
| `release` | Maven Central deployment (sources, javadoc, GPG, OSSRH) |

## Build Commands

```bash
# Full build
mvn clean install -DskipTests
# Tests
mvn test -pl point-my-auth-core
# Coverage
mvn clean verify -P coverage
# Skip tests for fast compile
mvn compile -DskipTests
```

## Implementation Phases

### Phase 0 — Done (Scaffolding)
- Root POM, module POMs, directory structure
- JDK 21 + 25 installed (`/usr/local/jdk-21`, `/usr/local/jdk-25`)
- Maven 3.8.7 installed
- .gitignore, BLUEPRINT.md created

### Phase 1 — Core Annotations & Base Classes
- `@AuthorizeEntity` annotation
- `AuthorizationHandler<U>` interface
- `AuthorizationContext<U>` (immutable with Builder)
- `CurrentUserProvider<U>` functional interface
- `AuthorizationException` runtime exception
- All with Javadoc and jakarta.annotation nullability

### Phase 2 — Parameter Resolution
- `ParameterResolver` interface
- `PathVariableResolver` (maps to @PathVariable args)
- `RequestBodyResolver` (field-level via reflection, depth 2)
- `HeaderResolver` (#header: prefix, HttpServletRequest)
- `CompositeParameterResolver` (chain of responsibility)
- Unit tests for each

### Phase 3 — Handler Registry
- `AuthorizationHandlerRegistry` (Spring Bean first, reflection fallback)
- ConcurrentHashMap caching
- `IllegalStateException` when unresolvable

### Phase 4 — Aspect
- `AuthorizeEntityAspect` with `@Around` advice
- @Order(HIGHEST_PRECEDENCE + 10)
- Read annotation → resolve params → get user → build context → call handler → proceed
- Integration test with @SpringBootTest

### Phase 5 — Auto-configuration
- `PointMyAuthAutoConfiguration` with @ConditionalOnMissingBean
- `PointMyAuthConfigurer` interface
- META-INF/spring auto-configuration imports
- Example app with Order entity, OrderAuthorizationHandler, SecurityContextHolder

### Phase 6 — Full Test Suite
- Unit tests: AuthorizationContext, aspect mock, resolvers
- Integration: Spring context with aspect, handler resolution
- >85% coverage via Jacoco

### Phase 7 — Advanced Features
- @AuthorizeEntities (repeatable)
- AuthorizationPostProcessor
- AuthorizationAuditListener
- @ConditionalAuthorize
- AuthorizationCacheSupport

### Phase 8 — Distribution
- Maven Central configuration (GPG, OSSRH)
- CI workflow (Java 21 + 25 matrix)
- Release workflow (tag → publish)
- CHANGELOG, CONTRIBUTING

## Conventions

- Javadoc in English on all public API
- Nullability annotations (`@Nullable`, `@NonNull` from jakarta.annotation)
- Tests with JUnit 5 + Mockito + AssertJ
- Conventional Commits style: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`
- No Spring Web dependency in core (optional only)
