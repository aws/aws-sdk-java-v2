/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.codegen.jmespath.component;

import java.util.List;

/**
 * A function allowing users to easily transform and filter data in JMESPath expressions.
 *
 * https://jmespath.org/specification.html#functions-expressions
 */
public class FunctionExpression {
    private final String function;
    private final List<FunctionArg> functionArgs;

    public FunctionExpression(String function, List<FunctionArg> functionArgs) {
        this.function = function;
        this.functionArgs = functionArgs;
    }

    public String function() {
        return function;
    }

    public List<FunctionArg> functionArgs() {
        return functionArgs;
    }
}
