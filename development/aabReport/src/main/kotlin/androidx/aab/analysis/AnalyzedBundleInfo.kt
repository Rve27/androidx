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

package androidx.aab.analysis

import androidx.aab.BundleInfo
import androidx.aab.R8JsonFileInfo
import androidx.aab.analysis.AnalyzedBundleInfo.LibraryAnalysis.Companion.getLibraryAnalysis
import androidx.aab.analysis.AnalyzedBundleInfo.ProfileAnalysis.Companion.getProfileAnalysis
import androidx.aab.analysis.AnalyzedBundleInfo.R8Analysis.Companion.getR8Analysis
import androidx.aab.analysis.BaselineProfileIssues.getPartlyCorruptedProfileIssue
import androidx.aab.analysis.R8Issues.getPrimaryOptimizationIssue
import kotlin.math.roundToInt

class AnalyzedBundleInfo(val bundleInfo: BundleInfo) {
    val profileAnalysis: ProfileAnalysis = bundleInfo.getProfileAnalysis()
    val r8Analysis: R8Analysis = bundleInfo.getR8Analysis()
    val libraryAnalysis = bundleInfo.getLibraryAnalysis()

    fun printAnalysis() {
        println("Analysis for ${bundleInfo.path}")
        listOf(profileAnalysis, r8Analysis, libraryAnalysis).map { it.getScore() }.print()
    }

    data class ProfileAnalysis(
        val status: Status,
        val corruptionRatio: Double,
        val dexCrc32ChecksumsMatching: Set<String>,
        val dexCrc32ChecksumsProfileOnly: Set<String>,
        val dexCrc32ChecksumsDexOnly: Set<String>,
        // TODO: Profile method / class presence percentage,
        //  e.g. 100% of methods in profile vs 0.1% of methods in profile
    ) : ScoreReporter {
        enum class Status {
            MISSING,
            EMPTY,
            PARTLY_CORRUPTED,
            FULLY_CORRUPTED,
            OK,
        }

        override fun getScore(): SubScore {
            val score: Int =
                when (status) {
                    Status.MISSING,
                    Status.EMPTY,
                    Status.FULLY_CORRUPTED -> 0
                    Status.PARTLY_CORRUPTED -> (20 * corruptionRatio).roundToInt()
                    Status.OK -> 20
                }

            val issue =
                when (status) {
                    Status.MISSING -> BaselineProfileIssues.MissingProfile
                    Status.EMPTY -> BaselineProfileIssues.EmptyProfile
                    Status.PARTLY_CORRUPTED -> this.getPartlyCorruptedProfileIssue()
                    Status.FULLY_CORRUPTED -> BaselineProfileIssues.FullyCorruptedProfile
                    Status.OK -> null
                }
            return SubScore("Baseline Profile", score, 20, listOfNotNull(issue))
        }

        companion object {
            fun BundleInfo.getProfileAnalysis(): ProfileAnalysis {
                val setOfDexCrc32FromDex = (dexInfo.map { it.crc32 }.toSet())
                val setOfDexCrc32FromProfiles =
                    profileInfo?.dexInfoList?.map { it.checksumCrc32 }?.toSet() ?: emptySet()

                val dexCrc32ChecksumsMatching =
                    setOfDexCrc32FromDex.intersect(setOfDexCrc32FromProfiles)
                val dexCrc32ChecksumsProfileOnly = setOfDexCrc32FromProfiles - setOfDexCrc32FromDex
                val dexCrc32ChecksumsDexOnly = setOfDexCrc32FromDex - setOfDexCrc32FromProfiles

                val status =
                    when {
                        profileInfo == null -> Status.MISSING
                        profileInfo.dexInfoList.isEmpty() -> Status.EMPTY
                        dexCrc32ChecksumsProfileOnly.isNotEmpty() &&
                            dexCrc32ChecksumsMatching.isEmpty() -> Status.FULLY_CORRUPTED
                        dexCrc32ChecksumsProfileOnly.isNotEmpty() -> Status.PARTLY_CORRUPTED
                        else -> Status.OK
                    }

                return ProfileAnalysis(
                    status = status,
                    corruptionRatio =
                        if (setOfDexCrc32FromProfiles.isNotEmpty()) {
                            dexCrc32ChecksumsProfileOnly.size * 1.0 /
                                (setOfDexCrc32FromProfiles.size)
                        } else 0.0,
                    dexCrc32ChecksumsMatching = dexCrc32ChecksumsMatching,
                    dexCrc32ChecksumsProfileOnly = dexCrc32ChecksumsProfileOnly,
                    dexCrc32ChecksumsDexOnly = dexCrc32ChecksumsDexOnly,
                )
            }
        }
    }

    data class R8Analysis(
        val mappingExpected: Boolean,
        val mappingPresent: Boolean,
        val r8JsonFileInfo: R8JsonFileInfo?,
        val dexSha256ChecksumsMatching: Set<String>,
        val dexSha256ChecksumsR8JsonOnly: Set<String>,
        val dexSha256ChecksumsDexOnly: Set<String>,
    ) : ScoreReporter {
        fun R8JsonFileInfo.getScore(): Int {
            return (50 *
                    ((if (this.shrinkingEnabled) 0.3 else 0.0) +
                        (if (this.optimizationEnabled) 0.5 else 0.0) +
                        (if (this.obfuscationEnabled) 0.2 else 0.0)))
                .roundToInt()
        }

        override fun getScore(): SubScore {
            val issues =
                listOfNotNull(
                    if (dexSha256ChecksumsR8JsonOnly.isNotEmpty()) R8Issues.DexChecksumsMismatched
                    else null,
                    if (!mappingPresent && r8JsonFileInfo == null)
                        R8Issues.NoMappingFileOrJsonMetadata
                    else null,
                    if (mappingPresent && r8JsonFileInfo == null) R8Issues.MissingJsonMetadata
                    else null,
                    r8JsonFileInfo?.getPrimaryOptimizationIssue(),
                )

            return SubScore(
                label = "R8 / Dex Optimization",
                score = r8JsonFileInfo?.getScore(),
                maxScore = 50,
                issues = issues,
            )
        }

        companion object {
            fun BundleInfo.getR8Analysis(): R8Analysis {
                val metadataJsonShas = r8JsonFileInfo?.dexShas?.toSet() ?: emptySet()
                val dexShas = dexInfo.map { it.sha256 }.toSet()
                return R8Analysis(
                    mappingExpected =
                        (this.appMetadataPropsInfoBundleMetadata
                                ?: this.appMetadataPropsInfoMetaInf)
                            ?.agpAtLeast(8, 8) ?: false,
                    mappingPresent = r8JsonFileInfo != null,
                    r8JsonFileInfo = r8JsonFileInfo,
                    dexSha256ChecksumsMatching = metadataJsonShas.intersect(dexShas),
                    dexSha256ChecksumsDexOnly = dexShas - metadataJsonShas,
                    dexSha256ChecksumsR8JsonOnly = metadataJsonShas - dexShas,
                )
            }
        }
    }

    data class LibraryAnalysis(
        val hasDotVersionFiles: Boolean,
        val hasAppBundleDependencies: Boolean,
    ) : ScoreReporter {
        // TODO: compose version
        // TODO: don't embed compose tooling, perfetto tracing binary
        override fun getScore(): SubScore {
            return SubScore(
                label = "Library Version Analysis",
                score = 0,
                maxScore = 0,
                issues =
                    if (!hasDotVersionFiles && !hasAppBundleDependencies) {
                        listOf(LibraryVersionIssues.MissingMetadata)
                    } else {
                        emptyList()
                    },
            )
        }

        companion object {
            fun BundleInfo.getLibraryAnalysis(): LibraryAnalysis {
                return LibraryAnalysis(
                    hasDotVersionFiles = dotVersionFiles.isNotEmpty(),
                    hasAppBundleDependencies = appBundleDependencies != null,
                )
            }
        }
    }
}
