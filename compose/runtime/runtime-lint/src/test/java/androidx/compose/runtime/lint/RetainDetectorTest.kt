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

package androidx.compose.runtime.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.bytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RetainDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = RetainDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(RetainDetector.RetainUnitType, RetainDetector.RetainRememberObserver)

    @Test
    fun testRetainUnit() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.retain

                @Composable
                fun Test() {
                    val foo = retain { Unit }
                    val bar = retain<Unit> {  }
                    val baz = retain { noop() }
                }

                fun noop() {}
            """
                ),
                RetainStub,
                Stubs.Composable,
                Stubs.RememberObserver,
                Stubs.RetainObserver,
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/test.kt:9: Error: retain calls must not return Unit. [RetainUnitType]
                    val foo = retain { Unit }
                              ~~~~~~
src/androidx/compose/runtime/foo/test.kt:10: Error: retain calls must not return Unit. [RetainUnitType]
                    val bar = retain<Unit> {  }
                              ~~~~~~
src/androidx/compose/runtime/foo/test.kt:11: Error: retain calls must not return Unit. [RetainUnitType]
                    val baz = retain { noop() }
                              ~~~~~~
3 errors
            """
            )
    }

    @Test
    fun testRetainObject_neitherCallback() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.retain

                @Composable
                fun Test() {
                    val foo = retain { Foo() }
                }

                class Foo
            """
                ),
                RetainStub,
                Stubs.Composable,
                Stubs.RememberObserver,
                Stubs.RetainObserver,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testRetainObject_onlyRememberObserver() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.retain
                import androidx.compose.runtime.RememberObserver

                @Composable
                fun Test() {
                    val foo = retain { Foo() }
                }

                class Foo : RememberObserver {
                    override fun onRemembered() {}
                    override fun onForgotten() {}
                    override fun onAbandoned() {}
                }
            """
                ),
                RetainStub,
                Stubs.Composable,
                Stubs.RememberObserver,
                Stubs.RetainObserver,
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/Foo.kt:10: Error: Declared retained type androidx.compose.runtime.foo.Foo implements RememberObserver but not RetainObserver. [RetainRememberObserver]
                    val foo = retain { Foo() }
                              ~~~~~~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun testRetainObject_rememberAndRetainObserver() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.retain
                import androidx.compose.runtime.RememberObserver
                import androidx.compose.runtime.RetainObserver

                @Composable
                fun Test() {
                    val foo = retain { Foo() }
                }

                class Foo : RememberObserver, RetainObserver {
                    override fun onRemembered() {}
                    override fun onForgotten() {}
                    override fun onAbandoned() {}
                    override fun onRetained() {}
                    override fun onEnteredComposition() {}
                    override fun onExitedComposition() {}
                    override fun onRetired() {}
                }
            """
                ),
                RetainStub,
                Stubs.Composable,
                Stubs.RememberObserver,
                Stubs.RetainObserver,
            )
            .run()
            .expectClean()
    }

    @Test
    fun testRetainObject_onlyRetainObserver() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.retain
                import androidx.compose.runtime.RememberObserver
                import androidx.compose.runtime.RetainObserver

                @Composable
                fun Test() {
                    val foo = retain { Foo() }
                }

                class Foo : RetainObserver {
                    override fun onRetained() {}
                    override fun onEnteredComposition() {}
                    override fun onExitedComposition() {}
                    override fun onRetired() {}
                    override fun onAbandoned() {}
                }
            """
                ),
                RetainStub,
                Stubs.Composable,
                Stubs.RememberObserver,
                Stubs.RetainObserver,
            )
            .run()
            .expectClean()
    }

    companion object {
        val RetainStub: TestFile =
            bytecodeStub(
                filename = "Retain.kt",
                filepath = "androidx/compose/runtime",
                checksum = 0xcdc5d7aa,
                source =
                    """
        package androidx.compose.runtime

        import androidx.compose.runtime.Composable

        @Composable
        inline fun <reified T> retain(
            noinline calculation: () -> T
        ): T {
            return retain(
                typeHash = T::class.hashCode(),
                calculation = calculation
            )
        }

        @Composable
        inline fun <reified T> retain(
            vararg keys: Any?,
            noinline calculation: () -> T
        ): T {
            return retain(
                typeHash = T::class.hashCode(),
                keys = keys,
                calculation = calculation
            )
        }

        @PublishedApi
        @Composable
        internal fun <T> retain(typeHash: Int, vararg keys: Any?, calculation: () -> T): T {
            return calculation()
        }
        """,
                """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAA/2NgYGBmYGBgAmJGBijgUuGSSMxLKcrPTKnQS87PLcgvTtUr
                Ks0rycxNFeIISi1JzMzzLuFS5ZLBpUovLT9fiNUtP9+7RIlBiwEA9+K1PWUA
                AAA=
                """,
                """
                androidx/compose/runtime/RetainKt.class:
                H4sIAAAAAAAA/61V21IbRxA9sxLSaqWAEAaDsAkXEetCvDIhNwtIFBLMBnEJ
                KLxQeVikBRatdqndFWXeqLzkG/KaL8ijnKq4KPKWn8kfpNIzkgwCEZdTqdJO
                z3T3nD7T09P68+/fXgNYQJlhSrerrmNWX6oVp37qeIbqNmzfrBvqjuHrpr3u
                h8EY4if6ma5aun2kbh2cGBXSBhhCrvBhWEiXao5vmbZ6clZXDxt2xTcd21NX
                27N8IVO6jVBgKC6Wn9/VL78NbDFXLheWCxkaGWZL955gRaz1A8sgv2hFtyoN
                S+cIMiIMEzeCmLZvuLZuqZrtu6btmRUvjCjDcOXYqNQ2HX+zYVnbuqvXDXJk
                eJK+S/uGZpeDHBUyezG8h34FMQwwsLKMQYYR1zAPTaO6dWq4gsyG7tY45kha
                6w0xhAccYvgeyjvGoWWIxITxkGCODH/LXXEN3TfWhfuKpXseQ/YmZ6GjS2kD
                ui0Mdb2lj2EMSQWjGKcU9HQJ4zGDfKx7xytO1WAIpDNaDO9jMooJTDGspbX9
                Hil69yKJoQ8zCiSk6ApTZuow1ak5pjFMvg2R4UX6/yHCUL6nWt8dv6uAgzXj
                3JOR6zyyhm9aatF19XPK8of0yCrO6fnWIUOmVyQt00MZg4q8gqd4xjDUw87w
                /T1n+Q+31nWY4Y7zduPAMr1jo1o8NUk/U3LcI/XE8A9cujxP1W3b8fUWUvt5
                Fbrru0ekMD6ldJj2mVOjknuQ7lkun+N5FJ+B0GT//NRYoxJlGOzQ2qDiqeq+
                TmapfhagLijxgfEBVFM10r80+SpPsyrl76/LiwlFkgOKNCqRpC8+SN/lRXsh
                K5cXyQytk9Iamw7JlxdxNsrm+2U5LiXlRDBB6nxg7eon+Y8mu7y4+iUkxYPJ
                4p0NI/G+ZCwhyywRHGX5cD40LeS/A8nXQEEBNB+KR5JSXrkXLtYFxgRK9OpH
                Kaz0yVc/z+cZP/Y8Q6KTsZtviTIUaf0tPK35VLmttz9QMm1js1E/MNwyb7Z8
                s0Pddk93Tb5uKyO75pGt+w2X5uM7rRat2WemZ5K5eF0PDKnb1jedt8tN2XUa
                bsVYNTn6WHvP3h08PKPuEeT3i0B8jNpJCAGUaPUDSYnkVDahNBEPLIYTiSZG
                EsEmHr3CNDW13xHMNjH7Ky8TbNDYT1uGEMUj6sePoWCTdCNkk6lhfoAnlB+C
                QwRpkltiVxjbJENkkQERPtMO75HkhZbJJuYofK5FYuk2iWz2dRPzuS4aCTrP
                GAWewEOkqE3PYlhQmSTAfowLKvxkGeTwkTh5RpBibVLZm6TASc0QIif1HVk5
                qaGcIDUnSM29wiLDdXi+kTf7TkhyxxKFhJjxkEzMeEipHXJBhOSN/GOSsggF
                DIjHh0/ejBJ2xLiOXZFChmVi98U+Ahq+1FDU8BVWaIqvNXyD1X0wDy+wto+Y
                hz4Pgx4iHjQPIQ85D9+KX8bDkpjM/ANVtVgb+QgAAA==
                """,
            )
    }
}
