# Liquid Calc

[English](README.md) | **Español**

SPDX-License-Identifier: GPL-3.0-or-later

Se instala en el celu como **Calculadora** (o *Calculator*, según el idioma).

Calculadora libre para Android, con la misma interfaz de cristal líquido que
[Liquid Calendar](https://github.com/mrsalvaa/liquid-calendar),
[Liquid Clock](https://github.com/mrsalvaa/liquid-clock) y
[Liquid Gallery](https://github.com/mrsalvaa/liquid-gallery), y el mismo estilo Sólido para quien
prefiera algo más sobrio.

## Privacidad

No pide ningún permiso y no se conecta a internet: no hay red, anuncios, telemetría ni servicios
de Google. El historial y los ajustes quedan en la carpeta privada de la app, sin copia en la nube,
y el historial se puede borrar o desactivar en Ajustes.

## Funciones

- **Básica y científica**: sin, cos, tan y sus inversas (en grados o radianes), ln, log, 10ˣ,
  eˣ, fracciones, potencias, raíz cuadrada, cúbica y n-ésima, factorial, valor absoluto, x⁻¹, π
  y e. El bloque científico sigue la distribución de una Casio: la segunda función de cada tecla
  (lo que en la Casio sale con SHIFT) se ve chica arriba y se usa manteniéndola apretada.
- **Escritura natural**: la pantalla muestra la cuenta como en una calculadora científica: x³
  con el exponente arriba y más chico, fracciones apiladas, raíces con la barra encima. Tocar la
  cuenta pone el cursor ahí para corregirla, incluso después de "="; tocar lo vacío lo lleva al
  final.
- Números con el separador de miles y el decimal del idioma: 27.000,3 en español.
- **Aritmética decimal exacta**: 0,1 + 0,2 da 0,3. Todo se calcula con 34 cifras (decimal128) y se
  muestran 15, así no aparecen errores de redondeo.
- **Parser propio**: la expresión se interpreta con un parser hecho para la calculadora; nunca se
  ejecuta como código.
- **Porcentaje de calculadora**: 200 + 10% = 220, 200 − 10% = 180, 50 × 10% = 5.
- Paréntesis (los que faltan al final se cierran solos), multiplicación implícita (2π, 3(4)),
  memoria (MC, MR, M+, M−), ANS y errores claros: división por cero, fuera del dominio, resultado
  demasiado grande o expresión incompleta.
- **Historial** local: tocar una cuenta la vuelve a poner en pantalla. Se borra entero o se
  desactiva (y se borra) desde Ajustes.
- Copiar el resultado: manteniendo apretado el número aparece "Copiar".
- Mantener apretado "borrar" 0,4 s borra todo (aunque se suelte enseguida) y la cuenta se va
  hacia arriba. Las teclas responden en toda su superficie, también en el borde.
- **Teclado físico**: números y `+ - * / ^ % ! ( )`; Enter o `=` calcula, Retroceso borra,
  Esc o Supr limpia, las flechas mueven el cursor. Letras: `s` sin, `c` cos, `t` tan, `l` ln, `g` log, `r` √, `p` π,
  `e`, `a` ANS. Ctrl+C copia, Ctrl+V pega, Ctrl+H abre el historial y Ctrl+D cambia entre
  grados y radianes.
- Vertical y horizontal: en horizontal, las teclas científicas van al lado de las básicas.
- Accesibilidad: cada tecla tiene nombre para TalkBack ("dividido", "seno"), el resultado se
  anuncia solo, las teclas miden 48 dp o más y los textos siguen el contraste del tema.

## Descargar e instalar

Bajá el APK desde [Releases](../../releases) e instalalo (Android 12 o posterior).

Comprobá que el archivo no fue alterado:

```bash
sha256sum -c liquid-calc-v0.5.0.apk.sha256
```

```
c6333ef8fd8bf92a13d934945407c460c2636438591e96aa3e8658625fbdb4a5  liquid-calc-v0.5.0.apk
```

Y que está firmado con la clave del proyecto (huella SHA-256 del certificado):

```
82:B2:D5:E7:86:2B:93:EA:87:9B:4A:0B:39:10:74:3F:77:ED:21:DF:58:D9:B2:47:7D:C4:D9:54:7B:23:4D:00
```

```bash
apksigner verify --print-certs liquid-calc-v0.5.0.apk
```

## Compilar

Se compila en Linux (probado en CachyOS) para Android 12 o posterior. No hay versión de
escritorio.

```bash
git clone https://github.com/mrsalvaa/liquid-calc.git
cd liquid-calc
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk   # JDK 17
export ANDROID_HOME=/ruta/al/android-sdk          # con la plataforma 35
./gradlew :core:test :app:testDebugUnitTest      # tests
./gradlew :app:assembleDebug                      # APK debug (dev.liquidcalc.debug)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

La release (`./gradlew :app:assembleRelease`) se firma con la clave de
`~/.android-keys/liquid-calc.properties` si existe, o con la de debug.

## Estructura

```
core/        Motor de cálculo en Kotlin puro (sin Android ni dependencias), con tests
app/         La app: pantalla, teclas, historial, ajustes, teclado físico; ui/glass son
             los componentes de cristal (los mismos que en las otras apps)
DESIGN.md    Qué se reutiliza de la suite y cómo
```

## Dependencias

El motor (`core`) no tiene dependencias: usa `java.math.BigDecimal` y series propias. La app
usa Jetpack Compose, Material Icons, kotlinx-serialization y
[Backdrop](https://github.com/Kyant0/AndroidLiquidGlass) (el efecto de cristal). Todas son
Apache-2.0, compatibles con GPL-3.0-or-later. En los tests además se usan kotlin-test y JUnit 4
(EPL-1.0), que no van en el APK.

### Auditoría

- **Árbol completo** (`./gradlew :app:dependencies --configuration releaseRuntimeClasspath`):
  95 artefactos, todos con licencia Apache-2.0 según su POM. La lista está abajo.
- **Nada de Google Play Services, Firebase ni datatransport**: ni en el árbol ni en el código del
  APK release (`com.google.android.gms`, `firebase`, `datatransport`: 0 apariciones).
- **Sin red**: el APK release no tiene referencias a `java.net.URL`, `HttpURLConnection`,
  sockets ni OkHttp. La única clase de `android.net` es `Uri`, que Compose usa para el
  portapapeles.
- **Manifest final** (mergeado, debug y release): sin permisos, sin providers, sin receivers y
  sin services; solo la actividad y los 5 alias de íconos. Se sacan con `tools:node="remove"` el
  permiso interno de androidx.core, `androidx.startup` (arrancaría emoji2, que puede pedir
  fuentes a Google Play Services, lifecycle-process y profileinstaller) y el receptor de
  profileinstaller.
- **Backdrop**: 57 clases, sin red ni permisos; solo depende de Compose y de la librería estándar
  de Kotlin.
- **F-Droid**: `dependenciesInfo` desactivado (sin el bloque firmado por Google) y sin perfiles
  ART en el APK (`assets/dexopt`), que pueden romper la reproducibilidad. Compose trae una
  librería nativa de AndroidX, `libandroidx.graphics.path.so` (Apache-2.0, código en AOSP), que
  llega desde Maven; el repo no tiene binarios.

<details>
<summary>Las 95 dependencias de la release</summary>

| Dependencia | Versión | Licencia |
|---|---|---|
| `androidx.activity:activity` | 1.10.1 | Apache-2.0 |
| `androidx.activity:activity-compose` | 1.10.1 | Apache-2.0 |
| `androidx.activity:activity-ktx` | 1.10.1 | Apache-2.0 |
| `androidx.annotation:annotation` | 1.9.1 | Apache-2.0 |
| `androidx.annotation:annotation-experimental` | 1.4.1 | Apache-2.0 |
| `androidx.annotation:annotation-jvm` | 1.9.1 | Apache-2.0 |
| `androidx.arch.core:core-common` | 2.2.0 | Apache-2.0 |
| `androidx.arch.core:core-runtime` | 2.2.0 | Apache-2.0 |
| `androidx.autofill:autofill` | 1.0.0 | Apache-2.0 |
| `androidx.collection:collection` | 1.5.0 | Apache-2.0 |
| `androidx.collection:collection-jvm` | 1.5.0 | Apache-2.0 |
| `androidx.collection:collection-ktx` | 1.5.0 | Apache-2.0 |
| `androidx.compose:compose-bom` | 2025.10.01 | Apache-2.0 |
| `androidx.compose.animation:animation` | 1.9.4 | Apache-2.0 |
| `androidx.compose.animation:animation-android` | 1.9.4 | Apache-2.0 |
| `androidx.compose.animation:animation-core` | 1.9.4 | Apache-2.0 |
| `androidx.compose.animation:animation-core-android` | 1.9.4 | Apache-2.0 |
| `androidx.compose.foundation:foundation` | 1.9.4 | Apache-2.0 |
| `androidx.compose.foundation:foundation-android` | 1.9.4 | Apache-2.0 |
| `androidx.compose.foundation:foundation-layout` | 1.9.4 | Apache-2.0 |
| `androidx.compose.foundation:foundation-layout-android` | 1.9.4 | Apache-2.0 |
| `androidx.compose.material:material-icons-core` | 1.7.8 | Apache-2.0 |
| `androidx.compose.material:material-icons-core-android` | 1.7.8 | Apache-2.0 |
| `androidx.compose.material:material-icons-extended` | 1.7.8 | Apache-2.0 |
| `androidx.compose.material:material-icons-extended-android` | 1.7.8 | Apache-2.0 |
| `androidx.compose.material:material-ripple` | 1.9.4 | Apache-2.0 |
| `androidx.compose.material:material-ripple-android` | 1.9.4 | Apache-2.0 |
| `androidx.compose.material3:material3` | 1.4.0 | Apache-2.0 |
| `androidx.compose.material3:material3-android` | 1.4.0 | Apache-2.0 |
| `androidx.compose.runtime:runtime` | 1.9.4 | Apache-2.0 |
| `androidx.compose.runtime:runtime-android` | 1.9.4 | Apache-2.0 |
| `androidx.compose.runtime:runtime-annotation` | 1.9.4 | Apache-2.0 |
| `androidx.compose.runtime:runtime-annotation-android` | 1.9.4 | Apache-2.0 |
| `androidx.compose.runtime:runtime-saveable` | 1.9.4 | Apache-2.0 |
| `androidx.compose.runtime:runtime-saveable-android` | 1.9.4 | Apache-2.0 |
| `androidx.compose.ui:ui` | 1.9.4 | Apache-2.0 |
| `androidx.compose.ui:ui-android` | 1.9.4 | Apache-2.0 |
| `androidx.compose.ui:ui-geometry` | 1.9.4 | Apache-2.0 |
| `androidx.compose.ui:ui-geometry-android` | 1.9.4 | Apache-2.0 |
| `androidx.compose.ui:ui-graphics` | 1.9.4 | Apache-2.0 |
| `androidx.compose.ui:ui-graphics-android` | 1.9.4 | Apache-2.0 |
| `androidx.compose.ui:ui-text` | 1.9.4 | Apache-2.0 |
| `androidx.compose.ui:ui-text-android` | 1.9.4 | Apache-2.0 |
| `androidx.compose.ui:ui-unit` | 1.9.4 | Apache-2.0 |
| `androidx.compose.ui:ui-unit-android` | 1.9.4 | Apache-2.0 |
| `androidx.compose.ui:ui-util` | 1.9.4 | Apache-2.0 |
| `androidx.compose.ui:ui-util-android` | 1.9.4 | Apache-2.0 |
| `androidx.concurrent:concurrent-futures` | 1.1.0 | Apache-2.0 |
| `androidx.core:core` | 1.16.0 | Apache-2.0 |
| `androidx.core:core-ktx` | 1.16.0 | Apache-2.0 |
| `androidx.core:core-viewtree` | 1.0.0 | Apache-2.0 |
| `androidx.customview:customview-poolingcontainer` | 1.0.0 | Apache-2.0 |
| `androidx.emoji2:emoji2` | 1.4.0 | Apache-2.0 |
| `androidx.graphics:graphics-path` | 1.0.1 | Apache-2.0 |
| `androidx.interpolator:interpolator` | 1.0.0 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-common` | 2.9.4 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-common-java8` | 2.9.4 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-common-jvm` | 2.9.4 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-livedata-core` | 2.9.4 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-process` | 2.9.4 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-runtime` | 2.9.4 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-runtime-android` | 2.9.4 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.9.4 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-runtime-compose-android` | 2.9.4 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.9.4 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-runtime-ktx-android` | 2.9.4 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-viewmodel` | 2.9.4 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-viewmodel-android` | 2.9.4 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-viewmodel-ktx` | 2.9.4 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-viewmodel-savedstate` | 2.9.4 | Apache-2.0 |
| `androidx.lifecycle:lifecycle-viewmodel-savedstate-android` | 2.9.4 | Apache-2.0 |
| `androidx.profileinstaller:profileinstaller` | 1.4.0 | Apache-2.0 |
| `androidx.savedstate:savedstate` | 1.3.3 | Apache-2.0 |
| `androidx.savedstate:savedstate-android` | 1.3.3 | Apache-2.0 |
| `androidx.savedstate:savedstate-compose` | 1.3.3 | Apache-2.0 |
| `androidx.savedstate:savedstate-compose-android` | 1.3.3 | Apache-2.0 |
| `androidx.savedstate:savedstate-ktx` | 1.3.3 | Apache-2.0 |
| `androidx.startup:startup-runtime` | 1.1.1 | Apache-2.0 |
| `androidx.tracing:tracing` | 1.2.0 | Apache-2.0 |
| `androidx.versionedparcelable:versionedparcelable` | 1.1.1 | Apache-2.0 |
| `com.google.guava:listenablefuture` | 1.0 | Apache-2.0 |
| `io.github.kyant0:backdrop` | 1.0.0 | Apache-2.0 |
| `org.jetbrains:annotations` | 23.0.0 | Apache-2.0 |
| `org.jetbrains.kotlin:kotlin-stdlib` | 2.2.21 | Apache-2.0 |
| `org.jetbrains.kotlin:kotlin-stdlib-common` | 2.2.21 | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.8.1 | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-bom` | 1.8.1 | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 1.8.1 | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm` | 1.8.1 | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-serialization-bom` | 1.8.1 | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-serialization-core` | 1.8.1 | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-serialization-core-jvm` | 1.8.1 | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.8.1 | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-serialization-json-jvm` | 1.8.1 | Apache-2.0 |
| `org.jspecify:jspecify` | 1.0.0 | Apache-2.0 |

</details>

## Licencia

[GPL-3.0-or-later](LICENSE).
