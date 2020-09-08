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

package software.amazon.awssdk.codegen.poet.waiters;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.WaiterDefinition;
import software.amazon.awssdk.codegen.poet.ClassSpec;

public class WaitersClassSpec implements ClassSpec {

    public WaitersClassSpec(IntermediateModel model, String operationName, WaiterDefinition waiterDefinition) {
    }

    @Override
    public TypeSpec poetSpec() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClassName className() {
        throw new UnsupportedOperationException();
    }
}
