/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appsearch.app;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeTrue;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;

public class SetSchemaRequestInternalTest {

    @Test
    public void testAddRequiredPermissionsForSchemaTypeVisibility_emptyPermissions() {
        // This test should only run in non-framework environments. The check for an empty
        // permission set is a client-side validation.
        // skip this validation when running in the framework environment to avoid
        // breaking compatibility.
        assumeTrue(AppSearchEnvironmentFactory.getEnvironmentInstance()
                .getEnvironment()
                != AppSearchEnvironment.FRAMEWORK_ENVIRONMENT);
        AppSearchSchema schema = new AppSearchSchema.Builder("Schema").build();
        SetSchemaRequest.Builder setSchemaRequestBuilder = new SetSchemaRequest.Builder()
                .addSchemas(schema);

        IllegalArgumentException expected = assertThrows(IllegalArgumentException.class,
                () -> setSchemaRequestBuilder.addRequiredPermissionsForSchemaTypeVisibility(
                        "Schema", ImmutableSet.of()));
        assertThat(expected).hasMessageThat().contains(
                "The set of required permissions cannot be empty");
    }
}
