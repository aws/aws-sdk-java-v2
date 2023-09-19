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

package software.amazon.awssdk.services.s3.internal.crossregion.utils;


import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.internal.crossregion.endpointprovider.BucketEndpointProvider;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Request;

@SdkInternalApi
public final class CrossRegionUtils {
    public static final int REDIRECT_STATUS_CODE = 301;
    public static final int TEMPORARY_REDIRECT_STATUS_CODE = 307;
    public static final String AMZ_BUCKET_REGION_HEADER = "x-amz-bucket-region";
    private static final ApiName API_NAME = ApiName.builder().version("cross-region").name("hll").build();
    private static final Consumer<AwsRequestOverrideConfiguration.Builder> USER_AGENT_APPLIER = b -> b.addApiName(API_NAME);


    private CrossRegionUtils() {
    }

    public static Optional<String> getBucketRegionFromException(S3Exception exception) {
        return exception.awsErrorDetails()
                        .sdkHttpResponse()
                        .firstMatchingHeader(AMZ_BUCKET_REGION_HEADER);
    }

    public static boolean isS3RedirectException(Throwable exception) {
        Throwable exceptionToBeChecked = exception instanceof CompletionException ? exception.getCause() : exception;
        return exceptionToBeChecked instanceof S3Exception
               && isRedirectError((S3Exception) exceptionToBeChecked);
    }

    private static boolean isRedirectError(S3Exception exceptionToBeChecked) {
        int statusCode = exceptionToBeChecked.statusCode();
        return statusCode == REDIRECT_STATUS_CODE || statusCode == TEMPORARY_REDIRECT_STATUS_CODE;
    }


    @SuppressWarnings("unchecked")
    public static <T extends S3Request> T requestWithDecoratedEndpointProvider(T request, Supplier<Region> regionSupplier,
                                                                               EndpointProvider clientEndpointProvider) {
        AwsRequestOverrideConfiguration requestOverrideConfig =
            request.overrideConfiguration().orElseGet(() -> AwsRequestOverrideConfiguration.builder().build());

        S3EndpointProvider delegateEndpointProvider = (S3EndpointProvider) requestOverrideConfig.endpointProvider()
                                                                                                .orElse(clientEndpointProvider);
        return (T) request.toBuilder()
                          .overrideConfiguration(
                              requestOverrideConfig.toBuilder()
                                                   .endpointProvider(
                                                       BucketEndpointProvider.create(delegateEndpointProvider, regionSupplier))
                                                   .build())
                          .build();
    }

    public static <T extends S3Request> AwsRequestOverrideConfiguration updateUserAgentInConfig(T request) {
        AwsRequestOverrideConfiguration overrideConfiguration =
            request.overrideConfiguration().map(c -> c.toBuilder()
                                                      .applyMutation(USER_AGENT_APPLIER)
                                                      .build())
                   .orElse(AwsRequestOverrideConfiguration.builder()
                                                          .applyMutation(USER_AGENT_APPLIER)
                                                          .build());
        return overrideConfiguration;
    }
}
