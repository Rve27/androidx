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

package androidx.build.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue

class FlaggedApiDetectorTest : LintDetectorTest() {
    override fun getIssues(): List<Issue> = listOf(FlaggedApiDetector.ISSUE)

    override fun getDetector(): Detector {
        return FlaggedApiDetector()
    }

    override fun lint(): TestLintTask {
        return super.lint().allowMissingSdk()
    }

    fun testBasic() {
        // Test case from b/303434307#comment2
        lint()
            .files(
                java(
                    """
          package test.pkg;
          
          import androidx.annotation.ChecksAconfigFlag;

          public final class Flags {
              @ChecksAconfigFlag("test.pkg.myFlag")
              public static boolean myFlag() { return true; }
          }
          """
                ),
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;

            public class JavaTest {
                @FlaggedApi("test.pkg.myFlag")
                class Foo {
                    public void someMethod() { }
                }

                public void testValid1() {
                    if (Flags.myFlag()) {
                        Foo f = new Foo(); // OK 1
                        f.someMethod();    // OK 2
                    }
                }
            }
            """
                    )
                    .indented(),
                Stubs.FlaggedApi,
                Stubs.ChecksAconfigFlag,
            )
            .run()
            .expectClean()
    }

    fun testApiGating() {
        // Test case from b/303434307#comment2
        lint()
            .files(
                java(
                    """
          package test.pkg;
          
          import androidx.annotation.ChecksAconfigFlag;

          public final class Flags {
              @ChecksAconfigFlag("test.pkg.myFlag")
              public static boolean myFlag() { return true; }
          }
          """
                ),
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;

            public class JavaTest {
                interface MyInterface {
                    void bar();
                }

                static class OldImpl implements MyInterface {
                    @Override
                    public void bar() {
                    }
                }

                @FlaggedApi("test.pkg.myFlag")
                static class NewImpl implements MyInterface {
                    @Override
                    public void bar() {
                    }
                 }

                 void test(MyInterface f) {
                     MyInterface obj = null;
                     if (Flags.myFlag()) {
                         obj = new NewImpl();
                     } else {
                         obj = new OldImpl();
                     }
                     f.bar();
                 }
            }
            """
                    )
                    .indented(),
                Stubs.FlaggedApi,
                Stubs.ChecksAconfigFlag,
            )
            .run()
            .expectClean()
    }

    fun testChecksAconfigFlagGating() {
        lint()
            .files(
                java(
                    """
          package test.pkg;

          public final class Flags {
              @ChecksAconfigFlag("test.pkg.myFlag")
              public static boolean myFlag() { return true; }
          }
          """
                ),
                kotlin(
                    """
          package test.pkg

          import androidx.annotation.ChecksAconfigFlag

          object FlagsCompat {
              @ChecksAconfigFlag("test.pkg.myFlag")
              fun myFlag() { return true; }
          }
          """
                ),
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;

            public class JavaTest {
                interface MyInterface {
                    void bar();
                }

                static class OldImpl implements MyInterface {
                    @Override
                    public void bar() {
                    }
                }

                @FlaggedApi("test.pkg.myFlag")
                static class NewImpl implements MyInterface {
                    @Override
                    public void bar() {
                    }
                 }

                 void test(MyInterface f) {
                     MyInterface obj = null;
                     if (FlagsCompat.myFlag()) {
                         obj = new NewImpl();
                     } else {
                         obj = new OldImpl();
                     }
                     f.bar();
                 }
            }
            """
                    )
                    .indented(),
                Stubs.FlaggedApi,
                Stubs.ChecksAconfigFlag,
            )
            .run()
            .expectClean()
    }

    fun testChecksAconfigFlagGating_withWrongFlag_raisesError() {
        lint()
            .files(
                kotlin(
                    """
          package test.pkg

          import androidx.annotation.ChecksAconfigFlag

          object FlagsCompat {
              @ChecksAconfigFlag("test.pkg.myOtherFlag")
              fun myFlag() { return true; }
          }
          """
                ),
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;

            public class JavaTest {
                interface MyInterface {
                    void bar();
                }

                static class OldImpl implements MyInterface {
                    @Override
                    public void bar() {
                    }
                }

                @FlaggedApi("test.pkg.myFlag")
                static class NewImpl implements MyInterface {
                    @Override
                    public void bar() {
                    }
                 }

                 void test(MyInterface f) {
                     MyInterface obj = null;
                     if (FlagsCompat.myFlag()) {
                         obj = new NewImpl();
                     } else {
                         obj = new OldImpl();
                     }
                     f.bar();
                 }
            }
            """
                    )
                    .indented(),
                Stubs.FlaggedApi,
                Stubs.ChecksAconfigFlag,
            )
            .run()
            .expect(
                """
        src/test/pkg/JavaTest.java:26: Error: Constructor for class NewImpl is a flagged API and must be inside a flag check for "test.pkg.myFlag" [AndroidXFlaggedApi]
                     obj = new NewImpl();
                           ~~~~~~~~~~~~~
        1 error
        """
            )
    }

    fun testChecksAconfigFlagGating_withFlaggedDeprecation_isClean() {
        lint()
            .files(
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;

            public class JavaTest {
                interface MyInterface {
                    void bar();
                }

                static class OldImpl implements MyInterface {
                    @Override
                    public void bar() {
                    }
                }

                @FlaggedApi("test.pkg.myFlag")
                @Deprecated
                static class NewImpl implements MyInterface {
                    @Override
                    public void bar() {
                    }
                 }

                 void test(MyInterface f) {
                     MyInterface obj = null;
                     if (FlagsCompat.myFlag()) {
                         obj = new NewImpl();
                     } else {
                         obj = new OldImpl();
                     }
                     f.bar();
                 }
            }
            """
                    )
                    .indented(),
                Stubs.FlaggedApi,
            )
            .run()
            .expectClean()
    }

    fun testFinalFields() {
        // Test case from b/303434307#comment2
        lint()
            .files(
                java(
                    """
          package test.pkg;
          
          import androidx.annotation.ChecksAconfigFlag;

          public final class Flags {
              @ChecksAconfigFlag("test.pkg.myFlag")
              public static boolean myFlag() { return true; }
          }
          """
                ),
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;

            public class JavaTest {
                static class Bar {
                    @FlaggedApi("test.pkg.myFlag")
                    public void bar() { }
                }
                static class Foo {
                    private static final boolean useNewStuff = Flags.myFlag();
                    private final Bar mBar = new Bar();

                    void someMethod() {
                        if (useNewStuff) {
                            // OK because flags can't change value without a reboot, though this might change in
                            // the future and in that case caching the flag value would be an error. We can restart
                            // apps due to a server push of new flag values but restarting the framework would be
                            // too disruptive
                            mBar.bar(); // OK
                        }
                    }
                }
            }
            """
                    )
                    .indented(),
                Stubs.FlaggedApi,
                Stubs.ChecksAconfigFlag,
            )
            .run()
            .expectClean()
    }

    fun testInverseLogic() {
        lint()
            .files(
                java(
                    """
          package test.pkg;
          
          import androidx.annotation.ChecksAconfigFlag;

          public final class Flags {
              @ChecksAconfigFlag("test.pkg.myFlag")
              public static boolean myFlag() { return true; }
          }
          """
                ),
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;

            public class JavaTest {
                @FlaggedApi("test.pkg.myFlag")
                class Foo {
                    public void someMethod() { }
                }

                public void testInverse() {
                    if (!Flags.myFlag()) {
                        // ...
                    } else {
                        Foo f = new Foo(); // OK 1
                        f.someMethod();    // OK 2
                    }
                }
            }
            """
                    )
                    .indented(),
                Stubs.FlaggedApi,
                Stubs.ChecksAconfigFlag,
            )
            .run()
            .expectClean()
    }

    fun testAnded() {
        lint()
            .files(
                java(
                    """
          package test.pkg;
          
          import androidx.annotation.ChecksAconfigFlag;

          public final class Flags {
              @ChecksAconfigFlag("test.pkg.myFlag")
              public static boolean myFlag() { return true; }
          }
          """
                ),
                java(
                        """
            package test.pkg;

            import android.annotation.FlaggedApi;
            import static test.pkg.Flags.myFlag;

            /** @noinspection InstantiationOfUtilityClass, AccessStaticViaInstance , ResultOfMethodCallIgnored , StatementWithEmptyBody */
            public class JavaTest {
                @FlaggedApi("test.pkg.myFlag")
                public static class Foo {
                    public static boolean someMethod() { return true; }
                }

                public void testValid1(boolean something) {
                    if (true && something && Flags.myFlag()) {
                        Foo f = new Foo(); // OK 1
                        f.someMethod();    // OK 2
                    }
                }

                public void testValid2(boolean something) {
                    if (something || !Flags.myFlag()) {
                    } else {
                        Foo f = new Foo(); // OK 3
                        f.someMethod();    // OK 4
                    }
                }

                public void testValid3(Foo f, boolean something) {
                    // b/b/383061307
                    if (Flags.myFlag() && f.someMethod()) { // OK 5
                    }
                    if (myFlag() && f.someMethod()) { // OK 6
                    }
                    if (Flags.myFlag() && something && f.someMethod()) { // OK 7
                    }
                }
            }
            """
                    )
                    .indented(),
                Stubs.FlaggedApi,
                Stubs.ChecksAconfigFlag,
            )
            .run()
            .expectClean()
    }
}
