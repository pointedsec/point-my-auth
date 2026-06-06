# Fase 3: Registry de Handlers

## Objetivo
Implementar el sistema de registro y caché de handlers de autorización.

## Tareas
- Implementar `AuthorizationHandlerRegistry`
- Sistema de cache para handlers resueltos
- Integración con beans de Spring para resolución de handlers
- Fallback con resolución por reflexión
- Manejo de errores cuando no se puede resolver un handler

## Componentes

### AuthorizationHandlerRegistry
Registro centralizado de handlers de autorización que:
- Mantiene cache de handlers resueltos con ConcurrentHashMap
- Resuelve handlers como beans de Spring primero
- Fallback a instanciación por reflexión
- Provee métodos para registro y resolución de handlers

### Caching Strategy
- Caché de handlers instanciados
- Clave de caché basada en clase de handler
- Sincronización para instanciación thread-safe

### Error Handling
- Lanzar `IllegalStateException` cuando un handler no puede ser resuelto
- Validación de handlers registrados

## Criterios de Aceptación
- Registro y resolución de handlers exitosa
- Caché de handlers funciona correctamente
- Fallback de reflexión funciona cuando el bean no existe
- Manejo adecuado de errores de resolución
- Tests unitarios para todos los escenarios