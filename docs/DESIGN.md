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

## Cómo ver el diseño sin teléfono
`ScreenshotsTest` (carpeta `androidTest/.../screenshots`) dibuja cada pantalla con datos de ejemplo en claro y oscuro y guarda las imágenes, sin tocar ninguna bóveda real. Hay que lanzarlo con `adb shell am instrument` (Gradle desinstala la app y sus archivos al terminar):

```
./gradlew installDebug installDebugAndroidTest
adb shell am instrument -w -e class com.salvarez.miboveda.screenshots.ScreenshotsTest \
  com.salvarez.miboveda.test/androidx.test.runner.AndroidJUnitRunner
adb pull /sdcard/Android/data/com.salvarez.miboveda/files/screenshots
```

Las imágenes de cada paso viven en `docs/ui/` (carpetas `antes` y `despues`, y una por cada paso siguiente). Los videos de animaciones se graban en el emulador con una copia local que desactiva `FLAG_SECURE`, que nunca se sube: en la app real las capturas de pantalla siguen bloqueadas.

## Hoja de ruta del rediseño
| Paso | Rama | Qué trae |
|---|---|---|
| 17 | `feature/ui-theme` | Sistema de diseño: paleta, tipografía, formas, movimiento, guía y capturas de antes y después |
| 18 | `feature/ui-auth-screens` | Pantallas de creación, restauración, clave de recuperación, desbloqueo y recuperación: candado animado, medidor de seguridad animado, revelación de la clave |
| 19 | `feature/ui-entries` | Lista con tarjetas e iniciales, estado vacío ilustrado, entrada escalonada, detalle y formulario rediseñados, aviso animado al copiar |
| 20 | `feature/ui-settings-transitions` | Ajustes por secciones con iconos, confirmaciones animadas al exportar o importar, transiciones entre pantallas y repaso final |
