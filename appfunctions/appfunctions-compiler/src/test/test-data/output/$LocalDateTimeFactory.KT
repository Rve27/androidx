package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import java.time.LocalDateTime
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$LocalDateTimeFactory` : AppFunctionSerializableFactory<LocalDateTime> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): LocalDateTime = LocalDateTime.now()

  override fun toAppFunctionData(appFunctionSerializable: LocalDateTime): AppFunctionData = AppFunctionData.Builder("").build()
}
