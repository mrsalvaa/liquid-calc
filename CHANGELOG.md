<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# Changelog

**English** | [Español](CHANGELOG.es.md)

## 0.4.0

### New
- Scientific keypad with a Casio layout: hold a key for its second function. New: fraction,
  n-th root, 10ˣ and x³. Digits stay as in basic mode;
  parentheses and % aren't repeated.
- Natural display: small raised exponents, stacked fractions and roots with a bar.
- Tap the calculation to move the cursor and edit anywhere; tap empty space to jump to the end.
- Thousands separator: 27,000.3 (depends on the language).
- Clearing everything makes the calculation float up and fade.

### Changed
- Holding "delete" clears everything in 0.4 s.
- Keys respond across their whole area, edges included.
- ( ) closes an open parenthesis after anything that can end one, not only a number.
- Glass is drawn the same as in Calendar, Contacts and Phone: refraction and blur on every
  surface and no background pause on touch.

## 0.3.2

### Changed
- Glass values aligned with the rest of the suite. Nothing changes on screen.

## 0.3.1

### Fixed
- Holding "delete" clears everything faster (0.7 s instead of 2 s).
- The background no longer changes while you touch the screen; it just stops moving.

## 0.3.0

### New
- Holding "delete" for 2 seconds clears everything, with a light "pop" on the display.

## 0.2.1

### New
- Basic and scientific calculator with exact decimal arithmetic (0.1 + 0.2 = 0.3) and its own expression parser.
- Trigonometry in degrees or radians, logarithms, powers, roots, factorial, π and e; calculator-style percent; memory and ANS.
- Local history that can be cleared or turned off; copy the result; physical keyboard with shortcuts.
- Liquid Glass and Solid styles with the same settings as the other Liquid apps, smooth at 120 Hz even with the moving background.
