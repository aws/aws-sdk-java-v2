/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.s3.handlers;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.annotation.ReviewBeforeRelease;
import software.amazon.awssdk.handlers.AwsHandlerKeys;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.services.s3.BucketUtils;
import software.amazon.awssdk.services.s3.S3AdvancedConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsRequest;

public class EndpointAddressRequestHandler extends RequestHandler {

    private static List<Class> ACCELERATE_DISABLED_OPERATIONS = Arrays.asList(
            ListBucketsRequest.class, CreateBucketRequest.class, DeleteBucketRequest.class);

    @Override
    public SdkHttpFullRequest beforeRequest(SdkHttpFullRequest request) {

        S3AdvancedConfiguration advancedConfiguration =
                (S3AdvancedConfiguration) request.handlerContext(AwsHandlerKeys.SERVICE_ADVANCED_CONFIG);
        Object originalRequest = request.handlerContext(AwsHandlerKeys.REQUEST_CONFIG).getOriginalRequest();
        SdkHttpFullRequest.Builder mutableRequest = request.toBuilder();

        if (advancedConfiguration == null || !advancedConfiguration.pathStyleAccessEnabled()) {
            try {
                String bucketName = getBucketName(originalRequest);

                if (BucketUtils.isValidDnsBucketName(bucketName, false)) {
                    changeToDnsEndpoint(mutableRequest, bucketName);
                }
            } catch (Exception e) {
                // Unable to convert to DNS style addressing. Fall back to continue using path style.
            }
        }

        // Remove accelerate from operations that don't allow it
        if (advancedConfiguration != null && advancedConfiguration.accelerateModeEnabled()
            && ACCELERATE_DISABLED_OPERATIONS.contains(originalRequest.getClass())) {
            removeAccelerate(mutableRequest);
        }

        return mutableRequest.build();
    }

    @ReviewBeforeRelease("Remove reflection here. Have some kind of interface where we can get bucket name or pass it" +
                         "in the handler context")
    private String getBucketName(Object originalRequest) throws IllegalAccessException, InvocationTargetException,
                                                                NoSuchMethodException {
        return (String) originalRequest.getClass()
                                       .getMethod("bucket")
                                       .invoke(originalRequest);
    }

    private void removeAccelerate(SdkHttpFullRequest.Builder mutableRequest) {
        mutableRequest.endpoint(URI.create(mutableRequest.getEndpoint().toASCIIString().replaceFirst("s3-accelerate", "s3")));
    }

    private void changeToDnsEndpoint(SdkHttpFullRequest.Builder mutableRequest, String bucketName) {
        // Replace /bucketName from resourcePath with nothing
        String resourcePath = mutableRequest.getResourcePath().replaceFirst("/" + bucketName, "");

        // Prepend bucket to endpoint
        URI endpoint = invokeSafely(() -> new URI(
                mutableRequest.getEndpoint().getScheme(), // Existing scheme
                // replace "s3" with "bucket.s3"
                mutableRequest.getEndpoint().getHost().replaceFirst("s3", bucketName + "." + "s3"),
                null,
                null));

        mutableRequest.endpoint(endpoint);

        mutableRequest.resourcePath(resourcePath);
    }
}
