# AUTOCAR: versionado solicitado por el usuario

- La actualización actual es v1.8. Cada nueva actualización entregada continúa v1.9, v1.10, etc.
- Incrementar `versionName` y `versionCode` en `app/build.gradle.kts` al preparar una nueva entrega. No incrementar la versión por cada compilación de prueba de la misma actualización.
- Guardar el APK en `dist/AUTOCAR-<version>.apk` mediante `./gradlew :app:saveVersionedApk`.
- Conservar los APK anteriores. No sobrescribir una versión ya entregada ni reutilizar `AUTOCAR-1.0.apk` para versiones nuevas.
- Actualizar la documentación de la versión. No subir a GitHub una nueva entrega salvo autorización del usuario.
