# Formato del archivo de copia de seguridad (versión 1)

Archivo binario, números en big-endian. Lo escribe `BackupCodec`.

## Parte en claro (cabecera)
| Campo | Tamaño | Contenido |
|---|---|---|
| Marca | 8 bytes | `MIBOVEDA` (ASCII) |
| Versión del formato | 4 bytes | `1` |
| Derivación de clave | 1 byte | `1` = Argon2id |
| Iteraciones | 4 bytes | por defecto 3 |
| Memoria (KiB) | 4 bytes | por defecto 65536 |
| Paralelismo | 4 bytes | por defecto 1 |
| Tamaño de la sal | 1 byte | 16 |
| Sal | N bytes | aleatoria, distinta en cada copia |

## Parte cifrada
| Campo | Tamaño | Contenido |
|---|---|---|
| IV | 12 bytes | aleatorio |
| Texto cifrado + etiqueta | resto | AES-256-GCM; la **cabecera completa va como datos autenticados (AAD)** |

La clave es `Argon2id(contraseña maestra, sal, parámetros de la cabecera)` con 32 bytes de salida. Si alguien cambia la sal o los parámetros del archivo, la etiqueta GCM deja de coincidir y el archivo no se abre.

## Contenido descifrado
```
int   número de entradas
por cada entrada:
  texto título, usuario, contraseña, url, notas   (int longitud + bytes UTF-8)
  long  fecha de modificación (milisegundos desde 1970)
```
Los identificadores internos no se guardan: al importar se crean nuevos.

## Límites al leer
Para que un archivo manipulado no pueda agotar la memoria o el tiempo del dispositivo, el lector rechaza (`CorruptBackup`) cabeceras con más de 20 iteraciones, memoria fuera de 8 KiB–256 MiB, más de 16 hilos o una sal de menos de 8 bytes, y archivos de más de 32 MiB. Estos límites se comprueban **antes** de derivar ninguna clave.

## Qué garantiza y qué no
- Sin la contraseña maestra con la que se hizo la copia no se puede leer ni verificar ningún dato; el archivo no contiene texto legible.
- Una contraseña incorrecta, un archivo modificado o una cabecera alterada fallan de la misma forma (la etiqueta GCM no coincide).
- La copia se abre con la contraseña maestra **que existía al exportarla**. Si después se cambia la contraseña con la clave de recuperación, esa copia sigue pidiendo la anterior.
- La clave de recuperación no abre copias de seguridad.
