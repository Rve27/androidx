package com.testdata

import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.`internal`.AppFunctionSerializableFactory
import javax.`annotation`.processing.Generated
import kotlin.String
import kotlin.collections.List

@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$UpdateNoteParamsFactory` : AppFunctionSerializableFactory<UpdateNoteParams> {
  override fun fromAppFunctionData(appFunctionData: AppFunctionData): UpdateNoteParams {
    val setFieldKotlinStringFactory = `$SetFieldFactory`<String>(
      AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(String::class.java),
    )
    val setFieldKotlinCollectionsListComTestdataAttachmentFactory = `$SetFieldFactory`<List<Attachment>>(
      AppFunctionSerializableFactory.TypeParameter.ListTypeParameter(Attachment::class.java),
    )

    val titleData = appFunctionData.getAppFunctionData("title")
    var title: SetField<String>? = null
    if (titleData != null) {
      title = setFieldKotlinStringFactory.fromAppFunctionData(titleData)
    }
    val attachmentsData = appFunctionData.getAppFunctionData("attachments")
    var attachments: SetField<List<Attachment>>? = null
    if (attachmentsData != null) {
      attachments = setFieldKotlinCollectionsListComTestdataAttachmentFactory.fromAppFunctionData(attachmentsData)
    }

    val resultUpdateNoteParams = UpdateNoteParams(title, attachments)
    return resultUpdateNoteParams
  }

  override fun toAppFunctionData(appFunctionSerializable: UpdateNoteParams): AppFunctionData {
    val updateNoteParams_appFunctionSerializable = appFunctionSerializable
    val setFieldKotlinStringFactory = `$SetFieldFactory`<String>(
      AppFunctionSerializableFactory.TypeParameter.PrimitiveTypeParameter(String::class.java),
    )
    val setFieldKotlinCollectionsListComTestdataAttachmentFactory = `$SetFieldFactory`<List<Attachment>>(
      AppFunctionSerializableFactory.TypeParameter.ListTypeParameter(Attachment::class.java),
    )

    val builder = AppFunctionData.Builder("com.testdata.UpdateNoteParams")
    val title = updateNoteParams_appFunctionSerializable.title
    if (title != null) {
      builder.setAppFunctionData("title", setFieldKotlinStringFactory.toAppFunctionData(title))
    }
    val attachments = updateNoteParams_appFunctionSerializable.attachments
    if (attachments != null) {
      builder.setAppFunctionData("attachments", setFieldKotlinCollectionsListComTestdataAttachmentFactory.toAppFunctionData(attachments))
    }

    return builder.build()
  }
}
