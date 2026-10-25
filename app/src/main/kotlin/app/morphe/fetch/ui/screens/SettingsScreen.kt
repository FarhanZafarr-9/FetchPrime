package app.morphe.fetch

import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.lifecycleScope
import app.morphe.fetch.aurora.MicroGAccountTokenProvider
import app.morphe.fetch.aurora.GPlayHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.height
import app.morphe.fetch.BuildConfig
import app.morphe.fetch.MarkdownChangelog
import app.morphe.fetch.updater.UpdateInfo
import app.morphe.fetch.updater.UpdateState
import java.io.File
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Locale

private enum class SettingsSection(
    val title: String,
    val icon: ImageVector
) {
    PREFERENCES("Preferences", Icons.Outlined.Tune),
    SOURCES("Download Sources", Icons.Outlined.Dns),
    HISTORY("Download History", Icons.Outlined.History),
    UPDATES("Updates & About", Icons.Outlined.Download)
}

@Composable
internal fun DownloadSourcesContent(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val enabledSources = remember(settings.disabledSources) {
        DownloadSource.entries.filter { it !in settings.disabledSources }
    }
    var showAuroraAccountDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sources list separated with connected card rounding
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            DownloadSource.entries.forEachIndexed { index, source ->
                val isEnabled = source !in settings.disabledSources
                val isPreferred = settings.preferredSource == source
                val isSolo = isEnabled && settings.disabledSources.size == DownloadSource.entries.size - 1 && source !in settings.disabledSources
                val position = connectedPosition(index, DownloadSource.entries.size)
                var expanded by rememberSaveable(source.name) { mutableStateOf(false) }

                val subtitleText = when {
                    !isEnabled -> "Disabled"
                    isPreferred && isSolo -> "Default source · Solo mode"
                    isPreferred -> "Default source"
                    isSolo -> "Solo mode active"
                    else -> "Active repository"
                }

                SurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { expanded = !expanded },
                    shape = connectedCardShape(position),
                    color = if (isPreferred) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    } else {
                        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Compact Main Row: Avatar + (Title & Subtitle Column) + Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SourceAvatar(source = source, size = 28.dp)
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = source.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isEnabled) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when {
                                        !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        isPreferred || isSolo -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { checked ->
                                    if (!checked) {
                                        val enabledCount = DownloadSource.entries.count { it !in settings.disabledSources }
                                        if (enabledCount > 1) {
                                            onSettingsChange(
                                                settings.copy(
                                                    disabledSources = settings.disabledSources + source,
                                                    preferredSource = if (isPreferred) null else settings.preferredSource
                                                )
                                            )
                                        }
                                    } else {
                                        onSettingsChange(
                                            settings.copy(
                                                disabledSources = settings.disabledSources - source
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.scale(0.80f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }

                        // Expanded Options (Connected rows with matching connectivity radius)
                        AnimatedVisibility(visible = expanded && isEnabled) {
                            val optionCount = if (source == DownloadSource.AURORA) 3 else 2
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                // Default Source Row
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = connectedCardShape(connectedPosition(0, optionCount)),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    onClick = {
                                        onSettingsChange(settings.copy(preferredSource = if (isPreferred) null else source))
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Default Source",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isPreferred) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Prioritize this source when searching for APKs",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = isPreferred,
                                            onCheckedChange = { checked ->
                                                onSettingsChange(settings.copy(preferredSource = if (checked) source else null))
                                            },
                                            modifier = Modifier.scale(0.80f),
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.surface,
                                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                    }
                                }

                                // Solo Source Row
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = connectedCardShape(connectedPosition(1, optionCount)),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    onClick = {
                                        if (!isSolo) {
                                            val allOthers = DownloadSource.entries.filter { it != source }.toSet()
                                            onSettingsChange(
                                                settings.copy(
                                                    disabledSources = allOthers,
                                                    preferredSource = source
                                                )
                                            )
                                        } else {
                                            onSettingsChange(settings.copy(disabledSources = emptySet()))
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Solo Mode",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSolo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Temporarily disable all other download sources",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = isSolo,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    val allOthers = DownloadSource.entries.filter { it != source }.toSet()
                                                    onSettingsChange(
                                                        settings.copy(
                                                            disabledSources = allOthers,
                                                            preferredSource = source
                                                        )
                                                    )
                                                } else {
                                                    onSettingsChange(settings.copy(disabledSources = emptySet()))
                                                }
                                            },
                                            modifier = Modifier.scale(0.80f),
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.surface,
                                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                    }
                                }

                                // Aurora Account Details & Config
                                if (source == DownloadSource.AURORA) {
                                    val hasAccount = !settings.auroraEmail.isNullOrBlank() && !settings.auroraAuthToken.isNullOrBlank()
                                    val accountText = if (hasAccount) settings.auroraEmail else "Anonymous session"
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = connectedCardShape(connectedPosition(2, optionCount)),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        onClick = { showAuroraAccountDialog = true }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Google Account",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = accountText,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (hasAccount) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                onClick = { showAuroraAccountDialog = true }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Tune,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Text(
                                                        text = "Configure",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAuroraAccountDialog) {
            AuroraAccountDialog(
                settings = settings,
                onSettingsChange = onSettingsChange,
                onDismiss = { showAuroraAccountDialog = false }
            )
        }

        // Quick Action Row: Modern Elevated Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val allEnabled = settings.disabledSources.isEmpty()
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (allEnabled) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                },
                onClick = {
                    onSettingsChange(settings.copy(disabledSources = emptySet()))
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (allEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Enable All",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (allEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            val isAuto = settings.preferredSource == null
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isAuto) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                },
                onClick = {
                    onSettingsChange(settings.copy(preferredSource = null))
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isAuto) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isAuto) "Auto Mode" else "Reset to Auto",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isAuto) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Text(
            text = "At least one source must remain active.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun AuroraAccountDialog(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hostActivity = remember(context) { context.findActivity() }
    val componentActivity = hostActivity as? ComponentActivity
    val lifecycleScope = componentActivity?.lifecycleScope ?: rememberCoroutineScope()

    var isAuthenticating by remember { mutableStateOf(false) }

    val tokenProvider = remember {
        MicroGAccountTokenProvider(
            context = context,
            httpClient = GPlayHttpClient(MorpheHttpClient.gplayClient)
        )
    }
    var availableAccounts by remember(tokenProvider) {
        mutableStateOf(tokenProvider.getAvailableAccounts())
    }

    val accountChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val chosenAccountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        val chosenAccountType = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE)
            ?: MicroGAccountTokenProvider.REVANCED_ACCOUNT_TYPE
        if (!chosenAccountName.isNullOrBlank()) {
            isAuthenticating = true
            lifecycleScope.launch {
                try {
                    val targetActivity = hostActivity ?: context.findActivity()
                    val token = withContext(Dispatchers.IO) {
                        tokenProvider.fetchTokenForEmail(chosenAccountName, targetActivity, chosenAccountType)
                    }
                    onSettingsChange(
                        settings.copy(
                            auroraEmail = chosenAccountName,
                            auroraAuthToken = token,
                            auroraTokenType = "AUTH"
                        )
                    )
                    availableAccounts = tokenProvider.getAvailableAccounts()
                    Toast.makeText(context, "MicroG-RE account connected: $chosenAccountName", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.util.Log.e("AuroraAccount", "Auth error: ${e.message}", e)
                    Toast.makeText(context, "Auth error: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isAuthenticating = false
                }
            }
        }
    }

    val isConfigured = !settings.auroraEmail.isNullOrBlank() && !settings.auroraAuthToken.isNullOrBlank()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 580.dp),
            shape = RoundedCornerShape(MorpheDefaults.SectionCornerRadius),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: Title + Status Badge + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Google Play Account",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        MorpheStatusBadge(
                            text = if (isConfigured) "Logged In" else "Anonymous",
                            tone = if (isConfigured) SemanticTone.Primary else SemanticTone.Neutral
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isConfigured) {
                        // Active Account Connected Card
                        SurfaceCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = connectedCardShape(ConnectedPosition.Single),
                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal = MorpheDefaults.ContentPadding,
                                        vertical = MorpheDefaults.ContentPaddingSmall + 4.dp
                                    ),
                                horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ContentPadding),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ThemedIcon(
                                    icon = Icons.Outlined.AccountCircle,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = settings.auroraEmail.orEmpty(),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Connected via MicroG-RE",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                MorpheStatusBadge(
                                    text = "Active",
                                    tone = SemanticTone.Primary
                                )
                            }
                        }

                        // Action Buttons: Refresh & Sign Out pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                onClick = {
                                    isAuthenticating = true
                                    lifecycleScope.launch {
                                        try {
                                            val freshAuth = withContext(Dispatchers.IO) {
                                                tokenProvider.refreshAccountToken(hostActivity ?: context.findActivity())
                                            }
                                            onSettingsChange(
                                                settings.copy(
                                                    auroraAuthToken = freshAuth.authToken,
                                                    auroraTokenType = "AUTH"
                                                )
                                            )
                                            Toast.makeText(context, "Session refreshed successfully", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Refresh failed: ${e.message}", Toast.LENGTH_LONG).show()
                                        } finally {
                                            isAuthenticating = false
                                        }
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Refresh Token",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                                onClick = {
                                    tokenProvider.invalidateToken(
                                        email = settings.auroraEmail,
                                        token = settings.auroraAuthToken
                                    )
                                    onSettingsChange(
                                        settings.copy(
                                            auroraEmail = null,
                                            auroraAuthToken = null
                                        )
                                    )
                                    Toast.makeText(context, "Switched to Anonymous Google Play session", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Sign Out",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Sign in using your device's MicroG-RE account for higher rate limits, or stay anonymous.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (isAuthenticating) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Authenticating with Google Play via MicroG...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            if (availableAccounts.isNotEmpty()) {
                                Text(
                                    text = "Detected MicroG-RE accounts (${availableAccounts.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    availableAccounts.forEachIndexed { index, acc ->
                                        val position = connectedPosition(index, availableAccounts.size)
                                        SurfaceCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = connectedCardShape(position),
                                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                                            onClick = {
                                                if (!isAuthenticating) {
                                                    isAuthenticating = true
                                                    lifecycleScope.launch {
                                                        try {
                                                            val targetActivity = hostActivity ?: context.findActivity()
                                                            val token = withContext(Dispatchers.IO) {
                                                                tokenProvider.fetchTokenForEmail(acc.name, targetActivity, acc.type)
                                                            }
                                                            onSettingsChange(
                                                                settings.copy(
                                                                    auroraEmail = acc.name,
                                                                    auroraAuthToken = token,
                                                                    auroraTokenType = "AUTH"
                                                                )
                                                            )
                                                            availableAccounts = tokenProvider.getAvailableAccounts()
                                                            Toast.makeText(context, "Connected: ${acc.name}", Toast.LENGTH_SHORT).show()
                                                        } catch (e: Exception) {
                                                            android.util.Log.e("AuroraAccount", "Auth error: ${e.message}", e)
                                                            Toast.makeText(context, "Auth error: ${e.message}", Toast.LENGTH_LONG).show()
                                                        } finally {
                                                            isAuthenticating = false
                                                        }
                                                    }
                                                }
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        horizontal = MorpheDefaults.ContentPadding,
                                                        vertical = MorpheDefaults.ContentPaddingSmall + 4.dp
                                                    ),
                                                horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ContentPadding),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                ThemedIcon(
                                                    icon = Icons.Outlined.AccountCircle,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Text(
                                                        text = acc.name,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "MicroG-RE Account",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "Connect",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No saved MicroG-RE accounts detected. Tap below to select or add an account from your device.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            // Choose / Add account button with modern pill style
                            Surface(
                                onClick = {
                                    val intent = AccountManager.newChooseAccountIntent(
                                        null,
                                        null,
                                        tokenProvider.getSupportedAccountTypes(),
                                        null,
                                        null,
                                        null,
                                        null
                                    )
                                    accountChooserLauncher.launch(intent)
                                },
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (availableAccounts.isEmpty()) "Select MicroG-RE Account" else "Other MicroG Account",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun HelperSettingsScreen(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit,
    historyEntries: List<DownloadHistoryEntry>,
    onOpenHistoryEntry: (DownloadHistoryEntry) -> Unit,
    onShareHistoryEntry: (DownloadHistoryEntry) -> Unit,
    onClearHistory: () -> Unit,
    updateState: UpdateState = UpdateState.Idle,
    onCheckForUpdates: () -> Unit = {},
    onDownloadUpdate: (UpdateInfo) -> Unit = {},
    onInstallUpdate: (File) -> Unit = {},
    onDismissUpdate: () -> Unit = {},
    onBack: () -> Unit
) {
    val isExpanded = isExpandedScreen()
    var selectedSection by rememberSaveable { mutableStateOf(SettingsSection.PREFERENCES) }
    val swipeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }
    val context = LocalContext.current
    var cacheBytes by remember(context) { mutableStateOf(context.temporaryDownloadsSize()) }
    var downloadsBytes by remember(context) { mutableStateOf(context.downloadsCopySize()) }
    var locationDialog by remember { mutableStateOf(false) }
    var policyDialog by remember { mutableStateOf(false) }
    var sourcesDialog by remember { mutableStateOf(false) }
    var historyDialog by remember { mutableStateOf(false) }
    var themeDialog by remember { mutableStateOf(false) }
    val locations = DownloadLocation.entries
    val policies = NetworkPolicy.entries
    val openUrl: (String) -> Unit = remember(context) {
        { url ->
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
        }
    }
    val enabledSources = remember(settings.disabledSources) {
        DownloadSource.entries.filter { it !in settings.disabledSources }
    }

    if (!isExpanded && sourcesDialog) {
        val maxDialogHeight = (LocalConfiguration.current.screenHeightDp * 0.88f).dp
        Dialog(
            onDismissRequest = { sourcesDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .widthIn(max = 580.dp)
                    .heightIn(max = maxDialogHeight),
                shape = RoundedCornerShape(MorpheDefaults.SectionCornerRadius),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Dns,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            Text("Download Sources", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${enabledSources.size} of ${DownloadSource.entries.size} active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { sourcesDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Content
                    DownloadSourcesContent(
                        settings = settings,
                        onSettingsChange = onSettingsChange
                    )
                }
            }
        }
    }
    if (!isExpanded && historyDialog) {
        val maxDialogHeight = (LocalConfiguration.current.screenHeightDp * 0.88f).dp
        Dialog(
            onDismissRequest = { historyDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .widthIn(max = 580.dp)
                    .heightIn(max = maxDialogHeight),
                shape = RoundedCornerShape(MorpheDefaults.SectionCornerRadius),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Download History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { historyDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        DownloadHistorySection(
                            entries = historyEntries,
                            onClear = onClearHistory,
                            onOpen = onOpenHistoryEntry,
                            onShare = onShareHistoryEntry
                        )
                    }
                }
            }
        }
    }

    if (themeDialog) {
        // Order must match ThemeMode.entries: SYSTEM(0), DARK(1), LIGHT(2)
        val themeChoices = remember {
            listOf(
                SettingsChoice(
                    icon = ThemeMode.SYSTEM.icon(),
                    title = "System default",
                    description = "Follow device system settings"
                ),
                SettingsChoice(
                    icon = ThemeMode.DARK.icon(),
                    title = "Dark",
                    description = "Always use dark theme"
                ),
                SettingsChoice(
                    icon = ThemeMode.LIGHT.icon(),
                    title = "Light",
                    description = "Always use light theme"
                )
            )
        }
        SettingsChoiceDialog(
            title = "Choose Theme",
            choices = themeChoices,
            selectedIndex = ThemeMode.entries.indexOf(settings.themeMode),
            onSelect = { index ->
                onSettingsChange(settings.copy(themeMode = ThemeMode.entries[index]))
                themeDialog = false
            },
            onDismiss = { themeDialog = false }
        )
    }

    if (locationDialog) {
        SettingsChoiceDialog(
            title = "Save downloads",
            choices = locations.map { SettingsChoice(it.icon(), it.title, it.description) },
            selectedIndex = locations.indexOf(settings.downloadLocation),
            onSelect = { index ->
                onSettingsChange(settings.copy(downloadLocation = locations[index]))
                locationDialog = false
            },
            onDismiss = { locationDialog = false }
        )
    }

    if (policyDialog) {
        SettingsChoiceDialog(
            title = "Connection",
            choices = policies.map { SettingsChoice(it.icon(), it.title, it.description) },
            selectedIndex = policies.indexOf(settings.networkPolicy),
            onSelect = { index ->
                onSettingsChange(settings.copy(networkPolicy = policies[index]))
                policyDialog = false
            },
            onDismiss = { policyDialog = false }
        )
    }

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        if (updateState is UpdateState.Idle) {
            onCheckForUpdates()
        }
    }

    if (isExpanded) {
        // Adaptive Two-Pane Master-Detail Tablet Layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(MorpheDefaults.ContentPadding),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Master Navigation Pane
            Column(
                modifier = Modifier
                    .width(MorpheDefaults.MasterPaneWidth)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
            ) {
                // Header: Title, version and Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Morphe Fetch v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        onClick = onBack,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Navigation Category Items
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsSection.entries.forEach { section ->
                        val isSelected = selectedSection == section
                        SurfaceCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 12.dp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            },
                            borderWidth = if (isSelected) 1.dp else 0.dp,
                            borderColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                            onClick = { selectedSection = section }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = section.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = section.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    val subtitle = when (section) {
                                        SettingsSection.PREFERENCES -> "Theme, cache & network"
                                        SettingsSection.SOURCES -> "${enabledSources.size} of ${DownloadSource.entries.size} active"
                                        SettingsSection.HISTORY -> "${historyEntries.size} APKs"
                                        SettingsSection.UPDATES -> when (updateState) {
                                            is UpdateState.Available -> "Update available!"
                                            is UpdateState.Checking -> "Checking..."
                                            else -> "v${BuildConfig.VERSION_NAME}"
                                        }
                                    }
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (section == SettingsSection.UPDATES && updateState is UpdateState.Available) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Trailing badge
                                when (section) {
                                    SettingsSection.SOURCES -> {
                                        MorpheStatusBadge(
                                            text = settings.preferredSource?.label ?: "Auto",
                                            tone = SemanticTone.Primary
                                        )
                                    }
                                    SettingsSection.UPDATES -> {
                                        if (updateState is UpdateState.Available) {
                                            MorpheStatusBadge(
                                                text = "New",
                                                tone = SemanticTone.Primary
                                            )
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    }
                }

                // Bottom Quick Cache Cleanup Tile
                CacheMetricTile(
                    modifier = Modifier.fillMaxWidth(),
                    cacheBytes = cacheBytes + downloadsBytes,
                    onClean = {
                        context.clearTemporaryDownloads()
                        context.clearDownloadsCopies()
                        cacheBytes = 0L
                        downloadsBytes = 0L
                        Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Vertical Divider
            MorpheVerticalDivider(
                modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp)
            )

            // Right Detail Pane
            SurfaceCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                cornerRadius = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(MorpheDefaults.ContentPaddingMedium),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedSection) {
                        SettingsSection.PREFERENCES -> {
                            SettingsPreferencesDetail(
                                settings = settings,
                                onSettingsChange = onSettingsChange,
                                cacheBytes = cacheBytes + downloadsBytes,
                                onCleanCache = {
                                    context.clearTemporaryDownloads()
                                    context.clearDownloadsCopies()
                                    cacheBytes = 0L
                                    downloadsBytes = 0L
                                },
                                onOpenLocationDialog = { locationDialog = true },
                                onOpenPolicyDialog = { policyDialog = true },
                                onOpenThemeDialog = { themeDialog = true }
                            )
                        }
                        SettingsSection.SOURCES -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Download Sources",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Manage repositories and mirrors queried during APK searches. Star a source to set it as preferred.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                DownloadSourcesContent(
                                    settings = settings,
                                    onSettingsChange = onSettingsChange
                                )
                            }
                        }
                        SettingsSection.HISTORY -> {
                            DownloadHistorySection(
                                entries = historyEntries,
                                onClear = onClearHistory,
                                onOpen = onOpenHistoryEntry,
                                onShare = onShareHistoryEntry
                            )
                        }
                        SettingsSection.UPDATES -> {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                UpdatesCard(
                                    updateState = updateState,
                                    onCheckForUpdates = onCheckForUpdates,
                                    onDownloadUpdate = onDownloadUpdate,
                                    onInstallUpdate = onInstallUpdate,
                                    onDismissUpdate = onDismissUpdate,
                                    position = ConnectedPosition.First
                                )
                                AboutRows(onOpenUrl = openUrl)
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Single-Column Phone Layout with swipe-to-dismiss
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = MorpheDefaults.MaxContentWidth)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = MorpheDefaults.ContentPadding, vertical = MorpheDefaults.ContentPadding)
                    .pointerInput(swipeThresholdPx) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                totalDrag += dragAmount
                            },
                            onDragEnd = {
                                if (totalDrag >= swipeThresholdPx) onBack()
                            },
                            onDragCancel = { totalDrag = 0f }
                        )
                    },
                verticalArrangement = Arrangement.spacedBy(MorpheDefaults.ItemSpacing)
            ) {
                // Top Header: Title and Close button (NO BACK ARROW)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Surface(
                        onClick = onBack,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Appearance
                SettingsGroup(
                    title = "Appearance",
                    icon = Icons.Outlined.Palette
                ) {
                    SettingsRow(
                        icon = settings.themeMode.icon(),
                        title = "Theme",
                        subtitle = when (settings.themeMode) {
                            ThemeMode.SYSTEM -> "System default"
                            ThemeMode.LIGHT -> "Light theme"
                            ThemeMode.DARK -> "Dark theme"
                        },
                        trailing = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MorpheStatusBadge(
                                    text = settings.themeMode.title,
                                    tone = SemanticTone.Neutral
                                )
                                ForwardChevronIcon(size = MorpheDefaults.IconSizeSmall)
                            }
                        },
                        position = ConnectedPosition.Single,
                        onClick = { themeDialog = true }
                    )
                }

                // 1. Sources & Automation
                SettingsGroup(
                    title = "Sources & Automation",
                    icon = Icons.Outlined.Dns
                ) {
                    // Download Sources
                    SettingsRow(
                        icon = Icons.Outlined.Dns,
                        title = "Download Sources",
                        subtitle = "${enabledSources.size} of ${DownloadSource.entries.size} active • Tap to configure",
                        trailing = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MorpheStatusBadge(
                                    text = settings.preferredSource?.label ?: "Auto",
                                    tone = SemanticTone.Primary
                                )
                                ForwardChevronIcon(size = MorpheDefaults.IconSizeSmall)
                            }
                        },
                        position = ConnectedPosition.First,
                        onClick = { sourcesDialog = true }
                    )

                    // Auto-fetch (Zero-Click)
                    SettingsSwitchItem(
                        icon = Icons.Outlined.AutoAwesome,
                        title = "Auto-fetch",
                        subtitle = "Automatically download and return best match for Morphe",
                        checked = settings.autoDownloadBestMatch,
                        position = ConnectedPosition.Middle,
                        onToggle = {
                            onSettingsChange(settings.copy(autoDownloadBestMatch = !settings.autoDownloadBestMatch))
                        }
                    )

                    // Network Policy
                    SettingsRow(
                        icon = Icons.Outlined.Wifi,
                        title = "Network Policy",
                        subtitle = when (settings.networkPolicy) {
                            NetworkPolicy.WIFI_AND_MOBILE -> "Wi-Fi & Mobile data allowed"
                            NetworkPolicy.WIFI_ONLY -> "Wi-Fi connections only"
                        },
                        position = ConnectedPosition.Last,
                        onClick = { policyDialog = true }
                    )
                }

                // 2. Storage & Cache
                SettingsGroup(
                    title = "Storage & Cache",
                    icon = Icons.Outlined.SdStorage
                ) {
                    // Save Location / App Cache Selector
                    SettingsRow(
                        icon = Icons.Outlined.FolderOpen,
                        title = "Download Location",
                        subtitle = if (settings.downloadLocation == DownloadLocation.DOWNLOADS) {
                            "Downloads / Morphe Fetch"
                        } else {
                            "App Cache (Temporary)"
                        },
                        trailing = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MorpheStatusBadge(
                                    text = if (settings.downloadLocation == DownloadLocation.DOWNLOADS) "Downloads" else "App Cache",
                                    tone = SemanticTone.Neutral
                                )
                                ForwardChevronIcon(size = MorpheDefaults.IconSizeSmall)
                            }
                        },
                        position = ConnectedPosition.First,
                        onClick = { locationDialog = true }
                    )

                    // Auto-clear Cache
                    SettingsSwitchItem(
                        icon = Icons.Outlined.CleaningServices,
                        title = "Auto-clear Cache",
                        subtitle = "Purge temporary download after Morphe saves its copy",
                        checked = settings.deleteTemporaryAfterHandoff,
                        position = ConnectedPosition.Middle,
                        onToggle = {
                            onSettingsChange(settings.copy(deleteTemporaryAfterHandoff = !settings.deleteTemporaryAfterHandoff))
                        }
                    )

                    // Clear Cache Action Row
                    val totalCache = cacheBytes + downloadsBytes
                    SettingsRow(
                        icon = Icons.Outlined.DeleteOutline,
                        title = "Clear Cache",
                        subtitle = if (totalCache > 0L) {
                            "${totalCache.formatBytes()} used by cached APKs"
                        } else {
                            "Cache is clean (0 B)"
                        },
                        isDestructive = totalCache > 0L,
                        position = ConnectedPosition.Middle,
                        trailing = {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (totalCache > 0L) {
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                },
                                onClick = {
                                    if (totalCache > 0L) {
                                        context.clearTemporaryDownloads()
                                        context.clearDownloadsCopies()
                                        cacheBytes = 0L
                                        downloadsBytes = 0L
                                        Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalCache > 0L) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    },
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    )

                    // Download History
                    SettingsRow(
                        icon = Icons.Outlined.History,
                        title = "Download History",
                        subtitle = "${historyEntries.size} saved APKs • Tap to view or share",
                        trailing = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MorpheStatusBadge(
                                    text = "${historyEntries.size} APKs",
                                    tone = SemanticTone.Neutral
                                )
                                ForwardChevronIcon(size = MorpheDefaults.IconSizeSmall)
                            }
                        },
                        position = ConnectedPosition.Last,
                        onClick = { historyDialog = true }
                    )
                }

                // 3. Updates & About
                SettingsGroup(
                    title = "Updates & About",
                    icon = Icons.Outlined.Download
                ) {
                    UpdatesCard(
                        updateState = updateState,
                        onCheckForUpdates = onCheckForUpdates,
                        onDownloadUpdate = onDownloadUpdate,
                        onInstallUpdate = onInstallUpdate,
                        onDismissUpdate = onDismissUpdate,
                        position = ConnectedPosition.First
                    )
                    AboutRows(onOpenUrl = openUrl)
                }
            }
        }
    }
}

@Composable
private fun SettingsPreferencesDetail(
    settings: HelperSettings,
    onSettingsChange: (HelperSettings) -> Unit,
    cacheBytes: Long,
    onCleanCache: () -> Unit,
    onOpenLocationDialog: () -> Unit,
    onOpenPolicyDialog: () -> Unit,
    onOpenThemeDialog: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // Section 1: Appearance
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            SettingsRow(
                icon = settings.themeMode.icon(),
                title = "Theme",
                subtitle = when (settings.themeMode) {
                    ThemeMode.SYSTEM -> "System default"
                    ThemeMode.LIGHT -> "Light theme"
                    ThemeMode.DARK -> "Dark theme"
                },
                trailing = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MorpheStatusBadge(
                            text = settings.themeMode.title,
                            tone = SemanticTone.Neutral
                        )
                        ForwardChevronIcon(size = MorpheDefaults.IconSizeSmall)
                    }
                },
                position = ConnectedPosition.Single,
                onClick = onOpenThemeDialog
            )
        }

        // Section 2: Storage & Connection
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Storage & Connection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                SettingsRow(
                    icon = Icons.Outlined.FolderOpen,
                    title = "Download Location",
                    subtitle = if (settings.downloadLocation == DownloadLocation.DOWNLOADS) {
                        "Downloads / Morphe Fetch"
                    } else {
                        "App Cache (Temporary)"
                    },
                    trailing = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MorpheStatusBadge(
                                text = if (settings.downloadLocation == DownloadLocation.DOWNLOADS) "Downloads" else "App Cache",
                                tone = SemanticTone.Neutral
                            )
                            ForwardChevronIcon(size = MorpheDefaults.IconSizeSmall)
                        }
                    },
                    position = ConnectedPosition.First,
                    onClick = onOpenLocationDialog
                )

                SettingsRow(
                    icon = Icons.Outlined.Wifi,
                    title = "Network Policy",
                    subtitle = when (settings.networkPolicy) {
                        NetworkPolicy.WIFI_AND_MOBILE -> "Wi-Fi & Mobile data allowed"
                        NetworkPolicy.WIFI_ONLY -> "Wi-Fi connections only"
                    },
                    trailing = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MorpheStatusBadge(
                                text = if (settings.networkPolicy == NetworkPolicy.WIFI_AND_MOBILE) "Wi-Fi & Data" else "Unmetered",
                                tone = SemanticTone.Neutral
                            )
                            ForwardChevronIcon(size = MorpheDefaults.IconSizeSmall)
                        }
                    },
                    position = ConnectedPosition.Middle,
                    onClick = onOpenPolicyDialog
                )

                SettingsRow(
                    icon = Icons.Outlined.DeleteOutline,
                    title = "Clear Cache",
                    subtitle = if (cacheBytes > 0L) {
                        "${cacheBytes.formatBytes()} used by cached APKs"
                    } else {
                        "Cache is clean (0 B)"
                    },
                    isDestructive = cacheBytes > 0L,
                    position = ConnectedPosition.Last,
                    trailing = {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (cacheBytes > 0L) {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            },
                            onClick = onCleanCache
                        ) {
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (cacheBytes > 0L) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                },
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                )
            }
        }

        // Section 3: Automation
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Automation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                SettingsSwitchItem(
                    icon = Icons.Outlined.AutoAwesome,
                    title = "Auto-fetch",
                    subtitle = "Automatically download best matching candidate for Morphe without extra prompt",
                    checked = settings.autoDownloadBestMatch,
                    position = ConnectedPosition.First,
                    onToggle = {
                        onSettingsChange(settings.copy(autoDownloadBestMatch = !settings.autoDownloadBestMatch))
                    }
                )

                SettingsSwitchItem(
                    icon = Icons.Outlined.CleaningServices,
                    title = "Auto-clear Cache",
                    subtitle = "Purge temporary download after Morphe saves its copy",
                    checked = settings.deleteTemporaryAfterHandoff,
                    position = ConnectedPosition.Last,
                    onToggle = {
                        onSettingsChange(settings.copy(deleteTemporaryAfterHandoff = !settings.deleteTemporaryAfterHandoff))
                    }
                )
            }
        }
    }
}

@Composable
private fun UpdatesCard(
    updateState: UpdateState,
    onCheckForUpdates: () -> Unit,
    onDownloadUpdate: (UpdateInfo) -> Unit,
    onInstallUpdate: (File) -> Unit,
    onDismissUpdate: () -> Unit,
    position: ConnectedPosition = ConnectedPosition.Single
) {
    val colors = MaterialTheme.colorScheme
    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        shape = connectedCardShape(position),
        color = colors.surfaceColorAtElevation(3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MorpheDefaults.ContentPadding,
                    vertical = MorpheDefaults.ContentPaddingSmall + 4.dp
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Main Row (Fixed min-height to prevent jitter during checks)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp),
                horizontalArrangement = Arrangement.spacedBy(MorpheDefaults.ContentPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemedIcon(
                    icon = Icons.Outlined.Download,
                    tint = colors.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "App Updates",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (updateState) {
                            is UpdateState.Idle -> "v${BuildConfig.VERSION_NAME} · Check for updates"
                            is UpdateState.Checking -> "Checking GitHub releases..."
                            is UpdateState.UpToDate -> "v${BuildConfig.VERSION_NAME} · Up to date"
                            is UpdateState.Available -> "${updateState.info.tagName} available"
                            is UpdateState.Downloading -> "Downloading ${updateState.info.tagName}..."
                            is UpdateState.ReadyToInstall -> "${updateState.info.tagName} ready to install"
                            is UpdateState.Error -> "Check failed"
                            else -> "v${BuildConfig.VERSION_NAME}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (updateState) {
                            is UpdateState.Available -> colors.primary
                            is UpdateState.Error -> colors.error
                            else -> colors.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Trailing Action Badge / Chip with fixed height to prevent layout shift
                Box(
                    modifier = Modifier.height(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (updateState) {
                        is UpdateState.Idle -> {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = colors.primary.copy(alpha = 0.15f),
                                onClick = onCheckForUpdates
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = colors.primary
                                    )
                                    Text(
                                        text = "Check",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primary
                                    )
                                }
                            }
                        }
                        is UpdateState.Checking -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = colors.primary
                            )
                        }
                        is UpdateState.UpToDate -> {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = colors.primary.copy(alpha = 0.12f),
                                onClick = onCheckForUpdates
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = colors.primary
                                    )
                                    Text(
                                        text = "Latest",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.primary
                                    )
                                }
                            }
                        }
                        is UpdateState.Available -> {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = colors.primary,
                                onClick = { onDownloadUpdate(updateState.info) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = colors.onPrimary
                                    )
                                    Text(
                                        text = "Update",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onPrimary
                                    )
                                }
                            }
                        }
                        is UpdateState.Downloading -> {
                            Text(
                                text = "${(updateState.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary
                            )
                        }
                        is UpdateState.ReadyToInstall -> {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = colors.primary,
                                onClick = { onInstallUpdate(updateState.apkFile) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = colors.onPrimary
                                    )
                                    Text(
                                        text = "Install",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onPrimary
                                    )
                                }
                            }
                        }
                        is UpdateState.Error -> {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = colors.error.copy(alpha = 0.15f),
                                onClick = onCheckForUpdates
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = colors.error
                                    )
                                    Text(
                                        text = "Retry",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.error
                                    )
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }

            // Expanding content for Downloading or Available or Error
            when (updateState) {
                is UpdateState.Downloading -> {
                    val progress = updateState.progress
                    val copiedMb = "%.1f".format(Locale.US, updateState.downloadedBytes / (1024f * 1024f))
                    val totalMb = "%.1f".format(Locale.US, updateState.totalBytes / (1024f * 1024f))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$copiedMb / $totalMb MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = colors.primary,
                            trackColor = colors.surfaceVariant
                        )
                    }
                }
                is UpdateState.Available -> {
                    val info = updateState.info
                    if (!info.changelog.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(10.dp)
                            ) {
                                MarkdownChangelog(
                                    markdown = info.changelog.trim(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                is UpdateState.Error -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = updateState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.error,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Dismiss",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onDismissUpdate() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun AboutRows(onOpenUrl: (String) -> Unit) {
    SettingsRow(
        icon = Icons.Outlined.AccountCircle,
        title = "Developer",
        subtitle = "FarhanZafarr-9",
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = "Open link",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        position = ConnectedPosition.Middle,
        onClick = { onOpenUrl("https://github.com/FarhanZafarr-9") }
    )
    SettingsRow(
        icon = Icons.Outlined.FolderOpen,
        title = "Fork Repository",
        subtitle = "FarhanZafarr-9/morphe-fetch",
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = "Open link",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        position = ConnectedPosition.Middle,
        onClick = { onOpenUrl("https://github.com/FarhanZafarr-9/morphe-fetch") }
    )
    SettingsRow(
        icon = Icons.Outlined.Info,
        title = "Original Repository",
        subtitle = "Akash-Sriram/morphe-fetch",
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = "Open link",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        position = ConnectedPosition.Last,
        onClick = { onOpenUrl("https://github.com/Akash-Sriram/morphe-fetch") }
    )
}

