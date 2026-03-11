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

package androidx.room3.integration.kotlintestapp.vo

import androidx.room3.DaoReturnTypeConverter
import androidx.room3.OperationType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class CustomDaoReturnType<T>(val data: T)

class CustomDaoReturnTypeConverter {
    @DaoReturnTypeConverter([OperationType.READ, OperationType.WRITE])
    suspend fun <T> convert(executeAndConvert: suspend () -> T): CustomDaoReturnType<T> {
        val data = executeAndConvert.invoke()
        return createCustomType(data)
    }

    private fun <T> createCustomType(data: T) = CustomDaoReturnType(data)
}

class ResultDaoReturnTypeConverter {
    @DaoReturnTypeConverter([OperationType.READ, OperationType.WRITE])
    suspend fun <T> convert(executeAndConvert: suspend () -> T): Result<T> {
        return runCatching { executeAndConvert.invoke() }
    }
}

class EitherDaoReturnTypeConverter {
    @DaoReturnTypeConverter([OperationType.READ, OperationType.WRITE])
    suspend fun <R> convert(executeAndConvert: suspend () -> R): Either<Throwable, R> {
        return try {
            Either.Right(executeAndConvert.invoke())
        } catch (e: Throwable) {
            Either.Left(e)
        }
    }
}

sealed class Either<out L, out R> {
    @OptIn(ExperimentalContracts::class)
    fun isLeft(): Boolean {
        contract {
            returns(true) implies (this@Either is Left<L>)
            returns(false) implies (this@Either is Right<R>)
        }
        return this@Either is Left<L>
    }

    @OptIn(ExperimentalContracts::class)
    fun isRight(): Boolean {
        contract {
            returns(true) implies (this@Either is Right<R>)
            returns(false) implies (this@Either is Left<L>)
        }
        return this@Either is Right<R>
    }

    data class Left<out L>(val value: L) : Either<L, Nothing>()

    data class Right<out R>(val value: R) : Either<Nothing, R>()
}
