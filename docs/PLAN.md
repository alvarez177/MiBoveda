# MiBoveda — Plan de implementación

## Contexto
App Android nativa (Kotlin + Compose + Material 3) para guardar contraseñas, como proyecto de aprendizaje. El repo hoy es la plantilla "Empty Activity" (un módulo `:app`, solo `MainActivity` + `ui/theme`), con ramas `master` y `develop`. Objetivo: Fase 1 = bóveda cifrada y bloqueable; Fase 2 = backup cifrado + clave de recuperación (el diseño de Fase 1 ya los prevé).

Decisiones tomadas con el usuario: Room 2.8.5 + `SupportOpenHelperFactory`; Hilt; rama estable = `master` (no se renombra); KDF = Argon2id (recomendado, ver abajo).

## Hallazgo importante: la toolchain está desactualizada
El proyecto usa AGP 8.13.2 / Kotlin 2.0.21 / BOM 2024.09.00. Lo estable hoy (verificado en docs oficiales) es AGP 9.4.0 (Gradle 9.6.0, JDK 17), Kotlin 2.4.21, BOM 2026.09.00. Hilt/Dagger 2.60.1 exige AGP 9 con su plugin. Por eso el primer paso técnico es actualizar la toolchain. Con AGP 9 Kotlin viene integrado (el plugin `kotlin-android` ya no se aplica): se ajustará en ese paso. Plan B si algo no cuadra: Dagger/Hilt anterior compatible con AGP 8.x (se consultaría contigo antes).

## Diseño de claves (decisión clave, a confirmar)
Tu requisito dice "la clave de la BD se deriva de la contraseña maestra". Propongo una variante estándar que lo respeta y habilita la Fase 2:

- `KEK = Argon2id(contraseña maestra, salt aleatorio)` (clave de envoltura derivada).
- `DEK` = clave aleatoria de 256 bits (con `SecureRandom`) que cifra la BD SQLCipher.
- En disco solo se guarda `DEK` **envuelta** con la KEK (AES-256-GCM de la JCA, sin cripto propia) + salt + parámetros Argon2 + versión. Nunca la contraseña ni la DEK en claro.
- Biometría: clave AES en Android Keystore (`setUserAuthenticationRequired`, invalidada al cambiar huellas) que envuelve la DEK; `BiometricPrompt` con `CryptoObject`. Es solo comodidad: la contraseña maestra siempre abre la bóveda.
- Fase 2: la clave de recuperación (generada una vez) es una segunda envoltura de la misma DEK; cambiar la contraseña maestra solo reenvuelve la DEK sin recifrar la BD. Si la DEK fuera la salida directa del KDF, no se podría tener clave de recuperación ni cambiar la contraseña sin recifrar.
- Backup (Fase 2): exportación lógica (entradas serializadas) cifrada con `Argon2id(contraseña, salt propio del backup)` + AES-GCM. Portable a otro dispositivo porque no depende del Keystore ni de la DEK.

### ¿Por qué Argon2id (para tu duda sobre el KDF)?
Un KDF convierte tu contraseña (débil, memorizable) en una clave criptográfica, y se hace deliberadamente lento para que un atacante que robe el archivo no pueda probar millones de contraseñas por segundo.
- **PBKDF2**: viene en Android, simple, pero solo gasta CPU; las GPUs/ASIC lo atacan muy bien. Exige cientos de miles de iteraciones.
- **Argon2id**: ganador del concurso PHC, además de CPU consume **memoria** (p. ej. 64 MiB), lo que encarece mucho los ataques en hardware especializado. Es la recomendación actual (OWASP). Coste: una dependencia nativa (`argon2kt` 1.6.0, soporta páginas de 16 KB). Se calibra para ~0,5–1 s en el dispositivo.
Se oculta tras una interfaz pequeña `KeyDerivation` (justificada: argon2kt es JNI y no corre en tests JVM; así se prueba el resto con un doble).

## Arquitectura y paquetes (un solo módulo `:app`, `com.salvarez.miboveda`)
```
domain/
  model/        PasswordEntry, ...            (Kotlin puro)
  error/        VaultError (sealed): WrongMasterPassword, VaultLocked, BiometricUnavailable,
                CorruptBackup, VaultNotCreated, StorageFailure, ...
  Outcome.kt    sealed interface Outcome<T> { Success / Failure(VaultError) }
  repository/   VaultRepository, EntryRepository (interfaces)
  usecase/      CreateVault, UnlockVault, LockVault, AddEntry, ... (operator invoke)
data/
  crypto/       KeyDerivation (Argon2kt), KeyWrapper (AES-GCM), KeystoreBiometricKey
  db/           Room (entities, DAO, database) + apertura con SupportOpenHelperFactory
  repository/   implementaciones; aquí se captura toda excepción técnica -> VaultError
  settings/     DataStore Preferences (cabecera de la bóveda, auto-bloqueo, biometría activa)
presentation/
  theme/        (mueve el actual ui/theme), navigation/, common/
  onboarding/ unlock/ list/ detail/ settings/   (cada una: XState, XIntent, XEffect, XViewModel, XScreen)
di/             módulos Hilt
```
- **MVI** sin framework: cada ViewModel expone `StateFlow<State>`, recibe `Intent`s y emite `Effect`s por `Channel`. Sin clase base genérica (YAGNI).
- **Outcome propio** en vez de `kotlin.Result`: `Result` solo lleva `Throwable`; necesitamos error de dominio tipado. Es el único wrapper; no hay más capas.
- **Errores**: `catch` en data → `VaultError`; `CancellationException` siempre se relanza; los casos de uso nunca lanzan hacia la UI; mensajes de usuario en `strings.xml`; sin logs de secretos (el `toString` de modelos sensibles se oculta).
- **Ciclo de vida de la BD**: la BD solo existe desbloqueada. Un `VaultSession` singleton (Hilt) la abre con la DEK y al bloquear la cierra y borra la DEK de memoria. Interfaz propia solo donde la haya probado necesaria (KeyDerivation).
- **Auto-bloqueo**: `ProcessLifecycleOwner` (`lifecycle-process`) → bloquea al salir de la app (con retardo configurable, por defecto inmediato).
- Navegación: `navigation-compose` con rutas simples (pocas pantallas).

## Dependencias (versiones verificadas en documentación oficial el 2026-10-23)
| Pieza | Versión | Fuente |
|---|---|---|
| AGP / Gradle / JDK | 9.4.0 / 9.6.0 / 17 | developer.android.com AGP release notes |
| Kotlin | 2.4.21 | kotlinlang.org releases |
| Compose BOM (estable) | 2026.09.00 (material3 1.4.0, ui 1.12.1) | página BOM + mapping |
| activity-compose | 1.13.0 | androidx versions |
| lifecycle (viewmodel-compose, runtime-compose, process) | 2.11.0 | androidx versions |
| navigation-compose | 2.10.2 | androidx versions |
| core-ktx | 1.19.1 | androidx versions |
| Room (runtime, ktx, compiler) | 2.8.5 | androidx Room |
| KSP | 2.3.12 | github.com/google/ksp |
| sqlcipher-android + androidx.sqlite | 4.19.1 + 2.7.0 | README oficial / Maven Central |
| argon2kt | 1.6.0 | github lambdapioneer |
| biometric | 1.1.0 | androidx biometric |
| datastore-preferences | 1.3.0 | androidx versions |
| Hilt (dagger) + androidx.hilt:hilt-lifecycle-viewmodel-compose | 2.60.1 + 1.4.0 | dagger releases / androidx hilt |

**No verificado todavía (se confirma en el paso que los use, antes de fijarlos):** kotlinx-coroutines (+test), MockK, Turbine, versiones de androidx.test; compatibilidad exacta KSP 2.3.12 ↔ Kotlin 2.4.21; si `hilt-navigation-compose` 1.4.0 exige `compileSdk` 37 (las betas lo mencionaban); comportamiento del plugin de Hilt con el Kotlin integrado de AGP 9. No se usan versiones alpha/beta/rc. `androidx.security:security-crypto` no se usa.

## Pasos (cada uno = rama `feature/...` desde `develop`; PR hacia `develop` que tú revisas y fusionas; yo me detengo tras cada uno)
Reglas por paso: commits pequeños; antes de avisarte `./gradlew assembleDebug` + tests; subo la rama y creo el PR a `develop` (`gh pr create --base develop`); resumen (qué/por qué/qué probar) con el enlace del PR; espero tu confirmación de que lo revisaste y fusionaste. Se actualiza `docs/PLAN.md` al terminar cada uno.

**Fase 0 — Base**
1. `feature/docs-plan`: crear `docs/PLAN.md` (este plan + tabla de estado) y ampliar `CLAUDE.md` (referencia al plan y reglas de arquitectura, calidad y forma de trabajo).
2. `feature/toolchain-update`: AGP/Gradle/Kotlin/BOM/KSP a las versiones de arriba, limpiar el catálogo. Verif.: compila, la app abre.
3. `feature/app-skeleton`: Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`), estructura de paquetes, mover tema a `presentation/theme`, tema M3 claro/oscuro, `NavHost` con pantalla vacía.

**Fase 1 — Seguridad**
4. `feature/domain-core`: modelo, `VaultError`, `Outcome`, interfaces de repositorio, helper de mapeo de excepciones (relanza `CancellationException`). Tests unitarios.
5. `feature/crypto-keys`: `KeyDerivation` (Argon2id), `KeyWrapper` (AES-GCM), generación de DEK. Tests unitarios (con doble de KDF) + instrumentados del KDF real; calibrar parámetros.
6. `feature/encrypted-database`: Room + SQLCipher, entidad/DAO de entradas, `VaultSession` abrir/cerrar. Test instrumentado: BD ilegible sin clave correcta.
7. `feature/hardening-manifest`: `allowBackup=false` + reglas de extracción vacías, `FLAG_SECURE`, R8 activado en release. Verif.: captura bloqueada.
8. `feature/create-vault`: cabecera en DataStore, caso de uso `CreateVault`, pantalla de onboarding (contraseña maestra + confirmación + medidor básico).
9. `feature/unlock-vault`: `UnlockVault`, pantalla de desbloqueo, error "contraseña incorrecta" comprensible, `LockVault`.
10. `feature/entries-crud`: lista, detalle, alta/edición/borrado, copiar con limpieza del portapapeles, mostrar/ocultar contraseña.
11. `feature/auto-lock`: bloqueo al salir de la app (+ timeout configurable), cierre de BD y borrado de claves en memoria.
12. `feature/biometric-unlock`: clave Keystore, envoltura de la DEK, `BiometricPrompt`, activar/desactivar en ajustes, fallback a contraseña, manejo de invalidación por cambio de huellas.
13. `feature/settings-and-polish`: pantalla de ajustes, revisión claro/oscuro, mensajes de error, repaso de logs, checklist de seguridad.

**Fase 2** (el diseño ya la contempla)
14. `feature/recovery-key`: generar una vez al crear la bóveda (mostrarla, confirmar guardado), segunda envoltura de la DEK, flujo "olvidé mi contraseña".
15. `feature/backup-export`: archivo cifrado con la contraseña maestra (Argon2id + AES-GCM) vía Storage Access Framework.
16. `feature/backup-import`: importar/restaurar en otro dispositivo, `CorruptBackup`/contraseña incorrecta bien manejados.
(Opcional posterior: cambiar contraseña maestra, generador de contraseñas.)

## Verificación global
- Cada paso: `./gradlew assembleDebug`, `./gradlew :app:testDebugUnitTest`, `:app:lintDebug`; `:app:connectedDebugAndroidTest` cuando hay tests instrumentados (requiere emulador/dispositivo).
- Manual por paso (lo describo en el resumen): p. ej. tras 6, abrir el `.db` con `sqlite3` debe fallar; tras 7, captura de pantalla bloqueada y `adb backup` sin datos; tras 12, huella válida/ inválida/ cambiada.

## Decisiones cerradas
- Diseño KEK + DEK (DEK envuelta) aprobado.
- KDF: Argon2id aprobado.
- Si el upgrade a AGP 9 da problemas con Hilt, preguntar al usuario antes de aplicar el plan B (versiones compatibles con AGP 8.x).

## Notas de ejecución
- Paso 2: las librerías estables actuales (Compose 1.12.1, core 1.19.1, lifecycle 2.11.0) exigen `compileSdk = 37`; `targetSdk` sigue en 36. El plugin KSP queda declarado en el catálogo pero se aplicará cuando se use (Hilt en el paso 3, Room en el 6).
- Paso 3: Hilt 2.60.1 funciona con AGP 9.4 y KSP 2.3.12 (no hizo falta el plan B). Las carpetas `domain/`, `data/` y `di/` se crean en el primer paso que las use, para no dejar paquetes vacíos.
- Paso 4: se añadió kotlinx-coroutines 1.11.0 (core + test), verificada en Maven Central. `outcomeOf` vive en `data/` (lo usan los repositorios) y las interfaces de repositorio son mínimas; se ajustarán en los pasos 6 y 10 si hace falta. Los casos de uso se crean junto a cada pantalla que los necesita (pasos 8–12).
- Paso 5: Argon2id con parámetros por defecto 3 iteraciones / 64 MiB / 1 hilo. En el emulador (API 29) tarda ~310 ms; en un teléfono real será más lento. Los parámetros se guardarán en la cabecera de la bóveda, así que se pueden recalibrar sin romper bóvedas existentes. Pendiente: medir en un dispositivo físico (la prueba `logsDerivationTimeForCalibration` escribe el tiempo en logcat con la etiqueta `KdfCalibration`).
- Paso 6: Room 2.8.5, sqlcipher-android 4.19.1 y androidx.sqlite 2.7.0 (verificadas). `VaultSession` pasa a SQLCipher una copia de la clave porque la librería borra la que recibe. El esquema se exporta a `app/schemas/` para futuras migraciones. `EntryRepository` se implementa en el paso 10.
- Paso 7: además de `allowBackup=false` se vaciaron los archivos de reglas (`backup_rules.xml`, `data_extraction_rules.xml`) excluyendo todo, para cubrir también la transferencia entre dispositivos (API 31+). R8 queda activo en release; argon2kt no trae reglas propias y se añadió una (SQLCipher sí las trae). Verificado: el mapping de release conserva las clases de argon2kt y SQLCipher sin ofuscar.
- Paso 8: DataStore Preferences 1.3.0 guarda la cabecera de la bóveda (versión, parámetros Argon2, sal y DEK envuelta). La contraseña maestra mínima es de 12 caracteres. `VaultRepository` solo tiene `exists` y `create`; `unlock` y `lock` se añaden en el paso 9. Limitación conocida: los campos de texto de Compose usan `String`, que no se puede borrar de memoria; el ViewModel convierte a `CharArray` para el caso de uso y lo borra al terminar, y vacía el estado cuando se crea la bóveda.
- Paso 9: una contraseña maestra incorrecta se detecta cuando falla la autenticación GCM al desenvolver la DEK (`BadPaddingException` → `WrongMasterPassword`). No hay bloqueo por intentos fallidos: Argon2id (~0,3 s en el emulador) es el freno. Si se quiere limitar intentos, sería un paso aparte. El arranque distingue bóveda inexistente / bloqueada / ya desbloqueada (proceso vivo). La navegación hacia la pantalla de desbloqueo cuando la bóveda se bloquea llega con el auto-bloqueo (paso 11).
- Paso 10: `EntryRepository` cambió respecto al paso 4: `get(id)` se sustituye por `observe(id)` (la pantalla de detalle se actualiza sola al editar). `VaultSession` lanza `VaultLockedException` (subclase de `IllegalStateException`) cuando está bloqueada y el repositorio la traduce a `VaultLocked`. Se añadió `material-icons-core` (1.7.8, vía BOM) solo para la flecha de volver. El portapapeles se limpia 30 s después de copiar mediante un singleton con su propio scope; si el proceso muere antes, no se limpia, y se limpia siempre el contenido actual del portapapeles aunque el usuario haya copiado otra cosa después. El auto-bloqueo (paso 11) también deberá limpiarlo. La lista no tiene intents (solo muestra datos); la navegación va por callbacks.
- Paso 11: el tiempo de auto-bloqueo (`AutoLockTimeout`: inmediato por defecto, 30 s, 1 min, 5 min) ya se guarda en DataStore y lo lee `AutoLockController`, pero la pantalla para cambiarlo llega en el paso 13. Usa `ProcessLifecycleOwner` (`lifecycle-process` 2.11.0): el sistema lo detiene ~0,7 s después de que la última actividad pasa a segundo plano, así que «inmediato» no es literal. Se combinan un temporizador con la app viva y una comprobación con reloj monotónico al volver. Al bloquear se cierra la BD, se limpia el portapapeles y la navegación vuelve a la pantalla de desbloqueo vaciando la pila (se pierde lo que estuviera sin guardar en un formulario). `hilt-navigation-compose` quedó obsoleto en 1.4.0: se usa `hilt-lifecycle-viewmodel-compose` 1.4.0.

## Estado de los pasos
Actualizar al terminar cada paso. Estados: pendiente / en progreso / hecho.

| # | Rama | Estado |
|---|---|---|
| 1 | feature/docs-plan | hecho |
| 2 | feature/toolchain-update | hecho |
| 3 | feature/app-skeleton | hecho |
| 4 | feature/domain-core | hecho |
| 5 | feature/crypto-keys | en progreso |
| 6 | feature/encrypted-database | hecho |
| 7 | feature/hardening-manifest | hecho |
| 8 | feature/create-vault | hecho |
| 9 | feature/unlock-vault | hecho |
| 10 | feature/entries-crud | hecho |
| 11 | feature/auto-lock | hecho |
| 12 | feature/biometric-unlock | pendiente |
| 13 | feature/settings-and-polish | pendiente |
| 14 | feature/recovery-key | pendiente |
| 15 | feature/backup-export | pendiente |
| 16 | feature/backup-import | pendiente |
