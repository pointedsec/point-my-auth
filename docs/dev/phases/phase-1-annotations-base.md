# Fase 1: Anotación y Clases Base

## Objetivo
Implementar las anotaciones y clases base del sistema de autorización.

## Tareas
- Implementar la anotación `@AuthorizeEntity`
- Crear la interfaz `AuthorizationHandler<U>`
- Implementar la clase `AuthorizationContext<U>` con su Builder
- Crear la interfaz `CurrentUserProvider<U>`
- Implementar la excepción `AuthorizationException`

## Componentes

### @AuthorizeEntity
Anotación principal que define:
- `ids()`: Array de identificadores a usar en la autorización
- `includeUser()`: Incluir información del usuario actual
- `includeAuthorizationCase()`: Incluir caso de autorización
- `authorizationCase()`: Valor del caso de autorización
- `authorizationHandler()`: Clase del handler de autorización

### AuthorizationHandler<U>
Interfaz funcional para manejar la lógica de autorización:
```java
public interface AuthorizationHandler<U> {
    void authorize(AuthorizationContext<U> context);
}
```

### AuthorizationContext<U>
Contexto inmutable que contiene:
- `Map<String, Object> resolvedIds`: IDs resueltos del método
- `U currentUser`: Usuario actual (puede ser null)
- `String authorizationCase`: Caso de autorización
- `Method interceptedMethod`: Método interceptado
- Métodos helper para obtener IDs tipados

### CurrentUserProvider<U>
Proveedor funcional de usuario actual:
```java
@FunctionalInterface
public interface CurrentUserProvider<U> {
    U getCurrentUser();
}
```

### AuthorizationException
Excepción de runtime para casos de acceso denegado.

## Criterios de Aceptación
- Todas las clases deben tener Javadoc completo
- Anotaciones de nullability de jakarta.annotation en API pública
- Compilación sin errores
- Tests unitarios básicos exitosos