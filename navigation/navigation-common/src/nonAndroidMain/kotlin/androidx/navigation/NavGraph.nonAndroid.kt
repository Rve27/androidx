/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.navigation

import androidx.annotation.RestrictTo
import androidx.collection.SparseArrayCompat
import androidx.collection.valueIterator
import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmSynthetic
import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer

public actual open class NavGraph actual constructor(navGraphNavigator: Navigator<out NavGraph>) :
    NavDestination(navGraphNavigator), Iterable<NavDestination> {
    public actual val nodes: SparseArrayCompat<NavDestination>
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) get() = implementedInJetBrainsFork()

    private var startDestId = 0
    private var startDestIdName: String? = null

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun matchRouteComprehensive(
        route: String,
        searchChildren: Boolean,
        searchParent: Boolean,
        lastVisited: NavDestination
    ): DeepLinkMatch? {
        implementedInJetBrainsFork()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun matchDeepLinkComprehensive(
        navDeepLinkRequest: NavDeepLinkRequest,
        searchChildren: Boolean,
        searchParent: Boolean,
        lastVisited: NavDestination
    ): DeepLinkMatch? {
        implementedInJetBrainsFork()
    }

    public actual fun addDestination(node: NavDestination) {
        implementedInJetBrainsFork()
    }

    public actual fun addDestinations(nodes: Collection<NavDestination?>) {
        implementedInJetBrainsFork()
    }

    public actual fun addDestinations(vararg nodes: NavDestination) {
        implementedInJetBrainsFork()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun findNodeComprehensive(
        resId: Int,
        lastVisited: NavDestination?,
        searchChildren: Boolean,
        matchingDest: NavDestination?,
    ): NavDestination? {
        // first search direct children
        var destination = nodes[resId]
        when {
            matchingDest != null ->
                // check parent in case of duplicated destinations to ensure it finds the correct
                // nested destination
                if (destination == matchingDest && destination.parent == matchingDest.parent)
                    return destination
                else destination = null
            else -> if (destination != null) return destination
        }

        if (searchChildren) {
            // then dfs through children. Avoid re-visiting children that were recursing up this
            // way.
            destination =
                nodes.valueIterator().asSequence().firstNotNullOfOrNull { child ->
                    if (child is NavGraph && child != lastVisited) {
                        child.findNodeComprehensive(resId, this, true, matchingDest)
                    } else null
                }
        }

        // lastly search through parents. Avoid re-visiting parents that were recursing down
        // this way.
        return destination
            ?: if (parent != null && parent != lastVisited) {
                parent!!.findNodeComprehensive(resId, this, searchChildren, matchingDest)
            } else null
    }

    public actual fun findNode(route: String?): NavDestination? {
        implementedInJetBrainsFork()
    }

    public actual inline fun <reified T> findNode(): NavDestination? {
        implementedInJetBrainsFork()
    }

    public actual fun findNode(route: KClass<*>): NavDestination? {
        implementedInJetBrainsFork()
    }

    public actual fun <T> findNode(route: T?): NavDestination? {
        implementedInJetBrainsFork()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun findNode(route: String, searchParents: Boolean): NavDestination? {
        implementedInJetBrainsFork()
    }

    actual override fun iterator(): MutableIterator<NavDestination> {
        implementedInJetBrainsFork()
    }

    public actual fun addAll(other: NavGraph) {
        implementedInJetBrainsFork()
    }

    public actual fun remove(node: NavDestination) {
        implementedInJetBrainsFork()
    }

    public actual fun clear() {
        implementedInJetBrainsFork()
    }

    /**
     * The starting destination id for this NavGraph. When navigating to the NavGraph, the
     * destination represented by this id is the one the user will initially see.
     */
    public actual var startDestinationId: Int
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) get() = startDestId
        private set(startDestId) {
            require(startDestId != id) {
                "Start destination $startDestId cannot use the same id as the graph $this"
            }
            if (startDestinationRoute != null) {
                startDestinationRoute = null
            }
            this.startDestId = startDestId
            startDestIdName = null
        }

    public actual fun setStartDestination(startDestRoute: String) {
        implementedInJetBrainsFork()
    }

    public actual inline fun <reified T : Any> setStartDestination() {
        implementedInJetBrainsFork()
    }

    @JvmSynthetic
    public actual fun <T : Any> setStartDestination(startDestRoute: KClass<T>) {
        implementedInJetBrainsFork()
    }

    public actual fun <T : Any> setStartDestination(startDestRoute: T) {
        implementedInJetBrainsFork()
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual fun <T> setStartDestination(
        serializer: KSerializer<T>,
        parseRoute: (NavDestination) -> String
    ) {
        implementedInJetBrainsFork()
    }

    public actual var startDestinationRoute: String?
        get() = implementedInJetBrainsFork()
        set(_) {
            implementedInJetBrainsFork()
        }

    public actual val startDestDisplayName: String
        get() = implementedInJetBrainsFork()

    public actual companion object {
        @JvmStatic
        public actual fun NavGraph.findStartDestination(): NavDestination {
            implementedInJetBrainsFork()
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public actual fun NavGraph.childHierarchy(): Sequence<NavDestination> {
            implementedInJetBrainsFork()
        }
    }
}
