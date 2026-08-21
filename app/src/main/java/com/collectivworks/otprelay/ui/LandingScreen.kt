package com.collectivworks.otprelay.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.collectivworks.otprelay.R
import com.collectivworks.otprelay.ui.theme.OtpRelayTheme

/** Diameter of the power button. */
private val POWER_BUTTON_SIZE = 150.dp

/** Keeps the requirement boxes from stretching edge to edge on a tablet. */
private val CONTENT_MAX_WIDTH = 380.dp

/**
 * One precondition for syncing. The strings are resource ids so the list stays
 * declarative; hoist this into a ViewModel once the checks are real.
 */
data class SyncRequirement(
    val labelRes: Int,
    val detailRes: Int,
    val isMet: Boolean
)

/**
 * Placeholder requirement list. Every flag is false so the unmet state is what
 * shows by default — replace with state fed by the account layer, the battery
 * optimization check, and the SMS permission check.
 */
private fun defaultRequirements(): List<SyncRequirement> = listOf(
    SyncRequirement(R.string.requirement_account, R.string.requirement_account_detail, isMet = false),
    SyncRequirement(R.string.requirement_background, R.string.requirement_background_detail, isMet = false),
    SyncRequirement(R.string.requirement_sms, R.string.requirement_sms_detail, isMet = false)
)

/**
 * The main app surface: wordmark and settings form the header, a large power
 * toggle sits below it, and under that is the checklist of what has to be true
 * before the toggle will do anything.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    requirements: List<SyncRequirement> = defaultRequirements(),
    isSyncing: Boolean = false,
    onPowerClick: () -> Unit = {}
) {
    // The button only goes live once every box is checked; until then it reads as
    // a status light rather than a control.
    val canSync = requirements.all { it.isMet }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Wordmark() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            PowerButton(
                isOn = isSyncing,
                enabled = canSync,
                onClick = onPowerClick
            )

            Spacer(Modifier.height(40.dp))

            Text(
                text = stringResource(R.string.requirements_heading),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier.widthIn(max = CONTENT_MAX_WIDTH),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                requirements.forEach { RequirementBox(it) }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Text stand-in for the logo. Swap for a vector drawable when there is one. */
@Composable
private fun Wordmark(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.app_name),
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.primary
    )
}

/**
 * The big circle. Disabled until every requirement is met, which is why the
 * colors key off [enabled] before [isOn].
 */
@Composable
private fun PowerButton(
    isOn: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant
        isOn -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val content = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        isOn -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(POWER_BUTTON_SIZE),
        shape = CircleShape,
        color = container,
        contentColor = content
    ) {
        PowerGlyph(
            tint = content,
            modifier = Modifier
                .fillMaxSize()
                .padding(POWER_BUTTON_SIZE * 0.28f)
        )
    }
}

/**
 * The power symbol, drawn rather than imported: the project only depends on
 * `material-icons-core`, which does not carry `PowerSettingsNew`, and pulling in
 * the extended icon set for one glyph is not worth the size.
 */
@Composable
private fun PowerGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val strokeWidth = size.minDimension * 0.11f
        val radius = (size.minDimension - strokeWidth) / 2f
        val topLeft = Offset(
            x = (size.width - radius * 2) / 2f,
            y = (size.height - radius * 2) / 2f
        )

        // Angles start at 3 o'clock and sweep clockwise, so -60..240 leaves the
        // 60-degree gap centred on -90 — straight up, where the stem goes.
        drawArc(
            color = tint,
            startAngle = -60f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawLine(
            color = tint,
            start = Offset(size.width / 2f, size.height / 2f - radius * 1.15f),
            end = Offset(size.width / 2f, size.height / 2f - radius * 0.1f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

/** One checklist row: label, one line of why, and the state on the right. */
@Composable
private fun RequirementBox(
    requirement: SyncRequirement,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(requirement.labelRes),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(requirement.detailRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RequirementCheck(
                isMet = requirement.isMet,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

/** Filled tick when met, empty ring when not. */
@Composable
private fun RequirementCheck(isMet: Boolean, modifier: Modifier = Modifier) {
    val description = stringResource(
        if (isMet) R.string.requirement_met else R.string.requirement_unmet
    )
    if (isMet) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier.size(24.dp)
        )
    } else {
        val outline = MaterialTheme.colorScheme.outline
        Canvas(
            modifier
                .size(24.dp)
                .semantics { contentDescription = description }
        ) {
            val stroke = 2.dp.toPx()
            drawCircle(
                color = outline,
                radius = (size.minDimension - stroke) / 2f,
                style = Stroke(width = stroke)
            )
        }
    }
}

@Preview(showBackground = true, name = "Nothing done yet")
@Composable
private fun LandingScreenPreview() {
    OtpRelayTheme(dynamicColor = false) {
        LandingScreen(onSettingsClick = {})
    }
}

@Preview(showBackground = true, name = "Ready to sync")
@Composable
private fun LandingScreenReadyPreview() {
    OtpRelayTheme(dynamicColor = false) {
        LandingScreen(
            onSettingsClick = {},
            requirements = defaultRequirements().map { it.copy(isMet = true) }
        )
    }
}
