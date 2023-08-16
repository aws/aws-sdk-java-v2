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

package software.amazon.awssdk.services.s3.internal.crossregion;


import java.util.concurrent.CompletableFuture;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;

public class TestEndpointProvider implements S3EndpointProvider {
    S3EndpointProvider s3EndpointProvider = S3EndpointProvider.defaultProvider();
    @Override
    public CompletableFuture<Endpoint> resolveEndpoint(S3EndpointParams endpointParams) {
        return s3EndpointProvider.resolveEndpoint(endpointParams.copy(c -> c.bucket("test_prefix_"+endpointParams.bucket())));

    }
}