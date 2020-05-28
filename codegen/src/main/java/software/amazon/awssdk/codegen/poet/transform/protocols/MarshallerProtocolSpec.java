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

package software.amazon.awssdk.codegen.poet.transform.protocols;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface MarshallerProtocolSpec {

    ParameterSpec protocolFactoryParameter();

    CodeBlock marshalCodeBlock(ClassName requestClassName);

    FieldSpec protocolFactory();

    default List<FieldSpec> memberVariables() {
        return new ArrayList<>();
    }

    default List<FieldSpec> additionalFields() {
        return new ArrayList<>();
    }

    default List<MethodSpec> additionalMethods() {
        return new ArrayList<>();
    }

    default Optional<MethodSpec> constructor() {
        return Optional.empty();
    }
}
