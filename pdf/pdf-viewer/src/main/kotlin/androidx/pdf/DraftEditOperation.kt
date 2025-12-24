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

package androidx.pdf

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.core.os.ParcelCompat
import androidx.pdf.annotation.models.PdfAnnotation

internal const val TAG_INSERT = 1
internal const val TAG_UPDATE = 2
internal const val TAG_REMOVE = 3

/** Represents a single edit operation on a PDF document's annotations. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface DraftEditOperation : Parcelable {

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<DraftEditOperation> =
            object : Parcelable.Creator<DraftEditOperation> {
                override fun createFromParcel(parcel: Parcel): DraftEditOperation {
                    return when (val tag = parcel.readInt()) {
                        TAG_INSERT -> InsertDraftEditOperation(parcel)
                        TAG_UPDATE -> UpdateDraftEditOperation(parcel)
                        TAG_REMOVE -> RemoveDraftEditOperation(parcel)
                        else ->
                            throw IllegalArgumentException("Unknown DraftEditOperation tag: $tag")
                    }
                }

                override fun newArray(size: Int): Array<DraftEditOperation?> {
                    return arrayOfNulls(size)
                }
            }
    }
}

/**
 * Represents an operation to insert a new annotation into the PDF document.
 *
 * @property annotation The [PdfAnnotation] object to be inserted.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class InsertDraftEditOperation(public val annotation: PdfAnnotation) : DraftEditOperation {

    internal constructor(
        parcel: Parcel
    ) : this(
        annotation =
            ParcelCompat.readParcelable(
                parcel,
                PdfAnnotation::class.java.classLoader,
                PdfAnnotation::class.java,
            )!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(TAG_INSERT)
        parcel.writeParcelable(annotation, flags)
    }

    override fun describeContents(): Int = 0

    public companion object CREATOR : Parcelable.Creator<InsertDraftEditOperation> {
        override fun createFromParcel(parcel: Parcel): InsertDraftEditOperation {
            val tag = parcel.readInt()
            if (tag != TAG_INSERT) {
                throw IllegalArgumentException("Invalid tag for Insert operation: $tag")
            }
            return InsertDraftEditOperation(parcel)
        }

        override fun newArray(size: Int): Array<InsertDraftEditOperation?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * Represents an operation to update an existing annotation in the PDF document.
 *
 * @property id The unique identifier of the annotation to be updated.
 * @property annotation The new [PdfAnnotation] data that will replace the existing annotation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class UpdateDraftEditOperation(public val id: String, public val annotation: PdfAnnotation) :
    DraftEditOperation {

    internal constructor(
        parcel: Parcel
    ) : this(
        id = parcel.readString() ?: "",
        annotation =
            ParcelCompat.readParcelable(
                parcel,
                PdfAnnotation::class.java.classLoader,
                PdfAnnotation::class.java,
            )!!,
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(TAG_UPDATE)
        parcel.writeString(id)
        parcel.writeParcelable(annotation, flags)
    }

    override fun describeContents(): Int = 0

    public companion object CREATOR : Parcelable.Creator<UpdateDraftEditOperation> {
        override fun createFromParcel(parcel: Parcel): UpdateDraftEditOperation {
            val tag = parcel.readInt()
            if (tag != TAG_UPDATE) {
                throw IllegalArgumentException("Invalid tag for Update operation: $tag")
            }
            return UpdateDraftEditOperation(parcel)
        }

        override fun newArray(size: Int): Array<UpdateDraftEditOperation?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * Represents an operation to remove an existing annotation from the PDF document.
 *
 * @property id The unique identifier of the annotation to be removed.
 * @property pageNum The page number where the annotation is located.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoveDraftEditOperation(public val id: String, public val pageNum: Int) :
    DraftEditOperation {

    internal constructor(
        parcel: Parcel
    ) : this(id = parcel.readString() ?: "", pageNum = parcel.readInt())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(TAG_REMOVE)
        parcel.writeString(id)
        parcel.writeInt(pageNum)
    }

    override fun describeContents(): Int = 0

    public companion object CREATOR : Parcelable.Creator<RemoveDraftEditOperation> {
        override fun createFromParcel(parcel: Parcel): RemoveDraftEditOperation {
            val tag = parcel.readInt()
            if (tag != TAG_REMOVE) {
                throw IllegalArgumentException("Invalid tag for Remove operation: $tag")
            }
            return RemoveDraftEditOperation(parcel)
        }

        override fun newArray(size: Int): Array<RemoveDraftEditOperation?> {
            return arrayOfNulls(size)
        }
    }
}
