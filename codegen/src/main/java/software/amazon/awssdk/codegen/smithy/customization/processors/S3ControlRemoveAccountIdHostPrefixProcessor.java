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
import software.amazon.awssdk.utils.Logger;
import software.amazon.smithy.aws.traits.ServiceTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.EndpointTrait;

/**
 * Smithy equivalent of
 * {@link software.amazon.awssdk.codegen.customization.processors.S3ControlRemoveAccountIdHostPrefixProcessor}.
 *
 * <p>This is a Category B processor (direct Smithy equivalent). With Endpoints 2.0, the endpoint
 * rule set for S3 Control is responsible for prepending the account ID to the host. To avoid
 * duplication, we preprocess the model to remove the {@code {AccountId}.} host prefix from the
 * {@code @endpoint} trait on applicable operations.
 */
public final class S3ControlRemoveAccountIdHostPrefixProcessor implements SmithyCustomizationProcessor {

    private static final Logger log = Logger.loggerFor(S3ControlRemoveAccountIdHostPrefixProcessor.class);

    private static final String ACCOUNT_ID_HOST_PREFIX = "{AccountId}.";

    @Override
    public Model preprocess(Model model, ServiceShape service) {
        if (!isS3Control(service)) {
            return model;
        }

        log.info(() -> "Preprocessing S3 Control model to remove AccountId host prefix from operations");

        Model.Builder builder = model.toBuilder();
        boolean modified = false;

        for (ShapeId operationId : service.getOperations()) {
            OperationShape operation = model.expectShape(operationId, OperationShape.class);
            EndpointTrait endpointTrait = operation.getTrait(EndpointTrait.class).orElse(null);
            if (endpointTrait == null) {
                continue;
            }

            String hostPrefix = endpointTrait.getHostPrefix().toString();
            if (!ACCOUNT_ID_HOST_PREFIX.equals(hostPrefix)) {
                continue;
            }

            log.info(() -> String.format("%s: removing AccountId host prefix '%s'",
                                         operationId.getName(), ACCOUNT_ID_HOST_PREFIX));

            OperationShape updatedOp = operation.toBuilder()
                .removeTrait(EndpointTrait.ID)
                .build();
            builder.addShape(updatedOp);
            modified = true;
        }

        return modified ? builder.build() : model;
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        // no-op
    }

    private static boolean isS3Control(ServiceShape service) {
        return service.getTrait(ServiceTrait.class)
            .map(t -> "S3 Control".equals(t.getSdkId()))
            .orElse(false);
    }
}
