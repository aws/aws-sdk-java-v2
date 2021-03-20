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

package software.amazon.awssdk.services.s3.internal.endpoints;

import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.services.s3.model.S3Request;
import software.amazon.awssdk.services.s3.model.WriteGetObjectResponseRequest;

/**
 * Get endpoint resolver.
 */
@SdkInternalApi
public final class S3EndpointResolverFactory {

    private static final S3EndpointResolver ACCESS_POINT_ENDPOINT_RESOLVER = S3AccessPointEndpointResolver.create();
    private static final S3EndpointResolver BUCKET_ENDPOINT_RESOLVER = S3BucketEndpointResolver.create();
    private static final S3EndpointResolver OBJECT_LAMBDA_OPERATION_RESOLVER = S3ObjectLambdaOperationEndpointResolver.create();

    private S3EndpointResolverFactory() {
    }

    public static S3EndpointResolver getEndpointResolver(S3EndpointResolverFactoryContext context) {
        Optional<String> bucketName = context.bucketName();
        if (bucketName.isPresent() && S3EndpointUtils.isArn(bucketName.get())) {
            return ACCESS_POINT_ENDPOINT_RESOLVER;
        }

        if (isObjectLambdaRequest(context.originalRequest())) {
            return OBJECT_LAMBDA_OPERATION_RESOLVER;
        }

        return BUCKET_ENDPOINT_RESOLVER;
    }

    private static boolean isObjectLambdaRequest(S3Request request) {
        return request instanceof WriteGetObjectResponseRequest;
    }
}
