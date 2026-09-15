package app.nodenote.worldbuilder.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.nodenote.worldbuilder.R

val Cobalt = Color(0xFF245CE8)
val AtlasDark =
    darkColorScheme(
        primary = Color(0xFFA9BFFF),
        onPrimary = Color(0xFF00246C),
        primaryContainer = Cobalt,
        onPrimaryContainer = Color.White,
        secondary = Color(0xFFA5DCC4),
        onSecondary = Color(0xFF06372A),
        secondaryContainer = Color(0xFF123E34),
        onSecondaryContainer = Color(0xFFD2F4E3),
        tertiary = Color(0xFFE3C990),
        onTertiary = Color(0xFF3D2E0B),
        background = Color(0xFF080D10),
        onBackground = Color(0xFFE7EEE9),
        surface = Color(0xFF0D1518),
        onSurface = Color(0xFFE7EEE9),
        surfaceVariant = Color(0xFF263B3D),
        onSurfaceVariant = Color(0xFFB9CBC7),
        surfaceContainer = Color(0xFF142124),
        surfaceContainerLow = Color(0xFF101B1E),
        surfaceContainerHigh = Color(0xFF1C2D30),
        surfaceContainerHighest = Color(0xFF263B3D),
        outline = Color(0xFF829894),
        outlineVariant = Color(0xFF304845),
    )
val AtlasLight =
    lightColorScheme(
        primary = Color(0xFF174BC5),
        onPrimary = Color.White,
        primaryContainer = Cobalt,
        onPrimaryContainer = Color.White,
        secondary = Color(0xFF235D47),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD5EADC),
        onSecondaryContainer = Color(0xFF123E34),
        background = Color(0xFFF3F5EF),
        onBackground = Color(0xFF132621),
        surface = Color(0xFFF9FBF5),
        onSurface = Color(0xFF132621),
        surfaceContainer = Color(0xFFE8EEE5),
        surfaceContainerLow = Color(0xFFF0F3EC),
        surfaceContainerHigh = Color(0xFFE1E9DF),
        surfaceContainerHighest = Color(0xFFD9E3D8),
        surfaceVariant = Color(0xFFDCE6DB),
        onSurfaceVariant = Color(0xFF435A50),
        outline = Color(0xFF687F74),
        outlineVariant = Color(0xFFC0CEC2),
    )
val AtlasTypography =
    Typography(
        headlineLarge =
            TextStyle(fontFamily = FontFamily.Serif, fontSize = 36.sp, lineHeight = 40.sp),
        headlineMedium =
            TextStyle(fontFamily = FontFamily.Serif, fontSize = 32.sp, lineHeight = 38.sp),
        headlineSmall =
            TextStyle(fontFamily = FontFamily.Serif, fontSize = 28.sp, lineHeight = 34.sp),
        titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontSize = 23.sp, lineHeight = 29.sp),
        titleMedium =
            TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 25.sp),
        bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 27.sp),
        bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 23.sp),
        labelLarge =
            TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        labelSmall =
            TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = .6.sp,
            ),
    )

/** Original vector marks: decoration has no separate accessibility node. */
@Composable
fun AtlasIcon(
    name: String,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
) {
    Canvas(modifier.size(24.dp)) {
        val s = size.width / 24f
        fun line(x: Float, y: Float, a: Float, b: Float) =
            drawLine(color, Offset(x * s, y * s), Offset(a * s, b * s), 1.65f * s, StrokeCap.Round)
        when (name) {
            "Library" -> {
                for (i in 0..2) {
                    val x = 4f + i * 6
                    line(x, 4f, x, 20f)
                    line(x, 4f, x + 3, 4f)
                    line(x + 3, 4f, x + 3, 20f)
                    line(x, 20f, x + 3, 20f)
                }
            }
            "Boards" -> {
                line(6f, 6f, 18f, 7f)
                line(6f, 6f, 9f, 18f)
                line(18f, 7f, 9f, 18f)
                listOf(Offset(6f, 6f), Offset(18f, 7f), Offset(9f, 18f)).forEach {
                    drawCircle(color, 3f * s, it * s, style = Stroke(1.6f * s))
                }
            }
            "Timeline" -> {
                line(4f, 12f, 20f, 12f)
                for (x in listOf(5f, 12f, 19f)) {
                    line(x, 7f, x, 17f)
                }
            }
            "Search" -> {
                drawCircle(color, 6.5f * s, Offset(10f * s, 10f * s), style = Stroke(1.65f * s))
                line(15f, 15f, 21f, 21f)
            }
            "Add" -> {
                line(5f, 12f, 19f, 12f)
                line(12f, 5f, 12f, 19f)
            }
            "Menu" -> {
                line(4f, 6f, 20f, 6f)
                line(9f, 12f, 20f, 12f)
                line(4f, 18f, 20f, 18f)
            }
            else -> {
                line(5f, 9f, 12f, 16f)
                line(12f, 16f, 19f, 9f)
            }
        }
    }
}

@Composable
fun AtlasNavigation(selected: String, onSelect: (String) -> Unit) {
    val largeText = androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f
    val names = listOf("Library", "Boards", "Timeline", "Search")
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.fillMaxWidth().selectableGroup().navigationBarsPadding().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            names.chunked(if (largeText) 2 else 4).forEach { group ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    group.forEach { name ->
                        val active = selected == name
                        val ink =
                            if (active) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        val label =
                            stringResource(
                                when (name) {
                                    "Library" -> R.string.library
                                    "Boards" -> R.string.boards
                                    "Timeline" -> R.string.timeline
                                    else -> R.string.search
                                }
                            )
                        val target =
                            Modifier.weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (active) MaterialTheme.colorScheme.secondaryContainer
                                    else Color.Transparent
                                )
                                .selectable(active, role = Role.Tab, onClick = { onSelect(name) })
                        if (largeText)
                            Row(
                                target
                                    .heightIn(min = 56.dp)
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                AtlasIcon(name, Modifier.size(20.dp), color = ink)
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ink,
                                    maxLines = 1,
                                )
                            }
                        else
                            Column(
                                target
                                    .heightIn(min = 64.dp)
                                    .padding(horizontal = 2.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                AtlasIcon(name, color = ink)
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ink,
                                    maxLines = 1,
                                )
                            }
                    }
                }
            }
        }
    }
}

@Composable
fun AtlasHero(title: String, subtitle: String, marker: String) {
    Surface(
        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(Modifier.padding(22.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(7.dp)
                        .background(
                            MaterialTheme.colorScheme.onSecondaryContainer,
                            RoundedCornerShape(2.dp),
                        )
                )
                Text(marker.uppercase(), style = MaterialTheme.typography.labelSmall)
            }
            Text(
                title,
                Modifier.padding(top = 14.dp, bottom = 8.dp),
                style =
                    if (androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f)
                        MaterialTheme.typography.headlineSmall
                    else MaterialTheme.typography.headlineLarge,
            )
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun AtlasTopBar(title: @Composable () -> Unit, actions: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.fillMaxWidth()
                .statusBarsPadding()
                .heightIn(min = 64.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) { title() }
            actions()
        }
    }
}
