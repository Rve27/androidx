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

package androidx.room3

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.kruth.assertThat
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.AssumptionViolatedException
import org.junit.Before

class MultiInstanceInvalidationTest {
    @Entity data class SampleEntity(@PrimaryKey val pk: Int)

    @Entity data class AnotherSampleEntity(@PrimaryKey val pk: Int)

    @Dao
    interface SampleDao {
        @Insert suspend fun insert(entity: SampleEntity)
    }

    @Database(
        entities = [SampleEntity::class, AnotherSampleEntity::class],
        version = 1,
        exportSchema = false,
    )
    abstract class SampleDatabase : RoomDatabase() {
        abstract fun dao(): SampleDao
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        context.deleteDatabase("test.db")
    }

    @Test
    @OptIn(ExperimentalRoomApi::class)
    fun invalidateInAnotherInstanceFlow() = runTest {
        val databaseOne =
            Room.databaseBuilder<SampleDatabase>(context, "test.db")
                .setDriver(AndroidSQLiteDriver())
                .enableMultiInstanceInvalidation()
                .build()
        val databaseTwo =
            Room.databaseBuilder<SampleDatabase>(context, "test.db")
                .setDriver(AndroidSQLiteDriver())
                .enableMultiInstanceInvalidation()
                .build()

        val channel =
            databaseOne.invalidationTracker
                .createFlow("SampleEntity", "AnotherSampleEntity")
                .produceIn(this)

        // Initial invalidation, all tables
        assertThat(channel.receive()).containsExactly("SampleEntity", "AnotherSampleEntity")

        // Assert multi-instance invalidation service is running.
        awaitService(isRunning = true)
        // Insert in second instance
        databaseTwo.dao().insert(SampleEntity(1))

        // Invalidation by second instance
        assertThat(channel.receive()).containsExactly("SampleEntity")

        channel.cancel()
        databaseOne.close()
        databaseTwo.close()
    }

    @Test
    @OptIn(ExperimentalRoomApi::class)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    fun autoCloseDatabaseStopsService() = runTest {
        val autoCloseDb =
            Room.databaseBuilder<SampleDatabase>(context, "test.db")
                .setDriver(AndroidSQLiteDriver())
                .enableMultiInstanceInvalidation()
                .setAutoCloseTimeout(200, TimeUnit.MILLISECONDS)
                .build()

        // Force open the database causing the multi-instance invalidation service to start
        autoCloseDb.withWriteTransaction {
            // Assert multi-instance invalidation service is running.
            awaitService(isRunning = true)
        }

        // Await and assert multi-instance invalidation service is no longer running. As this
        // function awaits, the database will be auto-closed and the service should be stopped.
        awaitService(isRunning = false)

        autoCloseDb.close()
    }

    @Suppress("DEPRECATION") // For getRunningServices()
    private suspend fun awaitService(isRunning: Boolean) {
        val manager = context.getSystemService(ActivityManager::class.java)
        withContext(Dispatchers.Main) {
            withTimeoutOrNull(10.seconds) {
                while (true) {
                    val hasRunningServices = manager.getRunningServices(100).isNotEmpty()
                    if (hasRunningServices == isRunning) {
                        return@withTimeoutOrNull
                    }
                    delay(200.milliseconds)
                }
            }
                ?: throw AssumptionViolatedException(
                    "Could not validate multi-instance service isRunning to be $isRunning."
                )
        }
    }
}
