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

package androidx.appfunctions.compiler.core

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType

/**
 * A class that represents a class annotated with @AppFunctionSerializableProxy.
 *
 * @param appFunctionSerializableProxyClass The class annotated with @AppFunctionSerializableProxy.
 */
data class AnnotatedAppFunctionSerializableProxy(
    private val appFunctionSerializableProxyClass: KSClassDeclaration
) : AnnotatedAppFunctionSerializable(appFunctionSerializableProxyClass) {

    /** The type of the class that the proxy class is proxying. */
    val targetClassDeclaration: KSClassDeclaration by lazy {
        (appFunctionSerializableProxyClass.annotations.findAnnotation(
                IntrospectionHelper.AppFunctionSerializableProxyAnnotation.CLASS_NAME
            )
                ?: throw ProcessingException(
                    "Class Must have @AppFunctionSerializableProxy annotation",
                    appFunctionSerializableProxyClass
                ))
            .requirePropertyValueOfType(
                IntrospectionHelper.AppFunctionSerializableProxyAnnotation.PROPERTY_TARGET_CLASS,
                KSType::class
            )
            .declaration as KSClassDeclaration
    }

    /**
     * Validates the class annotated with @AppFunctionSerializableProxy.
     *
     * @return The validated class.
     */
    override fun validate(): AnnotatedAppFunctionSerializableProxy {
        super.validate()
        validateProxyHasToTargetClassMethod()
        validateProxyHasFromTargetClassMethod()
        return this
    }

    /** Validates that the proxy class has a method that returns an instance of the target class. */
    private fun validateProxyHasToTargetClassMethod() {
        val targetClassName = checkNotNull(targetClassDeclaration.simpleName).asString()
        val toTargetClassNameFunctionName = "to$targetClassName"
        val toTargetClassNameFunctionList: List<KSFunctionDeclaration> =
            appFunctionSerializableProxyClass
                .getAllFunctions()
                .filter { it.simpleName.asString() == toTargetClassNameFunctionName }
                .toList()
        if (toTargetClassNameFunctionList.size != 1) {
            throw ProcessingException(
                "Class must have exactly one member function: $toTargetClassNameFunctionName",
                appFunctionSerializableProxyClass
            )
        }
        val toTargetClassNameFunction = toTargetClassNameFunctionList.first()
        if (
            checkNotNull(
                    checkNotNull(toTargetClassNameFunction.returnType)
                        .resolve()
                        .declaration
                        .qualifiedName
                )
                .asString() != checkNotNull(targetClassDeclaration.qualifiedName).asString()
        ) {
            throw ProcessingException(
                "Function $toTargetClassNameFunctionName should return an instance of target class",
                appFunctionSerializableProxyClass
            )
        }
    }

    /** Validates that the proxy class has a method that returns an instance of the target class. */
    private fun validateProxyHasFromTargetClassMethod() {
        val targetClassName = checkNotNull(targetClassDeclaration.simpleName).asString()
        val fromTargetClassNameFunctionName = "from$targetClassName"
        val targetCompanionClass =
            appFunctionSerializableProxyClass.declarations
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.isCompanionObject }
                .single()

        val fromTargetClassNameFunctionList =
            targetCompanionClass
                .getAllFunctions()
                .filter { it.simpleName.asString() == fromTargetClassNameFunctionName }
                .toList()
        if (fromTargetClassNameFunctionList.size != 1) {
            throw ProcessingException(
                "Companion Class must have exactly one member function: " +
                    fromTargetClassNameFunctionName,
                appFunctionSerializableProxyClass
            )
        }
        val fromTargetClassNameFunction = fromTargetClassNameFunctionList.first()
        if (
            fromTargetClassNameFunction.parameters.size != 1 ||
                fromTargetClassNameFunction.parameters.first().type.toTypeName().toString() !=
                    checkNotNull(targetClassDeclaration.qualifiedName).asString()
        ) {
            throw ProcessingException(
                "Function $fromTargetClassNameFunctionName should have one parameter of type " +
                    targetClassName,
                appFunctionSerializableProxyClass
            )
        }
        if (
            checkNotNull(fromTargetClassNameFunction.returnType).toTypeName().toString() !=
                checkNotNull(appFunctionSerializableProxyClass.qualifiedName).asString()
        ) {
            throw ProcessingException(
                "Function $fromTargetClassNameFunctionName should return an instance of " +
                    "this serializable class",
                appFunctionSerializableProxyClass
            )
        }
    }
}
