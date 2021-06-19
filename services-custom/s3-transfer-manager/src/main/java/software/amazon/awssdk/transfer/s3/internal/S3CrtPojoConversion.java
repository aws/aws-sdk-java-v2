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

package software.amazon.awssdk.transfer.s3.internal;

import com.amazonaws.s3.model.GetObjectOutput;
import com.amazonaws.s3.model.ObjectCannedACL;
import com.amazonaws.s3.model.ObjectLockLegalHoldStatus;
import com.amazonaws.s3.model.ObjectLockMode;
import com.amazonaws.s3.model.PutObjectOutput;
import com.amazonaws.s3.model.RequestPayer;
import com.amazonaws.s3.model.ServerSideEncryption;
import com.amazonaws.s3.model.StorageClass;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.awscore.DefaultAwsResponseMetadata;
import software.amazon.awssdk.core.util.SdkUserAgent;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3ResponseMetadata;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * Helper class to convert CRT POJOs to SDK POJOs and vice versa
 */
//TODO: codegen this class in the future
@SdkInternalApi
public final class S3CrtPojoConversion {
    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final String USER_AGENT_STRING = SdkUserAgent.create().userAgent() + " ft/s3-transfer";

    private S3CrtPojoConversion() {
    }

    public static com.amazonaws.s3.model.GetObjectRequest toCrtGetObjectRequest(GetObjectRequest request) {
        com.amazonaws.s3.model.GetObjectRequest.Builder getObjectBuilder =
            com.amazonaws.s3.model.GetObjectRequest.builder()
                                                   .bucket(request.bucket())
                                                   .ifMatch(request.ifMatch())
                                                   .ifModifiedSince(request.ifModifiedSince())
                                                   .ifNoneMatch(request.ifNoneMatch())
                                                   .ifUnmodifiedSince(request.ifUnmodifiedSince())
                                                   .key(request.key())
                                                   .range(request.range())
                                                   .responseCacheControl(request.responseCacheControl())
                                                   .responseContentDisposition(request.responseContentDisposition())
                                                   .responseContentEncoding(request.responseContentEncoding())
                                                   .responseContentLanguage(request.responseContentLanguage())
                                                   .responseContentType(request.responseContentType())
                                                   .responseExpires(request.responseExpires())
                                                   .versionId(request.versionId())
                                                   .sSECustomerAlgorithm(request.sseCustomerAlgorithm())
                                                   .sSECustomerKey(request.sseCustomerKey())
                                                   .sSECustomerKeyMD5(request.sseCustomerKeyMD5())
                                                   .requestPayer(RequestPayer.fromValue(request.requestPayerAsString()))
                                                   .partNumber(request.partNumber())
                                                   .expectedBucketOwner(request.expectedBucketOwner());

        processRequestOverrideConfiguration(request.overrideConfiguration().orElse(null),
                                            getObjectBuilder::customQueryParameters);

        addCustomHeaders(request.overrideConfiguration().orElse(null), getObjectBuilder::customHeaders);

        return getObjectBuilder.build();

    }

    public static GetObjectResponse fromCrtGetObjectOutput(GetObjectOutput response, SdkHttpResponse sdkHttpResponse) {
        S3ResponseMetadata s3ResponseMetadata = createS3ResponseMetadata(sdkHttpResponse);

        GetObjectResponse.Builder builder = GetObjectResponse.builder()
                                                             .deleteMarker(response.deleteMarker())
                                                             .acceptRanges(response.acceptRanges())
                                                             .expiration(response.expiration())
                                                             .restore(response.restore())
                                                             .lastModified(response.lastModified())
                                                             .contentLength(response.contentLength())
                                                             .eTag(response.eTag())
                                                             .missingMeta(response.missingMeta())
                                                             .versionId(response.versionId())
                                                             .cacheControl(response.cacheControl())
                                                             .contentDisposition(response.contentDisposition())
                                                             .contentEncoding(response.contentEncoding())
                                                             .contentLanguage(response.contentLanguage())
                                                             .contentRange(response.contentRange())
                                                             .contentType(response.contentType())
                                                             .expires(response.expires())
                                                             .websiteRedirectLocation(response.websiteRedirectLocation())
                                                             .metadata(response.metadata())
                                                             .sseCustomerAlgorithm(response.sSECustomerAlgorithm())
                                                             .sseCustomerKeyMD5(response.sSECustomerKeyMD5())
                                                             .ssekmsKeyId(response.sSEKMSKeyId())
                                                             .bucketKeyEnabled(response.bucketKeyEnabled())
                                                             .partsCount(response.partsCount())
                                                             .tagCount(response.tagCount())
                                                             .objectLockRetainUntilDate(response.objectLockRetainUntilDate());

        if (response.serverSideEncryption() != null) {
            builder.serverSideEncryption(response.serverSideEncryption().name());
        }

        if (response.storageClass() != null) {
            builder.storageClass(response.storageClass().name());
        }

        if (response.requestCharged() != null) {
            builder.requestCharged(response.requestCharged().name());
        }

        if (response.replicationStatus() != null) {
            builder.replicationStatus(response.replicationStatus().name());
        }

        if (response.objectLockMode() != null) {
            builder.objectLockMode(response.objectLockMode().name());
        }

        if (response.objectLockLegalHoldStatus() != null) {
            builder.objectLockLegalHoldStatus(response.objectLockLegalHoldStatus().name());
        }

        return (GetObjectResponse) builder.responseMetadata(s3ResponseMetadata)
                                          .sdkHttpResponse(sdkHttpResponse)
                                          .build();

    }

    public static com.amazonaws.s3.model.PutObjectRequest toCrtPutObjectRequest(PutObjectRequest sdkPutObject) {
        com.amazonaws.s3.model.PutObjectRequest.Builder putObjectBuilder =
            com.amazonaws.s3.model.PutObjectRequest.builder()
                                                   .contentLength(sdkPutObject.contentLength())
                                                   .bucket(sdkPutObject.bucket())
                                                   .key(sdkPutObject.key())
                                                   .bucketKeyEnabled(sdkPutObject.bucketKeyEnabled())
                                                   .cacheControl(sdkPutObject.cacheControl())
                                                   .contentDisposition(sdkPutObject.contentDisposition())
                                                   .contentEncoding(sdkPutObject.contentEncoding())
                                                   .contentLanguage(sdkPutObject.contentLanguage())
                                                   .contentMD5(sdkPutObject.contentMD5())
                                                   .contentType(sdkPutObject.contentType())
                                                   .expectedBucketOwner(sdkPutObject.expectedBucketOwner())
                                                   .expires(sdkPutObject.expires())
                                                   .grantFullControl(sdkPutObject.grantFullControl())
                                                   .grantRead(sdkPutObject.grantRead())
                                                   .grantReadACP(sdkPutObject.grantReadACP())
                                                   .grantWriteACP(sdkPutObject.grantWriteACP())
                                                   .metadata(sdkPutObject.metadata())
                                                   .objectLockRetainUntilDate(sdkPutObject.objectLockRetainUntilDate())
                                                   .sSECustomerAlgorithm(sdkPutObject.sseCustomerAlgorithm())
                                                   .sSECustomerKey(sdkPutObject.sseCustomerKey())
                                                   .sSECustomerKeyMD5(sdkPutObject.sseCustomerKeyMD5())
                                                   .sSEKMSEncryptionContext(sdkPutObject.ssekmsEncryptionContext())
                                                   .sSEKMSKeyId(sdkPutObject.ssekmsKeyId())
                                                   .tagging(sdkPutObject.tagging())
                                                   .websiteRedirectLocation(sdkPutObject.websiteRedirectLocation());

        if (sdkPutObject.acl() != null) {
            putObjectBuilder.aCL(ObjectCannedACL.fromValue(sdkPutObject.acl().name()));
        }

        if (sdkPutObject.objectLockLegalHoldStatus() != null) {
            putObjectBuilder.objectLockLegalHoldStatus(ObjectLockLegalHoldStatus.fromValue(
                sdkPutObject.objectLockLegalHoldStatus().name()));
        }

        if (sdkPutObject.objectLockMode() != null) {
            putObjectBuilder.objectLockMode(ObjectLockMode.fromValue(
                sdkPutObject.objectLockMode().name()));
        }

        if (sdkPutObject.requestPayer() != null) {
            putObjectBuilder.requestPayer(RequestPayer.fromValue(sdkPutObject.requestPayer().name()));
        }

        if (sdkPutObject.serverSideEncryption() != null) {
            putObjectBuilder.serverSideEncryption(ServerSideEncryption.fromValue(
                sdkPutObject.serverSideEncryption().name()));
        }

        if (sdkPutObject.storageClass() != null) {
            putObjectBuilder.storageClass(StorageClass.fromValue(
                sdkPutObject.storageClass().name()));
        }

        processRequestOverrideConfiguration(sdkPutObject.overrideConfiguration().orElse(null),
                                            putObjectBuilder::customQueryParameters);

        addCustomHeaders(sdkPutObject.overrideConfiguration().orElse(null), putObjectBuilder::customHeaders);

        return putObjectBuilder.build();
    }

    public static PutObjectResponse fromCrtPutObjectOutput(PutObjectOutput crtPutObjectOutput) {
        // TODO: Provide the HTTP request-level data (e.g. response metadata, HTTP response)
        PutObjectResponse.Builder builder = PutObjectResponse.builder()
                                                             .bucketKeyEnabled(crtPutObjectOutput.bucketKeyEnabled())
                                                             .eTag(crtPutObjectOutput.eTag())
                                                             .expiration(crtPutObjectOutput.expiration())
                                                             .sseCustomerAlgorithm(crtPutObjectOutput.sSECustomerAlgorithm())
                                                             .sseCustomerKeyMD5(crtPutObjectOutput.sSECustomerKeyMD5())
                                                             .ssekmsEncryptionContext(
                                                                 crtPutObjectOutput.sSEKMSEncryptionContext())
                                                             .ssekmsKeyId(crtPutObjectOutput.sSEKMSKeyId())
                                                             .versionId(crtPutObjectOutput.versionId());

        if (crtPutObjectOutput.requestCharged() != null) {
            builder.requestCharged(crtPutObjectOutput.requestCharged().name());
        }

        if (crtPutObjectOutput.serverSideEncryption() != null) {
            builder.serverSideEncryption(crtPutObjectOutput.serverSideEncryption().name());
        }

        return builder.build();
    }

    private static S3ResponseMetadata createS3ResponseMetadata(SdkHttpResponse sdkHttpResponse) {
        Map<String, String> metadata = new HashMap<>();
        sdkHttpResponse.headers().forEach((key, value) -> metadata.put(key, value.get(0)));
        return S3ResponseMetadata.create(DefaultAwsResponseMetadata.create(metadata));
    }

    private static void throwExceptionForUnsupportedConfigurations(AwsRequestOverrideConfiguration overrideConfiguration) {
        if (!overrideConfiguration.metricPublishers().isEmpty()) {
            throw new UnsupportedOperationException("Metric publishers are not supported");
        }

        if (overrideConfiguration.signer().isPresent()) {
            throw new UnsupportedOperationException("signer is not supported");
        }

        if (!overrideConfiguration.apiNames().isEmpty()) {
            throw new UnsupportedOperationException("apiNames is not supported");
        }

        if (overrideConfiguration.apiCallAttemptTimeout().isPresent()) {
            throw new UnsupportedOperationException("apiCallAttemptTimeout is not supported");
        }

        if (overrideConfiguration.apiCallTimeout().isPresent()) {
            throw new UnsupportedOperationException("apiCallTimeout is not supported");
        }

        if (overrideConfiguration.credentialsProvider().isPresent()) {
            throw new UnsupportedOperationException("credentialsProvider is not supported");
        }
    }

    private static void addRequestCustomHeaders(List<HttpHeader> crtHeaders, Map<String, List<String>> headers) {
        headers.forEach((key, value) -> {
            value.stream().map(val -> new HttpHeader(key, val)).forEach(crtHeaders::add);
        });
    }

    private static String encodedQueryString(Map<String, List<String>> rawQueryParameters) {
        return SdkHttpUtils.encodeAndFlattenQueryParameters(rawQueryParameters)
                           .map(value -> "?" + value)
                           .orElse("");
    }

    private static void processRequestOverrideConfiguration(AwsRequestOverrideConfiguration requestOverrideConfiguration,
                                                            Consumer<String> queryParametersConsumer) {
        if (requestOverrideConfiguration != null) {
            throwExceptionForUnsupportedConfigurations(requestOverrideConfiguration);

            if (!requestOverrideConfiguration.rawQueryParameters().isEmpty()) {
                String encodedQueryString = encodedQueryString(requestOverrideConfiguration.rawQueryParameters());
                queryParametersConsumer.accept(encodedQueryString);
            }
        }
    }

    private static void addCustomHeaders(AwsRequestOverrideConfiguration requestOverrideConfiguration,
                                         Consumer<HttpHeader[]> headersConsumer) {

        List<HttpHeader> crtHeaders = new ArrayList<>();
        crtHeaders.add(new HttpHeader(HEADER_USER_AGENT, USER_AGENT_STRING));

        if (requestOverrideConfiguration != null && !requestOverrideConfiguration.headers().isEmpty()) {
            addRequestCustomHeaders(crtHeaders, requestOverrideConfiguration.headers());
        }

        headersConsumer.accept(crtHeaders.toArray(new HttpHeader[0]));
    }
}
