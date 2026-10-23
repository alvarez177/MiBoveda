# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

MiBoveda is an Android password vault app (package `com.salvarez.miboveda`). It is currently the stock Compose template; no vault logic exists yet.

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
