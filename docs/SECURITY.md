# MiBoveda — checklist de seguridad

Estado a la fecha del paso 13. **Auto** = lo comprueba un test automático; **Manual** = lo comprobó una persona o hay que comprobarlo en un teléfono.

## Cifrado y claves
| Control | Estado | Cómo se comprueba |
|---|---|---|
| Base de datos cifrada con SQLCipher; el archivo no empieza con `SQLite format 3` ni contiene texto en claro | Auto | `VaultSessionTest` |
| La contraseña maestra pasa por Argon2id (3 iteraciones, 64 MiB, 1 hilo) y produce la KEK | Auto | `Argon2KeyDerivationTest` |
| La DEK es aleatoria (256 bits) y solo se guarda envuelta con AES-256-GCM | Auto | `AesGcmKeyWrapperTest`, `VaultRepositoryImplTest` |
| Una contraseña incorrecta no puede desenvolver la DEK; un dato alterado se detecta | Auto | `AesGcmKeyWrapperTest`, `VaultRepositoryImplTest` |
| Sin criptografía propia: solo JCA, Android Keystore, SQLCipher y argon2kt | Revisión | Código |
| La contraseña y las claves se borran de memoria tras usarse (`CharArray`/`ByteArray` rellenados con ceros) | Revisión | Código; ver limitaciones |

## Clave de recuperación
| Control | Estado | Cómo se comprueba |
|---|---|---|
| 256 bits aleatorios; solo se guarda la DEK envuelta con ella, nunca la clave | Auto | `VaultRepositoryImplTest`, `RecoveryKeyCodecTest` |
| Recuperar fija una contraseña nueva, conserva los datos y deja la anterior sin efecto | Auto | `VaultRepositoryImplTest` |
| Una clave mal escrita, ajena o antigua (tras generar otra) no sirve | Auto | `VaultRepositoryImplTest`, `RecoveryKeyCodecTest` |
| No se puede salir de la pantalla de creación sin confirmar que se guardó | Auto | `OnboardingViewModelTest`, `OnboardingScreenTest` |
| La clave no aparece en `toString` de estados e intents | Auto | `OnboardingViewModelTest`, `RecoveryViewModelTest`, `SettingsViewModelTest` |
| El portapapeles se limpia a los 30 s tras copiar la clave | Manual (pendiente) | Igual que las contraseñas |

## Copia de seguridad
| Control | Estado | Cómo se comprueba |
|---|---|---|
| El archivo no contiene texto legible y cada copia es distinta (IV y sal aleatorios) | Auto | `BackupCodecTest`, `BackupRepositoryImplTest` |
| Clave = Argon2id(contraseña maestra, sal propia de la copia); cabecera autenticada con GCM | Auto | `BackupCodecTest` (alterar la cabecera o los datos falla) |
| Se exige la contraseña maestra correcta antes de escribir; con otra no se crea archivo | Auto | `BackupRepositoryImplTest` |
| Con la bóveda bloqueada no se escribe nada | Auto | `BackupRepositoryImplTest` |
| El selector de archivos no bloquea la bóveda a mitad de la exportación (gracia única de 2 min) | Auto + Manual | `AutoLockControllerTest`; emulador con el selector real |
| La copia exportada se puede descifrar con la contraseña maestra y contiene todas las entradas | Auto | `BackupRepositoryImplTest` |
| El archivo exportado queda donde el usuario lo elija: protegerlo es cosa suya (está cifrado, pero una contraseña maestra débil lo hace atacable offline) | Limitación | |

## Biometría
| Control | Estado | Cómo se comprueba |
|---|---|---|
| La DEK se envuelve con una clave del Keystore que exige huella o rostro en cada uso | Manual (pendiente en teléfono) | El emulador API 29 no permite crearla |
| El ciclo activar → bloquear → desbloquear recupera los mismos datos | Auto (con clave sin exigencia de huella) | `BiometricRepositoryImplTest` |
| El blob guardado no contiene la DEK en claro | Auto | `BiometricRepositoryImplTest` |
| Si cambian las huellas registradas, la función se desactiva y se explica | Manual (pendiente en teléfono) | Ver paso 12 del PR #12 |
| La contraseña maestra siempre funciona | Auto | `BiometricRepositoryImplTest` |

## Exposición de datos
| Control | Estado | Cómo se comprueba |
|---|---|---|
| `allowBackup="false"` y reglas de extracción que excluyen todo | Auto | `HardeningTest` |
| `FLAG_SECURE`: sin capturas, grabación ni miniatura en recientes | Auto + Manual | `HardeningTest`; captura en el emulador sale bloqueada |
| Portapapeles marcado como sensible (Android 13+) y borrado a los 30 s o al bloquear | Manual (pendiente) | El sistema limita leerlo en segundo plano |
| Sin llamadas a `Log`, `println` ni `printStackTrace` en el código de la app | Auto | `grep` del paso 13 (sin resultados) |
| `toString` de modelos, estados e intents con secretos no los incluye | Auto | `PasswordEntryTest`, `EntryMappingTest`, `SettingsViewModelTest`, `EntryEditViewModelTest` |
| Los errores que ve el usuario son mensajes propios, sin datos técnicos ni secretos | Revisión | `ErrorMessages.kt` (`when` exhaustivo sobre `VaultError`) |

## Bloqueo
| Control | Estado | Cómo se comprueba |
|---|---|---|
| La bóveda se bloquea al salir de la app según el tiempo elegido (por defecto, al salir) | Auto + Manual | `AutoLockControllerTest`; emulador con Inicio y volver |
| Al bloquear se cierra la BD, se limpia el portapapeles y se vuelve al desbloqueo vaciando la pila | Manual | Emulador |
| Rotar la pantalla no bloquea | Manual (pendiente) | |

## Código y dependencias
| Control | Estado |
|---|---|
| Versiones estables (sin alpha/beta/rc), verificadas en fuentes oficiales | Revisado en cada paso |
| R8 activo en release; reglas para argon2kt y las de SQLCipher | Auto (build) + revisión del `mapping.txt` |
| El APK de release no está firmado todavía | Pendiente |

## Limitaciones conocidas
- Los campos de texto de Compose usan `String`, que no se puede borrar de memoria; se reduce el tiempo que viven pero no se elimina.
- No hay bloqueo por intentos fallidos: Argon2id (~0,3 s en el emulador, más en teléfonos) es el único freno.
- Quien pierda a la vez la contraseña maestra y la clave de recuperación pierde los datos (no hay forma de saltárselo, es a propósito).
- Si la app muere justo al crear la bóveda, antes de confirmar que se guardó la clave, esa clave se pierde; se genera otra desde Ajustes.
- Si el sistema mata la app antes de 30 s, el portapapeles no se limpia solo.
- Lo que haya sin guardar en un formulario se pierde al bloquear (a propósito).
- Un dispositivo con root o malware con permisos de accesibilidad puede leer la pantalla; FLAG_SECURE no lo evita.

## Pendiente de la Fase 2
Importación de la copia de seguridad (paso 16).
