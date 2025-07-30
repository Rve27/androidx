package appfunctions_aggregated_deps

import androidx.appfunctions.AppFunctionComponentRegistry
import javax.`annotation`.processing.Generated

@AppFunctionComponentRegistry(
  componentCategory = "SERIALIZABLE",
  componentNames = [
    "com.testdata.DerivedInputToFakeFreeFormFunction",
    "com.testdata.InputToFakeFreeFormFunction",
    "com.testdata.InputWithParametrizedTypes",
    "com.testdata.InputWithRepeatedTypes",
    "com.testdata.NotDescribedByKdoc",
    "com.testdata.Optional",
  ],
  componentDocStrings = [
    "A child class of [InputToFakeFreeFormFunction]. ",
    "A test input class to the free-form function. ",
    "An input class with parametrized types ",
    "An input class with repeated params. ",
    "",
    "A generic serializable class. ",
  ],
)
@Generated("androidx.appfunctions.compiler.AppFunctionCompiler")
public class `$Main_SerializableComponentRegistry`
