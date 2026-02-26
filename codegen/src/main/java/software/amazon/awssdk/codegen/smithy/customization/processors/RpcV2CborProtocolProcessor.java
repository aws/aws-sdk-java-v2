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

package software.amazon.awssdk.codegen.smithy.customization.processors;

import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.smithy.customization.SmithyCustomizationProcessor;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Smithy equivalent of {@link software.amazon.awssdk.codegen.customization.processors.RpcV2CborProtocolProcessor}.
 *
 * <p>This is a Category D (no-op placeholder) processor kept for pipeline parity with the C2J
 * chain. The C2J processor modifies the {@code ServiceModel} protocol field, but Smithy models
 * express protocol via traits which may already be correct.
 */
public final class RpcV2CborProtocolProcessor implements SmithyCustomizationProcessor {

    // TODO: Investigate whether Smithy models require any RPC v2 CBOR protocol
    //  adjustments. The C2J processor modifies the ServiceModel protocol field,
    //  but Smithy models express protocol via traits which may already be correct.

    @Override
    public Model preprocess(Model model, ServiceShape service) {
        return model; // no-op
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        // no-op
    }
}
