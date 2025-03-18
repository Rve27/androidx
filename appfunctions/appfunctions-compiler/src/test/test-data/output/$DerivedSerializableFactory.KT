package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$DerivedSerializableFactory` : AppFunctionSerializableFactory<DerivedSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): DerivedSerializable {

    val stringToOpen = checkNotNull(appFunctionData.getString("stringToOpen"))
    val age = checkNotNull(appFunctionData.getLongOrNull("age"))

    val resultDerivedSerializable = DerivedSerializable(stringToOpen, age)
    return resultDerivedSerializable
  }

  override fun toAppFunctionData(appFunctionSerializable: DerivedSerializable): AppFunctionData {

    val builder = AppFunctionData.Builder("com.testdata.DerivedSerializable")
    val stringToOpen = appFunctionSerializable.stringToOpen
    builder.setString("stringToOpen", stringToOpen)
    val age = appFunctionSerializable.age
    builder.setLong("age", age)

    return builder.build()
  }
}
