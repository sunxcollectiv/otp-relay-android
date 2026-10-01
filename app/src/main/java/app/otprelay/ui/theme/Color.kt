package app.otprelay.ui.theme

import androidx.compose.ui.graphics.Color

// A restrained slate/teal palette. Replace these once there's a real brand;
// nothing outside this file hardcodes a color.
val Primary = Color(0xFF00696E)
val OnPrimary = Color(0xFFFFFFFF)
val Secondary = Color(0xFF4A6365)
val OnSecondary = Color(0xFFFFFFFF)
val Background = Color(0xFFFAFDFC)
val OnBackground = Color(0xFF191C1C)

val PrimaryDark = Color(0xFF4CDADF)
val OnPrimaryDark = Color(0xFF00363A)
val SecondaryDark = Color(0xFFB1CBCD)
val OnSecondaryDark = Color(0xFF1C3438)
val BackgroundDark = Color(0xFF191C1C)
val OnBackgroundDark = Color(0xFFE0E3E2)

// Containers and variants. Without these Material falls back to its baseline
// purple, which is what the power button's "off" and "disabled" states and the
// requirement boxes were picking up.
val PrimaryContainer = Color(0xFF6FF7F6)
val OnPrimaryContainer = Color(0xFF002020)
val SurfaceVariant = Color(0xFFDAE5E4)
val OnSurfaceVariant = Color(0xFF3F4948)

val PrimaryContainerDark = Color(0xFF004F4F)
val OnPrimaryContainerDark = Color(0xFF6FF7F6)
val SurfaceVariantDark = Color(0xFF3F4948)
val OnSurfaceVariantDark = Color(0xFFBEC9C8)

// The power button's own colors. On/off is a meaning, not a brand accent, so
// these stay green even when wallpaper colors replace the rest of the palette,
// and they are deliberately not part of the Material color scheme.
val RelayActiveContainer = Color(0xFF2E7D32)
val RelayActiveContent = Color(0xFFFFFFFF)
val RelayInactiveContainer = Color(0xFFC5D3C6)
val RelayInactiveContent = Color(0xFF33413A)

val RelayActiveContainerDark = Color(0xFF6DBF73)
val RelayActiveContentDark = Color(0xFF0A2E0F)
val RelayInactiveContainerDark = Color(0xFF3E4B40)
val RelayInactiveContentDark = Color(0xFFB9C7BB)
