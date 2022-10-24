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
import software.amazon.awssdk.codegen.model.service.Operation;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.utils.StringUtils;

/**
 * With Endpoints 2.0, the endpoint rule set for S3 is responsible for taking the {@code Bucket} parameter from the input and
 * adding it to the request URI. To make this work, we preprocess the model to remove it from the HTTP definition so that the
 * marshallers don't add it to the path as well.
 */
public class S3RemoveBucketFromUriProcessor implements CodegenCustomizationProcessor {
    private static final Logger log = LoggerFactory.getLogger(S3RemoveBucketFromUriProcessor.class);

    @Override
    public void preprocess(ServiceModel serviceModel) {
        if (!isS3(serviceModel)) {
            return;
        }

        log.info("Preprocessing S3 model to remove {Bucket} from request URIs");

        serviceModel.getOperations().forEach(this::removeBucketFromUriIfNecessary);
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        // no-op
    }

    private boolean isS3(ServiceModel serviceModel) {
        return "S3".equals(serviceModel.getMetadata().getServiceId());
    }

    private void removeBucketFromUriIfNecessary(String opName, Operation operation) {
        String requestUri = operation.getHttp().getRequestUri();
        String newUri = StringUtils.replaceOnce(requestUri, "/{Bucket}", "");

        log.info("{}: replacing existing request URI '{}' with '{}", opName, requestUri, newUri);

        operation.getHttp().setRequestUri(newUri);
    }
}
