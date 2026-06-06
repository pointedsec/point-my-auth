# Fase 8: Distribución y Publicación

## Objetivo
Publicar la librería en Maven Central con todos los artefactos requeridos.

## Tareas
- Configurar publicación en Maven Central (GPG, OSSRH)
- Crear workflows de CI con matriz Java 21/25
- Configurar workflow de release (tag → publicación)
- Generar artefactos: sources, javadoc, jars
- Crear CHANGELOG y CONTRIBUTING.md

## Componentes

### Publicación en Maven Central
Configuración para despliegue en OSSRH:
- Cuentas de servicio con permisos
- Firma GPG de artefactos
- Configuración de staging
- Verificación de artefactos

### CI Workflow
Pipeline de integración continua:
- Matriz de build para Java 21 y 25
- Tests en múltiples versiones de Java
- Verificación de calidad de código
- Generación de cobertura con Jacoco

### Release Workflow
Automatización de releases:
- Creación de tags en Git
- Publicación automática desde CI
- Generación de release notes
- Verificación de artefactos publicados

### Artefactos
- JARs binarios
- JARs de fuentes (sources)
- JARs de documentación (javadoc)
- POMs con metadata de dependencias

### Documentación
- CHANGELOG con histórico de cambios
- CONTRIBUTING.md con guía para contribuidores
- README.md actualizado
- Ejemplos de uso

## Criterios de Aceptación
- Publicación exitosa en Maven Central
- Artefactos generados correctamente
- Workflows de CI/CD funcionales
- Documentación completa y actualizada
- Ejemplos de integración verificables