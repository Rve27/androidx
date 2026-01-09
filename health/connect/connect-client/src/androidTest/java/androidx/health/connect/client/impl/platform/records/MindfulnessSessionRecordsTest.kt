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

package androidx.health.connect.client.impl.platform.records

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.health.connect.client.AssumeRule
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.impl.HealthConnectClientUpsideDownImpl
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord.Companion.MINDFULNESS_SESSION_TYPE_BREATHING
import androidx.health.connect.client.records.MindfulnessSessionRecord.Companion.MINDFULNESS_SESSION_TYPE_MEDITATION
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
@TargetApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class MindfulnessSessionRecordTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val healthConnectClient: HealthConnectClient =
        HealthConnectClientUpsideDownImpl(context)

    private companion object {
        private val START_TIME =
            LocalDate.now().minusDays(5).atStartOfDay().toInstant(ZoneOffset.UTC)
    }

    private val assumeRule: AssumeRule =
        AssumeRule(SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) >= 15)

    private val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *listOf(
                    HealthPermission.getWritePermission(MindfulnessSessionRecord::class),
                    HealthPermission.getReadPermission(MindfulnessSessionRecord::class),
                )
                .toTypedArray()
        )

    @get:Rule val ruleChain: RuleChain = RuleChain.outerRule(assumeRule).around(grantPermissionRule)

    @After
    fun tearDown() = runTest {
        healthConnectClient.deleteRecords(
            MindfulnessSessionRecord::class,
            TimeRangeFilter.after(Instant.EPOCH),
        )
    }

    // The test aims to verify the wiring with the framework rather than the aggregation logic
    // itself.
    @Test
    fun aggregateRecords() = runTest {
        val insertRecordsResponse =
            healthConnectClient.insertRecords(
                listOf(
                    MindfulnessSessionRecord(
                        startTime = START_TIME,
                        startZoneOffset = ZoneOffset.UTC,
                        endTime = START_TIME.plusSeconds(150),
                        endZoneOffset = ZoneOffset.UTC,
                        metadata = Metadata.manualEntry(),
                        mindfulnessSessionType = MINDFULNESS_SESSION_TYPE_MEDITATION,
                    ),
                    MindfulnessSessionRecord(
                        startTime = START_TIME.plusSeconds(180),
                        startZoneOffset = ZoneOffset.UTC,
                        endTime = START_TIME.plusSeconds(300),
                        endZoneOffset = ZoneOffset.UTC,
                        metadata = Metadata.manualEntry(),
                        mindfulnessSessionType = MINDFULNESS_SESSION_TYPE_BREATHING,
                    ),
                )
            )
        assertThat(insertRecordsResponse.recordIdsList.size).isEqualTo(2)

        val aggregateResponse =
            healthConnectClient.aggregate(
                AggregateRequest(
                    setOf(MindfulnessSessionRecord.MINDFULNESS_DURATION_TOTAL),
                    TimeRangeFilter.after(Instant.EPOCH),
                )
            )

        with(aggregateResponse) {
            assertThat(this[MindfulnessSessionRecord.MINDFULNESS_DURATION_TOTAL])
                .isEqualTo(Duration.ofSeconds(270))
        }
    }
}
