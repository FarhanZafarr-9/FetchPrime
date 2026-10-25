package app.morphe.fetch

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.morphe.fetch.updater.UpdateInfo
import app.morphe.fetch.updater.UpdateState
import java.io.File
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun HelperButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
) {
    MorpheDialogButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        filled = true,
        contentPadding = contentPadding
    )
}

@Composable
internal fun HelperOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
) {
    MorpheDialogButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        filled = false,
        contentPadding = contentPadding
    )
}

/**
 * Square glass button used by the header row and other single-icon actions,
 * matching the manager's glass-button family (16dp shape, 48dp touch target).
 */
@Composable
internal fun HelperIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    size: Dp = 38.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    tint: Color? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    val contentColor = tint ?: if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .pressScale(interactionSource),
        shape = shape,
        color = if (selected) primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = contentColor,
        interactionSource = interactionSource
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
internal fun HelperHeaderIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HelperIconButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
internal fun ReuseOfferDialog(
    options: List<ReuseOption>,
    onUseExisting: (ReuseOption) -> Unit,
    onDownloadNew: () -> Unit
) {
    Dialog(
        onDismissRequest = onDownloadNew,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 500.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
            tonalElevation = 3.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Use an existing APK?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "A previous download for this version is already available. Pick one to return without re-downloading.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    options.forEachIndexed { index, option ->
                        val entry = option.entry
                        val position = connectedPosition(index, options.size)
                        val shape = connectedCardShape(position, largeRadius = 14.dp, smallRadius = 4.dp)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = shape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            onClick = { onUseExisting(option) }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = entry.sourceName,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    entry.versionName?.let {
                                        Text(
                                            text = "· $it",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (option.sizeBytes > 0L) {
                                        Text(
                                            text = "· ${option.sizeBytes.formatBytes()}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = entry.fileName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    onClick = onDownloadNew
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Download new instead",
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

internal data class SourceBrand(
    val resId: Int
)

internal fun DownloadSource.brand(): SourceBrand = when (this) {
    DownloadSource.AURORA -> SourceBrand(resId = R.drawable.ic_src_play)
    DownloadSource.APK_PURE -> SourceBrand(resId = R.drawable.ic_src_apkpure)
    DownloadSource.UPTODOWN -> SourceBrand(resId = R.drawable.ic_src_uptodown)
    DownloadSource.APK_MIRROR -> SourceBrand(resId = R.drawable.ic_src_apkmirror)
}

@Composable
internal fun SourceAvatar(source: DownloadSource, size: androidx.compose.ui.unit.Dp = 40.dp) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(source.brand().resId),
        contentDescription = null,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
    )
}

@Composable
internal fun RadioDot(selected: Boolean) {
    Icon(
        imageVector = if (selected) {
            Icons.Outlined.RadioButtonChecked
        } else {
            Icons.Outlined.RadioButtonUnchecked
        },
        contentDescription = if (selected) "Selected" else null,
        tint = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        },
        modifier = Modifier.size(18.dp)
    )
}

internal val sourceCategories: List<Pair<String, List<DownloadSource>>> = listOf(
    "Direct downloads" to listOf(
        DownloadSource.APK_PURE
    ),
    "Archive / Web verification" to listOf(
        DownloadSource.UPTODOWN,
        DownloadSource.APK_MIRROR
    )
)

@Composable
internal fun InfoCard(text: String) {
    InfoBox(
        title = text,
        titleColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {}
}

@Composable
internal fun sourceCardFill(): Color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)

@Composable
internal fun sourceCardBorder(): Color = MaterialTheme.colorScheme.outlineVariant

@Composable
internal fun HelperThemeButton(
    dark: Boolean,
    onToggle: () -> Unit
) {
    HelperIconButton(
        icon = if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
        contentDescription = if (dark) "Switch to light theme" else "Switch to dark theme",
        onClick = onToggle
    )
}

@Composable
internal fun AppAvatar(
    packageName: String,
    initial: Char,
    isInstalled: Boolean? = null,
    iconUrl: String? = null,
    apkUri: String? = null,
    size: Dp = 44.dp,
    shape: Shape = RoundedCornerShape(MorpheDefaults.CompactCornerRadius),
    allowWebScraping: Boolean = true
) {
    val context = LocalContext.current
    var iconBitmap by remember(packageName, iconUrl, apkUri) {
        mutableStateOf<ImageBitmap?>(AppIconResolver.getCached(packageName))
    }

    LaunchedEffect(packageName, iconUrl, apkUri, isInstalled, allowWebScraping) {
        if (iconBitmap == null) {
            val resolved = AppIconResolver.resolveIcon(
                context = context,
                packageName = packageName,
                iconUrl = iconUrl,
                apkUri = apkUri,
                isInstalled = isInstalled,
                allowWebScraping = allowWebScraping
            )
            if (resolved != null) {
                iconBitmap = resolved
            }
        }
    }

    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap!!,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(shape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial.toString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = if (size < 40.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
internal fun LazyListScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragStartY by remember { mutableFloatStateOf(0f) }
    var dragStartIndex by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var containerHeightPx by remember { mutableFloatStateOf(0f) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val thumbColor by animateColorAsState(
        targetValue = if (isDragging) primaryColor else primaryColor.copy(alpha = 0.5f),
        label = "scrollbar_thumb_color"
    )
    val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .width(16.dp)
            .fillMaxHeight()
            .onSizeChanged { containerHeightPx = it.height.toFloat() }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragStartY = offset.y
                        dragStartIndex = listState.firstVisibleItemIndex
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onVerticalDrag = { change, _ ->
                        val info = listState.layoutInfo
                        val total = info.totalItemsCount
                        val visible = info.visibleItemsInfo.size
                        if (total > visible && total > 0 && containerHeightPx > 0f) {
                            val fraction = (visible.toFloat() / total.toFloat()).coerceIn(0.06f, 1f)
                            val thumbPx = (containerHeightPx * fraction).coerceIn(48f, containerHeightPx)
                            val travelPx = (containerHeightPx - thumbPx).coerceAtLeast(1f)
                            val scrollable = (total - visible).coerceAtLeast(1)
                            val delta = change.position.y - dragStartY
                            val target = dragStartIndex + (delta / travelPx * scrollable).toInt()
                            scope.launch {
                                listState.scrollToItem(target.coerceIn(0, total - 1))
                            }
                        }
                    }
                )
            }
            .drawWithContent {
                drawContent()
                val info = listState.layoutInfo
                val total = info.totalItemsCount
                val visible = info.visibleItemsInfo.size
                if (total <= 0 || visible >= total || containerHeightPx <= 0f) return@drawWithContent

                val fraction = (visible.toFloat() / total.toFloat()).coerceIn(0.06f, 1f)
                val thumbPx = (containerHeightPx * fraction).coerceIn(48f, containerHeightPx)
                val travelPx = (containerHeightPx - thumbPx).coerceAtLeast(0f)
                val scrollable = (total - visible).coerceAtLeast(1)

                val firstIndex = listState.firstVisibleItemIndex
                val firstOffset = listState.firstVisibleItemScrollOffset
                val itemHeightPx = info.visibleItemsInfo.firstOrNull()?.size?.toFloat() ?: 1f
                val smoothIndex = firstIndex + (firstOffset / itemHeightPx).coerceIn(0f, 1f)
                val pos = (smoothIndex / scrollable.toFloat()).coerceIn(0f, 1f)
                val thumbY = (travelPx * pos)

                // Track
                val trackWidth = 4.dp.toPx()
                val trackRight = size.width - 2.dp.toPx()
                val trackLeft = trackRight - trackWidth
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(trackLeft, 4.dp.toPx()),
                    size = Size(trackWidth, (containerHeightPx - 8.dp.toPx()).coerceAtLeast(0f)),
                    cornerRadius = CornerRadius(trackWidth / 2, trackWidth / 2)
                )

                // Thumb
                val thumbWidth = if (isDragging) 6.dp.toPx() else 4.dp.toPx()
                val thumbRight = size.width - 2.dp.toPx()
                val thumbLeft = thumbRight - thumbWidth
                drawRoundRect(
                    color = thumbColor,
                    topLeft = Offset(thumbLeft, thumbY + 4.dp.toPx()),
                    size = Size(thumbWidth, (thumbPx - 8.dp.toPx()).coerceAtLeast(16f)),
                    cornerRadius = CornerRadius(thumbWidth / 2, thumbWidth / 2)
                )
            }
    )
}

@Composable
internal fun ScrollStateScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val maxPx = scrollState.maxValue
    if (maxPx <= 0) return
    val scope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var dragStartY by remember { mutableFloatStateOf(0f) }
    var dragStartVal by remember { mutableIntStateOf(0) }
    var containerHeightPx by remember { mutableFloatStateOf(0f) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val thumbColor by animateColorAsState(
        targetValue = if (isDragging) primaryColor else primaryColor.copy(alpha = 0.5f),
        label = "scrollbar_thumb_color"
    )
    val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .width(16.dp)
            .fillMaxHeight()
            .onSizeChanged { containerHeightPx = it.height.toFloat() }
            .pointerInput(maxPx) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragStartY = offset.y
                        dragStartVal = scrollState.value
                    },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onVerticalDrag = { change, _ ->
                        if (containerHeightPx > 0f) {
                            val thumbFraction = (containerHeightPx / (containerHeightPx + maxPx.toFloat())).coerceIn(0.08f, 1f)
                            val thumbPx = (containerHeightPx * thumbFraction).coerceIn(44f, containerHeightPx)
                            val travelPx = (containerHeightPx - thumbPx).coerceAtLeast(1f)
                            val delta = change.position.y - dragStartY
                            val target = dragStartVal + (delta / travelPx * maxPx).toInt()
                            scope.launch {
                                scrollState.scrollTo(target.coerceIn(0, maxPx))
                            }
                        }
                    }
                )
            }
            .drawWithContent {
                drawContent()
                if (containerHeightPx <= 0f || maxPx <= 0) return@drawWithContent

                val thumbFraction = (containerHeightPx / (containerHeightPx + maxPx.toFloat())).coerceIn(0.08f, 1f)
                val thumbPx = (containerHeightPx * thumbFraction).coerceIn(44f, containerHeightPx)
                val travelPx = (containerHeightPx - thumbPx).coerceAtLeast(0f)
                val pos = (scrollState.value.toFloat() / maxPx.toFloat()).coerceIn(0f, 1f)
                val thumbY = (travelPx * pos)

                // Track
                val trackWidth = 4.dp.toPx()
                val trackRight = size.width - 2.dp.toPx()
                val trackLeft = trackRight - trackWidth
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(trackLeft, 4.dp.toPx()),
                    size = Size(trackWidth, (containerHeightPx - 8.dp.toPx()).coerceAtLeast(0f)),
                    cornerRadius = CornerRadius(trackWidth / 2, trackWidth / 2)
                )

                // Thumb
                val thumbWidth = if (isDragging) 6.dp.toPx() else 4.dp.toPx()
                val thumbRight = size.width - 2.dp.toPx()
                val thumbLeft = thumbRight - thumbWidth
                drawRoundRect(
                    color = thumbColor,
                    topLeft = Offset(thumbLeft, thumbY + 4.dp.toPx()),
                    size = Size(thumbWidth, (thumbPx - 8.dp.toPx()).coerceAtLeast(16f)),
                    cornerRadius = CornerRadius(thumbWidth / 2, thumbWidth / 2)
                )
            }
    )
}

@Composable
internal fun AnimatedExpand(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = androidx.compose.animation.expandVertically(
            animationSpec = androidx.compose.animation.core.tween(MorpheDefaults.ANIMATION_DURATION),
            expandFrom = Alignment.Top
        ) + fadeIn(animationSpec = androidx.compose.animation.core.tween(MorpheDefaults.ANIMATION_DURATION)),
        exit = androidx.compose.animation.shrinkVertically(
            animationSpec = androidx.compose.animation.core.tween(MorpheDefaults.ANIMATION_DURATION_SHORT),
            shrinkTowards = Alignment.Top
        ) + fadeOut(animationSpec = androidx.compose.animation.core.tween(MorpheDefaults.ANIMATION_DURATION_SHORT))
    ) {
        content()
    }
}

@Composable
internal fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator()
    }
}

private fun parseMarkdownInline(
    text: String,
    primaryColor: Color,
    codeBgColor: Color
): AnnotatedString {
    val pattern = Regex("""(\*\*(.+?)\*\*)|(`{1,3}(.+?)`{1,3})|(\[(.+?)\]\((.+?)\))|(https?://[^\s)]+)""")
    return buildAnnotatedString {
        var lastIndex = 0
        for (match in pattern.findAll(text)) {
            val range = match.range
            if (range.first > lastIndex) {
                append(text.substring(lastIndex, range.first))
            }

            when {
                // **bold** -> group 2
                match.groups[2] != null -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(match.groups[2]!!.value)
                    }
                }
                // `code` or ```code``` -> group 4
                match.groups[4] != null -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            background = codeBgColor
                        )
                    ) {
                        append(" ${match.groups[4]!!.value.trim()} ")
                    }
                }
                // [label](url) -> group 6 is label, group 7 is url
                match.groups[6] != null -> {
                    withStyle(
                        SpanStyle(
                            color = primaryColor,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(match.groups[6]!!.value)
                    }
                }
                // raw URL -> group 8
                match.groups[8] != null -> {
                    withStyle(
                        SpanStyle(
                            color = primaryColor,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(match.groups[8]!!.value)
                    }
                }
                else -> {
                    append(match.value)
                }
            }
            lastIndex = range.last + 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

private sealed interface MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock
    data class Bullet(val text: String) : MarkdownBlock
    data class CodeBlock(val code: String, val language: String = "") : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data object EmptyLine : MarkdownBlock
}

private fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = markdown.lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            val language = trimmed.removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) {
                i++ // consume closing ```
            }
            blocks.add(MarkdownBlock.CodeBlock(codeLines.joinToString("\n"), language))
            continue
        }

        if (trimmed.isEmpty()) {
            blocks.add(MarkdownBlock.EmptyLine)
            i++
            continue
        }

        when {
            trimmed.startsWith("### ") -> {
                blocks.add(MarkdownBlock.Header(3, trimmed.removePrefix("### ").trim()))
            }
            trimmed.startsWith("## ") -> {
                blocks.add(MarkdownBlock.Header(2, trimmed.removePrefix("## ").trim()))
            }
            trimmed.startsWith("# ") -> {
                blocks.add(MarkdownBlock.Header(1, trimmed.removePrefix("# ").trim()))
            }
            trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("• ") -> {
                val content = when {
                    trimmed.startsWith("* ") -> trimmed.removePrefix("* ")
                    trimmed.startsWith("- ") -> trimmed.removePrefix("- ")
                    else -> trimmed.removePrefix("• ")
                }.trim()
                blocks.add(MarkdownBlock.Bullet(content))
            }
            else -> {
                blocks.add(MarkdownBlock.Paragraph(trimmed))
            }
        }
        i++
    }
    return blocks
}

@Composable
internal fun MarkdownChangelog(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (block in blocks) {
            when (block) {
                is MarkdownBlock.Header -> {
                    val (style, paddingTop) = when (block.level) {
                        1 -> MaterialTheme.typography.titleMedium to 6.dp
                        2 -> MaterialTheme.typography.titleSmall to 6.dp
                        else -> MaterialTheme.typography.titleSmall to 4.dp
                    }
                    Text(
                        text = block.text,
                        style = style,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor,
                        modifier = Modifier.padding(top = paddingTop, bottom = 2.dp)
                    )
                }
                is MarkdownBlock.Bullet -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Text(
                            text = parseMarkdownInline(block.text, primaryColor, codeBgColor),
                            style = MaterialTheme.typography.bodySmall,
                            color = onSurfaceColor
                        )
                    }
                }
                is MarkdownBlock.CodeBlock -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = block.code,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseMarkdownInline(block.text, primaryColor, codeBgColor),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (block.text.startsWith("**Full Changelog**", ignoreCase = true)) {
                            onSurfaceVariantColor
                        } else {
                            onSurfaceColor
                        }
                    )
                }
                MarkdownBlock.EmptyLine -> {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}

@Composable
internal fun AppUpdateDialog(
    updateState: UpdateState,
    onDownloadUpdate: (UpdateInfo) -> Unit,
    onInstallUpdate: (File) -> Unit,
    onDismiss: () -> Unit
) {
    when (updateState) {
        is UpdateState.Available -> {
            val info = updateState.info
            val sizeMb = if (info.apkSize > 0) {
                "%.1f MB".format(Locale.US, info.apkSize / (1024f * 1024f))
            } else ""

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
                        // Header: Icon + Title + Close Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Update Available",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
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

                        // Version & Size Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Morphe Fetch ${info.tagName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (sizeMb.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = sizeMb,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Current: v${BuildConfig.VERSION_NAME} → Latest: ${info.tagName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Changelog Container (Intact size, scrollable, markdown formatted, no border)
                        if (!info.changelog.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(12.dp)
                                ) {
                                    MarkdownChangelog(
                                        markdown = info.changelog.trim(),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Action Buttons: Pill shapes, borderless, full width
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
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                onClick = onDismiss
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Later",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary,
                                onClick = { onDownloadUpdate(info) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Download,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Update Now",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        is UpdateState.Downloading -> {
            val progress = updateState.progress
            val copiedMb = "%.1f".format(Locale.US, updateState.downloadedBytes / (1024f * 1024f))
            val totalMb = "%.1f".format(Locale.US, updateState.totalBytes / (1024f * 1024f))

            Dialog(
                onDismissRequest = { /* Keep active during download */ },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Downloading Update...",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "${updateState.info.tagName} • $copiedMb of $totalMb MB (${(progress * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
        is UpdateState.ReadyToInstall -> {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Update Ready",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
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

                        Text(
                            text = "${updateState.info.tagName} has been downloaded and is ready to install.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

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
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                onClick = onDismiss
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Later",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary,
                                onClick = { onInstallUpdate(updateState.apkFile) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Download,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Install Now",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        is UpdateState.Error -> {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Update Failed",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
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

                        Text(
                            text = updateState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp),
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            onClick = onDismiss
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Dismiss",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
        else -> {}
    }
}



