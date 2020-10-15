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
import software.amazon.awssdk.services.s3.internal.ConfiguredS3SdkHttpRequest;

/**
 * An S3 endpoint resolver returns a {@link ConfiguredS3SdkHttpRequest} based on the HTTP context and previously
 * set execution attributes.
 * <p/>
 * @see software.amazon.awssdk.services.s3.internal.handlers.EndpointAddressInterceptor
 */
@SdkInternalApi
public interface S3EndpointResolver {

    ConfiguredS3SdkHttpRequest applyEndpointConfiguration(S3EndpointResolverContext context);
}
