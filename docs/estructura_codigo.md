# Estructura del código

El código Android está en `app/src/main/java/com/example/app_autocar/`.

## Pantallas y navegación

- `MainActivity`: Inicio, selección de fuentes, navegación inferior e historial local. Controla una sola sesión de consulta y su cancelación.
- `SunarpServicesActivity`: catálogo de servicios registrales y configuración local de autorrelleno.
- `WebViewActivity`: navegador oficial individual, carga y errores de conexión.
- `GeneralReportActivity`: visualización de una captura del informe y acciones independientes de guardar/compartir el PDF.

## Fuentes y consultas

- `OfficialSource`: datos inmutables de una entidad.
- `OfficialSourceCatalog`: lista de fuentes y sus enlaces, sin lógica de consultas.
- `UnifiedQueryPanel`: coordina la sesión, progreso, verificación manual y resultados municipales. Integra el motor prioritario sin reemplazar sus adaptadores.
- `PriorityQueryEngine`: comportamiento específico de SUNARP, SBS, SUTRAN y MTC. Su vista interna conserva los WebViews y controles que necesita el motor; no es una segunda pantalla de Inicio.
- `FinesPortalAdapter`: formularios y respuestas específicos de portales municipales.
- `SutranResponse`: clasificación de respuestas oficiales de SUTRAN, incluida ausencia explícita de papeletas.

## Datos e informe

- `PlateUtils` y `PlateRegion`: normalización, validación y recomendación orientativa por placa.
- `CurrencyConverter`: conversión local y obtención de tasa referencial diaria.
- `GeneralVehicleReport`: reúne resultados en un resumen general sin tratar pendientes como ausencia de deuda.
- `FinesPdfReport`: generador único del PDF unificado.
- `IconActions`: iconos compactos, etiquetas de accesibilidad y áreas táctiles.

`FinesBatchActivity` se conserva como contenedor de pruebas de `UnifiedQueryPanel`: no está exportada ni enlazada desde la navegación. Se usa en pruebas de formularios y fuentes reales opcionales, por lo que no es código muerto.

## Comprobación

Con el JDK de Android Studio:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.app_autocar.FinesAutomationTest
```

Las pruebas de interfaz usan datos ficticios; no prueban que todos los servicios oficiales estén disponibles. El ensayo real de fuentes es optativo y requiere autorización y una placa.

Para una nueva entrega, incrementar una vez la versión y ejecutar `:app:saveVersionedApk`. Los APK anteriores son entregas históricas, no archivos temporales que deban borrarse.
