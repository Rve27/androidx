/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.core.processing.util

import androidx.annotation.RestrictTo
import androidx.camera.core.DynamicRange
import androidx.camera.core.processing.ShaderProvider
import androidx.core.util.Preconditions

/** Default shader providers for [GLUtils]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object ShaderProviders {
    @JvmField val DEFAULT_VERTEX_SHADER = createVertexShader(GLUtils.VAR_TEXTURE_COORD, isHdr = false)

    @JvmField val HDR_VERTEX_SHADER = createVertexShader(GLUtils.VAR_TEXTURE_COORD, isHdr = true)

    @JvmField
    val BLANK_VERTEX_SHADER =
        """
        uniform mat4 uTransMatrix;
        attribute vec4 aPosition;
        void main() {
            gl_Position = uTransMatrix * aPosition;
        }
        """
            .trimIndent()

    @JvmField
    val BLANK_FRAGMENT_SHADER =
        """
        precision mediump float;
        uniform float uAlphaScale;
        void main() {
            gl_FragColor = vec4(0.0, 0.0, 0.0, uAlphaScale);
        }
        """
            .trimIndent()

    @JvmField
    val SHADER_PROVIDER_DEFAULT =
        object : ShaderProvider {
            override fun createFragmentShader(
                samplerVarName: String,
                fragCoordsVarName: String,
            ): String {
                return createFragmentShaderInternal(
                    samplerVarName,
                    fragCoordsVarName,
                    isHdr = false,
                )
            }
        }

    @JvmField
    val SHADER_PROVIDER_HDR_DEFAULT =
        object : ShaderProvider {
            override fun createFragmentShader(
                samplerVarName: String,
                fragCoordsVarName: String,
            ): String {
                return createFragmentShaderInternal(samplerVarName, fragCoordsVarName, isHdr = true)
            }
        }

    @JvmField
    val SHADER_PROVIDER_HDR_YUV =
        object : ShaderProvider {
            override fun createFragmentShader(
                samplerVarName: String,
                fragCoordsVarName: String,
            ): String {
                return createFragmentShaderInternal(
                    samplerVarName,
                    fragCoordsVarName,
                    isHdr = true,
                    isYuv = true,
                )
            }
        }

    private fun createVertexShader(
        fragCoordsVarName: String,
        isHdr: Boolean
    ): String {
        val version = if (isHdr) "#version 300 es" else ""
        val attr = if (isHdr) "in" else "attribute"
        val varying = if (isHdr) "out" else "varying"

        return """
            $version
            $attr vec4 aPosition;
            $attr vec4 aTextureCoord;
            uniform mat4 uTexMatrix;
            uniform mat4 uTransMatrix;
            $varying vec2 $fragCoordsVarName;
            void main() {
              gl_Position = uTransMatrix * aPosition;
              $fragCoordsVarName = (uTexMatrix * aTextureCoord).xy;
            }
        """
            .trimIndent()
            .trim()
    }

    private fun createFragmentShaderInternal(
        samplerVarName: String,
        fragCoordsVarName: String,
        isHdr: Boolean,
        isYuv: Boolean = false,
    ): String {
        if (isYuv) {
            // TODO(b/502166517): This yuvToRgb matrix assumes a specific YUV color space (BT.709).
            //  The matrix should be selected based on the actual input color space, similar to
            //  how it's done in Transformer.
            return """ 
                #version 300 es
                #extension GL_EXT_YUV_target : require
                precision mediump float;
                uniform __samplerExternal2DY2YEXT $samplerVarName;
                uniform float uAlphaScale;
                in vec2 $fragCoordsVarName;
                out vec4 outColor;

                vec3 yuvToRgb(vec3 yuv) {
                  const vec3 yuvOffset = vec3(0.0625, 0.5, 0.5);
                  const mat3 yuvToRgbColorMat = mat3(
                    1.1689f, 1.1689f, 1.1689f,
                    0.0000f, -0.1881f, 2.1502f,
                    1.6853f, -0.6530f, 0.0000f
                  );
                  return clamp(yuvToRgbColorMat * (yuv - yuvOffset), 0.0, 1.0);
                }

                void main() {
                  vec3 srcYuv = texture($samplerVarName, $fragCoordsVarName).xyz;
                  vec3 srcRgb = yuvToRgb(srcYuv);
                  outColor = vec4(srcRgb, uAlphaScale);
                }
            """
                .trimIndent()
                .trim()
        }

        val version = if (isHdr) "#version 300 es" else ""
        val extension =
            if (isHdr) {
                "#extension GL_OES_EGL_image_external_essl3 : require"
            } else {
                "#extension GL_OES_EGL_image_external : require"
            }
        val varying = if (isHdr) "in" else "varying"
        val outVar = if (isHdr) "out vec4 outColor;" else ""
        val textureFunc = if (isHdr) "texture" else "texture2D"
        val fragColor = if (isHdr) "outColor" else "gl_FragColor"

        return """
            $version
            $extension
            precision mediump float;
            uniform samplerExternalOES $samplerVarName;
            uniform float uAlphaScale;
            $varying vec2 $fragCoordsVarName;
            $outVar
            void main() {
                vec4 src = $textureFunc($samplerVarName, $fragCoordsVarName);
                $fragColor = vec4(src.rgb, src.a * uAlphaScale);
            }
        """
            .trimIndent()
            .trim()
    }
}
