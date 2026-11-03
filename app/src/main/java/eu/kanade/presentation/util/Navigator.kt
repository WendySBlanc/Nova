package eu.kanade.presentation.util

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.ScreenModelStore
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.ScreenTransitionContent
import eu.kanade.presentation.home.NovaScreenMotion
import eu.kanade.presentation.home.resolveNovaScreenMotion
import eu.kanade.tachiyomi.util.system.isPreviewBuildType
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.plus
import logcat.LogPriority
import logcat.logcat
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance

/**
 * For invoking back press to the parent activity
 */
val LocalBackPress: ProvidableCompositionLocal<(() -> Unit)?> = staticCompositionLocalOf { null }

interface Tab : cafe.adriel.voyager.navigator.tab.Tab {
    suspend fun onReselect(navigator: Navigator) {}

    // SY -->
    @Composable
    fun isEnabled(): Boolean = true
    // SY <--
}

abstract class Screen : Screen {
    // known bug: https://github.com/mihonapp/mihon/issues/712
    // This is where it create a key Screen#uuid:transition which causes exception Key ... was used multiple times
    override val key: ScreenKey = "$uniqueScreenKey#${this::class.simpleName}"
}

/**
 * A variant of ScreenModel.coroutineScope except with the IO dispatcher instead of the
 * main dispatcher.
 */
val ScreenModel.ioCoroutineScope: CoroutineScope
    get() = ScreenModelStore.getOrPutDependency(
        screenModel = this,
        name = "ScreenModelIoCoroutineScope",
        factory = { key -> CoroutineScope(Dispatchers.IO + SupervisorJob()) + CoroutineName(key) },
        onDispose = { scope -> scope.cancel() },
    )

interface AssistContentScreen {
    fun onProvideAssistUrl(): String?
}

@Composable
fun DefaultNavigatorScreenTransition(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val slideDistance = rememberSlideDistance()
    ScreenTransition(
        navigator = navigator,
        transition = {
            materialSharedAxisX(
                forward = navigator.lastEvent != StackEvent.Pop,
                slideDistance = slideDistance,
            )
        },
        modifier = modifier,
    )
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun ScreenTransition(
    navigator: Navigator,
    transition: AnimatedContentTransitionScope<Screen>.() -> ContentTransform,
    modifier: Modifier = Modifier,
    content: ScreenTransitionContent = { it.Content() },
) {
    val sharedTransitionState = remember { NovaSharedTransitionState() }
    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(
            LocalNovaSharedTransitionScope provides this@SharedTransitionLayout,
            LocalNovaSharedTransitionState provides sharedTransitionState,
        ) {
            AnimatedContent(
                targetState = navigator.lastItem,
                transitionSpec = {
                    when (
                        resolveNovaScreenMotion(
                            initialIsTitleDetail = initialState is NovaTitleDetailScreen,
                            targetIsTitleDetail = targetState is NovaTitleDetailScreen,
                            isPop = navigator.lastEvent == StackEvent.Pop,
                        )
                    ) {
                        NovaScreenMotion.TitleForward -> titleDetailForwardTransform()
                        NovaScreenMotion.TitleBackward -> titleDetailBackwardTransform()
                        NovaScreenMotion.SharedAxis -> transition()
                    }
                },
                label = "screen-transition",
            ) { screen ->
                CompositionLocalProvider(
                    LocalNovaAnimatedVisibilityScope provides this@AnimatedContent,
                ) {
                    if (isPreviewBuildType) {
                        logcat(LogPriority.ERROR) { "ScreenTransition: ${screen.key}" }
                    }
                    navigator.saveableState("screen-transition-${screen.key}", screen) {
                        content(screen)
                    }
                }
            }
        }
    }
}

private fun AnimatedContentTransitionScope<Screen>.titleDetailForwardTransform(): ContentTransform =
    (
        fadeIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        ) + slideInVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            initialOffsetY = { height -> height / 7 },
        )
        ) togetherWith fadeOut()

private fun AnimatedContentTransitionScope<Screen>.titleDetailBackwardTransform(): ContentTransform =
    fadeIn() togetherWith
        (
            fadeOut() + slideOutVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
                targetOffsetY = { height -> height / 7 },
            )
            )
