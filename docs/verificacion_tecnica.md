# Verificación técnica del prototipo

Fecha: 10 de octubre de 2026 (America/Lima).

## Ejecución reproducible

Se ejecutó en el entorno de desarrollo macOS, utilizando el JDK incluido en Android Studio:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Resultado observado: `BUILD SUCCESSFUL in 12s`. Gradle informó 51 tareas procesadas: 4 ejecutadas y 47 previamente actualizadas. La ejecución aprovechó artefactos y dependencias locales; no prueba una instalación limpia desde cero.

| Comprobación | Resultado | Alcance |
|---|---|---|
| Pruebas unitarias | 2 ejecutadas, 0 fallos, 0 errores, 0 omitidas | Normalización y validación de placas |
| `assembleDebug` | Finalizó correctamente | Construcción del APK de depuración |
| `lintDebug` | 0 errores, 185 advertencias | Análisis estático; las advertencias requieren revisión |
| Pruebas instrumentadas | No ejecutadas en esta verificación | No se afirma cobertura de interfaz |
| Consultas oficiales completas | No ejecutadas en esta verificación | No se certifica disponibilidad de portales |
| Informe PDF | Implementación presente; sin inspección visual en esta verificación | No se certifica exactitud o diseño de un informe real |

El reporte de pruebas original se genera en `app/build/test-results/testDebugUnitTest/TEST-com.example.app_autocar.ExampleUnitTest.xml`. Los reportes de lint se generan en `app/build/reports/`. Los archivos generados y los datos identificativos del equipo no se publican en el repositorio.

## Casos unitarios comprobados

- `normalizesPeruvianPlate`: `abc-123` se normaliza a `ABC123`; `abc123` se muestra como `ABC-123`.
- `validatesPeruvianPlate`: acepta `ABC-123` y `A1B-234`; rechaza `ABC12` y `ABC-1234`.

Fuente ejecutable: [ExampleUnitTest.kt](../app/src/test/java/com/example/app_autocar/ExampleUnitTest.kt).

## Límites de la evidencia

Este registro describe una comprobación técnica local, no resultados de investigación. No demuestra reducción de tiempo o acciones, cobertura municipal, concordancia entre portales, resolución correcta de todas las verificaciones ni confiabilidad de las recomendaciones del informe.

Para el piloto deben conservarse resultados verificables, tiempos, pasos, incidencias y condiciones de cada par de consultas, asociados a una versión identificada. No se incluyen placas consultadas, nombres de propietarios, imágenes registrales ni pólizas reales como evidencia pública.
