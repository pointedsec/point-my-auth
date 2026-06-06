# Fase 2: Sistema de Resolución de Parámetros

## Objetivo
Implementar el sistema de resolución de parámetros para extraer valores del contexto de ejecución.

## Tareas
- Implementar la interfaz `ParameterResolver` y sus implementaciones
- Crear resolutor para parámetros de ruta `@PathVariable`
- Crear resolutor para cuerpos de solicitud `@RequestBody`
- Crear resolutor para encabezados HTTP
- Implementar resolutor compuesto usando patrón Chain of Responsibility
- Tests unitarios para cada resolutor

## Componentes

### ParameterResolver
Interfaz base para resolutores de parámetros:
```java
public interface ParameterResolver {
    Object resolve(String paramName, Method method, Object[] args);
}
```

### PathVariableResolver
Resuelve parámetros del método anotados con `@PathVariable`.

### RequestBodyResolver
Extrae campos de objetos en el cuerpo de la solicitud (profundidad 2).

### HeaderResolver
Resuelve valores de encabezados HTTP con prefijo `#header:`.

### CompositeParameterResolver
Implementa patrón Chain of Responsibility para encadenar resolutores.

## Criterios de Aceptación
- Resolución correcta de parámetros de ruta
- Resolución correcta de campos en cuerpos de solicitud
- Resolución correcta de encabezados HTTP
- Encadenamiento de resolutores funcional
- Tests unitarios verificando cada tipo de resolución