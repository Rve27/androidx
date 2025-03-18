package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$EntityWithValidPropertiesFactory` : AppFunctionSerializableFactory<EntityWithValidProperties> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): EntityWithValidProperties {
    val inputSerializableFactory = `$InputSerializableFactory`()

    val longParam = checkNotNull(appFunctionData.getLongOrNull("longParam"))
    val doubleParam = checkNotNull(appFunctionData.getDoubleOrNull("doubleParam"))
    val boolParam = checkNotNull(appFunctionData.getBooleanOrNull("boolParam"))
    val stringParam = checkNotNull(appFunctionData.getString("stringParam"))
    val longArray = checkNotNull(appFunctionData.getLongArray("longArray") ?: LongArray(0))
    val doubleArray = checkNotNull(appFunctionData.getDoubleArray("doubleArray") ?: DoubleArray(0))
    val boolArray = checkNotNull(appFunctionData.getBooleanArray("boolArray") ?: BooleanArray(0))
    val stringList = checkNotNull(appFunctionData.getStringList("stringList") ?: emptyList())
    val inputSerializableData = checkNotNull(appFunctionData.getAppFunctionData("inputSerializable"))
    val inputSerializable = inputSerializableFactory.fromAppFunctionData(inputSerializableData)
    val serializableListData = appFunctionData.getAppFunctionDataList("serializableList") ?: emptyList()
    val serializableList = serializableListData.map { data ->
      inputSerializableFactory.fromAppFunctionData(data)
    }

    val resultEntityWithValidProperties = EntityWithValidProperties(longParam, doubleParam, boolParam, stringParam, longArray, doubleArray, boolArray, stringList, inputSerializable, serializableList)
    return resultEntityWithValidProperties
  }

  override fun toAppFunctionData(appFunctionSerializable: EntityWithValidProperties): AppFunctionData {
    val inputSerializableFactory = `$InputSerializableFactory`()

    val builder = AppFunctionData.Builder("com.testdata.EntityWithValidProperties")
    val longParam = appFunctionSerializable.longParam
    builder.setLong("longParam", longParam)
    val doubleParam = appFunctionSerializable.doubleParam
    builder.setDouble("doubleParam", doubleParam)
    val boolParam = appFunctionSerializable.boolParam
    builder.setBoolean("boolParam", boolParam)
    val stringParam = appFunctionSerializable.stringParam
    builder.setString("stringParam", stringParam)
    val longArray = appFunctionSerializable.longArray
    builder.setLongArray("longArray", longArray)
    val doubleArray = appFunctionSerializable.doubleArray
    builder.setDoubleArray("doubleArray", doubleArray)
    val boolArray = appFunctionSerializable.boolArray
    builder.setBooleanArray("boolArray", boolArray)
    val stringList = appFunctionSerializable.stringList
    builder.setStringList("stringList", stringList)
    val inputSerializable = appFunctionSerializable.inputSerializable
    builder.setAppFunctionData("inputSerializable", inputSerializableFactory.toAppFunctionData(inputSerializable))
    val serializableList = appFunctionSerializable.serializableList
    builder.setAppFunctionDataList("serializableList", serializableList.map{ inputSerializable ->
      inputSerializableFactory.toAppFunctionData(inputSerializable)
    })

    return builder.build()
  }
}
