/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.navigation3.ui

import androidx.collection.mutableObjectFloatMapOf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEachReversed
import androidx.compose.ui.util.fastMap
import androidx.navigation3.runtime.LocalEntriesToRenderInCurrentScene
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.OverlayScene
import androidx.navigation3.runtime.Scene
import androidx.navigation3.runtime.SceneStrategy
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.runtime.rememberSceneSetupNavEntryDecorator
import androidx.navigation3.ui.NavDisplay.DEFAULT_TRANSITION_DURATION_MILLISECOND
import androidx.navigation3.ui.NavDisplay.POP_TRANSITION_SPEC
import androidx.navigation3.ui.NavDisplay.PREDICTIVE_POP_TRANSITION_SPEC
import androidx.navigation3.ui.NavDisplay.TRANSITION_SPEC
import androidx.navigation3.ui.NavDisplay.popTransitionSpec
import androidx.navigation3.ui.NavDisplay.predictivePopTransitionSpec
import androidx.navigation3.ui.NavDisplay.transitionSpec
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventTransitionState.Idle
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlin.reflect.KClass
import kotlinx.coroutines.launch

/** Object that indicates the features that can be handled by the [NavDisplay] */
public object NavDisplay {
    /**
     * Function to be called on the [NavEntry.metadata] to notify the [NavDisplay] that the content
     * should be animated using the provided [ContentTransform].
     *
     * @param transitionSpec the [ContentTransform] to be used when adding to the backstack. If this
     *   is null, the transition will fallback to the transition set on the [NavDisplay]
     */
    public fun transitionSpec(
        transitionSpec: AnimatedContentTransitionScope<Scene<*>>.() -> ContentTransform?
    ): Map<String, Any> = mapOf(TRANSITION_SPEC to transitionSpec)

    /**
     * Function to be called on the [NavEntry.metadata] to notify the [NavDisplay] that, when
     * popping from backstack, the content should be animated using the provided [ContentTransform].
     *
     * @param popTransitionSpec the [ContentTransform] to be used when popping from backstack. If
     *   this is null, the transition will fallback to the transition set on the [NavDisplay]
     */
    public fun popTransitionSpec(
        popTransitionSpec: AnimatedContentTransitionScope<Scene<*>>.() -> ContentTransform?
    ): Map<String, Any> = mapOf(POP_TRANSITION_SPEC to popTransitionSpec)

    /**
     * Function to be called on the [NavEntry.metadata] to notify the [NavDisplay] that, when
     * popping from backstack using a Predictive back gesture, the content should be animated using
     * the provided [ContentTransform].
     *
     * @param predictivePopTransitionSpec the [ContentTransform] to be used when popping from
     *   backStack with predictive back gesture. If this is null, the transition will fallback to
     *   the transition set on the [NavDisplay]
     */
    public fun predictivePopTransitionSpec(
        predictivePopTransitionSpec:
            AnimatedContentTransitionScope<Scene<*>>.(
                @NavigationEvent.SwipeEdge Int
            ) -> ContentTransform?
    ): Map<String, Any> = mapOf(PREDICTIVE_POP_TRANSITION_SPEC to predictivePopTransitionSpec)

    public val defaultPredictivePopTransitionSpec:
        AnimatedContentTransitionScope<Scene<*>>.(
            @NavigationEvent.SwipeEdge Int
        ) -> ContentTransform =
        {
            ContentTransform(
                fadeIn(
                    spring(
                        dampingRatio = 1.0f, // reflects material3 motionScheme.defaultEffectsSpec()
                        stiffness = 1600.0f, // reflects material3 motionScheme.defaultEffectsSpec()
                    )
                ),
                scaleOut(targetScale = 0.7f),
            )
        }

    internal const val TRANSITION_SPEC = "transitionSpec"
    internal const val POP_TRANSITION_SPEC = "popTransitionSpec"
    internal const val PREDICTIVE_POP_TRANSITION_SPEC = "predictivePopTransitionSpec"

    internal const val DEFAULT_TRANSITION_DURATION_MILLISECOND = 700
}

/**
 * A nav display that renders and animates between different [Scene]s, each of which can render one
 * or more [NavEntry]s.
 *
 * The [Scene]s are calculated with the given [SceneStrategy], which may be an assembled delegated
 * chain of [SceneStrategy]s. If no [Scene] is calculated, the fallback will be to a
 * [SinglePaneSceneStrategy].
 *
 * It is allowable for different [Scene]s to render the same [NavEntry]s, perhaps on some conditions
 * as determined by the [sceneStrategy] based on window size, form factor, other arbitrary logic.
 *
 * If this happens, and these [Scene]s are rendered at the same time due to animation or predictive
 * back, then the content for the [NavEntry] will only be rendered in the most recent [Scene] that
 * is the target for being the current scene as determined by [sceneStrategy]. This enforces a
 * unique invocation of each [NavEntry], even if it is displayable by two different [Scene]s.
 *
 * @param backStack the collection of keys that represents the state that needs to be handled
 * @param modifier the modifier to be applied to the layout.
 * @param contentAlignment The [Alignment] of the [AnimatedContent]
 * @param onBack a callback for handling system back press. The passed [Int] refers to the number of
 *   entries to pop from the end of the backstack, as calculated by the [sceneStrategy].
 * @param entryDecorators list of [NavEntryDecorator] to add information to the entry content
 * @param sceneStrategy the [SceneStrategy] to determine which scene to render a list of entries.
 * @param sizeTransform the [SizeTransform] for the [AnimatedContent].
 * @param transitionSpec Default [ContentTransform] when navigating to [NavEntry]s.
 * @param popTransitionSpec Default [ContentTransform] when popping [NavEntry]s.
 * @param predictivePopTransitionSpec Default [ContentTransform] when popping with predictive back
 *   [NavEntry]s.
 * @param entryProvider lambda used to construct each possible [NavEntry]
 * @sample androidx.navigation3.ui.samples.SceneNav
 * @sample androidx.navigation3.ui.samples.SceneNavSharedEntrySample
 * @sample androidx.navigation3.ui.samples.SceneNavSharedElementSample
 */
@Composable
public fun <T : Any> NavDisplay(
    backStack: List<T>,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    onBack: (Int) -> Unit = {
        if (backStack is MutableList<T>) {
            repeat(it) { backStack.removeAt(backStack.lastIndex) }
        }
    },
    entryDecorators: List<NavEntryDecorator<T>> =
        listOf(rememberSceneSetupNavEntryDecorator(), rememberSavedStateNavEntryDecorator()),
    sceneStrategy: SceneStrategy<T> = SinglePaneSceneStrategy(),
    sizeTransform: SizeTransform? = null,
    transitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        ContentTransform(
            fadeIn(animationSpec = tween(DEFAULT_TRANSITION_DURATION_MILLISECOND)),
            fadeOut(animationSpec = tween(DEFAULT_TRANSITION_DURATION_MILLISECOND)),
        )
    },
    popTransitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        ContentTransform(
            fadeIn(animationSpec = tween(DEFAULT_TRANSITION_DURATION_MILLISECOND)),
            fadeOut(animationSpec = tween(DEFAULT_TRANSITION_DURATION_MILLISECOND)),
        )
    },
    @Suppress("UNCHECKED_CAST")
    predictivePopTransitionSpec:
        AnimatedContentTransitionScope<Scene<T>>.(
            @NavigationEvent.SwipeEdge Int
        ) -> ContentTransform =
        NavDisplay.defaultPredictivePopTransitionSpec
            as
            AnimatedContentTransitionScope<Scene<T>>.(
                @NavigationEvent.SwipeEdge Int
            ) -> ContentTransform,
    entryProvider: (key: T) -> NavEntry<T>,
) {
    require(backStack.isNotEmpty()) { "NavDisplay backstack cannot be empty" }

    val transitionAwareLifecycleNavEntryDecorator =
        rememberTransitionAwareLifecycleNavEntryDecorator(backStack)

    val entries =
        rememberDecoratedNavEntries(
            backStack = backStack,
            entryDecorators = entryDecorators + transitionAwareLifecycleNavEntryDecorator,
            entryProvider = entryProvider,
        )
    val allScenes =
        mutableListOf(sceneStrategy.calculateSceneWithSinglePaneFallback(entries, onBack))
    do {
        val overlayScene = allScenes.last() as? OverlayScene
        val overlaidEntries = overlayScene?.overlaidEntries
        if (overlaidEntries != null) {
            // TODO Consider allowing a NavDisplay of only OverlayScene instances
            require(overlaidEntries.isNotEmpty()) {
                "Overlaid entries from $overlayScene must not be empty"
            }
            allScenes += sceneStrategy.calculateSceneWithSinglePaneFallback(overlaidEntries, onBack)
        }
    } while (overlaidEntries != null)
    val overlayScenes = allScenes.dropLast(1)
    val scene =
        remember(backStack.map { it }, entryDecorators.map { it }, sceneStrategy, onBack) {
            allScenes.last()
        }

    // Predictive Back Handling
    // TODO(mgalhardo): Replace with SceneController API once available. It will expose "back
    //  visible entries" directly, so this manual calculation can be removed.
    val previousVisibleEntries =
        if (scene.previousEntries.isEmpty()) {
            // Avoid calling calculateScene on an empty list (it throws).
            emptyList()
        } else {
            sceneStrategy
                .calculateSceneWithSinglePaneFallback(scene.previousEntries, onBack)
                .entries // From previousEntries passed into the calculation.
                .fastMap { it.contentKey }
        }
    val gestureState =
        rememberNavigationEventState(
            currentInfo = NavDisplayInfo(scene.entries.fastMap { it.contentKey }),
            backInfo = listOf(NavDisplayInfo(visibleEntries = previousVisibleEntries)),
        )
    val gestureTransition = gestureState.transitionState

    val inPredictiveBack = gestureTransition is InProgress
    val progress =
        when (gestureTransition) {
            is Idle -> 0f
            is InProgress -> gestureTransition.latestEvent.progress
        }
    val swipeEdge =
        when (gestureTransition) {
            is Idle -> NavigationEvent.EDGE_NONE
            is InProgress -> gestureTransition.latestEvent.swipeEdge
        }

    NavigationBackHandler(
        state = gestureState,
        isBackEnabled = scene.previousEntries.isNotEmpty(),
        onBackCompleted = {
            // If `enabled` becomes stale (e.g., it was set to false but a gesture was
            // dispatched in the same frame), this ensures that the calculated index is valid
            // before calling onBack, avoiding IndexOutOfBoundsException in edge cases.
            if (entries.size > scene.previousEntries.size) {
                onBack(entries.size - scene.previousEntries.size)
            }
        },
    )

    val transitionState = remember {
        // The state returned here cannot be nullable cause it produces the input of the
        // transitionSpec passed into the AnimatedContent and that must match the non-nullable
        // scope exposed by the transitions on the NavHost and composable APIs.
        SeekableTransitionState(scene)
    }

    val transition = rememberTransition(transitionState, label = "scene")

    // Transition Handling
    /** Keep track of the previous entries for the transition's current scene. */
    val transitionCurrentStateEntries = remember(transition.currentState) { entries.toList() }

    // Consider this a pop if the current entries match the previous entries we have recorded
    // from the current state of the transition
    val isPop =
        isPop(transitionCurrentStateEntries.map { it.contentKey }, entries.map { it.contentKey })

    val zIndices = remember { mutableObjectFloatMapOf<Pair<KClass<*>, Any>>() }
    val initialKey = transition.currentState::class to transition.currentState.key
    val targetKey = transition.targetState::class to transition.targetState.key
    val initialZIndex = zIndices.getOrPut(initialKey) { 0f }
    val targetZIndex =
        when {
            initialKey == targetKey -> initialZIndex
            isPop || inPredictiveBack -> initialZIndex - 1f
            else -> initialZIndex + 1f
        }
    zIndices[targetKey] = targetZIndex

    // Determine which entries should be rendered within each scene,
    // using the z-index of each screen to always show the entry on the topmost screen
    // The map is Pair<KCLass<Scene<T>, Scene.key> to a Set of NavEntry.key values
    val sceneToRenderableEntryMap =
        remember(
            transition.currentState,
            transition.targetState,
            overlayScenes.toList(),
            initialZIndex,
            targetZIndex,
        ) {
            buildMap {
                // First sort the scenes in the AnimatedContent by z-order
                val scenes =
                    if (transition.currentState == transition.targetState) {
                        listOf(transition.currentState)
                    } else if (initialZIndex < targetZIndex) {
                        listOf(transition.currentState, transition.targetState)
                    } else {
                        listOf(transition.targetState, transition.currentState)
                    }
                // Then combine them with the overlay scenes
                // to get the complete order of scenes in z-order
                val scenesInZOrder = scenes + overlayScenes
                // Track which entries are already covered
                val coveredEntryKeys = mutableSetOf<Any>()
                // Now in reverse order, go through each scene, marking
                // all of the entries not already covered as associated
                // with that scene
                scenesInZOrder.fastForEachReversed { scene ->
                    val newlyCoveredEntryKeys =
                        scene.entries
                            .map { it.contentKey }
                            .filterNot(coveredEntryKeys::contains)
                            .toSet()
                    put(scene::class to scene.key, newlyCoveredEntryKeys)
                    coveredEntryKeys.addAll(newlyCoveredEntryKeys)
                }
            }
        }

    val transitionEntry =
        if (initialZIndex >= targetZIndex) {
            transition.currentState.entries.last()
        } else {
            transition.targetState.entries.last()
        }

    // TODO(b/441933162): During predictive back, only build a peek scene when there is a
    //  previous entry. Empty previousEntries used to crash calculateScene(...).
    if (inPredictiveBack && scene.previousEntries.isNotEmpty()) {
        val peekScene =
            sceneStrategy.calculateSceneWithSinglePaneFallback(scene.previousEntries, onBack)
        if (
            transition.currentState::class != peekScene::class ||
                transition.currentState.key != peekScene.key
        ) {
            LaunchedEffect(peekScene::class, peekScene.key, progress) {
                // Retarget on key change; seek on progress updates.
                transitionState.seekTo(progress, peekScene)
            }
        }
    } else if (transitionState.fraction != 0f) {
        // Predictive Back has either been completed or cancelled
        // so now we need to seekTo+snapTo the final state
        LaunchedEffect(scene::class, scene.key) {
            // convert from nanoseconds to milliseconds
            val totalDuration = transition.totalDurationNanos / 1000000
            // Which way we have to seek depends on whether the
            // Predictive Back was completed or cancelled
            val predictiveBackCompleted = transition.targetState == scene
            val (finalFraction, remainingDuration) =
                if (predictiveBackCompleted) {
                    // If it completed, animate to the state we were
                    // already seeking to with the remaining duration
                    1f to ((1f - transitionState.fraction) * totalDuration).toInt()
                } else {
                    // It it got cancelled, animate back to the
                    // initial state, reversing what we seeked to
                    0f to (transitionState.fraction * totalDuration).toInt()
                }
            animate(
                transitionState.fraction,
                finalFraction,
                animationSpec = tween(remainingDuration),
            ) { value, _ ->
                this@LaunchedEffect.launch {
                    if (value != finalFraction) {
                        // Seek the transition towards the finalFraction
                        transitionState.seekTo(value)
                    }
                    if (value == finalFraction) {
                        // Once the animation finishes, we need to snap to the right state.
                        transitionState.snapTo(scene)
                    }
                }
            }
        }
    } else if (
        transitionState.currentState::class != scene::class ||
            transitionState.currentState.key != scene.key
    ) {
        // We are animating to the final state
        LaunchedEffect(scene::class, scene.key) { transitionState.animateTo(scene) }
    }

    val contentTransform: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        when {
            inPredictiveBack -> {
                transitionEntry.predictivePopSpec()?.invoke(this, swipeEdge)
                    ?: predictivePopTransitionSpec(swipeEdge)
            }
            isPop -> {
                transitionEntry.contentTransform(POP_TRANSITION_SPEC)?.invoke(this)
                    ?: popTransitionSpec(this)
            }
            else -> {
                transitionEntry.contentTransform(TRANSITION_SPEC)?.invoke(this)
                    ?: transitionSpec(this)
            }
        }
    }

    transition.AnimatedContent(
        contentKey = { scene -> scene::class to scene.key },
        contentAlignment = contentAlignment,
        modifier = modifier,
        transitionSpec = {
            ContentTransform(
                targetContentEnter = contentTransform(this).targetContentEnter,
                initialContentExit = contentTransform(this).initialContentExit,
                // z-index increases during navigate and decreases during pop.
                targetContentZIndex = targetZIndex,
                sizeTransform = sizeTransform,
            )
        },
    ) { targetScene ->
        val isSettled = transition.currentState == transition.targetState
        CompositionLocalProvider(
            LocalNavTransitionSettledState provides isSettled,
            LocalNavAnimatedContentScope provides this,
            LocalEntriesToRenderInCurrentScene provides
                sceneToRenderableEntryMap.getValue(targetScene::class to targetScene.key),
        ) {
            targetScene.content()
        }
    }

    // Show all OverlayScene instances above the AnimatedContent
    overlayScenes.fastForEachReversed { overlayScene ->
        CompositionLocalProvider(
            LocalEntriesToRenderInCurrentScene provides
                sceneToRenderableEntryMap.getValue(overlayScene::class to overlayScene.key)
        ) {
            overlayScene.content.invoke()
        }
    }
}

private fun <T : Any> isPop(oldBackStack: List<T>, newBackStack: List<T>): Boolean {
    // entire stack replaced
    if (oldBackStack.first() != newBackStack.first()) return false
    // navigated
    if (newBackStack.size > oldBackStack.size) return false

    val divergingIndex =
        newBackStack.indices.firstOrNull { index -> newBackStack[index] != oldBackStack[index] }
    // if newBackStack never diverged from oldBackStack, then it is a clean subset of the oldStack
    // and is a pop
    return divergingIndex == null && newBackStack.size != oldBackStack.size
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> NavEntry<T>.contentTransform(
    key: String
): (AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform)? {
    return metadata[key] as? AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> NavEntry<T>.predictivePopSpec():
    (AnimatedContentTransitionScope<Scene<T>>.(
        @NavigationEvent.SwipeEdge Int
    ) -> ContentTransform)? {
    return metadata[PREDICTIVE_POP_TRANSITION_SPEC]
        as?
        AnimatedContentTransitionScope<Scene<T>>.(
            @NavigationEvent.SwipeEdge Int
        ) -> ContentTransform
}
