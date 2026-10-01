package app.otprelay.ui

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
import app.otprelay.R
import app.otprelay.ui.theme.OtpRelayTheme
import app.otprelay.ui.theme.relayColors

/** Diameter of the power button. */
private val POWER_BUTTON_SIZE = 150.dp

/** Keeps the requirement boxes from stretching edge to edge on a tablet. */
private val CONTENT_MAX_WIDTH = 380.dp

/**
 * One precondition for syncing.
 *
 * [onManage] is what the box does when tapped, in either direction: a met
 * requirement stays tappable so it can be undone. A null [onManage] means the
 * check is still scaffolding — the box renders but does not respond, rather
 * than pretending to be a control that goes nowhere.
 */
data class SyncRequirement(
    val labelRes: Int,
    val detailRes: Int,
    val isMet: Boolean,
    val onManage: (() -> Unit)? = null
)

/** The live requirement list, each row backed by a real check. */
@Composable
private fun rememberSyncRequirements(
    isSignedIn: Boolean,
    onAccountClick: () -> Unit
): List<SyncRequirement> {
    val battery = rememberBatteryOptimizationState()
    val sms = rememberSmsPermissionState()
    return listOf(
        SyncRequirement(
            labelRes = R.string.requirement_account,
            detailRes = R.string.requirement_account_detail,
            isMet = isSignedIn,
            onManage = onAccountClick
        ),
        SyncRequirement(
            labelRes = R.string.requirement_background,
            detailRes = R.string.requirement_background_detail,
            isMet = battery.isUnrestricted,
            onManage = battery.manage
        ),
        SyncRequirement(
            labelRes = R.string.requirement_sms,
            detailRes = R.string.requirement_sms_detail,
            isMet = sms.isGranted,
            onManage = sms.manage
        )
    )
}

/**
 * Static stand-in for [rememberSyncRequirements] so previews render without
 * touching the permission system, which has no Activity behind it in a preview.
 */
private fun previewRequirements(isMet: Boolean = false): List<SyncRequirement> = listOf(
    SyncRequirement(R.string.requirement_account, R.string.requirement_account_detail, isMet, onManage = {}),
    SyncRequirement(R.string.requirement_background, R.string.requirement_background_detail, isMet, onManage = {}),
    SyncRequirement(R.string.requirement_sms, R.string.requirement_sms_detail, isMet, onManage = {})
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
    isSignedIn: Boolean = false,
    onAccountClick: () -> Unit = {},
    requirements: List<SyncRequirement> = rememberSyncRequirements(isSignedIn, onAccountClick),
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

            // What the button will do next. While a requirement is unmet the tap
            // does nothing, so the heading points at the checklist instead.
            Text(
                text = stringResource(
                    when {
                        !canSync -> R.string.requirements_heading
                        isSyncing -> R.string.syncing_on_heading
                        else -> R.string.syncing_off_heading
                    }
                ),
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
 * The big circle, in one of three states:
 * - disabled (neutral grey): a requirement is unmet, so the tap does nothing;
 * - off (desaturated green): ready, not relaying;
 * - on (solid green): relaying incoming codes.
 *
 * The colors key off [enabled] before [isOn], so a relay left on reads as
 * disabled while a requirement is unmet, which is also when it cannot work.
 */
@Composable
private fun PowerButton(
    isOn: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val relay = relayColors()
    val container = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant
        isOn -> relay.activeContainer
        else -> relay.inactiveContainer
    }
    val content = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        isOn -> relay.activeContent
        else -> relay.inactiveContent
    }

    val description = stringResource(if (isOn) R.string.stop_syncing else R.string.start_syncing)

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(POWER_BUTTON_SIZE)
            .semantics { contentDescription = description },
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

/**
 * One checklist row: label, one line of why, and the state on the right.
 *
 * A wired requirement is tappable whichever way it currently sits: unmet to go
 * and satisfy it, met to go and undo it. Only an unwired one is inert, so the
 * ripple always means something is about to happen.
 */
@Composable
private fun RequirementBox(
    requirement: SyncRequirement,
    modifier: Modifier = Modifier
) {
    val onManage = requirement.onManage
    val boxModifier = modifier.fillMaxWidth()
    val shape = MaterialTheme.shapes.large
    val color = MaterialTheme.colorScheme.surfaceVariant

    // Two Surface overloads rather than a Modifier.clickable, so the ripple is
    // clipped to the rounded shape instead of bleeding into its bounding box.
    if (onManage != null) {
        Surface(onClick = onManage, modifier = boxModifier, shape = shape, color = color) {
            RequirementBoxContent(requirement)
        }
    } else {
        Surface(modifier = boxModifier, shape = shape, color = color) {
            RequirementBoxContent(requirement)
        }
    }
}

@Composable
private fun RequirementBoxContent(requirement: SyncRequirement) {
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
        LandingScreen(
            onSettingsClick = {},
            requirements = previewRequirements(isMet = false)
        )
    }
}

@Preview(showBackground = true, name = "Ready, relay off")
@Composable
private fun LandingScreenOffPreview() {
    OtpRelayTheme(dynamicColor = false) {
        LandingScreen(
            onSettingsClick = {},
            requirements = previewRequirements(isMet = true),
            isSyncing = false
        )
    }
}

@Preview(showBackground = true, name = "Relaying")
@Composable
private fun LandingScreenReadyPreview() {
    OtpRelayTheme(dynamicColor = false) {
        LandingScreen(
            onSettingsClick = {},
            requirements = previewRequirements(isMet = true),
            isSyncing = true
        )
    }
}
