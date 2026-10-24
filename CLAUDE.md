# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

MiBoveda is an Android password vault app (package `com.salvarez.miboveda`). It is currently the stock Compose template; no vault logic exists yet.

## Plan y forma de trabajo
El plan aprobado y el estado de cada paso están en `@docs/PLAN.md`. Léelo al empezar una sesión y actualízalo al terminar cada paso.
- Ramas: `master` (estable) y `develop` (integración). Cada paso va en `feature/<nombre-del-paso>` creada desde `develop` actualizado. Al terminar un paso, súbela a `origin` y crea un pull request hacia `develop` con `gh pr create --base develop` (título y descripción en español, con resumen y qué probar, sin atribución a Claude). El usuario revisa y fusiona el PR; yo no fusiono.
- Sigue el orden de los pasos. Por paso: commits pequeños y descriptivos; antes de avisar, `./gradlew assembleDebug` y tests existentes en verde; crear el PR a `develop`; resumen (qué, por qué, qué debe probar el usuario) con el enlace del PR; luego DETENTE. No fusiones a `develop`/`master` ni empieces el siguiente paso hasta que el usuario confirme que revisó, probó y fusionó el PR.
- Autoría: todos los commits van a nombre del usuario. No añadir `Co-Authored-By` de Claude ni ninguna otra atribución a Claude en commits o descripciones.
- Si algo no compila o hay una duda de diseño, pregunta antes de seguir.

## Arquitectura
- Kotlin + Jetpack Compose + Material 3 (solo versiones estables), claro/oscuro. Un solo módulo `:app` con paquetes `domain` / `data` / `presentation` / `di`.
- Clean Architecture + MVI sin framework: por pantalla `State`, `Intent`, `Effect`, `ViewModel` (`StateFlow` + `Channel`); sin clases base genéricas.
- Errores: las excepciones técnicas (BD, cifrado, biometría, IO) se capturan en `data` y se traducen a `VaultError` (sealed). Los casos de uso devuelven `Outcome` y no lanzan hacia la UI. Nunca tragar `CancellationException`. Nunca registrar contraseñas, claves ni datos sensibles.
- Seguridad: Room + SQLCipher; `KEK = Argon2id(contraseña maestra)` envuelve una `DEK` aleatoria que cifra la BD; Keystore/biometría solo envuelven la DEK como comodidad, y una clave de recuperación de 256 bits la envuelve también. Sin criptografía propia: librerías estándar.

## Calidad de código
- Idioma: respuestas, resúmenes y documentación en español; todo el código de implementación (identificadores, comentarios de código) en inglés.
- Código limpio, SOLID, KISS/YAGNI. Patrones de diseño solo si resuelven un problema real (explica brevemente por qué). Sin abstracciones, capas ni clases genéricas que no se necesiten hoy.
- Nombres claros, funciones pequeñas, comentarios solo cuando aporten.
- Verifica versiones de dependencias en la documentación oficial antes de fijarlas; nada alpha/beta/rc.

## Stack
- Single module `:app`, Kotlin + Jetpack Compose (Material3), Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`) — add dependencies there, not inline.
- Hilt (KSP) for DI; Navigation Compose with simple string routes. Theme lives in `presentation/theme`.
- minSdk 26, targetSdk 36, compileSdk 37 (required by the stable Compose BOM 2026.09.00), Java/jvmTarget 11. AGP 9.4.0 with built-in Kotlin 2.4.21 (no `kotlin-android` plugin), Gradle 9.6.0.

## Commands
- Single unit test: `./gradlew :app:testDebugUnitTest --tests "com.salvarez.miboveda.<Class>"`
- Instrumented tests (needs device/emulator): `ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest`. Always set `ANDROID_SERIAL` to the emulator: the task runs on every connected device and uninstalls the app (wiping its data) from each, including the user's phone.
- Lint (no custom lint config): `./gradlew :app:lintDebug`

## Security gotchas
This app stores secrets:
- Backup is disabled: `allowBackup="false"` plus explicit excludes in `backup_rules.xml` and `data_extraction_rules.xml`. Do not relax them.
- `MainActivity` sets `FLAG_SECURE` (no screenshots or recents thumbnail); keep it on any new window or activity.
- Release builds use R8 (`isMinifyEnabled`, `isShrinkResources`). argon2kt is kept via `proguard-rules.pro`; SQLCipher ships its own rules. Release is unsigned (no `signingConfigs`).
- `local.properties`, `*.jks`, and `*.keystore` are gitignored; never commit keys or secrets.
