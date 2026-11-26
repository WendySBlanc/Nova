package eu.kanade.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.NovaIcons
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clearFocusOnSoftKeyboardHide
import tachiyomi.presentation.core.util.isItemScrollingUp
import tachiyomi.presentation.core.util.runOnEnterKeyPressed

@Composable
fun AnimatedFloatingSearchBox(
    listState: LazyListState,
    searchQuery: String?,
    onChangeSearchQuery: (String?) -> Unit,
    modifier: Modifier = Modifier,
    onGloballyPositioned: (LayoutCoordinates) -> Unit = { },
    placeholderText: String? = null,
    focusManager: FocusManager = LocalFocusManager.current,
    focusRequester: FocusRequester = remember { FocusRequester() },
    keyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current,
) {
    AnimatedVisibility(
        visible = listState.isItemScrollingUp(),
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier,
    ) {
        SourcesSearchBox(
            searchQuery = searchQuery,
            onChangeSearchQuery = onChangeSearchQuery,
            onGloballyPositioned = onGloballyPositioned,
            placeholderText = placeholderText,
            focusManager = focusManager,
            focusRequester = focusRequester,
            keyboardController = keyboardController,
        )
    }
}

@Composable
fun SourcesSearchBox(
    searchQuery: String?,
    onChangeSearchQuery: (String?) -> Unit,
    modifier: Modifier = Modifier,
    onGloballyPositioned: (LayoutCoordinates) -> Unit = { },
    placeholderText: String? = null,
    focusManager: FocusManager = LocalFocusManager.current,
    focusRequester: FocusRequester = remember { FocusRequester() },
    keyboardController: SoftwareKeyboardController? = LocalSoftwareKeyboardController.current,
) {
    val searchAndClearFocus: () -> Unit = f@{
        if (searchQuery.isNullOrBlank()) return@f
        focusManager.clearFocus()
        keyboardController?.hide()
    }
    val onClickClearSearch: () -> Unit = {
        onChangeSearchQuery("")
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    val onClickCloseSearch: () -> Unit = {
        onChangeSearchQuery("")
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    var isFocused by remember { mutableStateOf(false) }

    NovaCommandField(
        query = searchQuery.orEmpty(),
        onQueryChange = onChangeSearchQuery,
        placeholder = placeholderText ?: stringResource(MR.strings.action_search_hint),
        onSubmit = searchAndClearFocus,
        leadingContent = {
            SearchBoxLeadingIcon(
                isSearching = isFocused || !searchQuery.isNullOrBlank(),
                onClickCloseSearch = onClickCloseSearch,
            )
        },
        trailingContent = {
            SearchBoxTrailingIcon(
                isEmpty = searchQuery.isNullOrEmpty(),
                onClickClearSearch = onClickClearSearch,
            )
        },
        modifier = modifier
            .onGloballyPositioned(onGloballyPositioned)
            .runOnEnterKeyPressed(action = searchAndClearFocus)
            .clearFocusOnSoftKeyboardHide(),
        focusRequester = focusRequester,
        onFocusChanged = { isFocused = it },
    )
}

@Composable
fun SearchBoxLeadingIcon(
    isSearching: Boolean,
    modifier: Modifier = Modifier,
    onClickCloseSearch: () -> Unit = {},
) {
    if (isSearching) {
        IconButton(
            modifier = modifier,
            onClick = onClickCloseSearch,
        ) {
            Icon(
                imageVector = NovaIcons.Back,
                contentDescription = stringResource(MR.strings.action_close),
            )
        }
    } else {
        Icon(
            modifier = modifier,
            imageVector = NovaIcons.Search,
            contentDescription = stringResource(MR.strings.action_search),
        )
    }
}

@Composable
fun SearchBoxTrailingIcon(
    isEmpty: Boolean,
    modifier: Modifier = Modifier,
    onClickClearSearch: () -> Unit = {},
) {
    if (!isEmpty) {
        IconButton(
            modifier = modifier,
            onClick = onClickClearSearch,
        ) {
            Icon(
                imageVector = NovaIcons.Close,
                contentDescription = stringResource(MR.strings.action_reset),
            )
        }
    }
}

internal val SOURCE_SEARCH_BOX_HEIGHT = 64.dp

@Preview
@Composable
fun PreviewClearFocusOnKeyboardDismissExample() {
    SourcesSearchBox(
        searchQuery = "Hello World",
        onChangeSearchQuery = {},
    )
}
