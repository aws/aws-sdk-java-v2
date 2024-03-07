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

package software.amazon.awssdk.codegen.model.internal;

import com.squareup.javapoet.CodeBlock;

/**
 * Represents a generic parameter that can be code generated, but isn't tied to a model shape
 */
public class LocalParameter {

    private final String name;
    private final Class<?> type;
    private final CodeBlock documentation;

    public LocalParameter(String name, Class<?> type, CodeBlock documentation) {
        this.name = name;
        this.type = type;
        this.documentation = documentation;
    }

    public String name() {
        return name;
    }

    public Class<?> type() {
        return type;
    }

    public CodeBlock documentation() {
        return documentation;
    }
}
