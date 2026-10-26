# MiBoveda — guía de diseño

Dirección elegida: **«Bóveda segura»**. Aspecto de caja fuerte moderna: azul noche con acento turquesa, un candado como emblema y movimiento contenido. Paleta **propia y fija** (la app no toma colores del fondo de pantalla) y animación **rica pero con medida**.

## Principios
1. **Confianza ante todo**: nada decorativo que distraiga; lo importante (contraseñas, claves, errores) siempre se lee bien.
2. **Un solo lenguaje**: todo color, forma, tamaño de letra y duración sale de los tokens de abajo, no de valores sueltos en las pantallas.
3. **El movimiento explica**: una animación debe mostrar qué cambió (se abrió el candado, entró un elemento, se copió algo), nunca solo adornar. Todas duran como mucho 450 ms.
4. **Accesible por defecto**: contraste suficiente en claro y oscuro, zonas táctiles de 48 dp, descripciones para los iconos, y respeto del ajuste del sistema de reducir animaciones.

## Color
Definidos en `presentation/theme/Color.kt` (`VaultDarkColors`, `VaultLightColors`).

| Rol | Oscuro | Claro | Uso |
|---|---|---|---|
| `background` / `surface` | `#0B1220` | `#F3F6FB` | Fondo de pantallas |
| `surfaceContainer*` | `#080D18` … `#1F2E49` | `#FFFFFF` … `#D9E3F2` | Tarjetas y campos, de más hundido a más elevado |
| `onBackground` | `#E6EDF7` | `#0B1220` | Texto principal |
| `onSurfaceVariant` | `#9FB0CC` | `#44546F` | Texto secundario |
| `primary` | `#2DD4BF` turquesa | `#0F766E` turquesa oscuro | Acción principal, enlaces, acentos |
| `primaryContainer` | `#0E4A44` | `#C9F5EE` | Botón flotante, elementos destacados |
| `secondary` | `#7AA2F7` | `#3456B8` | Elementos informativos |
| `tertiary` | `#F5B84B` ámbar | `#9A5B00` | Avisos (por ejemplo, contraseña débil) |
| `error` | `#FF7B7B` | `#B3261E` | Errores |
| `outline` / `outlineVariant` | `#3A4B6B` / `#26354F` | `#7A8BA8` / `#CBD6E6` | Bordes y divisores |

El fondo de la ventana (lo que se ve antes del primer dibujo) también sigue el modo (`window_background`), para que no haya un destello blanco al abrir en oscuro.

## Tipografía
Fuente del sistema (sin dependencias ni descargas) con una jerarquía más marcada, en `Type.kt`:
títulos en semi-negrita y algo más ajustados (`headline*`, `titleLarge`), texto de lectura con interletrado normal (`body*`) y `MonospaceStyle` para lo que se lee carácter a carácter, como la clave de recuperación.

## Formas
`Shape.kt`: esquinas de 6 / 10 / 14 / 20 / 28 dp (`extraSmall` … `extraLarge`). Campos y tarjetas usan `medium`; botones, `large`; hojas y diálogos, `extraLarge`.

## Espaciado
Rejilla de 4 dp. Márgenes de pantalla 24 dp; separación entre bloques 16 dp; entre elementos relacionados 8–12 dp.

## Movimiento
`Motion.kt`:

| Token | Valor | Para |
|---|---|---|
| `Short` | 150 ms | Cambios de estado pequeños (pulsar, marcar) |
| `Medium` | 300 ms | Entradas y salidas de elementos, barras de progreso |
| `Long` | 450 ms | Transiciones entre pantallas y el candado |
| `Emphasized` | curva (0,2 · 0 · 0 · 1) | Movimiento general |
| `EmphasizedDecelerate` | curva (0,05 · 0,7 · 0,1 · 1) | Lo que llega |
| `EmphasizedAccelerate` | curva (0,3 · 0 · 0,8 · 0,15) | Lo que se va |

Compose ya respeta el ajuste del sistema «Escala de duración de animación»: con valor 0 todas las animaciones se saltan, así que no hace falta comprobarlo a mano.

## Componentes (`presentation/ui`)
| Componente | Qué hace |
|---|---|
| `VaultBackground` | Fondo común: degradado profundo con dos resplandores suaves. Se queda quieto mientras las pantallas se deslizan encima |
| `LockEmblem` | Emblema de la app: dial de caja fuerte con candado. «Respira» mientras espera, el dial da una vuelta y el candado se abre al desbloquear, y se sacude y se tiñe de rojo ante un error |
| `VaultButton` / `VaultOutlinedButton` / `VaultTextAction` | Botones: degradado de marca con rebote al pulsar, spinner que sustituye al texto al cargar, contorno turquesa y acción de texto |
| `VaultTextField` / `PasswordTextField` | Campos tonales con icono; el ojo de contraseña se transforma entre abierto y cerrado; marca verificada animada en la confirmación |
| `VaultCard` | Panel con borde luminoso (y sombra turquesa suave en modo claro) |
| `StrengthMeter` | Tres segmentos que se encienden uno a uno y cambian de rojo a ámbar a turquesa, con la etiqueta deslizándose |
| `Avatar` | Círculo con la inicial sobre un degradado elegido por el título |
| `SecretText` | Contraseña como puntos, o «descifrándose»: caracteres aleatorios que se asientan de izquierda a derecha |
| `RecoveryKeyPanel` | Clave tapada por un velo hasta tocarla; se revela con desvanecimiento |
| `AnimatedCheck` | Círculo que se rellena y marca que se dibuja trazo a trazo |
| `cascadeEntrance` | Los elementos de un grupo suben y aparecen uno tras otro |
| `VaultTopBar` / `VaultLargeTopBar` | Barras transparentes; la grande de la lista se contrae al hacer scroll |

## Animaciones
| Dónde | Qué ocurre | Duración |
|---|---|---|
| Desbloqueo correcto | El dial gira, el candado se abre y la pantalla entra en la lista con zoom | ~0,8 s |
| Contraseña incorrecta | El candado se sacude y se pone rojo; el mensaje de error se despliega | ~0,3 s |
| Crear bóveda | Medidor de seguridad que se llena, marca verificada al coincidir, pasos que se deslizan, clave que se revela | 0,15–0,45 s |
| Lista | Tarjetas que entran en cascada; el botón «Añadir» se encoge al bajar y crece al subir | 0,45 s + 55 ms por tarjeta |
| Detalle | Cascada de bloques; la contraseña se «descifra»; el aviso de copiado sube con su marca y se va solo | 0,4 s / 2,8 s |
| Ajustes | Tarjetas en cascada; la opción elegida se ilumina y muestra una marca que aparece con rebote; confirmaciones con marca dibujada | 0,3 s |
| Entre pantallas | Deslizamiento lateral con desvanecimiento; de las pantallas de acceso a la lista, zoom de «abrir la bóveda» | 0,3–0,45 s |

Todo respeta el ajuste del sistema «escala de duración de animación».

## Cómo ver el diseño sin teléfono
Tres tipos de evidencia, todas en `docs/ui/`:
- **Capturas** (`antes/`, `despues/`): `ScreenshotsTest` dibuja cada pantalla con datos de ejemplo en claro y oscuro, incluidos momentos intermedios de algunas animaciones.
- **Tiras de animación** (`animaciones/`): `FilmstripsTest` mueve a mano el reloj de animación y dibuja la misma pantalla en varios instantes, con la hora bajo cada cuadro. Son exactas y repetibles.
- **Videos** (`videos/`, formato webm): grabaciones del emulador con la app real. Se hicieron con una copia local que desactiva `FLAG_SECURE` (si no, Android graba pantalla en negro); esa copia nunca se sube y en la app real las capturas siguen bloqueadas.

Las dos pruebas se lanzan con `adb shell am instrument` (Gradle desinstala la app y sus archivos al terminar):

```
./gradlew installDebug installDebugAndroidTest
adb shell am instrument -w -e class com.salvarez.miboveda.screenshots.ScreenshotsTest \
  com.salvarez.miboveda.test/androidx.test.runner.AndroidJUnitRunner
adb pull /sdcard/Android/data/com.salvarez.miboveda/files/screenshots
```
(`FilmstripsTest` deja sus imágenes en `.../files/filmstrips`.)

## Hoja de ruta del rediseño
El rediseño se planeó en cuatro pasos (17 a 20), pero tras la primera revisión («demasiado plano») se entregó entero en el paso 17: sistema de diseño, todas las pantallas, animaciones y transiciones. Lo que queda es afinar según los comentarios.
