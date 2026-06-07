# point-my-auth

[![CI](https://github.com/pointedsec/point-my-auth/actions/workflows/ci.yml/badge.svg)](https://github.com/pointedsec/point-my-auth/actions/workflows/ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=pointedsec_point-my-auth&metric=alert_status)](https://sonarcloud.io/dashboard?id=pointedsec_point-my-auth)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=pointedsec_point-my-auth&metric=coverage)](https://sonarcloud.io/dashboard?id=pointedsec_point-my-auth)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.pointedsec/point-my-auth-core)](https://central.sonatype.com/search?q=io.github.pointedsec)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**Declarative, annotation-driven authorization for Spring Boot.**

point-my-auth lets you enforce authorization rules on any Spring-managed method using simple annotations. No manual interceptor wiring, no boilerplate security checks — just annotate, implement a handler, and go.

```java
@AuthorizeEntity(
    ids = {"orderId"},
    includeUser = true,
    authorizationCase = "DELETE",
    authorizationHandler = OrderAuthorizationHandler.class)
public void deleteOrder(Long orderId) {
    // only reached if the handler approves
}
```

---

## Table of Contents

- [Why point-my-auth?](#why-point-my-auth)
- [Quick Start](#quick-start)
- [Installation](#installation)
- [Core Concepts](#core-concepts)
  - [Annotations](#annotations)
  - [AuthorizationHandler](#authorizationhandler)
  - [AuthorizationContext](#authorizationcontext)
  - [CurrentUserProvider](#currentuserprovider)
- [Advanced Features](#advanced-features)
  - [Repeatable Annotations](#repeatable-annotations)
  - [Admin Bypass](#admin-bypass)
  - [Conditional Authorization (SpEL)](#conditional-authorization-sel)
  - [Parameter Resolution](#parameter-resolution)
  - [Post-Processors](#post-processors)
  - [Audit Event Bus](#audit-event-bus)
  - [Authorization Cache](#authorization-cache)
- [Configuration](#configuration)
  - [PointMyAuthConfigurer](#pointmyauthconfigurer)
  - [AdminChecker](#adminchecker)
  - [Auto-Configuration](#auto-configuration)
  - [Cache Tuning](#cache-tuning)
- [Complete Examples](#complete-examples)
  - [Order Management API](#order-management-api)
  - [Multi-Tenant Application](#multi-tenant-application)
  - [Role-Based Access Control](#role-based-access-control)
  - [Custom User Type](#custom-user-type)
  - [Testing Authorization](#testing-authorization)
- [Testing Utilities](#testing-utilities)
  - [MockAuthorizationHandler](#mockauthorizationhandler)
  - [AuthorizationTestSupport](#authorizationtestsupport)
  - [JUnit 5 Extension](#junit-5-extension)
- [Project Structure](#project-structure)
- [Building from Source](#building-from-source)
- [Contributing](#contributing)
- [License](#license)

---

## Why point-my-auth?

Traditional Spring Security requires configuring method security, writing `@PreAuthorize` expressions, or building custom interceptors. point-my-auth takes a different approach:

| Feature | point-my-auth | Spring Security Method Security |
|---|---|---|
| **Learning curve** | 3 concepts (annotation, handler, user provider) | SpEL, SecurityExpressionHandler, MethodSecurityInterceptor |
| **Boilerplate** | One annotation per method | Expression strings + configuration classes |
| **Type safety** | Generic handler receives your user type directly | Requires casting from `Object` |
| **Parameter access** | Automatic resolution from method args, request body, headers | Manual SPEL path resolution |
| **Repeatable** | Stack multiple handlers on one method | Limited chaining |
| **Conditional logic** | SpEL expressions in `@ConditionalAuthorize` | Complex expression strings |
| **Cache** | Built-in per-context caching | No built-in caching |
| **Testing** | Dedicated test module with mocks | Mockito setup required |

---

## Quick Start

**5 minutes from zero to working authorization:**

### 1. Add the dependency

```xml
<dependency>
    <groupId>io.github.pointedsec</groupId>
    <artifactId>point-my-auth-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

### 2. Define your user type

```java
public record User(Long id, String name, boolean admin) {}
```

### 3. Create a handler

```java
@Component
public class OrderAuthorizationHandler implements AuthorizationHandler<User> {

    @Override
    public void authorize(AuthorizationContext<User> context) {
        User user = context.getCurrentUser();
        if (user == null) {
            throw new AuthorizationException("Not authenticated");
        }

        Long orderId = context.getLongId("orderId");
        if ("DELETE".equals(context.getAuthorizationCase()) && !user.admin()) {
            throw new AuthorizationException("Only admins can delete orders");
        }
    }
}
```

### 4. Annotate your methods

```java
@Service
public class OrderService {

    @AuthorizeEntity(
        ids = {"orderId"},
        includeUser = true,
        authorizationCase = "DELETE",
        authorizationHandler = OrderAuthorizationHandler.class)
    public void deleteOrder(Long orderId) {
        // deletion logic
    }
}
```

### 5. Wire the user provider

```java
@Configuration
public class AuthConfig {

    @Bean
    public PointMyAuthConfigurer authConfigurer() {
        return new PointMyAuthConfigurer() {
            @Override
            public CurrentUserProvider<Object> currentUserProvider() {
                return () -> SecurityContextHolder.getContext()
                        .getAuthentication().getPrincipal();
            }

            @Override
            public AdminChecker<Object> adminChecker() {
                return user -> ((User) user).admin();
            }
        };
    }
}
```

That's it. The aspect intercepts `deleteOrder`, resolves `orderId`, fetches the user, builds a context, and calls your handler. If the handler throws, the method is never reached.

---

## Installation

### Maven

```xml
<!-- Spring Boot Starter (recommended) -->
<dependency>
    <groupId>io.github.pointedsec</groupId>
    <artifactId>point-my-auth-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>

<!-- Or just the core library -->
<dependency>
    <groupId>io.github.pointedsec</groupId>
    <artifactId>point-my-auth-core</artifactId>
    <version>0.1.0</version>
</dependency>

<!-- Test utilities (for testing your handlers) -->
<dependency>
    <groupId>io.github.pointedsec</groupId>
    <artifactId>point-my-auth-test</artifactId>
    <version>0.1.0</version>
    <scope>test</scope>
</dependency>
```

### Gradle

```groovy
implementation 'io.github.pointedsec:point-my-auth-spring-boot-starter:0.1.0'
testImplementation 'io.github.pointedsec:point-my-auth-test:0.1.0'
```

### Requirements

- Java 21 or later
- Spring Boot 3.2+ or Spring Framework 6.1+
- AspectJ weaving (included via `spring-boot-starter-aop`)

---

## Core Concepts

### Annotations

#### `@AuthorizeEntity`

The primary annotation. Place it on any Spring-managed method to trigger authorization.

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(AuthorizeEntities.class)
public @interface AuthorizeEntity {
    String[] ids() default {};          // parameters to resolve
    boolean includeUser() default true;  // inject current user?
    String authorizationCase() default ""; // case label (e.g., "CREATE", "DELETE")
    Class<? extends AuthorizationHandler> authorizationHandler(); // handler class
}
```

**Parameters:**

| Attribute | Type | Default | Description |
|---|---|---|---|
| `ids` | `String[]` | `{}` | Names of parameters to resolve from method arguments |
| `includeUser` | `boolean` | `true` | Whether to fetch the current user via `CurrentUserProvider` |
| `authorizationCase` | `String` | `""` | Label passed to the handler (e.g., operation type) |
| `authorizationHandler` | `Class` | *required* | The handler class that makes the authorization decision |
| `skipForAdmin` | `boolean` | `true` | Skip handler for admin users when `AdminChecker` is configured |

#### `@ConditionalAuthorize`

SpEL-based conditional authorization. Evaluates an expression before calling the handler.

```java
@ConditionalAuthorize(
    condition = "#orderId > 0",
    authorizationHandler = OrderAuthorizationHandler.class)
public OrderDto getOrder(Long orderId) { ... }
```

The SpEL expression has access to all method parameters by name. Also supports `skipForAdmin` — set to `false` to force evaluation even for admin users.

#### `@AuthorizeEntities`

Container annotation for repeatable `@AuthorizeEntity`. Used when stacking multiple handlers:

```java
@AuthorizeEntities({
    @AuthorizeEntity(ids = {"orderId"}, authorizationHandler = OrderHandler.class),
    @AuthorizeEntity(ids = {"#header:X-Tenant-Id"}, authorizationHandler = TenantHandler.class)
})
public OrderDto getOrder(Long orderId) { ... }
```

---

### AuthorizationHandler

The core interface your authorization logic implements:

```java
public interface AuthorizationHandler<U> {
    void authorize(AuthorizationContext<U> context);
}
```

- **Return silently** → access granted, method executes
- **Throw `AuthorizationException`** → access denied, method skipped

The generic parameter `U` is your application's user type — it's whatever your `CurrentUserProvider` returns.

---

### AuthorizationContext

Immutable object passed to your handler with all the information it needs:

```java
AuthorizationContext<User> context = ...;

// Current user (if includeUser = true)
User user = context.getCurrentUser();

// Resolved parameters
Long orderId = context.getLongId("orderId");
String name = context.getStringId("name");
Object value = context.getId("paramName", Object.class);

// Authorization case label
String authCase = context.getAuthorizationCase(); // e.g., "DELETE"

// The intercepted method (for reflection-based logic)
Method method = context.getInterceptedMethod();
```

---

### CurrentUserProvider

A functional interface that provides the current authenticated user:

```java
@FunctionalInterface
public interface CurrentUserProvider<U> {
    U getCurrentUser(); // return null if unauthenticated
}
```

You register it via `PointMyAuthConfigurer`:

```java
@Bean
public PointMyAuthConfigurer authConfigurer() {
    return () -> () -> {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return (User) auth.getPrincipal();
    };
}
```

---

## Advanced Features

### Repeatable Annotations

Apply multiple authorization rules to a single method:

```java
@Service
public class OrderService {

    @AuthorizeEntity(
        ids = {"orderId"},
        includeUser = true,
        authorizationHandler = OrderAuthorizationHandler.class)
    @AuthorizeEntity(
        ids = {"#header:X-Tenant-Id"},
        includeUser = false,
        authorizationHandler = TenantAuthorizationHandler.class)
    public OrderDto getOrder(Long orderId) {
        return orderRepository.findById(orderId);
    }
}
```

Both handlers are called in order. If either throws `AuthorizationException`, the method is skipped.

---

### Admin Bypass

Skip authorization entirely for admin users without modifying your handlers. Register an `AdminChecker` bean that defines how to identify admin users, and all `@AuthorizeEntity` / `@ConditionalAuthorize` annotations with `skipForAdmin = true` (the default) will be skipped for admin users.

#### Configuration

```java
@Bean
public PointMyAuthConfigurer authConfigurer() {
    return new PointMyAuthConfigurer() {
        @Override
        public CurrentUserProvider<Object> currentUserProvider() {
            return () -> SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
        }

        @Override
        public AdminChecker<?> adminChecker() {
            return user -> ((AppUser) user).isAdmin();
        }
    };
}
```

#### How it works

| `skipForAdmin` | User is admin | Behavior |
|---|---|---|
| `true` (default) | Yes | Handler skipped entirely |
| `true` (default) | No | Handler called normally |
| `false` | Yes | Handler called normally |
| `false` | No | Handler called normally |

```java
// Admins skip this handler entirely (skipForAdmin = true by default)
@AuthorizeEntity(
    ids = {"orderId"},
    includeUser = true,
    authorizationCase = "DELETE",
    authorizationHandler = OrderAuthorizationHandler.class)
public void deleteOrder(Long orderId) { ... }

// Handler always runs, even for admins
@AuthorizeEntity(
    ids = {"orderId"},
    includeUser = true,
    skipForAdmin = false,
    authorizationCase = "DELETE",
    authorizationHandler = OrderAuthorizationHandler.class)
public void forceDeleteOrder(Long orderId) { ... }

// ConditionalAuthorize also supports skipForAdmin
@ConditionalAuthorize(
    condition = "#orderId > 0",
    skipForAdmin = true,
    authorizationHandler = OrderAuthorizationHandler.class)
public OrderDto getOrder(Long orderId) { ... }
```

Works with repeatable annotations too — each annotation is checked independently:

```java
@AuthorizeEntity(
    ids = {"orderId"},
    includeUser = true,
    authorizationHandler = OrderHandler.class)      // skipped for admin
@AuthorizeEntity(
    ids = {"#header:X-Tenant-Id"},
    includeUser = false,
    skipForAdmin = false,
    authorizationHandler = TenantHandler.class)     // NOT skipped for admin
public OrderDto getOrder(Long orderId) { ... }
```

---

### Conditional Authorization (SpEL)

Use Spring Expression Language to evaluate conditions before the handler:

```java
// Numeric comparison
@ConditionalAuthorize(condition = "#orderId > 0", authorizationHandler = OrderAuthorizationHandler.class)
public OrderDto getOrder(Long orderId) { ... }

// String validation
@ConditionalAuthorize(
    condition = "#name != null and #name.length() > 0",
    authorizationHandler = OrderAuthorizationHandler.class)
public List<OrderDto> findByName(String name) { ... }

// Object attribute access
@ConditionalAuthorize(
    condition = "#request.companyId != null and #request.companyId.length() >= 3",
    authorizationHandler = OrderAuthorizationHandler.class)
public OrderDto createOrder(CreateOrderRequest request) { ... }

// List size check
@ConditionalAuthorize(
    condition = "#orderIds.size() <= 10",
    authorizationHandler = OrderAuthorizationHandler.class)
public void bulkProcess(List<Long> orderIds) { ... }

// Multiple conditions
@ConditionalAuthorize(
    condition = "#orderId > 0 and #orderId < 1000000",
    authorizationHandler = OrderAuthorizationHandler.class)
public OrderDto getOrder(Long orderId) { ... }

// Enum/string equality
@ConditionalAuthorize(
    condition = "#status == 'ACTIVE' or #status == 'PENDING'",
    authorizationHandler = OrderAuthorizationHandler.class)
public List<OrderDto> getByStatus(String status) { ... }

// Nested object attribute
@ConditionalAuthorize(
    condition = "#request != null and #request.name != null and #request.name.length() > 2",
    authorizationHandler = OrderAuthorizationHandler.class)
public OrderDto create(CreateOrderRequest request) { ... }

// Null check
@ConditionalAuthorize(
    condition = "#orderId != null",
    authorizationHandler = OrderAuthorizationHandler.class)
public OrderDto getOrder(Long orderId) { ... }
```

---

### Parameter Resolution

The `ids` array tells the framework which method parameters to resolve and pass to the handler:

#### Method parameters by name

```java
@AuthorizeEntity(ids = {"orderId"}, ...)
public OrderDto getOrder(Long orderId) { ... }
```

The `orderId` parameter value is resolved and available in `context.getLongId("orderId")`.

#### Request body field access

```java
@AuthorizeEntity(ids = {"request.companyId"}, ...)
public OrderDto createOrder(@RequestBody CreateOrderRequest request) { ... }
```

Navigates into `request.companyId` via reflection.

#### HTTP Headers

Prefix with `#header:` to resolve HTTP headers:

```java
@AuthorizeEntity(ids = {"#header:X-Tenant-Id"}, ...)
public List<OrderDto> getOrdersByTenant(@RequestHeader("X-Tenant-Id") String tenantId) { ... }
```

#### Multiple parameters

```java
@AuthorizeEntity(ids = {"orderId", "userId"}, ...)
public OrderDto getOrderForUser(Long orderId, Long userId) { ... }
```

Both values are resolved and available in the context.

#### Full request body

```java
@AuthorizeEntity(ids = {"request"}, ...)
public OrderDto createOrder(@RequestBody CreateOrderRequest request) { ... }
```

The entire request body object is available via `context.getId("request", CreateOrderRequest.class)`.

---

### Post-Processors

Implement `AuthorizationPostProcessor` to run code after every authorization attempt:

```java
public interface AuthorizationPostProcessor {
    void afterAuthorization(AuthorizationContext<?> context, boolean success);
}
```

Register as a Spring bean:

```java
@Component
public class LoggingPostProcessor implements AuthorizationPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(LoggingPostProcessor.class);

    @Override
    public void afterAuthorization(AuthorizationContext<?> context, boolean success) {
        String method = context.getInterceptedMethod().getName();
        if (success) {
            log.info("[AUTH GRANTED] method={}", method);
        } else {
            log.warn("[AUTH DENIED] method={}", method);
        }
    }
}
```

Multiple post-processors can be registered — they all run in order.

---

### Audit Event Bus

`AuthorizationAuditListener` publishes events for every authorization attempt:

```java
@Configuration
public class AuditConfig {

    @Bean
    public AuthorizationAuditListener auditListener() {
        AuthorizationAuditListener listener = new AuthorizationAuditListener();

        listener.addEventListener(event -> {
            if (event.succeeded()) {
                log.info("[AUDIT] GRANTED {} case={} duration={}ns",
                    event.handlerClass().getSimpleName(),
                    event.authorizationCase(),
                    event.durationNanos());
            } else {
                log.warn("[AUDIT] DENIED {} case={} error={} duration={}ns",
                    event.handlerClass().getSimpleName(),
                    event.authorizationCase(),
                    event.errorMessage(),
                    event.durationNanos());
            }
        });

        return listener;
    }
}
```

The `AuthorizationEvent` record contains:

| Field | Type | Description |
|---|---|---|
| `context` | `AuthorizationContext` | The authorization context |
| `handlerClass` | `Class<?>` | The handler that was invoked |
| `succeeded` | `boolean` | Whether authorization was granted |
| `durationNanos` | `long` | Execution time in nanoseconds |
| `errorMessage` | `String` | Error message if denied (null if granted) |

---

### Authorization Cache

`AuthorizationCacheSupport` caches authorization results to avoid redundant handler invocations:

```java
// The cache is managed by auto-configuration and injected into the aspect.
// Results are cached per (method + resolvedIds + authCase + user).
```

The cache key includes:
- Method name
- Resolved parameter values
- Authorization case
- Current user identity

This ensures different users get different cache entries for the same method.

---

## Configuration

### PointMyAuthConfigurer

Register a `PointMyAuthConfigurer` bean to wire your user provider:

```java
@Configuration
public class AuthConfig {

    private User currentUser;

    @Bean
    public PointMyAuthConfigurer authConfigurer() {
        return () -> () -> currentUser;
    }

    // For testing:
    public void setCurrentUser(User user) { this.currentUser = user; }
    public void clearCurrentUser() { this.currentUser = null; }
}
```

### AdminChecker

Implement `AdminChecker` to define how admin users are identified. When registered via `PointMyAuthConfigurer.adminChecker()`, the aspect skips authorization for admin users on annotations with `skipForAdmin = true`.

```java
@Bean
public PointMyAuthConfigurer authConfigurer() {
    return new PointMyAuthConfigurer() {
        @Override
        public CurrentUserProvider<Object> currentUserProvider() {
            return () -> SecurityContextHolder.getContext()
                    .getAuthentication().getPrincipal();
        }

        @Override
        public AdminChecker<?> adminChecker() {
            return user -> ((AppUser) user).isAdmin();
        }
    };
}
```

The `AdminChecker` is a functional interface — you can also use a lambda:

```java
@Override
public AdminChecker<?> adminChecker() {
    return user -> user instanceof AppUser appUser && appUser.admin();
}
```

If `adminChecker()` returns `null` (the default), admin bypass is disabled entirely and all handlers run normally.

### Auto-Configuration

`PointMyAuthAutoConfiguration` registers these beans automatically:

| Bean | Description |
|---|---|
| `AuthorizationHandlerRegistry` | Resolves handler classes to instances |
| `AuthorizeEntityAspect` | AOP aspect intercepting annotated methods |
| `CurrentUserProvider` | From your `PointMyAuthConfigurer` |
| `AdminChecker` | From your `PointMyAuthConfigurer` (optional, enables admin bypass) |
| `AuthorizationAuditListener` | Event bus for audit logging |
| `AuthorizationCacheSupport` | Result caching |

All beans use `@ConditionalOnMissingBean` — override any of them with your own `@Bean`.

### Cache Tuning

```java
@Bean
public AuthorizationCacheSupport authorizationCacheSupport() {
    AuthorizationCacheSupport cache = new AuthorizationCacheSupport();
    cache.setTtlMillis(300_000); // 5 minutes
    return cache;
}
```

---

## Complete Examples

### Order Management API

A full REST API with authorization on every endpoint:

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // 1. Simple GET with path variable
    @GetMapping("/{orderId}")
    @AuthorizeEntity(
        ids = {"orderId"},
        includeUser = false,
        authorizationCase = "READ",
        authorizationHandler = OrderAuthorizationHandler.class)
    public ResponseEntity<OrderDto> getOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    // 2. POST with request body field resolution
    @PostMapping
    @AuthorizeEntity(
        ids = {"request.companyId"},
        includeUser = true,
        authorizationCase = "CREATE",
        authorizationHandler = OrderAuthorizationHandler.class)
    public ResponseEntity<OrderDto> createOrder(
            @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    // 3. DELETE with user check and case label
    @DeleteMapping("/{orderId}")
    @AuthorizeEntity(
        ids = {"orderId"},
        includeUser = true,
        authorizationCase = "DELETE",
        authorizationHandler = OrderAuthorizationHandler.class)
    public ResponseEntity<Void> deleteOrder(@PathVariable Long orderId) {
        orderService.deleteOrder(orderId);
        return ResponseEntity.noContent().build();
    }

    // 4. GET with header-based tenant authorization
    @GetMapping("/tenant")
    @AuthorizeEntity(
        ids = {"#header:X-Tenant-Id"},
        includeUser = false,
        authorizationCase = "TENANT_READ",
        authorizationHandler = TenantAuthorizationHandler.class)
    public ResponseEntity<List<OrderDto>> getOrdersByTenant(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        return ResponseEntity.ok(orderService.getByTenant(tenantId));
    }

    // 5. Multiple authorization rules on one method
    @GetMapping("/{orderId}/user/{userId}")
    @AuthorizeEntity(
        ids = {"orderId", "userId"},
        includeUser = true,
        authorizationCase = "OWNERSHIP_CHECK",
        authorizationHandler = OrderAuthorizationHandler.class)
    @AuthorizeEntity(
        ids = {"#header:X-Tenant-Id"},
        includeUser = false,
        authorizationHandler = TenantAuthorizationHandler.class)
    public ResponseEntity<OrderDto> getOrderForUser(
            @PathVariable Long orderId,
            @PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrderForUser(orderId, userId));
    }

    // 6. Health check — always allowed
    @GetMapping("/health")
    @AuthorizeEntity(
        ids = {},
        includeUser = false,
        authorizationHandler = AllowAllAuthorizationHandler.class)
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
```

---

### Multi-Tenant Application

```java
@Component
public class TenantAuthorizationHandler implements AuthorizationHandler<Object> {

    private final TenantRepository tenantRepository;

    public TenantAuthorizationHandler(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public void authorize(AuthorizationContext<Object> context) {
        String tenantId = context.getStringId("X-Tenant-Id");
        if (tenantId == null || !tenantRepository.existsById(tenantId)) {
            throw new AuthorizationException("Invalid tenant: " + tenantId);
        }
    }
}
```

---

### Role-Based Access Control

```java
@Component
public class AdminOnlyHandler implements AuthorizationHandler<User> {

    @Override
    public void authorize(AuthorizationContext<User> context) {
        User user = context.getCurrentUser();
        if (user == null) {
            throw new AuthorizationException("Not authenticated");
        }
        if (!user.isAdmin()) {
            throw new AuthorizationException(
                "Admin role required, got: " + user.role());
        }
    }
}

// Usage:
@AuthorizeEntity(ids = {}, authorizationHandler = AdminOnlyHandler.class)
public void adminOnlyOperation() { ... }

// Or use Admin Bypass instead of a dedicated handler:
// 1. Configure AdminChecker in your PointMyAuthConfigurer
// 2. Use skipForAdmin=true (default) — admins skip the handler entirely
@AuthorizeEntity(ids = {}, authorizationHandler = AnyHandler.class)
public void adminBypassOperation() { ... }

// Or disable bypass for specific endpoints:
@AuthorizeEntity(ids = {}, skipForAdmin = false, authorizationHandler = AuditHandler.class)
public void auditTrailOperation() { ... }
```

---

### Custom User Type

The framework is fully generic — use any user type:

```java
// Your domain user
public record Employee(
    Long id,
    String email,
    Department department,
    Set<String> permissions) {}

// Handler using your type
@Component
public class EmployeeAuthorizationHandler implements AuthorizationHandler<Employee> {

    @Override
    public void authorize(AuthorizationContext<Employee> context) {
        Employee employee = context.getCurrentUser();
        if (employee == null) {
            throw new AuthorizationException("Not authenticated");
        }

        String requiredPermission = context.getAuthorizationCase();
        if (!employee.permissions().contains(requiredPermission)) {
            throw new AuthorizationException(
                "Missing permission: " + requiredPermission);
        }
    }
}

// Configuration
@Bean
public PointMyAuthConfigurer authConfigurer() {
    return () -> {
        Employee emp = employeeService.getCurrentEmployee();
        return () -> emp;
    };
}
```

---

### Testing Authorization

```java
@SpringBootTest(classes = ExampleApplication.class)
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AuthConfig authConfig;

    @BeforeEach
    void setUp() {
        authConfig.clearCurrentUser();
    }

    @Test
    void shouldAllowWithValidUser() {
        authConfig.setCurrentUser(new User(1L, "Alice", false));
        OrderDto result = orderService.getOrder(100L);
        assertThat(result).isNotNull();
    }

    @Test
    void shouldDenyWithoutUser() {
        assertThatThrownBy(() -> orderService.getOrder(100L))
            .isInstanceOf(AuthorizationException.class)
            .hasMessageContaining("Not authenticated");
    }

    @Test
    void shouldDenyDeleteForNonAdmin() {
        authConfig.setCurrentUser(new User(1L, "Alice", false));
        assertThatThrownBy(() -> orderService.deleteOrder(100L))
            .isInstanceOf(AuthorizationException.class)
            .hasMessageContaining("Only admins can delete orders");
    }

    @Test
    void adminShouldBypassHandler() {
        authConfig.setCurrentUser(new User(1L, "Admin", true));
        // With AdminChecker configured, admin bypasses the handler
        orderService.deleteOrder(100L);
        // No exception — handler was skipped
    }
}
```

---

## Testing Utilities

The `point-my-auth-test` module provides dedicated test support:

### MockAuthorizationHandler

A configurable handler for unit tests:

```java
@PointMyAuthTest
class HandlerTest {

    @Test
    void testDenyRules(MockAuthorizationHandler<User> handler) {
        // Default: grants all
        handler.authorize(context);
        assertThat(handler.wasInvoked()).isTrue();
        assertThat(handler.invocationCount()).isEqualTo(1);

        // Verify last invocation
        handler.lastContext(ctx -> {
            assertThat(ctx.getCurrentUser()).isNotNull();
            assertThat(ctx.getAuthorizationCase()).isEqualTo("DELETE");
        });

        // Reset between tests
        handler.reset();
    }

    @Test
    void testDenyAll() {
        handler.denyAll("Access denied");
        assertThatThrownBy(() -> handler.authorize(context))
            .isInstanceOf(AuthorizationException.class)
            .hasMessageContaining("Access denied");
    }

    @Test
    void testDenyWhenUnauthenticated() {
        handler.denyWhenUnauthenticated("Not authenticated");
        handler.denyWhen(ctx -> "ADMIN".equals(ctx.getAuthorizationCase()));
    }
}
```

### AuthorizationTestSupport

Static helpers for building test contexts:

```java
@Test
void buildTestContext() {
    AuthorizationContext<User> ctx = AuthorizationTestSupport.context()
        .resolvedId("orderId", 42L)
        .resolvedId("name", "Test Order")
        .user(new User(1L, "Alice", false))
        .authCase("DELETE")
        .method(OrderService.class, "deleteOrder")
        .build();

    assertThat(ctx.getLongId("orderId")).isEqualTo(42L);
    assertThat(ctx.getCurrentUser()).isNotNull();
    assertThat(ctx.getAuthorizationCase()).isEqualTo("DELETE");
}

@Test
void createMockProviders() {
    CurrentUserProvider<User> provider = AuthorizationTestSupport.userProvider(user);
    CurrentUserProvider<User> unauth = AuthorizationTestSupport.unauthenticatedProvider();

    PointMyAuthConfigurer configurer = AuthorizationTestSupport.configurer(user);
    PointMyAuthConfigurer unauthConfig = AuthorizationTestSupport.unauthenticatedConfigurer();
}

@Test
void assertAuthorizationDenied() {
    AuthorizationException ex = new AuthorizationException("Access denied");
    AuthorizationTestSupport.assertAuthorizationDenied(ex, "Access denied");
}
```

### JUnit 5 Extension

Auto-clears cache and provides injectable test utilities:

```java
@PointMyAuthTest
class IntegrationTest {

    @Test
    void testWithAutoInjectedUtils(
            MockAuthorizationHandler<User> handler,
            AuthorizationCacheSupport cache) {
        // cache is cleared before this test
        handler.denyAll("denied");
        handler.authorize(context);
        // handler is reset after this test
    }
}
```

---

## Project Structure

```
point-my-auth/
├── pom.xml                              # Parent POM
├── point-my-auth-core/                  # Core library
│   └── src/main/java/com/pointmyauth/
│       ├── annotation/                  # @AuthorizeEntity, @ConditionalAuthorize
│       ├── aspect/                      # AOP aspect
│       ├── audit/                       # Audit event bus
│       ├── cache/                       # Authorization cache
│       ├── config/                      # Auto-configuration
│       ├── context/                     # AuthorizationContext
│       ├── exception/                   # AuthorizationException
│       ├── handler/                     # Handler interface + registry
│       ├── processor/                   # Post-processor interface
│       ├── resolver/                    # Parameter resolvers
│       └── user/                        # CurrentUserProvider, AdminChecker
├── point-my-auth-test/                  # Test utilities
│   └── src/main/java/com/pointmyauth/test/
│       ├── AuthorizationTestSupport.java
│       ├── MockAuthorizationHandler.java
│       ├── PointMyAuthTest.java
│       └── PointMyAuthTestExtension.java
├── point-my-auth-spring-boot-starter/   # Spring Boot starter
├── examples/point-my-auth-example/      # Example application
├── .github/workflows/
│   ├── ci.yml                           # CI pipeline
│   └── release.yml                      # Release pipeline
├── CHANGELOG.md
├── CONTRIBUTING.md
└── README.md
```

---

## Building from Source

### Prerequisites

- JDK 21 (LTS) installed
- Maven 3.8+ installed

### Build Commands

```bash
# Full build
mvn clean install

# Skip tests (faster)
mvn clean install -DskipTests

# Run tests only
mvn test -pl point-my-auth-core

# Run with coverage
mvn clean verify -P coverage

# Run with JDK 25
export JAVA_HOME=/usr/local/jdk-25
mvn clean verify -P java-25

# Check code formatting
mvn spotless:check

# Apply code formatting
mvn spotless:apply
```

### CI/CD

- **CI workflow** runs on push/PR to `develop` and `main`
  - Build & test matrix: Java 21 + Java 25
  - Code formatting check (Spotless)
  - SonarCloud analysis
  - JaCoCo coverage + Codecov upload

- **Release workflow** runs on tag push (`v*`)
  - Full build with coverage
  - GitHub Release creation with JARs attached

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on:

- Development setup
- Code style and conventions
- Pull request process
- Commit message format

---

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
