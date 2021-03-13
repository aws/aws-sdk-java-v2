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

package software.amazon.awssdk.services.s3.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.crt.auth.credentials.CredentialsProvider;
import software.amazon.awssdk.crt.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@SdkInternalApi
public final class S3CrtUtils {

    private S3CrtUtils() {
    }

    // TODO: Add more adapters if there are any new crt credentials providers.

    /**
     * Adapter between the sdk credentials provider and the crt credentials provider.
     */
    public static CredentialsProvider createCrtCredentialsProvider(AwsCredentialsProvider awsCredentialsProvider) {
        AwsCredentials sdkCredentials = awsCredentialsProvider.resolveCredentials();
        StaticCredentialsProvider.StaticCredentialsProviderBuilder builder =
            new StaticCredentialsProvider.StaticCredentialsProviderBuilder();

        if (sdkCredentials instanceof AwsSessionCredentials) {
            builder.withSessionToken(((AwsSessionCredentials) sdkCredentials).sessionToken().getBytes());
        }

        return builder.withAccessKeyId(sdkCredentials.accessKeyId().getBytes())
                      .withSecretAccessKey(sdkCredentials.secretAccessKey().getBytes())
                      .build();
    }

    // TODO: codegen
    public static com.amazonaws.s3.model.GetObjectRequest adaptGetObjectRequest(GetObjectRequest request) {
        return com.amazonaws.s3.model.GetObjectRequest.builder()
                                                      .key(request.key())
                                                      .bucket(request.bucket())
                                                      .expectedBucketOwner(request.expectedBucketOwner())
                                                      .ifMatch(request.ifMatch())
                                                      .ifModifiedSince(request.ifModifiedSince())
                                                      .ifNoneMatch(request.ifNoneMatch())
                                                      .build();
    }

    // TODO: codegen
    public static GetObjectResponse adaptGetObjectOutput(com.amazonaws.s3.model.GetObjectOutput response) {
        return GetObjectResponse.builder()
                                .bucketKeyEnabled(response.bucketKeyEnabled())
                                .acceptRanges(response.acceptRanges())
                                .contentDisposition(response.contentDisposition())
                                .cacheControl(response.cacheControl())
                                .contentEncoding(response.contentEncoding())
                                .contentLanguage(response.contentLanguage())
                                .contentRange(response.contentRange())
                                .contentLength(response.contentLength())
                                .contentType(response.contentType())
                                .deleteMarker(response.deleteMarker())
                                .eTag(response.eTag())
                                .expiration(response.expiration())
                                .expires(response.expires())
                                .lastModified(response.lastModified())
                                .metadata(response.metadata())
                                .build();
    }
}
