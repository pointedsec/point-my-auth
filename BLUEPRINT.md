# point-my-auth — Blueprint de Desarrollo

> Documento de referencia para construir la librería `point-my-auth` de forma agéntica con IA.  
> Stack: Java 17+, Spring Boot 3.x, AOP (AspectJ), Maven Central.

---

## 1. Visión y Propósito

`point-my-auth` desacopla completamente la **lógica de autorización** de los servicios de negocio en aplicaciones Spring Boot. Mediante una anotación declarativa `@AuthorizeEntity` y un sistema de *handlers*, cada dominio/entidad define sus propias reglas de autorización sin contaminar el código de servicio.

### Problema que resuelve

```java
// ❌ Sin la librería — lógica de autorización mezclada con negocio
public OrderDto getOrder(Long orderId, Long userId) {
    Order order = orderRepo.findById(orderId).orElseThrow();
    if (!order.getUserId().equals(userId) && !currentUser.isAdmin()) {
        throw new AccessDeniedException("Not authorized");
    }
    return mapper.toDto(order); // lógica real
}

// ✅ Con point-my-auth — autorización declarativa
@AuthorizeEntity(
    ids = {"orderId"},
    includeUser = true,
    authorizationHandler = OrderAuthorizationHandler.class
)
public OrderDto getOrder(Long orderId) {
    return mapper.toDto(orderRepo.findById(orderId).orElseThrow());
}
```

---

## 2. Estructura del Proyecto

```
point-my-auth/
├── point-my-auth-core/                  # Módulo principal de la librería
│   ├── src/main/java/com/pointmyauth/
│   │   ├── annotation/
│   │   │   └── AuthorizeEntity.java     # Anotación principal
│   │   ├── aspect/
│   │   │   └── AuthorizeEntityAspect.java
│   │   ├── config/
│   │   │   ├── PointMyAuthAutoConfiguration.java
│   │   │   └── PointMyAuthConfigurer.java
│   │   ├── context/
│   │   │   └── AuthorizationContext.java
│   │   ├── exception/
│   │   │   └── AuthorizationException.java
│   │   ├── handler/
│   │   │   ├── AuthorizationHandler.java       # Interface
│   │   │   └── AuthorizationHandlerRegistry.java
│   │   ├── resolver/
│   │   │   ├── ParameterResolver.java          # Interface
│   │   │   ├── PathVariableResolver.java
│   │   │   ├── RequestBodyResolver.java
│   │   │   └── CompositeParameterResolver.java
│   │   └── user/
│   │       └── CurrentUserProvider.java        # Interface a implementar
│   └── src/main/resources/
│       └── META-INF/spring/
│           └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│
├── point-my-auth-test/                  # Módulo de utilidades para tests
│   └── src/main/java/com/pointmyauth/test/
│       ├── MockCurrentUserProvider.java
│       └── AuthorizationTestSupport.java
│
├── point-my-auth-spring-boot-starter/   # Starter de Spring Boot
│   └── pom.xml
│
├── examples/
│   └── point-my-auth-example/           # App Spring Boot de ejemplo
│
├── .github/
│   └── workflows/
│       ├── ci.yml
│       └── release.yml
│
├── pom.xml                              # POM raíz (multi-módulo)
└── BLUEPRINT.md                         # Este documento
```

---

## 3. API Pública — Diseño Detallado

### 3.1 `@AuthorizeEntity`

```java
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthorizeEntity {

    String[] ids() default {};
    boolean includeUser() default true;
    boolean includeAuthorizationCase() default false;
    String authorizationCase() default "";
    Class<? extends AuthorizationHandler> authorizationHandler();
}
```

### 3.2 `AuthorizationHandler<U>`

```java
public interface AuthorizationHandler<U> {
    void authorize(AuthorizationContext<U> context);
}
```

### 3.3 `AuthorizationContext<U>`

- `Map<String, Object> resolvedIds`
- `U currentUser` (nullable)
- `String authorizationCase`
- `Method interceptedMethod`
- Helper methods: `getId(String, Class<T>)`, `getLongId(String)`, `getStringId(String)`
- Builder estático

### 3.4 `CurrentUserProvider<U>`

```java
@FunctionalInterface
public interface CurrentUserProvider<U> {
    U getCurrentUser();
}
```

### 3.5 `PointMyAuthConfigurer`

```java
public interface PointMyAuthConfigurer {
    CurrentUserProvider<?> currentUserProvider();
}
```

---

## 4. Flujo Interno del Aspect

```
Request HTTP
    │
    ▼
@AuthorizeEntity interceptada por AuthorizeEntityAspect (Around)
    │
    ├─► 1. Leer metadatos de la anotación
    ├─► 2. ParameterResolver: resolver cada nombre en ids[]
    ├─► 3. Si includeUser=true → CurrentUserProvider.getCurrentUser()
    ├─► 4. Construir AuthorizationContext
    ├─► 5. HandlerRegistry.resolve(authorizationHandler)
    ├─► 6. handler.authorize(context)
    │       ├─ OK → continuar con joinpoint.proceed()
    │       └─ AuthorizationException → propagar (Spring lo convierte en 403)
    └─► 7. Retornar resultado del método
```

---

## 5. Resolución de Parámetros (SpEL support)

| Expresión en `ids[]`      | Resuelve                                        |
|--------------------------|--------------------------------------------------|
| `"orderId"`              | `@PathVariable Long orderId` del método          |
| `"requestDto"`           | El objeto `@RequestBody` completo               |
| `"requestDto.companyId"` | El campo `companyId` del objeto `@RequestBody`  |
| `"#header:X-Tenant-Id"`  | Valor de un header HTTP (con prefijo `#header:`) |

---

## 6. Plan de Implementación Agéntica

### FASE 0 — Scaffolding del Proyecto
Crear estructura Maven multi-módulo con Spring Boot BOM, Java 21/25 profiles, Jacoco, Surefire.

### FASE 1 — Anotación y Clases Base
AuthorizeEntity, AuthorizationHandler, AuthorizationContext, CurrentUserProvider, AuthorizationException.

### FASE 2 — Sistema de Resolución de Parámetros
ParameterResolver, PathVariableResolver, RequestBodyResolver, HeaderResolver, CompositeParameterResolver.

### FASE 3 — Registry de Handlers
AuthorizationHandlerRegistry (Spring Bean → reflexión caching).

### FASE 4 — Aspect Principal
AuthorizeEntityAspect (@Around advice con orden HIGHEST_PRECEDENCE + 10).

### FASE 5 — Autoconfiguración y Starter
PointMyAuthAutoConfiguration, PointMyAuthConfigurer, AutoConfiguration.imports, starter POM.

### FASE 6 — Tests Unitarios Completos
>85% cobertura con JUnit 5, Mockito, AssertJ, @SpringBootTest integration tests.

### FASE 7 — Funcionalidades Adicionales
@AuthorizeEntities, AuthorizationPostProcessor, AuthorizationAuditListener, @ConditionalAuthorize, AuthorizationCacheSupport.

### FASE 8 — Distribución y Publicación
Maven Central: source/javadoc jars, GPG signing, OSSRH staging, CI/CD workflows.

---

## 7. Dependencias Clave

| Dependencia | Versión mínima | Scope |
|---|---|---|
| Java | 17 | compile |
| Spring Context | 6.0 (Boot 3.x) | provided |
| Spring AOP / AspectJ | 6.0 | provided |
| Spring Web (opcional) | 6.0 | optional |
| Jakarta Annotation API | 2.0 | compile |
| JUnit 5 | 5.10 | test |
| Mockito | 5.x | test |
| AssertJ | 3.x | test |

---

*Documento generado como blueprint de desarrollo para `point-my-auth` v0.1.0*
