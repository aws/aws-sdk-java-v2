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

package software.amazon.awssdk.codegen.customization.processors;

import software.amazon.awssdk.codegen.customization.CodegenCustomizationProcessor;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.Http;
import software.amazon.awssdk.codegen.model.service.Operation;
import software.amazon.awssdk.codegen.model.service.ServiceModel;

/**
 * This processor only runs for services using the <code>smithy-rpc-v2-cbor</code> protocol.
 *
 * Sets a request URI that conforms to the Smithy RPC v2 protocol to each operation in the model.
 */
public class SmithyRpcV2CborProtocolProcessor implements CodegenCustomizationProcessor {
    @Override
    public void preprocess(ServiceModel serviceModel) {
        if (!"smithy-rpc-v2-cbor".equals(serviceModel.getMetadata().getProtocol())) {
            return;
        }
        serviceModel.getOperations().forEach((name, op) -> setRequestUri(serviceModel, name, op));
    }

    private void setRequestUri(ServiceModel service, String name, Operation op) {
        String uri = String.format("/service/%s/operation/%s", service.getMetadata().getTargetPrefix(), op.getName());
        if (op.getHttp() == null) {
            op.setHttp(new Http().withMethod("POST").withResponseCode("200"));
        }
        op.getHttp().withRequestUri(uri);
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
    }
}
