# Fase 4: Aspect Principal

## Objetivo
Implementar el aspecto principal de autorización usando Spring AOP.

## Tareas
- Implementar `AuthorizeEntityAspect` con advice @Around
- Configurar prioridad HIGHEST_PRECEDENCE + 10
- Implementar flujo de interceptación y autorización
- Integración con sistema de resolución de parámetros
- Integración con registro de handlers
- Tests de integración con Spring context

## Componentes

### AuthorizeEntityAspect
Aspecto AOP que:
- Intercepta métodos anotados con `@AuthorizeEntity`
- Lee metadatos de la anotación
- Resuelve parámetros del contexto
- Obtiene usuario actual cuando se requiere
- Construye `AuthorizationContext`
- Ejecuta handler de autorización
- Procede con ejecución del método si autorizado
- Propaga `AuthorizationException` como 403 HTTP

### Flujo de Ejecución
1. Interceptar método con `@AuthorizeEntity`
2. Extraer configuración de la anotación
3. Resolver parámetros definidos en `ids[]`
4. Obtener usuario actual si `includeUser=true`
5. Construir contexto de autorización
6. Resolver y ejecutar handler correspondiente
7. Proceder con ejecución del método si autorizado
8. Lanzar excepción si acceso denegado

## Criterios de Aceptación
- Aspecto se aplica correctamente a métodos anotados
- Parámetros se resuelven según especificación
- Contexto de autorización se construye correctamente
- Handlers se ejecutan con el contexto apropiado
- Excepciones se propagan correctamente como 403 HTTP