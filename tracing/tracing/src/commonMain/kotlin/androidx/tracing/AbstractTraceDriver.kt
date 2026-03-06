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

package androidx.tracing

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope

/** The entry point for the tracing API. */
public open class AbstractTraceDriver
internal constructor(@get:RestrictTo(Scope.LIBRARY_GROUP) public val context: TraceContext) :
    AutoCloseable {
    /**
     * Builds an instance of [AbstractTraceDriver] using the provided [AbstractTraceSink] if
     * `isEnabled` is `true`. Otherwise, you get an instance of a no-op [AbstractTraceDriver].
     */
    public constructor(
        sink: AbstractTraceSink,
        isEnabled: Boolean,
    ) : this(
        context =
            if (isEnabled) {
                TraceContext(sink = sink, isEnabled = isEnabled)
            } else {
                EmptyTraceContext
            }
    )

    /** Return an instance of a [Tracer] that can be used to emit trace events. */
    public open val tracer: Tracer by
        lazy(mode = LazyThreadSafetyMode.PUBLICATION) { context.createTracer() }

    /** Flushes the trace packets into the underlying [AbstractTraceSink]. */
    public open fun flush() {
        context.flush()
    }

    /**
     * Flushes all outstanding packets to the [AbstractTraceSink] and then closes the
     * [AbstractTraceSink].
     */
    public override fun close() {
        context.close()
    }
}
