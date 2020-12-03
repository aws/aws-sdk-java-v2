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

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Get endpoint resolver.
 */
@SdkInternalApi
public final class S3EndpointResolverFactory {

    private static final S3EndpointResolver ACCESS_POINT_ENDPOINT_RESOLVER = S3AccessPointEndpointResolver.create();
    private static final S3EndpointResolver BUCKET_ENDPOINT_RESOLVER = S3BucketEndpointResolver.create();

    private S3EndpointResolverFactory() {
    }

    public static S3EndpointResolver getEndpointResolver(String bucketName) {
        if (bucketName != null && S3EndpointUtils.isArn(bucketName)) {
            return ACCESS_POINT_ENDPOINT_RESOLVER;
        }
        return BUCKET_ENDPOINT_RESOLVER;
    }
}
