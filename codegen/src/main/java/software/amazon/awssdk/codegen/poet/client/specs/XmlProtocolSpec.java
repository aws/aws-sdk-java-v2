/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.poet.client.specs;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.poet.PoetExtensions;
import software.amazon.awssdk.core.http.HttpResponseHandler;
import software.amazon.awssdk.protocols.xml.AwsXmlProtocolFactory;
import software.amazon.awssdk.protocols.xml.XmlOperationMetadata;

public final class XmlProtocolSpec extends QueryProtocolSpec {

    private final IntermediateModel model;

    public XmlProtocolSpec(IntermediateModel model,
                           PoetExtensions poetExtensions) {
        super(model, poetExtensions);
        this.model = model;
    }

    @Override
    protected Class<?> protocolFactoryClass() {
        if (model.getCustomizationConfig().getCustomProtocolFactoryFqcn() != null) {
            try {
                return Class.forName(model.getCustomizationConfig().getCustomProtocolFactoryFqcn());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Could not find custom protocol factory class", e);
            }
        }
        return AwsXmlProtocolFactory.class;
    }

    @Override
    public CodeBlock responseHandler(IntermediateModel model,
                                     OperationModel opModel) {
        ClassName responseType = poetExtensions.getModelClass(opModel.getReturnType().getReturnType());

        return CodeBlock.builder()
                        .addStatement("\n\n$T<$T> responseHandler = protocolFactory.createResponseHandler($T::builder,"
                                      + "new $T().withHasStreamingSuccessResponse($L))",
                                      HttpResponseHandler.class,
                                      responseType,
                                      responseType,
                                      XmlOperationMetadata.class,
                                      opModel.hasStreamingOutput())
                        .build();
    }

}
