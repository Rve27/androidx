package com.testdata

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.service.AppFunction

class FakeFreeFormFunction {
    /**
     * A fake free-form AppFunction to test the description element in the function's generated
     * metadata xml.
     *
     * This kdoc consists of multiple paragraphs and is used as the function's description to the
     * agent.
     *
     * @param appFunctionContext the AppFunction's context.
     * @param inputString the input string.
     * @return the output string.
     * @throws IllegalArgumentException if the inputString is in an incorrect format.
     */
    @AppFunction
    fun fakeFreeFormFunction(appFunctionContext: AppFunctionContext, inputString: String): String {
        return ""
    }
}
