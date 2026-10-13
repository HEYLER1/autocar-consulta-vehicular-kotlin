# Revisión de consultas de multas

Fecha de revisión: 13 de octubre de 2026. Se revisaron formularios públicos mediante HTML y navegador. Las pruebas de WebView con formularios reproducidos verifican autorrelleno, envío único y espera de verificación; no acreditan por sí solas disponibilidad ni concordancia de los servicios externos.

| Fuente | Preparación verificada | Flujo implementado |
|---|---|---|
| Arequipa | `#placa`, `#btnConsultar` | Envío automático sin captcha visible en el formulario revisado |
| Cusco | Seleccionar placa; `#tx_numero`, `#bt_consultar` | Envío automático |
| Chachapoyas | `#placa_cnt`, `#btnConsulta` | Envío automático; resultado AJAX |
| Huánuco | Formulario `#consulta_placa` | Envío automático; no utilizar el formulario de PIT |
| Pucallpa | Formulario `f1`, campo placa | Envío automático por HTTP |
| Cajamarca | Consultas → infracciones → placa | Selección y envío automático; esperar actualización de campos |
| Chiclayo | Campo `search` de `#frm` | Envío automático |
| Ica | Formulario `buscar`; botón Buscar papeletas | Envío automático |
| Piura | Pestaña placa; campo `PlaMot`; destino `www2.munipiura.gob.pe` | Envío en la misma sesión; HTTP autorizado por el usuario únicamente para ese dominio |
| SUTRAN | `#txtPlaca`, `#TxtCodImagen`, imagen del iframe `#iimage` | Imagen y código en el panel nativo |
| Callao | Tipo de búsqueda placa; `#valor_busqueda`, `#captcha`, `#idBuscar` | Imagen y código en panel nativo; conservar redirección institucional a `pagonube.municallao.gob.pe` |
| Tacna | Selección placa; `#busca`, `#codigo`, `img.img-captcha` | Imagen y código en panel nativo |
| Tarapoto | `#placa_vehiculo`, `#gd_captcha_code`, `#gd_captcha_img` | Imagen y código en panel nativo |
| SAT Lima | Sesión nueva → papeletas → búsqueda por placa | Autorrelleno y reCAPTCHA interactivo oficial; no convertirlo en imagen o texto |
| ATU | Consulta de infracciones → selector Por Placa | Autorrelleno y verificación interactiva oficial |
| Trujillo | El enlace de gob.pe es orientación; el acceso digital consultado redirige a cuenta de usuario | No automatizado sólo con placa; no reutilizar DNI de SUNARP ni afirmar cero multas |

## Límites y seguridad

El dominio de contenido de SATT mostró contenido ajeno en su página; no se utiliza como base para automatizar ni enviar datos personales. La consulta digital observada requiere iniciar sesión. No se sortearon controles de seguridad.

Los captchas no se resuelven con OCR ni se modifican sus tokens. Sólo se muestra la imagen existente cuando el navegador permite leerla, y el usuario introduce el código. Las verificaciones interactivas permanecen en el portal oficial. Las consultas conservan WebViews independientes y no recargan la página al enviar un código.

Las respuestas vacías, errores HTTP, errores TLS, fuentes que requieren cuenta y controles pendientes deben permanecer diferenciados en el informe PDF. El HTTP de Piura y Pucallpa no cifra el tráfico; el resto de la política TLS permanece sin cambios.

## Pruebas reproducibles

### Versión publicada con la interfaz actual

La consulta se aloja en Inicio mediante `UnifiedQueryPanel`; sus formularios ocultos no determinan la altura visible. El botón Consultar pasa a Cancelar durante la ejecución. «Ver resultados» está debajo del progreso y abre una captura del informe general en `GeneralReportActivity`. El PDF de esa captura se conserva mientras otras respuestas llegan a Inicio.

Verificación del 13/10/2026: compilación y lint correctos, nueve pruebas unitarias sin fallos y nueve pruebas de `FinesAutomationTest` aprobadas. Incluyen bloqueo de solicitudes repetidas, cancelación, altura del panel oculto, distinción de fallos en el progreso, envío de códigos ficticios sin recarga, capturas sin datos personales y renderizado del informe general. No prueban la disponibilidad permanente de las fuentes ni una consulta real completa sin papeletas en SUTRAN.

Las nuevas imágenes del README se generan con `readmeHomeScreenshotUsesOnlyExamplePlate` y `fullScreenGeneralReportRendersSnapshot`: la placa de Inicio no se consulta y el informe contiene datos ficticios. Son evidencia de interfaz, no resultados de un piloto.

### Recuperación del motor prioritario

SUNARP, SOAT/SBS, SUTRAN y MTC reutilizan ahora `PriorityQueryEngine`, extraído del flujo anterior de `MultiCheckActivity`. La pantalla unificada presenta sus respuestas y captchas sin sustituir su lógica de consulta por el adaptador municipal. Las fuentes desmarcadas no se cargan; las municipales mantienen sesiones independientes.

Se comprobó en el emulador la carga real de MTC en `rec.mtc.gob.pe/Citv/ArConsultaCitv`: campo de placa rellenado y captcha de imagen visible en el panel nativo. No se envió ni resolvió el captcha en esa comprobación; por tanto, no acredita todavía el resultado final de una revisión técnica ni una consulta completa de las cuatro fuentes.

### Observación en el emulador

En las pruebas reales de esta fecha se recibieron respuestas de Chachapoyas, Pucallpa, Cajamarca, Ica, Piura, Cusco y Chiclayo. SUTRAN, Callao, Tacna y Tarapoto mostraron captcha de imagen; SAT Lima mostró verificación interactiva. Esto acredita el flujo observado, no disponibilidad permanente ni ausencia de deudas en otras jurisdicciones.

Arequipa no estuvo disponible desde la app; Huánuco quedó en blanco y ahora tiene un límite de espera. ATU no produjo una respuesta confirmada en la prueba dirigida: su mensaje inicial «Sin registros» no se considera resultado. Trujillo requiere acceso adicional. Las fuentes pendientes o fallidas no se presentan como consultas sin multas.

La compilación, las pruebas unitarias y lint finalizaron correctamente. La prueba de captcha usa un formulario ficticio y comprueba que el código se transmite sin cambiar la ubicación de la página.

- `FinesPortalAdapterTest`: redirecciones institucionales permitidas y campos de códigos.
- `FinesAutomationTest`: nueve formularios sin captcha, cuatro con imagen y dos con verificación interactiva; prueba nativa del panel de imagen y campo de código usando datos ficticios.
- `LiveFinesSourceTest`: prueba opcional de servicios reales. Está omitida por defecto; requiere argumentos `liveSources=true` y `testPlate`. No imprime propietarios ni detalles de papeletas, únicamente estados técnicos por fuente.

Los datos reales de las consultas no deben publicarse como capturas ni incluirse en el código o en GitHub.
