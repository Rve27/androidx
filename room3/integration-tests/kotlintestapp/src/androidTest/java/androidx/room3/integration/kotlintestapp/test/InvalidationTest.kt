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

package androidx.room3.integration.kotlintestapp.test

import androidx.kruth.assertThat
import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import androidx.test.filters.SmallTest
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@SmallTest
@RunWith(Parameterized::class)
class InvalidationTest(driver: UseDriver) :
    TestDatabaseTest(useDriver = driver, useInMemoryDatabase = false) {

    private companion object {
        @JvmStatic
        @Parameters(name = "useDriver={0}")
        fun parameters() = arrayOf(UseDriver.ANDROID, UseDriver.BUNDLED)
    }

    @Test
    fun invalidationOnInsert() = runTest {
        val publishers = booksDao.getPublishersFlow().produceIn(this)
        assertThat(publishers.receive()).isEmpty()

        booksDao.addPublishers(TestUtil.PUBLISHER)
        assertThat(publishers.receive()).containsExactly(TestUtil.PUBLISHER)

        publishers.cancel()
    }

    @Test
    fun invalidationOnUpdate() = runTest {
        booksDao.addPublishers(TestUtil.PUBLISHER)

        val publishers = booksDao.getPublishersFlow().produceIn(this)
        assertThat(publishers.receive()).containsExactly(TestUtil.PUBLISHER)

        booksDao.updatePublishers(TestUtil.PUBLISHER.copy(name = "updatedName"))
        assertThat(publishers.receive().single().name).isEqualTo("updatedName")

        publishers.cancel()
    }

    @Test
    fun invalidationOnDelete() = runTest {
        booksDao.addPublishers(TestUtil.PUBLISHER)

        val publishers = booksDao.getPublishersFlow().produceIn(this)
        assertThat(publishers.receive()).containsExactly(TestUtil.PUBLISHER)

        booksDao.deletePublishers(TestUtil.PUBLISHER)
        assertThat(publishers.receive()).isEmpty()

        publishers.cancel()
    }

    @Test
    fun invalidationUsingWriteConnection() = runTest {
        val publishers = booksDao.getPublishersFlow().produceIn(this)
        assertThat(publishers.receive()).isEmpty()

        database.useWriterConnection { connection ->
            connection.usePrepared("INSERT INTO publisher (publisherId, name) VALUES (?, ?)") { stmt
                ->
                stmt.bindText(1, TestUtil.PUBLISHER.publisherId)
                stmt.bindText(2, TestUtil.PUBLISHER.name)
                stmt.step()
            }
        }
        assertThat(publishers.receive()).containsExactly(TestUtil.PUBLISHER)

        publishers.cancel()
    }

    @Test
    fun invalidationUsingWriteTransaction() = runTest {
        val publishers = booksDao.getPublishersFlow().produceIn(this)
        assertThat(publishers.receive()).isEmpty()

        database.useWriterConnection { connection ->
            connection.immediateTransaction {
                connection.usePrepared("INSERT INTO publisher (publisherId, name) VALUES (?, ?)") {
                    stmt ->
                    stmt.reset()
                    stmt.bindText(1, TestUtil.PUBLISHER.publisherId)
                    stmt.bindText(2, TestUtil.PUBLISHER.name)
                    stmt.step()

                    stmt.reset()
                    stmt.bindText(1, TestUtil.PUBLISHER2.publisherId)
                    stmt.bindText(2, TestUtil.PUBLISHER2.name)
                    stmt.step()

                    stmt.reset()
                    stmt.bindText(1, TestUtil.PUBLISHER3.publisherId)
                    stmt.bindText(2, TestUtil.PUBLISHER3.name)
                    stmt.step()
                }
            }
        }
        assertThat(publishers.receive())
            .containsExactly(TestUtil.PUBLISHER, TestUtil.PUBLISHER2, TestUtil.PUBLISHER3)

        publishers.cancel()
    }
}
