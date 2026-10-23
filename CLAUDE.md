# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

MiBoveda is an Android password vault app (package `com.salvarez.miboveda`). It is currently the stock Compose template; no vault logic exists yet.

## Plan y forma de trabajo
El plan aprobado y el estado de cada paso están en `@docs/PLAN.md`. Léelo al empezar una sesión y actualízalo al terminar cada paso.
- Ramas: `master` (estable) y `develop` (integración). Cada paso va en `feature/<nombre-del-paso>` creada desde `develop` actualizado. Sin pull requests.
- Sigue el orden de los pasos. Por paso: commits pequeños y descriptivos; antes de avisar, `./gradlew assembleDebug` y tests existentes en verde; resumen (qué, por qué, qué debe probar el usuario); luego DETENTE. No fusiones a `develop`/`master` ni empieces el siguiente paso hasta que el usuario confirme que revisó, probó y fusionó.
- Si algo no compila o hay una duda de diseño, pregunta antes de seguir.

## Arquitectura
- Kotlin + Jetpack Compose + Material 3 (solo versiones estables), claro/oscuro. Un solo módulo `:app` con paquetes `domain` / `data` / `presentation` / `di`.
- Clean Architecture + MVI sin framework: por pantalla `State`, `Intent`, `Effect`, `ViewModel` (`StateFlow` + `Channel`); sin clases base genéricas.
- Errores: las excepciones técnicas (BD, cifrado, biometría, IO) se capturan en `data` y se traducen a `VaultError` (sealed). Los casos de uso devuelven `Outcome` y no lanzan hacia la UI. Nunca tragar `CancellationException`. Nunca registrar contraseñas, claves ni datos sensibles.
- Seguridad: Room + SQLCipher; `KEK = Argon2id(contraseña maestra)` envuelve una `DEK` aleatoria que cifra la BD; Keystore/biometría solo envuelven la DEK como comodidad. Sin criptografía propia: librerías estándar.

## Calidad de código
- Idioma: respuestas, resúmenes y documentación en español; todo el código de implementación (identificadores, comentarios de código) en inglés.
- Código limpio, SOLID, KISS/YAGNI. Patrones de diseño solo si resuelven un problema real (explica brevemente por qué). Sin abstracciones, capas ni clases genéricas que no se necesiten hoy.
- Nombres claros, funciones pequeñas, comentarios solo cuando aporten.
- Verifica versiones de dependencias en la documentación oficial antes de fijarlas; nada alpha/beta/rc.

## Stack
- Single module `:app`, Kotlin + Jetpack Compose (Material3), Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`) — add dependencies there, not inline.
- minSdk 26, compileSdk/targetSdk 36, Java/jvmTarget 11.

## Commands
- Single unit test: `./gradlew :app:testDebugUnitTest --tests "com.salvarez.miboveda.<Class>"`
- Instrumented tests (needs device/emulator): `./gradlew :app:connectedDebugAndroidTest`
- Lint (no custom lint config): `./gradlew :app:lintDebug`

## Security gotchas
This app will store secrets, so the template defaults need attention:
- Manifest has `allowBackup="true"` with stock `backup_rules.xml` / `data_extraction_rules.xml`; exclude vault data from backups before storing anything.
- `FLAG_SECURE` is not set on any window.
- Release build has `isMinifyEnabled = false` and no `signingConfigs`.
- `local.properties`, `*.jks`, and `*.keystore` are gitignored; never commit keys or secrets.
