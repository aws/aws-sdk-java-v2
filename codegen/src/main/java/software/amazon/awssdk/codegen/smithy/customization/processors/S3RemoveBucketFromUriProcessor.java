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
import software.amazon.smithy.model.pattern.UriPattern;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpTrait;

/**
 * Smithy equivalent of {@link software.amazon.awssdk.codegen.customization.processors.S3RemoveBucketFromUriProcessor}.
 *
 * <p>This is a Category B processor (direct Smithy equivalent). With Endpoints 2.0, the endpoint
 * rule set for S3 is responsible for taking the {@code Bucket} parameter from the input and adding
 * it to the request URI. To make this work, we preprocess the model to remove {@code /{Bucket}}
 * from the {@code @http} trait URI so that the marshallers don't add it to the path as well.
 */
public final class S3RemoveBucketFromUriProcessor implements SmithyCustomizationProcessor {

    private static final Logger log = Logger.loggerFor(S3RemoveBucketFromUriProcessor.class);

    private static final String BUCKET_URI_SEGMENT = "/{Bucket}";

    @Override
    public Model preprocess(Model model, ServiceShape service) {
        if (!isS3(service)) {
            return model;
        }

        log.info(() -> "Preprocessing S3 model to remove {Bucket} from request URIs");

        Model.Builder builder = model.toBuilder();
        boolean modified = false;

        for (ShapeId operationId : service.getOperations()) {
            OperationShape operation = model.expectShape(operationId, OperationShape.class);
            HttpTrait httpTrait = operation.getTrait(HttpTrait.class).orElse(null);
            if (httpTrait == null) {
                continue;
            }

            String uri = httpTrait.getUri().toString();
            if (!uri.contains(BUCKET_URI_SEGMENT)) {
                continue;
            }

            String newUri = uri.replace(BUCKET_URI_SEGMENT, "");
            if (newUri.isEmpty()) {
                newUri = "/";
            }

            String finalNewUri = newUri;
            log.info(() -> String.format("%s: replacing existing request URI '%s' with '%s'",
                                         operationId.getName(), uri, finalNewUri));

            HttpTrait updatedTrait = httpTrait.toBuilder()
                .uri(UriPattern.parse(newUri))
                .build();
            OperationShape updatedOp = operation.toBuilder()
                .addTrait(updatedTrait)
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

    private static boolean isS3(ServiceShape service) {
        return service.getTrait(ServiceTrait.class)
            .map(t -> "S3".equals(t.getSdkId()))
            .orElse(false);
    }
}
