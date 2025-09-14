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

package androidx.compose.remote.core;

import androidx.compose.remote.core.layout.ApplyTouchDown;
import androidx.compose.remote.core.layout.CaptureComponentTree;
import androidx.compose.remote.core.layout.Color;
import androidx.compose.remote.core.layout.LayoutTestPlayer;
import androidx.compose.remote.core.layout.TestComponentVisibility;
import androidx.compose.remote.core.layout.TestOperation;
import androidx.compose.remote.core.layout.TestParameters;
import androidx.compose.remote.core.operations.layout.Component;
import androidx.compose.remote.core.operations.layout.managers.RowLayout;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.creation.actions.ValueIntegerChange;
import androidx.compose.remote.creation.modifiers.RecordingModifier;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.ArrayList;

public class LayoutTest extends LayoutTestPlayer {
    private static final boolean GENERATE_GOLD_FILES = false;
    Platform mPlatform = Platform.None;

    @Rule
    public TestName mName = new TestName();

    @Test
    public void testTouchDownVisibilityChange() {
        RemoteComposeWriter writer = new RemoteComposeWriter(1000, 1000, "Layout", mPlatform);
        writer.root(
                () -> {
                    long visibilityId = writer.addInteger(Component.Visibility.GONE);
                    writer.row(
                            new RecordingModifier()
                                    .componentId(1)
                                    .fillMaxHeight()
                                    .background(Color.YELLOW)
                                    .padding(8),
                            RowLayout.CENTER,
                            RowLayout.TOP,
                            () -> {
                                writer.box(
                                        new RecordingModifier()
                                                .componentId(2)
                                                .size(200)
                                                .visibility((int) visibilityId)
                                                .background(Color.GREEN));

                                writer.box(
                                        new RecordingModifier()
                                                .componentId(3)
                                                .size(100)
                                                .background(Color.RED)
                                                .onTouchDown(new ValueIntegerChange(
                                                        (int) visibilityId,
                                                        Component.Visibility.VISIBLE)));
                            });
                });

        ArrayList<TestOperation> ops = new ArrayList<>();
        ops.add(new TestComponentVisibility(2, Component.Visibility.GONE));
        ops.add(new ApplyTouchDown(50, 50));
        ops.add(new TestComponentVisibility(2, Component.Visibility.VISIBLE));
        ops.add(new CaptureComponentTree());
        play(writer, ops, new TestParameters(mName.getMethodName(), GENERATE_GOLD_FILES));
    }

}
