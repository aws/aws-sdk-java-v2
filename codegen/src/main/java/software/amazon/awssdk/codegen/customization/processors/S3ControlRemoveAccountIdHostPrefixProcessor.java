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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.customization.CodegenCustomizationProcessor;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.service.EndpointTrait;
import software.amazon.awssdk.codegen.model.service.Operation;
import software.amazon.awssdk.codegen.model.service.ServiceModel;

/**
 * With Endpoints 2.0, the endpoint rule set is responsible for adding the prefix, so we remove it from the model to avoid
 * errors in constructing the endpoint.
 */
public class S3ControlRemoveAccountIdHostPrefixProcessor implements CodegenCustomizationProcessor {
    private static final String ACCOUNT_ID_HOST_PREFIX = "{AccountId}.";
    private static final Logger log = LoggerFactory.getLogger(S3ControlRemoveAccountIdHostPrefixProcessor.class);

    @Override
    public void preprocess(ServiceModel serviceModel) {
        if (!isS3Control(serviceModel)) {
            return;
        }

        log.info("Preprocessing S3 Control model to remove '{AccountId}.' hostPrefix from operations");

        serviceModel.getOperations().forEach(this::removeAccountIdHostPrefixIfNecessary);
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        // no-op
    }

    private boolean isS3Control(ServiceModel serviceModel) {
        return "S3 Control".equals(serviceModel.getMetadata().getServiceId());
    }

    private void removeAccountIdHostPrefixIfNecessary(String opName, Operation operation) {
        EndpointTrait endpoint = operation.getEndpoint();
        if (endpoint == null) {
            return;
        }

        if (ACCOUNT_ID_HOST_PREFIX.equals(endpoint.getHostPrefix())) {
            log.info("{}: Removing '{AccountId}.' hostPrefix", opName);
            endpoint.setHostPrefix(null);
        }
    }
}
