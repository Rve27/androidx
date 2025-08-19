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

@file:Suppress("UnstableApiUsage")

package androidx.compose.runtime.lint

import androidx.compose.lint.Names
import androidx.compose.lint.inheritsFrom
import androidx.compose.lint.isInPackageName
import androidx.compose.lint.isVoidOrUnit
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import java.util.EnumSet
import org.jetbrains.uast.UCallExpression

class RetainDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames() = listOf(Names.Runtime.Retain.shortName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!method.isInPackageName(Names.Runtime.PackageName)) return
        val callExpressionType = node.getExpressionType()

        if (callExpressionType.isVoidOrUnit && RememberDetector.isReallyUnit(node, method)) {
            context.report(
                issue = RetainUnitType,
                scope = node,
                location = context.getNameLocation(node),
                message = "`retain` calls must not return `Unit`.",
                quickfixData = null,
            )
        }

        if (callExpressionType?.isNotRetainable() == true) {
            context.report(
                issue = RetainRememberObserver,
                scope = node,
                location = context.getNameLocation(node),
                message =
                    "Declared retained type `${callExpressionType.canonicalText}` implements " +
                        "`RememberObserver` but not `RetainObserver`.",
                quickfixData = null,
            )
        }
    }

    private fun PsiType.isNotRetainable(): Boolean {
        val isRememberObserver = inheritsFrom(Names.Runtime.RememberObserver)
        val isRetainObserver = inheritsFrom(Names.Runtime.RetainObserver)

        return isRememberObserver && !isRetainObserver
    }

    companion object {
        val RetainUnitType =
            Issue.create(
                id = "RetainUnitType",
                briefDescription = "`retain` calls must not return `Unit`",
                explanation =
                    "A call to `retain` that returns `Unit` is always an error. This typically " +
                        "happens when using `retain` to perform an action or mutate variables " +
                        "on an object. Instead, use `SideEffect` (or `RetainedEffect`) to make " +
                        "deferred changes once the composition succeeds, or mutate " +
                        "`MutableState` backed variables directly, as these will handle " +
                        "composition failure for you.",
                category = Category.CORRECTNESS,
                priority = 3,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        RetainDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                    ),
            )

        val RetainRememberObserver =
            Issue.create(
                id = "RetainRememberObserver",
                briefDescription =
                    "Values returned by `retain { ... }` must not implement RememberObserver " +
                        "unless they also implement RetainObserver.",
                explanation =
                    "Objects that implement RememberObserver and not RetainObserver are unaware " +
                        "of the retainment lifecycle. They cannot be correctly retained because " +
                        "there is no valid way to dispatch the RememberObserver callbacks and " +
                        "are therefore prohibited as return values to the `calculation` lambda " +
                        "of `retain`. Attempting to retain a value that implements " +
                        "`RememberObserver` without also implementing `RetainObserver` will " +
                        "throw an exception. Either remember the value instead of retaining it, " +
                        "or implement RetainObserver on the object." +
                        "\n\nNote that this inspection checks the statically declared return " +
                        "type to ensure it does not declare itself as implementing " +
                        "`RememberObserver` without also implementing `RetainObserver`. The " +
                        "actual runtime types are not checked, which may lead to false negatives " +
                        "or false positives if the `calculation` lambda returns a different type " +
                        "than the call to `retain`.",
                category = Category.CORRECTNESS,
                priority = 3,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        RetainDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
                    ),
            )
    }
}
