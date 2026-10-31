# Diseño: qué reutiliza Liquid Calc de la suite

SPDX-License-Identifier: GPL-3.0-or-later

Este documento es la receta para las próximas apps de la suite (phone, contacts, messages): qué
se toma de [liquid-ui](liquid-ui/README.md), qué valores no se tocan y qué decide cada app. La
referencia es Liquid Calendar; liquid-ui es su `ui/glass` extraído, sin cambios de aspecto.

## Regla general

- La interfaz se arma con los componentes de liquid-ui (`GlassSurface`, `GlassCard`,
  `GlassButton`, `GlassChip`, `GlassSwitch`, `SettingRow`, `BackTitle`…). No se dibujan
  superficies propias con `background()` ni se cambian colores, radios o blur a mano.
- Lo que haga falta y no exista se agrega a liquid-ui (con un tag nuevo), no a la app.
- La app apunta su submódulo a un tag de liquid-ui, nunca a una rama.

## Colores (`dev.liquidui.theme`)

| Paleta | Fondo | Texto | Texto secundario | Acento |
|---|---|---|---|---|
| Oscuro (movimiento / énfasis) | `#030407` | blanco | blanco 60 % | `#7CC8FF` o el del set |
| Claro (movimiento / énfasis) | `#F1F3F8` | `#0E1222` | 58 % | `#1C7ED6` o el del set |
| Liso oscuro (por defecto) | `#0E0F12` | blanco | blanco 60 % | `#8AB4F8` |
| Liso claro | `#F2F3F5` | `#111318` | 58 % | `#1A73E8` |
| Sólido oscuro | `#0A0A0C`, niveles `#1A1A1E` `#26262B` `#313137` | blanco | blanco 55 % | `solidAccent` |
| Sólido claro | `#EEEEF2`, niveles blanco `#F1F1F5` `#E6E6EB` | `#111318` | 55 % | `solidAccent` |

- **Sets del fondo en movimiento** (`COLOR_SETS`): Celeste y rosa, Aurora, Atardecer y Océano,
  cada uno con 4 manchas y su acento claro/oscuro.
- **Colores de énfasis** (`ACCENT_HUES`): 10 colores × 3 tonos (claro, medio, oscuro). Siempre
  pasan por `muted()` (30 % hacia un gris de la misma luminosidad).
- **Derivados**: `accentFg` (el acento aclarado u oscurecido para íconos y textos), `onAccent`
  (texto legible sobre el acento) y `accentFill()` (relleno de lo destacado: acento entero en
  Sólido; 60 % en oscuro y 92 % en claro en Liquid). Los textos sobre cristal usan `content` y
  `contentSecondary`, que están pensados para dar contraste con cualquier fondo.
- Rojo `danger` `#FF7A8A` en oscuro y `#E5484D` en claro.

## Formas, blur y sombras

| Componente | Forma | Blur (`frost`) | Sombra |
|---|---|---|---|
| `GlassSurface` (por defecto) | 28 dp | 6 dp | 20 dp |
| `GlassCard` | 28 dp | 14 dp | 20 dp |
| `GlassButton`, `GlassIconButton` | píldora / círculo (48 dp) | 6 dp | 20 dp |
| `GlassChip` | píldora, 40 dp de alto | 4 dp | 6 dp |
| `GlassTextField` | 20 dp, sin refracción | 12 dp | 20 dp |
| Diálogos, menús, avisos | 34 dp | 16 dp | 20 dp |
| Barra inferior flotante | píldora, 70 dp | 4 dp | 20 dp |

- Liquid Glass refracta lo que tiene detrás (lente con aberración cromática, vibrancy y brillo
  en el borde). Con fondo liso o de énfasis, lo que está dentro del contenido se dibuja sin
  efectos (el aspecto es el mismo y la GPU trabaja menos); lo que flota encima sí refracta.
- **Sólido**: superficies opacas por niveles (una tarjeta dentro de otra va un tono más allá),
  borde de 1 dp, brillo leve arriba en oscuro y sombra solo por fuera en claro. `SolidShape`
  achica las esquinas a la mitad (6 a 16 dp) y las píldoras pasan a rectángulos de 10 dp; lo
  cuadrado y redondo sigue siendo círculo.

## Tipografía y espacios

- Fuente del sistema. Títulos de pantalla de 34 sp en negrita (`ScreenTitle`), 26 sp con el botón
  de volver (`BackTitle`), etiquetas de sección de 12 sp en mayúsculas con 1,2 sp de espaciado
  (`SectionLabel`), filas de ajustes de 16/13 sp. Los números grandes van en `FontWeight.Light`.
- Márgenes de pantalla de 16 dp, relleno de tarjeta de 20 × 16 dp, `TitleTopPadding` de 24 dp.

## Animación

- Gota al apretar en Liquid: spring 0,42 / 520 (se estira 4 % a lo ancho y se achica 5 % a lo
  alto). En Sólido: se achica 3 %, sin rebote (spring 900).
- Barra inferior: spring 0,62 / 420. Interruptor: 0,55 / 600. Paneles que suben: 0,8 / 500.
- Todo respeta `LocalReducedMotion` (Accesibilidad → Quitar animaciones).

## Rendimiento

La pantalla redibuja todo en cada cuadro, y con el fondo en movimiento cada superficie de cristal
refracta y desenfoca lo que tiene atrás. Con pocas superficies (Calendar) no se nota; con muchas a
la vez (el teclado de Calc, 45 teclas) no llega a 120 Hz. Por eso:

- **`LocalLiteGlass`**: en pantallas con muchas superficies, la app las pide livianas (el mismo
  dibujo que con fondo liso: tinte, brillo y borde; en oscuro, sin sombra). Calc lo usa en todo
  el contenido; lo que flota encima (el panel del historial) sigue refractando de verdad.
- **Filas de un panel que se desliza**: livianas y la lista en su propia capa
  (`Modifier.graphicsLayer {}`), así deslizar no obliga a volver a desenfocar el panel.
- **Fondo animado**: `GlassRoot` lo pausa mientras se toca la pantalla y la app lo pausa cuando
  lo tapa un panel (en Calc, con el historial abierto). Está en su propia textura: solo se
  repinta cuando se mueve.
- **Medir con la build `profile`** (optimizada como la release, instalable como `.debug`): la
  debug de Compose va lenta por sí sola. `adb shell dumpsys gfxinfo <paquete> framestats` dice
  si el tiempo se va en el hilo de la interfaz, en el de dibujo o en la GPU.

## Íconos

- **Interfaz**: Material Icons (Apache-2.0). Se escriben en su versión **Rounded**; en Sólido
  se dibuja la **Outlined**. Cada app registra al arrancar los pares que usa:
  `SolidIcons.register(mapOf(Icons.Rounded.X to Icons.Outlined.X, …))`. Los componentes de
  liquid-ui ya pasan los íconos por `styled()`; si una app dibuja un `Icon` propio, también.
- **Launcher**: cinco estilos en el mismo orden en toda la suite (`AppIcons.STYLES`):
  0 Liquid, 1 Material, 2 Samsung, 3 iPhone, 4 Motorola, más un ícono monocromo para los íconos
  temáticos. Son vectores originales (sin assets de nadie) con la paleta de cada estilo; los
  fondos son los de Calendar. Cada uno es un `activity-alias` (`.IconLiquid`, `.IconMaterial`,
  `.IconSamsung`, `.IconIphone`, `.IconMotorola`) y solo uno está activo.

## Ajustes compartidos (`dev.liquidui.settings`)

`files/settings.json`, plano, escrito con `JsonStore` (ignora campos desconocidos y escribe los
valores por defecto). La clase de ajustes de la app implementa `LiquidSettings`:

| Campo | Valores | Por defecto |
|---|---|---|
| `themeMode` | 0 sistema, 1 claro, 2 oscuro | 0 |
| `backgroundMode` | 0 en movimiento, 1 color de énfasis, 2 liso | 2 |
| `colorSet` | índice en `COLOR_SETS` (0–3) | 0 |
| `accentColor` | ARGB, uno de los 30 tonos de `ACCENT_HUES` | `0xFF0EA5E9` |
| `animatedBackground` | true / false | true |
| `uiStyle` | 0 Liquid Glass, 1 Sólido | 0 |
| `solidAccent` | ARGB, uno de los 30 tonos | `0xFF0EA5E9` |
| `appIcon` | 0–4, ver íconos de launcher | **propio de cada app** (Calc: 0) |

- Los valores por defecto están en `LiquidDefaults`; un test de la app
  (`SettingsFormatTest`) comprueba que el JSON tenga esas claves y valores y que lea el
  `settings.json` de otra app de la suite.
- La pantalla de Ajustes usa `AppearanceCard(settings, appIcons) { prefs -> … it.with(prefs) }`:
  estilo de interfaz, tema, fondo o color de énfasis según el estilo, e ícono de la app.
- Los campos propios de la app van en el mismo archivo (en Calc: `angleUnit`, `scientific`,
  `historyEnabled`).

## Textos

Inglés por defecto y español en `values-es`, con voseo rioplatense. Los textos de liquid-ui llevan
el prefijo `liquid_` para que ninguna app los pise sin querer.

## Privacidad y manifest

Lo mismo en cada app: `allowBackup="false"` y `data_extraction_rules` que excluyen todo,
`dependenciesInfo` desactivado, ningún permiso que la app no use y, con `tools:node="remove"`,
fuera los componentes que agregan las librerías y no se usan (`androidx.startup`, el receptor de
profileinstaller, el permiso interno de androidx.core). Después de compilar se revisa el manifest
mergeado del APK con `aapt2 dump xmltree`.

## Checklist para una app nueva

1. `git submodule add ../liquid-ui liquid-ui` y fijarlo al último tag; `include(":liquid-ui")`.
2. Copiar `gradle/libs.versions.toml` (las mismas versiones).
3. `AppSettings` implementa `LiquidSettings` con `LiquidDefaults`; elegir el `appIcon` por defecto.
4. `GlassTheme(settings.glassStyle)` y `GlassRoot` en la actividad; `SolidIcons.register` en
   la `Application`.
5. Cinco íconos de launcher en el orden de la suite, con sus `activity-alias`.
6. Ajustes con `AppearanceCard`.
7. Auditar dependencias y manifest como en el README de Liquid Calc.
