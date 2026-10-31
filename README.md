# Liquid Calc

**English** | [Español](README.es.md)

SPDX-License-Identifier: GPL-3.0-or-later

Installs on the phone as **Calculator** (*Calculadora* in Spanish).

A free calculator for Android with the same liquid-glass interface as
[Liquid Calendar](https://github.com/mrsalvaa/liquid-calendar),
[Liquid Clock](https://github.com/mrsalvaa/liquid-clock) and
[Liquid Gallery](https://github.com/mrsalvaa/liquid-gallery), plus the same plainer Solid style.
It's the first app of the suite built on [liquid-ui](liquid-ui/README.md), the shared components.

## Privacy

It requests no permissions and never connects to the internet: no network, ads, telemetry or
Google services. History and settings stay in the app's private folder, with no cloud backup, and
history can be cleared or turned off in Settings.

## Features

- **Basic and scientific**: sin, cos, tan and their inverses (degrees or radians), ln, log, eˣ,
  powers, square and cube roots, factorial, absolute value, 1/x, π and e.
- **Exact decimal arithmetic**: 0.1 + 0.2 gives 0.3. Everything is computed with 34 digits
  (decimal128) and 15 are shown, so rounding noise never appears.
- **Own parser**: expressions are read by a parser written for the calculator; they're never run
  as code.
- **Calculator-style percent**: 200 + 10% = 220, 200 − 10% = 180, 50 × 10% = 5.
- Parentheses (missing ones at the end close themselves), implicit multiplication (2π, 3(4)),
  memory (MC, MR, M+, M−), ANS and clear errors: division by zero, outside the domain, result too
  large or incomplete expression.
- Local **history**: tap a calculation to bring it back. Clear it all, or turn it off (which also
  clears it) in Settings.
- Copy the result with the button or by long-pressing the number.
- **Physical keyboard**: digits and `+ - * / ^ % ! ( )`; Enter or `=` calculates, Backspace
  deletes, Esc or Delete clears. Letters: `s` sin, `c` cos, `t` tan, `l` ln, `g` log,
  `r` √, `p` π, `e`, `a` ANS. Ctrl+C copies, Ctrl+V pastes, Ctrl+H opens the history and
  Ctrl+D switches degrees/radians.
- Portrait and landscape: in landscape the scientific keys sit next to the basic ones.
- Accessibility: every key has a TalkBack name ("divided by", "sine"), the result is announced,
  keys are at least 48 dp and text follows the theme's contrast.

## Building

Built on Linux (tested on CachyOS) for Android 12 or later. There's no desktop version.

```bash
git clone --recurse-submodules <liquid-calc-url>
cd liquid-calc
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk   # JDK 17
export ANDROID_HOME=/path/to/android-sdk          # with platform 35
./gradlew :core:test :app:testDebugUnitTest      # tests
./gradlew :app:assembleDebug                      # debug APK (dev.liquidcalc.debug)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The release (`./gradlew :app:assembleRelease`) is signed with the key in
`~/.android-keys/liquid-calc.properties` if it exists, otherwise with the debug key.

### liquid-ui (submodule)

`liquid-ui/` is a git submodule pinned to a tag (currently `v0.1.0`). Its URL in
`.gitmodules` is relative (`../liquid-ui`): **for now it resolves to a local folder**. Before
publishing on F-Droid, liquid-ui has to be pushed to a public URL; with both repos under the same
GitHub account, the relative URL already points to the right place.

## Layout

```
core/        Calculation engine in plain Kotlin (no Android, no dependencies), with tests
app/         The app: screen, keys, history, settings, physical keyboard
liquid-ui/   Shared components and settings of the suite (submodule)
DESIGN.md    What is reused from the suite and how (in Spanish)
```

## Dependencies

The engine (`core`) has no dependencies: it uses `java.math.BigDecimal` and its own series.
The app uses Jetpack Compose, Material Icons, kotlinx-serialization and
[Backdrop](https://github.com/Kyant0/AndroidLiquidGlass) (the glass effect). All of them are
Apache-2.0, compatible with GPL-3.0-or-later. Tests also use kotlin-test and JUnit 4 (EPL-1.0),
which don't ship in the APK.

### Audit

- **Full tree** (`./gradlew :app:dependencies --configuration releaseRuntimeClasspath`): 95
  artifacts, all Apache-2.0 according to their POM. The list is below.
- **No Google Play Services, Firebase or datatransport**: not in the tree and not in the release
  APK's code (`com.google.android.gms`, `firebase`, `datatransport`: 0 matches).
- **No network**: the release APK has no references to `java.net.URL`, `HttpURLConnection`,
  sockets or OkHttp. The only `android.net` class is `Uri`, which Compose uses for the
  clipboard.
- **Final manifest** (merged, debug and release): no permissions, providers, receivers or
  services; only the activity and the 5 icon aliases. `tools:node="remove"` drops androidx.core's
  internal permission, `androidx.startup` (it would start emoji2, which can ask Google Play
  Services for fonts, lifecycle-process and profileinstaller) and profileinstaller's receiver.
- **Backdrop**: 57 classes, no network or permissions; it only depends on Compose and the Kotlin
  standard library.
- **F-Droid**: `dependenciesInfo` is off (no Google-signed block) and the APK has no ART profiles
  (`assets/dexopt`), which can break reproducibility. Compose brings an AndroidX native library,
  `libandroidx.graphics.path.so` (Apache-2.0, source in AOSP), from Maven; the repo itself has
  no binaries.

<details>
<summary>The 95 release dependencies</summary>

| Dependency | Version | License |
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

## License

[GPL-3.0-or-later](LICENSE).
