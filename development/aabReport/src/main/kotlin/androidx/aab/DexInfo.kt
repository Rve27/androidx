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

package androidx.aab

import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.CRC32

data class DexInfo(
    /** Entry name (relative path) within the containing bundle */
    val entryName: String,

    /**
     * crc32 of whole dex file
     *
     * Despite being 4 bytes this is *NOT* the dex-embedded checksum. It's the one embedded in
     * profiles to verify the dex they match with.
     */
    val crc32: String,

    /** Sha256 of whole file */
    val sha256: String,

    /** Size of bytes in the uncompressed dex */
    val uncompressedSize: Long,

    /** Size of bytes in the zip container */
    val compressedSize: Long,
) {
    companion object {
        fun from(entryName: String, compressedSize: Long, src: InputStream): DexInfo {
            val crc = CRC32()
            val sha256 = MessageDigest.getInstance("SHA-256")
            var uncompressedSize = 0L

            // Process the stream in chunks, updating both hashes in the same loop.
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE) // Typically 8192
            generateSequence { src.read(buffer).takeIf { it != -1 } }
                .forEach { bytesRead ->
                    // Feed the same chunk of data to both algorithms
                    crc.update(buffer, 0, bytesRead)
                    sha256.update(buffer, 0, bytesRead)
                    uncompressedSize += bytesRead
                }

            // Finalize the SHA-256 hash and format it as a hex string.
            val sha256Bytes = sha256.digest()
            val sha256Hex = sha256Bytes.joinToString("") { "%02x".format(it) }
            val crc32Hex = crc.value.toInt().toHexString()

            // 4. Return the results in the data class.
            return DexInfo(
                entryName = entryName,
                crc32 = crc32Hex,
                sha256 = sha256Hex,
                uncompressedSize = uncompressedSize,
                compressedSize = compressedSize,
            )
        }

        val CSV_TITLES =
            listOf(
                "dex_names",
                "dex_totalSize",
                "dex_sortedChecksumsSha256",
                "dex_sortedChecksumsCrc32",
            )

        fun List<DexInfo>.csvEntries(): List<String> {
            return listOf(
                // NOTE: we individually sort each of these, so they aren't associated with each
                // other, but they are easy to compare when joined
                joinToString(INTERNAL_CSV_SEPARATOR) { it.entryName },
                this.sumOf { it.uncompressedSize }.toString(),
                this.map { it.sha256 }.sorted().joinToString(INTERNAL_CSV_SEPARATOR),
                this.map { it.crc32 }.sorted().joinToString(INTERNAL_CSV_SEPARATOR),
            )
        }
    }
}
