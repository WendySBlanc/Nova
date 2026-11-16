package eu.kanade.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.NovaSignalDimensions
import tachiyomi.presentation.core.components.material.padding

/**
 * A compact number transition inspired by NumberFlow. Only the changed value moves,
 * which keeps labels and tabs stable while counts update.
 */
@Composable
fun NovaAnimatedNumber(
    value: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.labelMedium,
    contentDescription: String = value.toString(),
) {
    val motionScheme = MaterialTheme.motionScheme
    Box(
        modifier = modifier.clearAndSetSemantics {
            text = AnnotatedString(contentDescription)
        },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                val direction = if (targetState >= initialState) 1 else -1
                (
                    slideInVertically(
                        animationSpec = motionScheme.fastSpatialSpec(),
                        initialOffsetY = { height -> direction * height },
                    ) + fadeIn(animationSpec = motionScheme.fastEffectsSpec())
                    ).togetherWith(
                    slideOutVertically(
                        animationSpec = motionScheme.fastSpatialSpec(),
                        targetOffsetY = { height -> -direction * height },
                    ) + fadeOut(animationSpec = motionScheme.fastEffectsSpec()),
                )
            },
            label = "nova_number_flow",
        ) { animatedValue ->
            Text(
                text = animatedValue.toString(),
                modifier = Modifier.clearAndSetSemantics { },
                color = color,
                style = style,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun NovaCountBadge(
    value: Int,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    contentDescription: String = value.toString(),
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        NovaAnimatedNumber(
            value = value,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            style = MaterialTheme.typography.labelSmall,
            contentDescription = contentDescription,
        )
    }
}

@Composable
fun NovaMenuItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
    )
}

/**
 * App-level notification host with a strong action affordance and polite screen-reader announcements.
 */
@Composable
fun NovaSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
    ) { data ->
        Snackbar(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            action = {
                data.visuals.actionLabel?.let { label ->
                    TextButton(onClick = data::performAction) {
                        Text(label)
                    }
                }
            },
            dismissAction = if (data.visuals.withDismissAction) {
                {
                    IconButton(onClick = data::dismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Fermer",
                        )
                    }
                }
            } else {
                null
            },
            shape = RoundedCornerShape(NovaSignalDimensions.ControlCorner),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
            actionContentColor = MaterialTheme.colorScheme.primary,
            dismissActionContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 28.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(NovaSignalDimensions.SmallCorner),
                        ),
                )
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/**
 * Command field whose leading and trailing controls are supplied by the caller,
 * while focus, keyboard and accessible sizing remain consistent across screens.
 */
@Composable
fun NovaCommandField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    onSubmit: () -> Unit,
    leadingContent: @Composable () -> Unit,
    trailingContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onFocusChanged: (Boolean) -> Unit = { },
) {
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(NovaSignalDimensions.ControlCorner)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = NovaSignalDimensions.MinimumTouchTarget)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                width = 1.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            )
            .focusRequester(focusRequester)
            .onFocusChanged {
                focused = it.isFocused
                onFocusChanged(it.isFocused)
            },
        enabled = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = query,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                placeholder = {
                    Text(
                        text = placeholder,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                leadingIcon = leadingContent,
                trailingIcon = trailingContent,
                shape = shape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
                contentPadding = PaddingValues(MaterialTheme.padding.small),
            )
        },
    )
}
