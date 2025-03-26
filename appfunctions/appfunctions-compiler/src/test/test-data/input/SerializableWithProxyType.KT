package com.testdata

import androidx.appfunctions.AppFunctionSerializable
import java.time.LocalDateTime

@AppFunctionSerializable
class SerializableWithProxyType(
    val localDateTime: LocalDateTime,
    val localDateTimeList: List<LocalDateTime>
)
