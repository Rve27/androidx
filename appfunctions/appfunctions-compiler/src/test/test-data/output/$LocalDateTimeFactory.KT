package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import java.time.LocalDateTime
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$LocalDateTimeFactory` : AppFunctionSerializableFactory<LocalDateTime> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): LocalDateTime {

    val year = checkNotNull(appFunctionData.getIntOrNull("year"))
    val month = checkNotNull(appFunctionData.getIntOrNull("month"))
    val dayOfMonth = checkNotNull(appFunctionData.getIntOrNull("dayOfMonth"))
    val hour = checkNotNull(appFunctionData.getIntOrNull("hour"))
    val minute = checkNotNull(appFunctionData.getIntOrNull("minute"))
    val second = checkNotNull(appFunctionData.getIntOrNull("second"))
    val nanoOfSecond = checkNotNull(appFunctionData.getIntOrNull("nanoOfSecond"))

    val resultAppFunctionLocalDateTime = AppFunctionLocalDateTime(year, month, dayOfMonth, hour, minute, second, nanoOfSecond)
    return resultAppFunctionLocalDateTime.toLocalDateTime()
  }

  override fun toAppFunctionData(appFunctionSerializable: LocalDateTime): AppFunctionData = AppFunctionData.Builder("").build()
}
