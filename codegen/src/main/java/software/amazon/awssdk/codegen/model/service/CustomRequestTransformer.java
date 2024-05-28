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

package software.amazon.awssdk.codegen.model.service;

/**
 * Represents a custom request transformer for API requests.
 *
 * <p>This class allows for dynamic and specific transformation of API requests,
 * ensuring that each request is appropriately transformed based on the
 * transformation logic defined in the specified {@link CustomRequestTransformer#getClassName()} and
 * {@link CustomRequestTransformer#getMethodName()}.
 *
 * <p>Example:
 * <pre>
 * {
 *     "methodName": "dummyRequestModifier",
 *     "className": "software.amazon.awssdk.codegen.internal.UtilsTest"
 * }
 * </pre>
 *
 * <p>The class should have a public static method   dummyRequestModifier
 * that takes an input and returns an output of ApiRequest for which Customization is applied.
 */

public class CustomRequestTransformer {

    /**
     * The fully qualified name of the class that defines the transformation method. The {@code methodName} is the
     */
    private String className;

    /**
     * The name of the method within that class which will perform the transformation
     */
    private String methodName;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}