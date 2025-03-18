package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$NonNestedChildSerializableFactory` : AppFunctionSerializableFactory<NonNestedChildSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): NonNestedChildSerializable {
    val nestedBaseSerializableFactory = `$NestedBaseSerializableFactory`()

    val stringBaseName = checkNotNull(appFunctionData.getString("stringBaseName"))
    val nestedBaseData = appFunctionData.getAppFunctionData("nestedBase")
    var nestedBase: NestedBaseSerializable? = null
    if (nestedBaseData != null) {
      nestedBase = nestedBaseSerializableFactory.fromAppFunctionData(nestedBaseData)
    }
    val longBaseValue = checkNotNull(appFunctionData.getLongOrNull("longBaseValue"))

    val resultNonNestedChildSerializable = NonNestedChildSerializable(stringBaseName, nestedBase, longBaseValue)
    return resultNonNestedChildSerializable
  }

  override fun toAppFunctionData(appFunctionSerializable: NonNestedChildSerializable): AppFunctionData {
    val nestedBaseSerializableFactory = `$NestedBaseSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.NonNestedChildSerializable")
    val stringBaseName = appFunctionSerializable.stringBaseName
    builder.setString("stringBaseName", stringBaseName)
    val nestedBase = appFunctionSerializable.nestedBase
    if (nestedBase != null) {
      builder.setAppFunctionData("nestedBase", nestedBaseSerializableFactory.toAppFunctionData(nestedBase))
    }
    val longBaseValue = appFunctionSerializable.longBaseValue
    builder.setLong("longBaseValue", longBaseValue)

    return builder.build()
  }
}
