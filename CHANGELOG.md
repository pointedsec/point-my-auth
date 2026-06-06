# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- CI workflow with Java 21/25 matrix, Spotless, and SonarCloud
- Release workflow with tag-based GitHub Releases
- CHANGELOG.md
- CONTRIBUTING.md
- Comprehensive README.md with examples

## [0.1.0] - 2026-06-06

### Added

#### Core Annotations & Base Classes
- `@AuthorizeEntity` annotation with `ids`, `includeUser`, `authorizationCase`, `authorizationHandler` attributes
- `@AuthorizeEntities` container for repeatable annotations
- `@ConditionalAuthorize` for SpEL-based conditional authorization
- `AuthorizationHandler<U>` interface for domain-specific authorization logic
- `AuthorizationContext<U>` immutable context with Builder pattern
- `CurrentUserProvider<U>` functional interface for user resolution
- `PointMyAuthConfigurer` interface for auto-configuration customization
- `AuthorizationException` runtime exception for access denial

#### Parameter Resolution
- `ParameterResolver` interface
- `PathVariableResolver` for method parameter resolution
- `RequestBodyResolver` for `@RequestBody` field-level resolution (depth 2)
- `HeaderResolver` for HTTP header resolution via `#header:` prefix
- `CompositeParameterResolver` chain-of-responsibility resolver

#### AOP Aspect
- `AuthorizeEntityAspect` with `@Around` advice
- `@Order(HIGHEST_PRECEDENCE + 10)` priority
- Automatic parameter resolution, user injection, handler invocation
- Support for repeatable annotations and conditional authorization

#### Auto-Configuration
- `PointMyAuthAutoConfiguration` with `@ConditionalOnMissingBean`
- Spring Boot auto-configuration via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

#### Handler Registry
- `AuthorizationHandlerRegistry` with Spring Bean resolution + reflection fallback
- ConcurrentHashMap caching for resolved handlers

#### Advanced Features
- `AuthorizationPostProcessor` interface for post-authorization hooks
- `AuthorizationAuditListener` event bus for audit logging
- `AuthorizationEvent` record with success/failure, duration, and error details
- `AuthorizationCacheSupport` with configurable TTL and user-aware cache keys

#### Test Module
- `MockAuthorizationHandler` with configurable deny rules and invocation tracking
- `AuthorizationTestSupport` for building test contexts and mock providers
- `@PointMyAuthTest` annotation and `PointMyAuthTestExtension` JUnit 5 extension

#### Distribution
- CI workflow (Java 21/25 matrix, Spotless, SonarCloud)
- Release workflow (tag-based GitHub Releases)
- Maven Central configuration (GPG, OSSRH)

### Fixed
- `@Repeatable` annotation AOP pointcut matching
- Double `joinPoint.proceed()` bug in aspect
- Cache key now includes user identity to prevent cross-user cache hits
