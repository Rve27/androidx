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

package androidx.privacysandbox.databridge.client

import android.content.Context
import androidx.privacysandbox.databridge.core.Key
import androidx.privacysandbox.databridge.core.KeyUpdateCallback
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Expect
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DataBridgeClientTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var dataBridgeClient = DataBridgeClient.getInstance(context)

    private val intKey = Key.createIntKey("intKey")
    private val doubleKey = Key.createDoubleKey("doubleKey")
    private val longKey = Key.createLongKey("longKey")
    private val floatKey = Key.createFloatKey("floatKey")
    private val booleanKey = Key.createBooleanKey("booleanKey")
    private val stringKey = Key.createStringKey("stringKey")
    private val stringSetKey = Key.createStringSetKey("stringSetKey")
    private val byteArrayKey = Key.createByteArrayKey("byteArrayKey")
    private val setOfKeys =
        setOf(
            intKey,
            doubleKey,
            longKey,
            floatKey,
            booleanKey,
            stringKey,
            stringSetKey,
            byteArrayKey,
        )

    private val currentThreadExecutor = Executor { command -> command.run() }

    @Rule @JvmField val expect = Expect.create()

    @After
    fun tearDown() {
        runBlocking { dataBridgeClient.removeValues(setOfKeys) }
        DataBridgeClient.resetInstanceForTesting()
    }

    @Test
    fun testDatBridgeProxyRegisteredToAppOwnedSdkSandboxInterfaceCompat() {
        val sdkSandboxManagerCompat = SdkSandboxManagerCompat.from(context)
        val appOwnedSdkSandboxInterfaces = sdkSandboxManagerCompat.getAppOwnedSdkSandboxInterfaces()
        expect.that(appOwnedSdkSandboxInterfaces.size).isEqualTo(1)
        expect
            .that(appOwnedSdkSandboxInterfaces[0].getName())
            .isEqualTo("androidx.privacysandbox.databridge")
        expect.that(appOwnedSdkSandboxInterfaces[0].getVersion()).isEqualTo(1)
    }

    @Test
    fun testGetInstanceSingleton() {
        expect.that(dataBridgeClient).isNotNull()
        val dataBridgeClient2 = DataBridgeClient.getInstance(context)

        assertSame(
            "Both references should point to the same singleton instance",
            dataBridgeClient,
            dataBridgeClient2,
        )
    }

    // TODO (b/414380274): Move to parameterized tests
    @Test
    fun testInt_getValue_forUnsetValue_returnsNull() = runBlocking { expectKeyIsMissing(intKey) }

    @Test
    fun testInt_setValue_andGetValue_returnsSetValue() = runBlocking {
        expectKeySetSuccessfully(intKey, 1)
    }

    @Test
    fun testInt_setValue_withIntValue_canBeOverwrittenByStringValue() = runBlocking {
        dataBridgeClient.setValue(intKey, 1)
        // Overwriting with different key type is also successful
        expectKeySetSuccessfully(Key.createStringKey(intKey.name), "stringValue")
    }

    @Test
    fun testInt_getValue_forWrongTypeAfterValueSet_returnsFailureWithClassCastException() =
        runBlocking {
            dataBridgeClient.setValue(Key.createStringKey(intKey.name), "stringValue")
            expectGetValueThrowsClassCastException(intKey)
        }

    @Test
    fun testDouble_getValue_forUnsetValue_returnsNull() = runBlocking {
        expectKeyIsMissing(doubleKey)
    }

    @Test
    fun testDouble_setValue_andGetValue_returnsSetValue() = runBlocking {
        expectKeySetSuccessfully(doubleKey, 1.1)
    }

    @Test
    fun testDouble_setValue_withIntValue_canBeOverwrittenByStringValue() = runBlocking {
        dataBridgeClient.setValue(doubleKey, 1.1)
        // Overwriting with different key type is also successful
        expectKeySetSuccessfully(Key.createStringKey(doubleKey.name), "stringValue")
    }

    @Test
    fun testDouble_getValue_forWrongTypeAfterValueSet_returnsFailureWithClassCastException() =
        runBlocking {
            dataBridgeClient.setValue(Key.createStringKey(doubleKey.name), "stringValue")
            expectGetValueThrowsClassCastException(doubleKey)
        }

    @Test
    fun testLong_getValue_forUnsetValue_returnsNull() = runBlocking { expectKeyIsMissing(longKey) }

    @Test
    fun testLong_setValue_andGetValue_returnsSetValue() = runBlocking {
        expectKeySetSuccessfully(longKey, 1L)
    }

    @Test
    fun testLong_setValue_withIntValue_canBeOverwrittenByStringValue() = runBlocking {
        dataBridgeClient.setValue(longKey, 1L)
        // Overwriting with different key type is also successful
        expectKeySetSuccessfully(Key.createStringKey(longKey.name), "stringValue")
    }

    @Test
    fun testLong_getValue_forWrongTypeAfterValueSet_returnsFailureWithClassCastException() =
        runBlocking {
            dataBridgeClient.setValue(Key.createStringKey(longKey.name), "stringValue")
            expectGetValueThrowsClassCastException(longKey)
        }

    @Test
    fun testFloat_getValue_forUnsetValue_returnsNull() = runBlocking {
        expectKeyIsMissing(floatKey)
    }

    @Test
    fun testFloat_setValue_andGetValue_returnsSetValue() = runBlocking {
        expectKeySetSuccessfully(floatKey, 1.1f)
    }

    @Test
    fun testFloat_setValue_withIntValue_canBeOverwrittenByStringValue() = runBlocking {
        dataBridgeClient.setValue(floatKey, 1.1f)
        // Overwriting with different key type is also successful
        expectKeySetSuccessfully(Key.createStringKey(floatKey.name), "stringValue")
    }

    @Test
    fun testFloat_getValue_forWrongTypeAfterValueSet_returnsFailureWithClassCastException() =
        runBlocking {
            dataBridgeClient.setValue(Key.createStringKey(floatKey.name), "stringValue")
            expectGetValueThrowsClassCastException(floatKey)
        }

    @Test
    fun testBoolean_getValue_forUnsetValue_returnsNull() = runBlocking {
        expectKeyIsMissing(booleanKey)
    }

    @Test
    fun testBoolean_setValue_andGetValue_returnsSetValue() = runBlocking {
        expectKeySetSuccessfully(booleanKey, true)
    }

    @Test
    fun testBoolean_setValue_withIntValue_canBeOverwrittenByStringValue() = runBlocking {
        dataBridgeClient.setValue(booleanKey, true)
        // Overwriting with different key type is also successful
        expectKeySetSuccessfully(Key.createStringKey(booleanKey.name), "stringValue")
    }

    @Test
    fun testBoolean_getValue_forWrongTypeAfterValueSet_returnsFailureWithClassCastException() =
        runBlocking {
            dataBridgeClient.setValue(Key.createStringKey(booleanKey.name), "stringValue")
            expectGetValueThrowsClassCastException(booleanKey)
        }

    @Test
    fun testString_getValue_forUnsetValue_returnsNull() = runBlocking {
        expectKeyIsMissing(stringKey)
    }

    @Test
    fun testString_setValue_andGetValue_returnsSetValue() = runBlocking {
        expectKeySetSuccessfully(stringKey, "stringValue")
    }

    @Test
    fun testString_setValue_withIntValue_canBeOverwrittenByStringValue() = runBlocking {
        dataBridgeClient.setValue(stringKey, "stringValue")
        // Overwriting with different key type is also successful
        expectKeySetSuccessfully(Key.createBooleanKey(stringKey.name), true)
    }

    @Test
    fun testString_getValue_forWrongTypeAfterValueSet_returnsFailureWithClassCastException() =
        runBlocking {
            dataBridgeClient.setValue(Key.createBooleanKey(stringKey.name), true)
            expectGetValueThrowsClassCastException(stringKey)
        }

    @Test
    fun testStringSet_getValue_forUnsetValue_returnsNull() = runBlocking {
        expectKeyIsMissing(stringSetKey)
    }

    @Test
    fun testStringSet_setValue_andGetValue_returnsSetValue() = runBlocking {
        expectKeySetSuccessfully(stringSetKey, setOf("stringValue1", "stringValue2"))
    }

    @Test
    fun testStringSet_setValue_withIntValue_canBeOverwrittenByStringValue() = runBlocking {
        dataBridgeClient.setValue(stringSetKey, setOf("stringValue1", "stringValue2"))
        // Overwriting with different key type is also successful
        expectKeySetSuccessfully(Key.createBooleanKey(stringSetKey.name), true)
    }

    @Test
    fun testStringSet_getValue_forWrongTypeAfterValueSet_returnsFailureWithClassCastException() =
        runBlocking {
            dataBridgeClient.setValue(Key.createBooleanKey(stringSetKey.name), true)
            expectGetValueThrowsClassCastException(stringSetKey)
        }

    @Test
    fun testByteArray_getValue_forUnsetValue_returnsNull() = runBlocking {
        expectKeyIsMissing(byteArrayKey)
    }

    @Test
    fun testByteArray_setValue_andGetValue_returnsSetValue() = runBlocking {
        expectKeySetSuccessfully(byteArrayKey, byteArrayOf(1, 2, 3, 4))
    }

    @Test
    fun testByteArray_setValue_withIntValue_canBeOverwrittenByStringValue() = runBlocking {
        dataBridgeClient.setValue(byteArrayKey, byteArrayOf(1, 2, 3, 4))
        // Overwriting with different key type is also successful
        expectKeySetSuccessfully(Key.createBooleanKey(byteArrayKey.name), true)
    }

    @Test
    fun testByteArray_getValue_forWrongTypeAfterValueSet_returnsFailureWithClassCastException() =
        runBlocking {
            dataBridgeClient.setValue(Key.createBooleanKey(byteArrayKey.name), true)
            expectGetValueThrowsClassCastException(byteArrayKey)
        }

    @Test
    fun testRemoveValue() = runBlocking {
        expectKeySetSuccessfully(intKey, 1)

        dataBridgeClient.removeValue(intKey)
        expectKeyIsMissing(intKey)
    }

    @Test
    fun testRemoveValues() = runBlocking {
        expectKeySetSuccessfully(intKey, 1)
        expectKeySetSuccessfully(booleanKey, true)

        dataBridgeClient.removeValues(setOf(intKey, booleanKey))
        expectKeyIsMissing(intKey)
        expectKeyIsMissing(booleanKey)
    }

    @Test
    fun testGetValues_NullValues() = runBlocking {
        val keyValueMap = dataBridgeClient.getValues(setOfKeys)

        setOfKeys.forEach {
            expect.that(keyValueMap[it]?.isSuccess).isTrue()
            expect.that(keyValueMap[it]?.getOrNull()).isNull()
        }
    }

    @Test
    fun testGetValues() = runBlocking {
        expectKeySetSuccessfully(intKey, 1)
        expectKeySetSuccessfully(booleanKey, false)
        expectKeySetSuccessfully(stringKey, "stringValue")

        val keyValueMap = dataBridgeClient.getValues(setOfKeys)
        expect.that(keyValueMap[intKey]?.getOrNull()).isEqualTo(1)
        expect.that(keyValueMap[booleanKey]?.getOrNull()).isEqualTo(false)
        expect.that(keyValueMap[stringKey]?.getOrNull()).isEqualTo("stringValue")
        expect.that(keyValueMap[floatKey]?.getOrNull()).isNull()
        expect.that(keyValueMap[doubleKey]?.getOrNull()).isNull()
        expect.that(keyValueMap[longKey]?.getOrNull()).isNull()
        expect.that(keyValueMap[stringSetKey]?.getOrNull()).isNull()
        expect.that(keyValueMap[byteArrayKey]?.getOrNull()).isNull()
    }

    @Test
    fun testSetValues_andGetValues_sameKeyUpdateInSingleCall() {
        runBlocking {
            val tempStringKey = Key.createStringKey("intKey")

            assertThrows(ClassCastException::class.java) {
                runBlocking {
                    // This will try to override an int key "intKey" to the string value
                    // "tempString" which will result in a ClassCastException.
                    dataBridgeClient.setValues(mapOf(intKey to 1, tempStringKey to "tempString"))
                }
            }
        }
    }

    @Test
    fun testSetValues() = runBlocking {
        dataBridgeClient.setValues(
            mapOf(
                intKey to 1,
                longKey to 1L,
                booleanKey to true,
                stringSetKey to setOf("string1", "string2"),
            )
        )

        val keyValueMap = dataBridgeClient.getValues(setOfKeys)
        expect.that(keyValueMap[intKey]?.getOrNull()).isEqualTo(1)
        expect.that(keyValueMap[booleanKey]?.getOrNull()).isEqualTo(true)
        expect.that(keyValueMap[stringKey]?.getOrNull()).isNull()
        expect.that(keyValueMap[floatKey]?.getOrNull()).isNull()
        expect.that(keyValueMap[doubleKey]?.getOrNull()).isNull()
        expect.that(keyValueMap[longKey]?.getOrNull()).isEqualTo(1L)
        expect.that(keyValueMap[stringSetKey]?.getOrNull()).isEqualTo(setOf("string1", "string2"))
        expect.that(keyValueMap[byteArrayKey]?.getOrNull()).isNull()
    }

    @Test
    fun testSetValues_wrongType() {
        assertThrows(ClassCastException::class.java) {
            runBlocking {
                dataBridgeClient.setValues(
                    mapOf(
                        intKey to "1",
                        longKey to 1L,
                        booleanKey to true,
                        stringSetKey to setOf("string1", "string2"),
                    )
                )
            }
        }

        // Verify that the action is atomic and none of the keys are set
        val keySet = setOf(intKey, longKey, booleanKey, stringSetKey)
        runBlocking { keySet.forEach { expectKeyIsMissing(it) } }
    }

    @Test
    fun testGetValues_wrongType() {
        runBlocking {
            val tempIntKey = Key.createIntKey("tempIntKey")
            dataBridgeClient.setValue(tempIntKey, 10)
            dataBridgeClient.setValues(mapOf(intKey to 1, booleanKey to true))

            val tempStringKey = Key.createStringKey("tempIntKey")
            val keyValueMap = dataBridgeClient.getValues(setOf(intKey, booleanKey, tempStringKey))

            expect.that(keyValueMap[intKey]?.isSuccess).isTrue()
            expect.that(keyValueMap[intKey]?.getOrNull()).isEqualTo(1)

            expect.that(keyValueMap[booleanKey]?.isSuccess).isTrue()
            expect.that(keyValueMap[booleanKey]?.getOrNull()).isEqualTo(true)

            expect.that(keyValueMap[tempStringKey]?.isFailure).isTrue()
            expect
                .that(keyValueMap[tempStringKey]?.exceptionOrNull() is ClassCastException)
                .isTrue()
        }
    }

    @Test
    fun testRegisterKeyUpdates_oneCallback_oneKey() = runBlocking {
        val callback = KeyUpdateCallbackImpl()

        callback.initializeLatch(listOf(intKey))
        dataBridgeClient.registerKeyUpdateCallback(setOf(intKey), currentThreadExecutor, callback)
        verifyCountAndValue(callback, intKey, 1, null)

        callback.initializeLatch(listOf(intKey))
        dataBridgeClient.setValue(intKey, 123)
        verifyCountAndValue(callback, intKey, 2, 123)

        callback.initializeLatch(listOf(intKey))
        dataBridgeClient.removeValue(intKey)
        verifyCountAndValue(callback, intKey, 3, null)

        dataBridgeClient.unregisterKeyUpdateCallback(callback)
    }

    @Test
    fun testRegisterKeyUpdates_oneCallback_multipleKeys_registeredMultipleTimes() = runBlocking {
        val callback = KeyUpdateCallbackImpl()

        callback.initializeLatch(listOf(intKey))
        dataBridgeClient.registerKeyUpdateCallback(setOf(intKey), currentThreadExecutor, callback)
        verifyCountAndValue(callback, intKey, 1, null)

        callback.initializeLatch(listOf(intKey))
        dataBridgeClient.setValue(intKey, 123)
        verifyCountAndValue(callback, intKey, 2, 123)

        callback.initializeLatch(listOf(intKey))
        dataBridgeClient.removeValue(intKey)
        verifyCountAndValue(callback, intKey, 3, null)

        callback.initializeLatch(listOf(stringKey))
        dataBridgeClient.registerKeyUpdateCallback(
            setOf(stringKey),
            currentThreadExecutor,
            callback,
        )
        verifyCountAndValue(callback, stringKey, 1, null)

        callback.initializeLatch(listOf(stringKey))
        dataBridgeClient.setValue(stringKey, "stringValue")
        verifyCountAndValue(callback, stringKey, 2, "stringValue")

        dataBridgeClient.unregisterKeyUpdateCallback(callback)
    }

    @Test
    fun testRegisterKeyUpdates_oneCallback_multipleKeys() = runBlocking {
        val callback = KeyUpdateCallbackImpl()

        callback.initializeLatch(listOf(intKey, stringKey))
        dataBridgeClient.registerKeyUpdateCallback(
            setOf(intKey, stringKey),
            currentThreadExecutor,
            callback,
        )
        verifyCountAndValue(callback, intKey, 1, null)
        verifyCountAndValue(callback, stringKey, 1, null)

        callback.initializeLatch(listOf(intKey, stringKey))
        dataBridgeClient.setValues(mapOf(intKey to 123, stringKey to "stringValue"))
        verifyCountAndValue(callback, intKey, 2, 123)
        verifyCountAndValue(callback, stringKey, 2, "stringValue")

        callback.initializeLatch(listOf(intKey, stringKey))
        dataBridgeClient.removeValues(setOf(intKey, stringKey))
        verifyCountAndValue(callback, intKey, 3, null)
        verifyCountAndValue(callback, stringKey, 3, null)

        dataBridgeClient.unregisterKeyUpdateCallback(callback)
    }

    @Test
    fun testRegisterKeyUpdates_multipleCallbacks_multipleKeys() = runBlocking {
        val callback1 = KeyUpdateCallbackImpl()
        val callback2 = KeyUpdateCallbackImpl()

        callback1.initializeLatch(listOf(intKey, stringKey))
        dataBridgeClient.registerKeyUpdateCallback(
            setOf(intKey, stringKey),
            currentThreadExecutor,
            callback1,
        )
        verifyCountAndValue(callback1, stringKey, 1, null)

        callback2.initializeLatch(listOf(doubleKey, stringKey))
        dataBridgeClient.registerKeyUpdateCallback(
            setOf(doubleKey, stringKey),
            currentThreadExecutor,
            callback2,
        )
        verifyCountAndValue(callback2, stringKey, 1, null)

        callback1.initializeLatch(listOf(stringKey))
        callback2.initializeLatch(listOf(stringKey))
        dataBridgeClient.setValue(stringKey, "stringValue")
        verifyCountAndValue(callback1, stringKey, 2, "stringValue")
        verifyCountAndValue(callback2, stringKey, 2, "stringValue")

        dataBridgeClient.unregisterKeyUpdateCallback(callback1)
        dataBridgeClient.unregisterKeyUpdateCallback(callback2)
    }

    @Test(expected = TimeoutException::class)
    fun testUnregisterKeyUpdates() = runBlocking {
        val callback = KeyUpdateCallbackImpl()

        callback.initializeLatch(listOf(intKey))
        dataBridgeClient.registerKeyUpdateCallback(setOf(intKey), currentThreadExecutor, callback)
        verifyCountAndValue(callback, intKey, 1, null)

        callback.initializeLatch(listOf(intKey))
        dataBridgeClient.setValue(intKey, 123)
        verifyCountAndValue(callback, intKey, 2, 123)

        dataBridgeClient.unregisterKeyUpdateCallback(callback)
        callback.initializeLatch(listOf(intKey))
        dataBridgeClient.setValue(intKey, 11)

        // This throws a TimeoutException exception because it CountDownLatch.awaits returns a
        // boolean as the callback has been unregistered
        val unused = callback.getCounterForKey(intKey)
    }

    private suspend fun expectKeySetSuccessfully(key: Key, value: Any) {
        dataBridgeClient.setValue(key, value)
        val res = dataBridgeClient.getValue(key)
        expect.that(res.isSuccess).isTrue()
        expect.that(dataBridgeClient.getValue(key).getOrNull()).isEqualTo(value)
    }

    private suspend fun expectGetValueThrowsClassCastException(key: Key) {
        val result = dataBridgeClient.getValue(key)

        expect.that(result.isFailure).isTrue()
        expect.that(result.exceptionOrNull() is ClassCastException).isTrue()
    }

    private suspend fun expectKeyIsMissing(key: Key) {
        val result = dataBridgeClient.getValue(key)
        expect.that(result.isSuccess).isTrue()

        expect.that(result.getOrNull()).isNull()
    }

    class KeyUpdateCallbackImpl : KeyUpdateCallback {
        private var keyUpdatedCounterMap = mutableMapOf<Key, Int>()
        private var keyToValueMap = mutableMapOf<Key, Any?>()
        // The latch will be used to ensure that the counter value and the value has been updated.
        // Wait for the latch in [getCounterForKey] or [getValueForKey] to ensure that the
        // [onKeyUpdated] function has been called
        private val latchMap = mutableMapOf<Key, CountDownLatch>()

        override fun onKeyUpdated(key: Key, value: Any?) {
            val counter = keyUpdatedCounterMap[key]
            keyUpdatedCounterMap[key] = if (counter == null) 1 else counter + 1

            keyToValueMap[key] = value
            latchMap[key]?.countDown()
        }

        fun initializeLatch(keys: List<Key>) {
            keys.forEach { key -> latchMap[key] = CountDownLatch(1) }
        }

        fun getCounterForKey(key: Key): Int {
            val res = latchMap[key]?.await(5, TimeUnit.SECONDS)
            res?.let {
                if (!it) {
                    throw TimeoutException()
                }
            }
            return keyUpdatedCounterMap[key] ?: 0
        }

        fun getValueForKey(key: Key): Any? {
            val res = latchMap[key]?.await(5, TimeUnit.SECONDS)
            res?.let {
                if (!it) {
                    throw TimeoutException()
                }
            }
            return keyToValueMap[key]
        }
    }

    private fun verifyCountAndValue(
        callback: KeyUpdateCallbackImpl,
        key: Key,
        count: Int,
        value: Any?,
    ) {
        expect.that(callback.getCounterForKey(key)).isEqualTo(count)
        expect.that(callback.getValueForKey(key)).isEqualTo(value)
    }
}
