# MiBoveda — pendientes

Lista viva de fallos conocidos, pruebas que faltan y mejoras para hacer después. Se actualiza cada vez que aparece algo nuevo; lo resuelto pasa a la sección «Hecho» con el PR que lo cerró.

Prioridad: **Alta** = afecta al uso normal o a la seguridad · **Media** = conviene hacerlo · **Baja** = mejora opcional.

## Fallos conocidos
| # | Prioridad | Qué pasa | Origen |
|---|---|---|---|
| 1 | Alta | Al crear la bóveda, si sales de la app en la pantalla de la clave de recuperación (por ejemplo para copiarla) y al volver pulsas «Continuar», la lista dice «La bóveda está bloqueada» en vez de llevarte al desbloqueo. Causa: el bloqueo automático cierra la bóveda mientras la pantalla de la clave sigue abierta, y la guardia de navegación solo reacciona al cambio de estado, no a que se abra una pantalla de datos con la bóveda ya cerrada. Arreglo previsto: decidir por ruta actual + estado de bloqueo (si la ruta es de datos y está bloqueada, ir al desbloqueo). | Paso 14 |
| 2 | Media | Si el selector de archivos del sistema falla o se cancela a mitad de una exportación, el sistema puede dejar un archivo vacío (0 bytes) en la carpeta elegida. Mejorar: borrar el documento si la escritura falla. | Paso 15 |
| 20 | Media | Al restaurar en un dispositivo nuevo, la bóveda se crea antes de insertar las entradas. Si esa inserción fallara (muy raro), quedaría una bóveda creada y vacía; habría que importar la copia otra vez desde Ajustes. Mejorar: borrar la bóveda recién creada si falla. | Paso 16 |
| 21 | Baja | Importar detecta duplicados solo si coinciden todos los campos. Una entrada editada desde la copia se añade como nueva (no se fusiona por fecha). | Paso 16 |
| 22 | Baja | El selector de importación muestra todos los archivos (`*/*`); no puede filtrar por tipo porque la copia no tiene un tipo MIME propio. | Paso 16 |
| 23 | Baja | La gracia de 2 minutos del selector de archivos mantiene la bóveda abierta ese tiempo si el usuario se va a otra app durante la elección. Está acotada, pero podría reducirse. | Paso 15 |

## Pruebas pendientes en un teléfono real
| # | Prioridad | Qué falta probar | Origen |
|---|---|---|---|
| 3 | Alta | Desbloqueo con huella o rostro de punta a punta (el emulador API 29 no puede crear la clave que exige biometría): activar, desbloquear, cancelar el prompt, cambiar las huellas registradas, desactivar. | Paso 12 |
| 4 | Media | El portapapeles se borra a los 30 s tras copiar (y al bloquear). Android limita leerlo en segundo plano, así que no hay prueba automática. | Paso 10 |
| 5 | Media | Rotar la pantalla con la bóveda abierta no debe bloquearla. | Paso 11 |
| 6 | Media | Compilación **release** firmada y ejecutada en un dispositivo: R8 con las librerías nativas (argon2kt y SQLCipher) solo se comprobó revisando el `mapping.txt`, no en ejecución. | Paso 7 |
| 7 | Baja | Revisión visual de todas las pantallas en modo claro y oscuro con un teléfono real (`FLAG_SECURE` impide capturas, así que solo se comprobó que abren sin errores). | Paso 13 |

## Limitaciones conocidas (decisiones de diseño a revisar)
| # | Prioridad | Qué | Origen |
|---|---|---|---|
| 8 | Media | No hay bloqueo por intentos fallidos al desbloquear; el único freno es el coste de Argon2id. | Paso 9 |
| 9 | Baja | Los campos de texto de Compose usan `String`, que no se puede borrar de memoria. Se acorta su vida, pero no se elimina. | Paso 8 |
| 10 | Media | Si la app muere justo al crear la bóveda, antes de confirmar que se guardó la clave de recuperación, esa clave se pierde y hay que generar otra desde Ajustes. | Paso 14 |
| 11 | Baja | Lo que haya sin guardar en un formulario se pierde al bloquear la bóveda (a propósito). Podría guardarse cifrado un borrador. | Paso 11 |
| 12 | Baja | Antes de Android 11 el sistema no distingue sensores biométricos «fuertes»; allí solo se comprueba que haya uno registrado. | Paso 12 |
| 13 | Media | Una copia de seguridad solo se abre con la contraseña maestra vigente al exportarla; si luego se cambia con la clave de recuperación, esa copia pide la anterior. Valorar avisar o exportar de nuevo tras recuperar. | Paso 15 |
| 14 | Baja | «Inmediato» en el bloqueo automático no es literal: el sistema avisa de que la app salió ~0,7 s después. | Paso 11 |

## Mejoras de código
| # | Prioridad | Qué | Origen |
|---|---|---|---|
| 15 | Baja | `SettingsViewModel` ya recibe 13 dependencias (biometría, clave de recuperación, copias, tiempo de bloqueo). Dividirlo por secciones si sigue creciendo. | Paso 15 |
| 16 | Baja | `.idea/deploymentTargetSelector.xml` aparece modificado cada vez que se abre Android Studio. Valorar quitarlo del control de versiones (`.gitignore`). | Varios |
| 17 | Baja | Varios casos de uso son solo un traspaso al repositorio (por ejemplo `LockVault`, `DeleteEntry`). Se dejaron por coherencia con la arquitectura; revisar si compensan. | Pasos 9–10 |

## Ideas opcionales (ya mencionadas en el plan)
| # | Prioridad | Qué |
|---|---|---|
| 18 | Baja | Cambiar la contraseña maestra desde Ajustes (hoy solo se cambia con la clave de recuperación). |
| 19 | Baja | Generador de contraseñas al crear o editar una entrada. |

## Hecho
_(vacío por ahora)_
