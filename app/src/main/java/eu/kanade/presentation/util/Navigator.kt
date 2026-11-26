package eu.kanade.presentation.util

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.ScreenModelStore
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.ScreenTransitionContent
import eu.kanade.presentation.home.NovaScreenMotion
import eu.kanade.presentation.home.novaInkEntrance
import eu.kanade.presentation.home.novaInkHold
import eu.kanade.presentation.home.resolveNovaScreenMotion
import eu.kanade.presentation.theme.novaReducedMotion
import eu.kanade.tachiyomi.util.system.isPreviewBuildType
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.plus
import logcat.LogPriority
import logcat.logcat

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
    val motionScheme = MaterialTheme.motionScheme
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    ScreenTransition(
        navigator = navigator,
        transition = {
            val direction = if (navigator.lastEvent != StackEvent.Pop) {
                AnimatedContentTransitionScope.SlideDirection.Start
            } else {
                AnimatedContentTransitionScope.SlideDirection.End
            }
            slideIntoContainer(
                towards = direction,
                animationSpec = motionScheme.defaultSpatialSpec(),
                initialOffset = { it / 5 },
            ) + fadeIn(
                animationSpec = motionScheme.defaultEffectsSpec(),
            ) togetherWith slideOutOfContainer(
                towards = direction,
                animationSpec = motionScheme.defaultSpatialSpec(),
                targetOffset = { it / 8 },
            ) + fadeOut(
                animationSpec = motionScheme.defaultEffectsSpec(),
            )
        },
        modifier = modifier,
        inkFromLeft = (navigator.lastEvent == StackEvent.Pop) != isRtl,
    )
}

@Composable
@OptIn(ExperimentalSharedTransitionApi::class)
fun ScreenTransition(
    navigator: Navigator,
    transition: AnimatedContentTransitionScope<Screen>.() -> ContentTransform,
    modifier: Modifier = Modifier,
    inkFromLeft: Boolean? = null,
    content: ScreenTransitionContent = { it.Content() },
) {
    val sharedTransitionState = remember { NovaSharedTransitionState() }
    val motionScheme = MaterialTheme.motionScheme
    val useInk = inkFromLeft != null && !novaReducedMotion()
    val screenTransition = updateTransition(navigator.lastItem, label = "screen-transition")
    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(
            LocalNovaSharedTransitionScope provides this@SharedTransitionLayout,
            LocalNovaSharedTransitionState provides sharedTransitionState,
        ) {
            screenTransition.AnimatedContent(
                transitionSpec = {
                    val matchesCover = sharedTransitionState.matchesArtwork(initialState, targetState)
                    when {
                        matchesCover -> titleDetailTransform(motionScheme, hasSharedArtwork = true)
                        useInk -> novaInkHold()
                        resolveNovaScreenMotion(
                            initialIsTitleDetail = initialState is NovaTitleDetailScreen,
                            targetIsTitleDetail = targetState is NovaTitleDetailScreen,
                        ) == NovaScreenMotion.TitleDetail -> titleDetailTransform(motionScheme, hasSharedArtwork = false)
                        else -> transition()
                    }
                },
            ) { screen ->
                val visibility = this
                val matchesCover = sharedTransitionState.matchesArtwork(screenTransition.currentState, screenTransition.targetState)
                CompositionLocalProvider(
                    LocalNovaAnimatedVisibilityScope provides this@AnimatedContent,
                    LocalNovaTransitionScreenKey provides screen.key,
                ) {
                    if (isPreviewBuildType) {
                        logcat(LogPriority.ERROR) { "ScreenTransition: ${screen.key}" }
                    }
                    navigator.saveableState("screen-transition-${screen.key}", screen) {
                        if (inkFromLeft != null) {
                            val motion = if (useInk && !matchesCover) {
                                Modifier.novaInkEntrance(visibility, fromLeft = inkFromLeft)
                            } else {
                                Modifier
                            }
                            Box(Modifier.fillMaxSize().then(motion)) { content(screen) }
                        } else {
                            content(screen)
                        }
                    }
                }
            }
        }
    }
}

private fun NovaSharedTransitionState.matchesArtwork(initial: Screen, target: Screen): Boolean {
    val fromDetail = initial as? NovaTitleDetailScreen
    val toDetail = target as? NovaTitleDetailScreen
    return when {
        fromDetail == null && toDetail != null -> hasArtworkFor(toDetail.artworkMangaId, initial.key)
        fromDetail != null && toDetail == null -> hasArtworkFor(fromDetail.artworkMangaId, target.key)
        else -> false
    }
}

private fun titleDetailTransform(
    motionScheme: MotionScheme,
    hasSharedArtwork: Boolean,
): ContentTransform {
    val enter = fadeIn(animationSpec = motionScheme.defaultEffectsSpec())
    val exit = fadeOut(animationSpec = motionScheme.fastEffectsSpec())
    if (hasSharedArtwork) return enter togetherWith exit
    return (
        enter + scaleIn(initialScale = 0.97f, animationSpec = motionScheme.defaultSpatialSpec())
        ) togetherWith (
        exit + scaleOut(targetScale = 0.99f, animationSpec = motionScheme.fastSpatialSpec())
        )
}
