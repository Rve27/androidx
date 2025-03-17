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

package androidx.compose.foundation.textfield

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.internal.readText
import androidx.compose.foundation.internal.toClipEntry
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.FocusedWindowTest
import androidx.compose.foundation.text.TextContextMenuItems
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys.AutofillKey
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys.CopyKey
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys.CutKey
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys.PasteKey
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys.SelectAllKey
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.test.TestTextContextMenuDataInvoker
import androidx.compose.foundation.text.contextmenu.test.assertItems
import androidx.compose.foundation.text.contextmenu.test.testTextContextMenuDataReader
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.internal.selection.FakeClipboard
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class TextFieldContextMenuBuilderTest : FocusedWindowTest {
    @get:Rule val rule = createComposeRule()

    private val textFieldTag = "BTF"

    private var initialIsNewContextMenuEnabled = false

    @OptIn(ExperimentalFoundationApi::class)
    @Before
    fun setup() {
        initialIsNewContextMenuEnabled = ComposeFoundationFlags.isNewContextMenuEnabled
        ComposeFoundationFlags.isNewContextMenuEnabled = true
    }

    @OptIn(ExperimentalFoundationApi::class)
    @After
    fun cleanup() {
        ComposeFoundationFlags.isNewContextMenuEnabled = initialIsNewContextMenuEnabled
    }

    private enum class SelectionAmount {
        NONE,
        PARTIAL,
        ALL
    }

    // region BTF2 Context Menu Item Action Tests
    @Test
    fun btf2_contextMenu_whenMouse_onClickCut() = runTest {
        runBtf2ClickContextMenuItemTest(
            isMouse = true,
            expectedSessionClosed = true,
            itemToInvoke = TextContextMenuItems.Cut,
            expectedText = "Text  Text",
            expectedSelection = TextRange(5),
            expectedClipboardContent = "Text",
        )
    }

    @Test
    fun btf2_contextMenu_whenMouse_onClickCopy() = runTest {
        runBtf2ClickContextMenuItemTest(
            isMouse = true,
            expectedSessionClosed = true,
            itemToInvoke = TextContextMenuItems.Copy,
            expectedText = "Text Text Text",
            expectedSelection = TextRange(5, 9),
            expectedClipboardContent = "Text",
        )
    }

    @Test
    fun btf2_contextMenu_whenMouse_onClickPaste() = runTest {
        runBtf2ClickContextMenuItemTest(
            isMouse = true,
            expectedSessionClosed = true,
            itemToInvoke = TextContextMenuItems.Paste,
            expectedText = "Text clip Text",
            expectedSelection = TextRange(9),
            expectedClipboardContent = "clip",
        )
    }

    @Test
    fun btf2_contextMenu_whenMouse_onClickSelectAll() = runTest {
        runBtf2ClickContextMenuItemTest(
            isMouse = true,
            expectedSessionClosed = true,
            itemToInvoke = TextContextMenuItems.SelectAll,
            expectedText = "Text Text Text",
            expectedSelection = TextRange(0, 14),
            expectedClipboardContent = "clip",
        )
    }

    @Test
    fun btf2_contextMenu_whenTouch_onClickCut() = runTest {
        runBtf2ClickContextMenuItemTest(
            isMouse = false,
            expectedSessionClosed = true,
            itemToInvoke = TextContextMenuItems.Cut,
            expectedText = "Text  Text",
            expectedSelection = TextRange(5),
            expectedClipboardContent = "Text",
        )
    }

    @Test
    fun btf2_contextMenu_whenTouch_onClickCopy() = runTest {
        runBtf2ClickContextMenuItemTest(
            isMouse = false,
            expectedSessionClosed = true,
            itemToInvoke = TextContextMenuItems.Copy,
            expectedText = "Text Text Text",
            expectedSelection = TextRange(9),
            expectedClipboardContent = "Text",
        )
    }

    @Test
    fun btf2_contextMenu_whenTouch_onClickPaste() = runTest {
        runBtf2ClickContextMenuItemTest(
            isMouse = false,
            expectedSessionClosed = true,
            itemToInvoke = TextContextMenuItems.Paste,
            expectedText = "Text clip Text",
            expectedSelection = TextRange(9),
            expectedClipboardContent = "clip",
        )
    }

    @Test
    fun btf2_contextMenu_whenTouch_onClickSelectAll() = runTest {
        runBtf2ClickContextMenuItemTest(
            isMouse = false,
            expectedSessionClosed = false,
            itemToInvoke = TextContextMenuItems.SelectAll,
            expectedText = "Text Text Text",
            expectedSelection = TextRange(0, 14),
            expectedClipboardContent = "clip",
        )
    }

    private suspend fun runBtf2ClickContextMenuItemTest(
        isMouse: Boolean,
        expectedSessionClosed: Boolean,
        itemToInvoke: TextContextMenuItems,
        expectedText: String,
        expectedSelection: TextRange,
        expectedClipboardContent: String,
    ) {
        val initialText = "Text Text Text"
        val initialClipboardText = "clip"

        val state = TextFieldState(initialText)

        val clipboard =
            FakeClipboard(
                initialText = initialClipboardText,
                supportsClipEntry = true,
            )

        var sessionClosed = false
        val fakeSession =
            object : TextContextMenuSession {
                override fun close() {
                    sessionClosed = true
                }
            }

        val reader = TestTextContextMenuDataInvoker()
        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                BasicTextField(
                    state = state,
                    modifier = Modifier.testTag(textFieldTag),
                    decorator = { innerTextField ->
                        Box(
                            propagateMinConstraints = true,
                            modifier = Modifier.testTextContextMenuDataReader(reader)
                        ) {
                            innerTextField()
                        }
                    }
                )
            }
        }

        // start selection of middle word
        if (isMouse) {
            rule.onNodeWithTag(textFieldTag).performMouseInput { repeat(2) { click() } }
        } else {
            rule.onNodeWithTag(textFieldTag).performTouchInput { longClick() }
        }

        // collect our context menu items.
        val data = reader.invokeTraversal()

        // get and verify component
        val component = data.components.singleOrNull { it.key == itemToInvoke.key }
        assertWithMessage("Component with key %s not found", itemToInvoke.key)
            .that(component)
            .isNotNull()
        assertThat(component).isInstanceOf(TextContextMenuItem::class.java)
        val item = component as TextContextMenuItem

        // simulate clicking the item.
        rule.runOnUiThread { item.onClick(fakeSession) }

        // verify whether close called
        if (expectedSessionClosed) {
            assertThat(sessionClosed).isTrue()
        } else {
            assertThat(sessionClosed).isFalse()
        }

        // Operation was applied
        assertThat(state.text).isEqualTo(expectedText)
        assertThat(state.selection).isEqualTo(expectedSelection)
        val clipboardContent = clipboard.getClipEntry()?.readText()
        assertThat(clipboardContent).isNotNull()
        assertThat(clipboardContent!!).isEqualTo(expectedClipboardContent)
    }

    // endregion BTF2 Context Menu Item Action Tests

    // region BTF2 Context Menu Correct Item Tests
    @Test
    @SdkSuppress(maxSdkVersion = 25)
    fun btf2_contextMenu_emptyClipboard_noSelection_itemsMatch_beforeApi26() =
        runBtf2CorrectItemsTest(
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.NONE,
            expectedItems = listOf(SelectAllKey),
        )

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun btf2_contextMenu_emptyClipboard_noSelection_itemsMatch_afterApi26() =
        runBtf2CorrectItemsTest(
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.NONE,
            expectedItems = listOf(SelectAllKey, AutofillKey),
        )

    @Test
    fun btf2_contextMenu_emptyClipboard_partialSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.PARTIAL,
            expectedItems = listOf(CutKey, CopyKey, SelectAllKey),
        )

    @Test
    fun btf2_contextMenu_emptyClipboard_fullSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.ALL,
            expectedItems = listOf(CutKey, CopyKey),
        )

    @Test
    @SdkSuppress(maxSdkVersion = 25)
    fun btf2_contextMenu_nonEmptyClipboard_noSelection_itemsMatch_beforeApi26() =
        runBtf2CorrectItemsTest(
            isEmptyClipboard = false,
            selectionAmount = SelectionAmount.NONE,
            expectedItems = listOf(PasteKey, SelectAllKey),
        )

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun btf2_contextMenu_nonEmptyClipboard_noSelection_itemsMatch_afterApi26() =
        runBtf2CorrectItemsTest(
            isEmptyClipboard = false,
            selectionAmount = SelectionAmount.NONE,
            expectedItems = listOf(PasteKey, SelectAllKey, AutofillKey),
        )

    @Test
    fun btf2_contextMenu_nonEmptyClipboard_partialSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isEmptyClipboard = false,
            selectionAmount = SelectionAmount.PARTIAL,
            expectedItems = listOf(CutKey, CopyKey, PasteKey, SelectAllKey),
        )

    @Test
    fun btf2_contextMenu_nonEmptyClipboard_fullSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isEmptyClipboard = false,
            selectionAmount = SelectionAmount.ALL,
            expectedItems = listOf(CutKey, CopyKey, PasteKey),
        )

    @Test
    @SdkSuppress(maxSdkVersion = 25)
    fun btf2_contextMenu_password_noSelection_itemsMatch_beforeApi26() =
        runBtf2CorrectItemsTest(
            isPassword = true,
            selectionAmount = SelectionAmount.NONE,
            expectedItems = listOf(PasteKey, SelectAllKey),
        )

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun btf2_contextMenu_password_noSelection_itemsMatch_afterApi26() =
        runBtf2CorrectItemsTest(
            isPassword = true,
            selectionAmount = SelectionAmount.NONE,
            expectedItems = listOf(PasteKey, SelectAllKey, AutofillKey),
        )

    @Test
    fun btf2_contextMenu_password_partialSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isPassword = true,
            selectionAmount = SelectionAmount.PARTIAL,
            expectedItems = listOf(PasteKey, SelectAllKey),
        )

    @Test
    fun btf2_contextMenu_password_fullSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isPassword = true,
            selectionAmount = SelectionAmount.ALL,
            expectedItems = listOf(PasteKey),
        )

    @Test
    fun btf2_contextMenu_readOnly_noSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isReadOnly = true,
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.NONE,
            expectedItems = listOf(SelectAllKey),
        )

    @Test
    fun btf2_contextMenu_readOnly_partialSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isReadOnly = true,
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.PARTIAL,
            expectedItems = listOf(CopyKey, SelectAllKey),
        )

    @Test
    fun btf2_contextMenu_readOnly_fullSelection_itemsMatch() =
        runBtf2CorrectItemsTest(
            isReadOnly = true,
            isEmptyClipboard = true,
            selectionAmount = SelectionAmount.ALL,
            expectedItems = listOf(CopyKey),
        )

    private fun runBtf2CorrectItemsTest(
        isPassword: Boolean = false,
        isReadOnly: Boolean = false,
        isEmptyClipboard: Boolean = false,
        selectionAmount: SelectionAmount = SelectionAmount.PARTIAL,
        expectedItems: List<Any>,
    ) = runTest {
        check(!(isPassword && isReadOnly)) { "Can't be a password field and read-only." }

        val initialText = "Text Text Text"
        val state =
            TextFieldState(
                initialText = initialText,
                initialSelection =
                    when (selectionAmount) {
                        SelectionAmount.NONE -> TextRange.Zero
                        SelectionAmount.PARTIAL -> TextRange(5, 9)
                        SelectionAmount.ALL -> TextRange(0, 14)
                    }
            )

        val clipboard =
            FakeClipboard(supportsClipEntry = true).apply {
                if (isEmptyClipboard) {
                    setClipEntry(null)
                } else {
                    setClipEntry(AnnotatedString("Clipboard Text").toClipEntry())
                }
            }

        val reader = TestTextContextMenuDataInvoker()
        val decorator = TextFieldDecorator { innerTextField ->
            Box(
                propagateMinConstraints = true,
                modifier = Modifier.testTextContextMenuDataReader(reader)
            ) {
                innerTextField()
            }
        }

        rule.setTextFieldTestContent {
            CompositionLocalProvider(LocalClipboard provides clipboard) {
                if (isPassword) {
                    BasicSecureTextField(
                        state = state,
                        decorator = decorator,
                        modifier = Modifier.testTag(textFieldTag)
                    )
                } else {
                    BasicTextField(
                        state = state,
                        decorator = decorator,
                        readOnly = isReadOnly,
                        modifier = Modifier.testTag(textFieldTag)
                    )
                }
            }
        }

        val data = reader.invokeTraversal()
        data.assertItems(expectedItems)
    }
    // endregion BTF2 Context Menu Correct Item Tests
}
