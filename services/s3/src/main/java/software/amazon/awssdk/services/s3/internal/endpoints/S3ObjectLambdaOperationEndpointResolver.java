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

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.PartitionMetadata;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.internal.ConfiguredS3SdkHttpRequest;
import software.amazon.awssdk.services.s3.model.S3Request;
import software.amazon.awssdk.services.s3.model.WriteGetObjectResponseRequest;
import software.amazon.awssdk.utils.StringUtils;

@SdkInternalApi
public class S3ObjectLambdaOperationEndpointResolver implements S3EndpointResolver {
    private static final String SIGNING_NAME = "s3-object-lambda";

    @Override
    public ConfiguredS3SdkHttpRequest applyEndpointConfiguration(S3EndpointResolverContext context) {
        S3Request originalRequest = (S3Request) context.originalRequest();
        validateObjectLambdaRequest(originalRequest);

        S3Configuration configuration = context.serviceConfiguration();
        validateConfiguration(configuration);

        SdkHttpRequest updatedRequest = context.request();

        if (context.endpointOverride() == null) {
            String newHost = getUriForObjectLambdaOperation(context).getHost();

            if (!context.isDisableHostPrefixInjection()) {
                newHost = applyHostPrefix(newHost, originalRequest);
            }

            updatedRequest = updatedRequest.toBuilder()
                    .host(newHost)
                    .build();
        }

        return ConfiguredS3SdkHttpRequest.builder()
                .sdkHttpRequest(updatedRequest)
                .signingServiceModification(SIGNING_NAME)
                .build();
    }

    public static S3ObjectLambdaOperationEndpointResolver create() {
        return new S3ObjectLambdaOperationEndpointResolver();
    }

    private static void validateConfiguration(S3Configuration configuration) {
        validateNotAccelerateEnabled(configuration);
        validateNotDualStackEnabled(configuration);
    }

    private static void validateObjectLambdaRequest(SdkRequest request) {
        if (!(request instanceof WriteGetObjectResponseRequest)) {
            String msg = String.format("%s is not an S3 Object Lambda operation", request);
            throw new IllegalArgumentException(msg);
        }
    }

    private static void validateNotAccelerateEnabled(S3Configuration configuration) {
        if (configuration.accelerateModeEnabled()) {
            throw new IllegalArgumentException("S3 Object Lambda does not support accelerate endpoints");
        }
    }

    private static void validateNotDualStackEnabled(S3Configuration configuration) {
        if (configuration.dualstackEnabled()) {
            throw new IllegalArgumentException("S3 Object Lambda does not support dualstack endpoints");
        }
    }

    private static URI getUriForObjectLambdaOperation(S3EndpointResolverContext context) {
        PartitionMetadata clientPartitionMetadata = PartitionMetadata.of(context.region());

        return S3ObjectLambdaOperationEndpointBuilder.create()
                .domain(clientPartitionMetadata.dnsSuffix())
                .protocol(context.request().protocol())
                .region(context.region().id())
                .toUri();
    }

    private static String applyHostPrefix(String host, S3Request request) {
        String prefix =  getPrefix(request);
        if (!StringUtils.isBlank(prefix)) {
            return prefix + "." + host;
        }
        return host;
    }

    private static String getPrefix(S3Request request) {
        if (request instanceof WriteGetObjectResponseRequest) {
            return ((WriteGetObjectResponseRequest) request).requestRoute();
        }

        throw new RuntimeException("Unable to determine prefix for request " + request);
    }
}
