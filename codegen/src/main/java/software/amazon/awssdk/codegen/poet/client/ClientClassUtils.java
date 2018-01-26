/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.poet.client;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import java.util.Optional;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.core.http.HttpResponseHandler;

public final class ClientClassUtils {
    private ClientClassUtils() {
    }

    protected static Optional<CodeBlock> getCustomResponseHandler(OperationModel operationModel, ClassName returnType) {
        Optional<String> customUnmarshaller = Optional.ofNullable(operationModel.getOutputShape())
                                                      .map(ShapeModel::getCustomization)
                                                      .flatMap(c -> Optional.ofNullable(c.getCustomUnmarshallerFqcn()));
        return customUnmarshaller.map(unmarshaller -> {
            if (operationModel.hasStreamingOutput()) {
                throw new UnsupportedOperationException("Custom unmarshallers cannot be applied to streaming operations yet.");
            }

            return CodeBlock.builder().add("$T<$T> responseHandler = (response, __) -> new $T().unmarshall(response);",
                                           HttpResponseHandler.class,
                                           returnType,
                                           ClassName.bestGuess(unmarshaller)).build();
        });
    }
}
