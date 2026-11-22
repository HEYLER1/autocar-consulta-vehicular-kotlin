# Reconocimiento de captchas (ddddocr)

AUTOCAR incorpora el reconocimiento de captchas de imagen de
[ddddocr](https://github.com/sml2h3/ddddocr) ejecutado íntegramente en el dispositivo. El objetivo
es reducir la escritura manual de códigos en los portales que muestran una imagen de verificación.
No se resuelven verificaciones interactivas (reCAPTCHA, hCaptcha o Cloudflare Turnstile): esas
permanecen en el formulario oficial y las completa el usuario.

## Qué se integra y cómo

- `app/src/main/assets/ddddocr/common_old.onnx`: modelo OCR original de ddddocr, sin modificaciones
  (13.6 MB), tomado del paquete `ddddocr` 1.6.1.
- `app/src/main/assets/ddddocr/ascii_charset.json`: subconjunto del alfabeto `CHARSET_OLD` con los
  índices de las letras A-Z, a-z y los dígitos 0-9. El modelo conserva sus 8210 clases; el subconjunto
  solo filtra la salida a los caracteres que usan los portales.
- `DdddOcrEngine.java`: preprocesamiento (alto 64 px, escala proporcional, gris, normalización a
  [0, 1], tensor `1×1×64×ancho`), inferencia con ONNX Runtime y decodificación CTC.
- `UnifiedQueryPanel.java`: al mostrar un captcha municipal o prioritario, el motor reconoce la
  imagen en segundo plano, completa el campo y envía el formulario automáticamente. Si el portal
  rechaza el código, el nuevo captcha se reconoce y se vuelve a enviar, con un máximo de 10 intentos
  automáticos por fuente. Al alcanzar el límite el campo sigue editable para el ingreso manual.
- Se completa el campo solo si el texto tiene al menos 3 caracteres y la confianza media supera 0.5.
  En caso contrario, el campo queda vacío para el ingreso manual.
- El resultado se normaliza a mayúsculas porque los portales comparan el código sin distinguir
  mayúsculas de minúsculas.

## Envío automático y reintentos

Después de reconocer el captcha, el panel completa el código y pulsa el botón de envío oculto de esa
fuente. El flujo reutiliza el mismo envío que el botón «Continuar» manual, sin fabricar solicitudes
ni modificar tokens.

Cuando la verificación no se supera, la fuente no se reintenta sobre el mismo formulario: la página
se recarga desde su dirección original para pedir una sesión y un captcha nuevos, y el código
reconocido se vuelve a introducir y enviar. Así no se repite el mismo intento una y otra vez. Se
permiten hasta 3 recargas por fuente; si aun así no se supera, la imagen más reciente permanece en el
panel para escribir el código a mano.

Dentro de una misma página, cada imagen se identifica por su contenido: al detectar una imagen
distinta se descarta el código anterior, se reconoce la nueva y se envía ese resultado. Así nunca se
reenvía el código rechazado ni se rellena dos veces el mismo captcha, incluso si el portal sustituye
la imagen sin recargar la página. Si el OCR no puede leer la imagen, se solicita otra en lugar de
dejar el mismo captcha ilegible.

El límite es de 10 intentos automáticos por WebView. Si se agota, se recarga la fuente y, cuando no
quedan recargas, el envío automático se detiene y el código reconocido permanece en el campo para
corregirlo y pulsar «Continuar». Un código escrito a mano nunca se sobrescribe con el resultado del
OCR. Las verificaciones interactivas (reCAPTCHA, hCaptcha, Turnstile) no se envían automáticamente.

Dependencia: `com.microsoft.onnxruntime:onnxruntime-android:1.20.0`. El APK se empaqueta para
`arm64-v8a`, `armeabi-v7a` y `x86_64`. En una ABI sin librería nativa, el motor se desactiva y la
aplicación sigue funcionando con el ingreso manual.

## Privacidad

La imagen del captcha se procesa en memoria en el dispositivo. No se guarda en disco ni se envía a
un servicio externo. El motor no accede a la red: solo usa el modelo incluido en el APK.

## Alcance y limitaciones

- El reconocimiento es una ayuda, no una garantía. Captchas con mucho ruido, distorsión o caracteres
  ambiguos pueden leerse mal. Un código incorrecto suele generar un captcha nuevo y retrasar la
  consulta.
- El modelo fue entrenado para captchas cortos de letras y dígitos. No está pensado para imágenes
  con texto largo ni para verificación interactiva.
- Automatizar la lectura de un captcha puede contravenir los términos de uso de un portal. La
  función se ofrece para consultas propias y el envío sigue requiriendo confirmación del usuario.
  No debe usarse para recolección masiva de datos ni para eludir controles de acceso.
- Un fallo de reconocimiento no implica que el portal no tenga datos: la consulta queda pendiente.

## Atribución y licencia

ddddocr es un proyecto de [sml2h3](https://github.com/sml2h3/ddddocr) distribuido bajo la licencia
MIT:

```text
The MIT License (MIT)

Copyright © 2022 <copyright holders>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

ONNX Runtime se distribuye bajo la licencia MIT de Microsoft. Los nombres y servicios de terceros
pertenecen a sus respectivas entidades.

## Acceso automático a portales con cuenta

Portales como SUNARP historial registral, SIGM o PNP exigen login. `CredentialVault` guarda el usuario y
la contraseña cifrados con una clave AES-256 del AndroidKeyStore (nunca salen del dispositivo) y
`AutoLoginAutomation` detecta el formulario de login de forma genérica (campo de contraseña + usuario
+ botón de envío), lo rellena y lo envía cuando existen credenciales para el host. Requiere
consentimiento explícito del usuario ("Gestionar accesos a portales"). Si el portal muestra
reCAPTCHA/hCaptcha/Turnstile, se deja para que el usuario lo resuelva y el envío se dispara al
detectar el token.

## Formularios asistidos de tránsito

Los formularios asistidos de tránsito (Cinemómetro SUTRAN, Captura y Depósito SAT, e Impuesto
vehicular de SAT Lima) se abren en un `WebView` propio. `TrafficFormAutomation` rellena los datos
guardados (DNI y código verificador para Cinemómetro; placa para SAT), reconoce el CAPTCHA de imagen
con `DdddOcrEngine`, lo escribe en el campo propio del portal y pulsa el botón oficial. Nunca se
alteran verificaciones interactivas ni se fabrican tokens.

- Cinemómetro: si hay DNI guardado se queda en modo Acta/Papeleta (`ddlTipoBusqueda=2`, `TxtBuscar`,
  `txtCodigoVerificador`); si no, cambia a modo Placa. El `onclick` de `BtnBuscar` (que compara
  contra un código fijo del cliente) se anula para que valide el servidor.
- SAT Captura/Depósito: rellena `ctl00_cplPrincipal_txtPlaca` y resuelve el captcha
  `img.captcha_class`.
- SAT Impuesto: la página inicial es un frameset con sesión; se navega al formulario de tributos
  (`BusquedaTributario.aspx`) y se rellena el documento de identidad.
- Si el OCR no es fiable o el portal rechaza el código, se pide una imagen nueva (botón de refresco
  de SAT o recarga de SUTRAN) y se reintenta hasta 3 veces. Agotados los intentos, el formulario
  queda editable para el ingreso manual y se muestra un aviso.

## Comprobación

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ./gradlew :app:testDebugUnitTest \
  :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.app_autocar.DdddOcrEngineInstrumentedTest,com.example.app_autocar.UnifiedQueryPanelAutoVerifyInstrumentedTest
```

La prueba unitaria valida la decodificación CTC con tensores sintéticos. La prueba instrumentada del
motor dibuja caracteres en un `Bitmap`, carga el modelo desde los assets y comprueba que la salida
sea alfanumérica. La prueba del panel comprueba que el código reconocido se complete y se envíe solo,
que no se sobrescriba lo escrito por el usuario, que un captcha nuevo sustituya al anterior y que el
envío automático se detenga al llegar al límite. Ninguna mide la precisión frente a captchas reales de
los portales.

Para sustituir el modelo por otro compatible, reemplaza `common_old.onnx` y el archivo de alfabeto,
y ajusta `DdddOcrEngine` si cambia la forma de entrada o el número de clases.
