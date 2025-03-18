package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$NestedBaseSerializableFactory` : AppFunctionSerializableFactory<NestedBaseSerializable> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): NestedBaseSerializable {
    val nestedBaseSerializableFactory = `$NestedBaseSerializableFactory`()

    val stringBaseName = checkNotNull(appFunctionData.getString("stringBaseName"))
    val nestedBaseData = appFunctionData.getAppFunctionData("nestedBase")
    var nestedBase: NestedBaseSerializable? = null
    if (nestedBaseData != null) {
      nestedBase = nestedBaseSerializableFactory.fromAppFunctionData(nestedBaseData)
    }

    val resultNestedBaseSerializable = NestedBaseSerializable(stringBaseName, nestedBase)
    return resultNestedBaseSerializable
  }

  override fun toAppFunctionData(appFunctionSerializable: NestedBaseSerializable): AppFunctionData {
    val nestedBaseSerializableFactory = `$NestedBaseSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.NestedBaseSerializable")
    val stringBaseName = appFunctionSerializable.stringBaseName
    builder.setString("stringBaseName", stringBaseName)
    val nestedBase = appFunctionSerializable.nestedBase
    if (nestedBase != null) {
      builder.setAppFunctionData("nestedBase", nestedBaseSerializableFactory.toAppFunctionData(nestedBase))
    }

    return builder.build()
  }
}
