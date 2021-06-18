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


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.amazonaws.s3.model.GetObjectOutput;
import com.amazonaws.s3.model.PutObjectOutput;
import com.amazonaws.s3.model.ReplicationStatus;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.SdkField;
import software.amazon.awssdk.core.util.SdkUserAgent;
import software.amazon.awssdk.crt.auth.credentials.Credentials;
import software.amazon.awssdk.crt.auth.credentials.CredentialsProvider;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.metrics.LoggingMetricPublisher;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.ObjectLockLegalHoldStatus;
import software.amazon.awssdk.services.s3.model.ObjectLockMode;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.RequestPayer;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.utils.Logger;

public class S3CrtPojoConversionTest {
    private static final Logger log = Logger.loggerFor(S3CrtPojoConversionTest.class);
    private static final Random RNG = new Random();
    private static final String ACCESS_KEY = "accessKey";
    private static final String SECRET_ACCESS_KEY = "secretAccessKey";
    private static final String SESSION_TOKEN = "sessionToken";

    @Test
    public void createCrtCredentialsProviderTest() throws ExecutionException, InterruptedException {
        AwsCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider
            .create(AwsSessionCredentials.create(ACCESS_KEY, SECRET_ACCESS_KEY, SESSION_TOKEN));
        CredentialsProvider crtCredentialsProvider = S3CrtPojoConversion.createCrtCredentialsProvider(awsCredentialsProvider);

        Credentials credentials = crtCredentialsProvider.getCredentials().get();

        assertThat(ACCESS_KEY.getBytes(StandardCharsets.UTF_8)).isEqualTo(credentials.getAccessKeyId());
        assertThat(SECRET_ACCESS_KEY.getBytes(StandardCharsets.UTF_8)).isEqualTo(credentials.getSecretAccessKey());
        assertThat(SESSION_TOKEN.getBytes(StandardCharsets.UTF_8)).isEqualTo(credentials.getSessionToken());
    }

    @Test
    public void fromCrtPutObjectOutputAllFields_shouldConvert() throws IllegalAccessException {

        PutObjectOutput crtResponse = randomCrtPutObjectOutput();
        PutObjectResponse sdkResponse = S3CrtPojoConversion.fromCrtPutObjectOutput(crtResponse);

        // ignoring fields with different casings and enum fields.
        assertThat(sdkResponse).isEqualToIgnoringGivenFields(crtResponse,
                                                             "sseCustomerAlgorithm",
                                                            "sseCustomerKeyMD5",
                                                            "ssekmsKeyId",
                                                             "ssekmsEncryptionContext",
                                                            "serverSideEncryption",
                                                            "requestCharged",
                                                            "responseMetadata",
                                                            "sdkHttpResponse");
        assertThat(sdkResponse.serverSideEncryption().name()).isEqualTo(crtResponse.serverSideEncryption().name());
        assertThat(sdkResponse.sseCustomerAlgorithm()).isEqualTo(crtResponse.sSECustomerAlgorithm());
        assertThat(sdkResponse.ssekmsKeyId()).isEqualTo(crtResponse.sSEKMSKeyId());
        assertThat(sdkResponse.sseCustomerKeyMD5()).isEqualTo(crtResponse.sSECustomerKeyMD5());
        assertThat(sdkResponse.ssekmsEncryptionContext()).isEqualTo(crtResponse.sSEKMSEncryptionContext());

        // TODO: CRT enums dont' have valid values. Uncomment this once it's fixed in CRT.
        //assertThat(sdkResponse.requestCharged().name()).isEqualTo(crtResponse.requestCharged().name());
    }

    @Test
    public void fromCrtGetObjectOutput_shouldAddSdkHttpResponse() {
        String expectedRequestId = "123456";
        GetObjectOutput output = GetObjectOutput.builder().build();
        SdkHttpResponse response = SdkHttpResponse.builder()
                                                  .statusCode(200)
                                                  .appendHeader("x-amz-request-id", expectedRequestId)
                                                  .build();


        GetObjectResponse getObjectResponse = S3CrtPojoConversion.fromCrtGetObjectOutput(output, response);
        assertThat(output).isEqualToIgnoringGivenFields(getObjectResponse, "body",
                                                        "sSECustomerAlgorithm",
                                                        "sSECustomerKeyMD5",
                                                        "sSEKMSKeyId",
                                                        "metadata");

        assertThat(getObjectResponse.sdkHttpResponse()).isEqualTo(response);
        assertThat(getObjectResponse.responseMetadata().requestId()).isEqualTo(expectedRequestId);

    }

    @Test
    public void fromCrtGetObjectOutputAllFields_shouldConvert() throws IllegalAccessException {
        GetObjectOutput crtResponse = randomCrtGetObjectOutput();
        GetObjectResponse sdkResponse = S3CrtPojoConversion.fromCrtGetObjectOutput(crtResponse, SdkHttpResponse.builder().build());

        // ignoring fields with different casings and enum fields.
        assertThat(sdkResponse).isEqualToIgnoringGivenFields(crtResponse,
                                                             "sseCustomerAlgorithm",
                                                             "body",
                                                             "sseCustomerKeyMD5",
                                                             "ssekmsKeyId",
                                                             "ssekmsEncryptionContext",
                                                             "serverSideEncryption",
                                                             "responseMetadata",
                                                             "sdkHttpResponse",
                                                             "storageClass",
                                                             "requestCharged",
                                                             "replicationStatus",
                                                             "objectLockMode",
                                                             "objectLockLegalHoldStatus");
        assertThat(sdkResponse.serverSideEncryption().name()).isEqualTo(crtResponse.serverSideEncryption().name());
        assertThat(sdkResponse.sseCustomerAlgorithm()).isEqualTo(crtResponse.sSECustomerAlgorithm());
        assertThat(sdkResponse.ssekmsKeyId()).isEqualTo(crtResponse.sSEKMSKeyId());
        assertThat(sdkResponse.sseCustomerKeyMD5()).isEqualTo(crtResponse.sSECustomerKeyMD5());
        assertThat(sdkResponse.storageClass().name()).isEqualTo(crtResponse.storageClass().name());
        assertThat(sdkResponse.replicationStatus().name()).isEqualTo(crtResponse.replicationStatus().name());
        assertThat(sdkResponse.objectLockMode().name()).isEqualTo(crtResponse.objectLockMode().name());
        assertThat(sdkResponse.objectLockLegalHoldStatus().name()).isEqualTo(crtResponse.objectLockLegalHoldStatus().name());

        // TODO: CRT enums dont' have valid values. Uncomment this once it's fixed in CRT.
        // assertThat(sdkResponse.requestCharged().name()).isEqualTo(crtResponse.requestCharged().name());
    }

    @Test
    public void toCrtPutObjectRequest_shouldAddUserAgent() {

        PutObjectRequest sdkRequest = PutObjectRequest.builder()
                                                      .build();

        com.amazonaws.s3.model.PutObjectRequest crtRequest = S3CrtPojoConversion.toCrtPutObjectRequest(sdkRequest);
        HttpHeader[] headers = crtRequest.customHeaders();
        verifyUserAgent(headers);
    }

    @Test
    public void toCrtPutObjectRequestAllFields_shouldConvert() {
        PutObjectRequest sdkRequest = randomPutObjectRequest();

        com.amazonaws.s3.model.PutObjectRequest crtRequest = S3CrtPojoConversion.toCrtPutObjectRequest(sdkRequest);

        // ignoring fields with different casings and enum fields.
        assertThat(crtRequest).isEqualToIgnoringGivenFields(sdkRequest,
                                                            "aCL", "body", "sSECustomerAlgorithm",
                                                            "sSECustomerKey", "sSECustomerKeyMD5",
                                                            "sSEKMSKeyId", "sSEKMSEncryptionContext",
                                                            "customHeaders", "customQueryParameters",
                                                            "serverSideEncryption",
                                                            "storageClass",
                                                            "requestPayer",
                                                            "objectLockMode",
                                                            "objectLockLegalHoldStatus");
        assertThat(crtRequest.aCL().name()).isEqualTo(sdkRequest.acl().name());
        assertThat(crtRequest.serverSideEncryption().name()).isEqualTo(sdkRequest.serverSideEncryption().name());
        assertThat(crtRequest.storageClass().name()).isEqualTo(sdkRequest.storageClass().name());
        assertThat(crtRequest.requestPayer().name()).isEqualTo(sdkRequest.requestPayer().name());
        assertThat(crtRequest.objectLockMode().name()).isEqualTo(sdkRequest.objectLockMode().name());
        assertThat(crtRequest.objectLockLegalHoldStatus().name()).isEqualTo(sdkRequest.objectLockLegalHoldStatus().name());

        assertThat(crtRequest.sSECustomerAlgorithm()).isEqualTo(sdkRequest.sseCustomerAlgorithm());
        assertThat(crtRequest.sSECustomerKey()).isEqualTo(sdkRequest.sseCustomerKey());
        assertThat(crtRequest.sSECustomerKeyMD5()).isEqualTo(sdkRequest.sseCustomerKeyMD5());
        assertThat(crtRequest.sSEKMSKeyId()).isEqualTo(sdkRequest.ssekmsKeyId());
        assertThat(crtRequest.sSEKMSEncryptionContext()).isEqualTo(sdkRequest.ssekmsEncryptionContext());
        assertThat(crtRequest.sSECustomerAlgorithm()).isEqualTo(sdkRequest.sseCustomerAlgorithm());
    }

    @Test
    public void toCrtPutObjectRequest_withCustomHeaders_shouldAttach() {

        AwsRequestOverrideConfiguration requestOverrideConfiguration = requestOverrideConfigWithCustomHeaders();

        PutObjectRequest sdkRequest = PutObjectRequest.builder()
                                                      .overrideConfiguration(requestOverrideConfiguration)
                                                      .build();

        com.amazonaws.s3.model.PutObjectRequest crtRequest = S3CrtPojoConversion.toCrtPutObjectRequest(sdkRequest);
        HttpHeader[] headers = crtRequest.customHeaders();
        verifyHeaders(headers);
        assertThat(crtRequest.customQueryParameters()).isEqualTo("?hello1=world1&hello2=world2");
    }

    @Test
    public void toCrtGetObjectRequest_shouldAddUserAgent() {
        GetObjectRequest sdkRequest = GetObjectRequest.builder()
                                                      .build();

        com.amazonaws.s3.model.GetObjectRequest crtRequest = S3CrtPojoConversion.toCrtGetObjectRequest(sdkRequest);

        HttpHeader[] headers = crtRequest.customHeaders();
        verifyUserAgent(headers);
    }

    @Test
    public void toCrtGetObjectRequestAllFields_shouldConvert() {
        GetObjectRequest sdkRequest = randomGetObjectRequest();

        com.amazonaws.s3.model.GetObjectRequest crtRequest = S3CrtPojoConversion.toCrtGetObjectRequest(sdkRequest);

        // ignoring fields with different casings and enum fields.
        assertThat(crtRequest).isEqualToIgnoringGivenFields(sdkRequest, "body", "sSECustomerAlgorithm",
                                                            "sSECustomerKey", "sSECustomerKeyMD5",
                                                            "customHeaders", "customQueryParameters",
                                                            "requestPayer");
        assertThat(crtRequest.requestPayer().name()).isEqualTo(sdkRequest.requestPayer().name());
        assertThat(crtRequest.sSECustomerAlgorithm()).isEqualTo(sdkRequest.sseCustomerAlgorithm());
        assertThat(crtRequest.sSECustomerKey()).isEqualTo(sdkRequest.sseCustomerKey());
        assertThat(crtRequest.sSECustomerKeyMD5()).isEqualTo(sdkRequest.sseCustomerKeyMD5());
        assertThat(crtRequest.sSECustomerAlgorithm()).isEqualTo(sdkRequest.sseCustomerAlgorithm());
    }

    @Test
    public void toCrtGetObjectRequest_withCustomHeaders_shouldAttach() {
        AwsRequestOverrideConfiguration requestOverrideConfiguration = requestOverrideConfigWithCustomHeaders();

        GetObjectRequest sdkRequest = GetObjectRequest.builder()
                                                      .overrideConfiguration(requestOverrideConfiguration)
                                                      .build();

        com.amazonaws.s3.model.GetObjectRequest crtRequest = S3CrtPojoConversion.toCrtGetObjectRequest(sdkRequest);

        HttpHeader[] headers = crtRequest.customHeaders();
        verifyHeaders(headers);
        assertThat(crtRequest.customQueryParameters()).isEqualTo("?hello1=world1&hello2=world2");
    }

    @Test
    public void toCrtPutObjectRequest_withUnsupportedConfigs_shouldThrowException() {
        assertThatThrownBy(() -> S3CrtPojoConversion.toCrtPutObjectRequest(PutObjectRequest.builder()
                                                                                           .overrideConfiguration(b -> b.apiCallAttemptTimeout(Duration.ofMinutes(1)))
                                                                                           .build())).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> S3CrtPojoConversion.toCrtPutObjectRequest(PutObjectRequest.builder()
                                                                                           .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMinutes(1)))
                                                                                           .build())).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> S3CrtPojoConversion.toCrtPutObjectRequest(PutObjectRequest.builder()
                                                                                           .overrideConfiguration(b -> b.credentialsProvider(() ->
                                                                                                                                        AwsBasicCredentials.create("", "")))
                                                                                           .build())).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> S3CrtPojoConversion.toCrtPutObjectRequest(PutObjectRequest.builder()
                                                                                           .overrideConfiguration(b -> b.addApiName(ApiName.builder()
                                                                                                                                  .name("test")
                                                                                                                                  .version("1")
                                                                                                                                  .build()))
                                                                                           .build())).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> S3CrtPojoConversion.toCrtPutObjectRequest(PutObjectRequest.builder()
                                                                                           .overrideConfiguration(b -> b.addMetricPublisher(LoggingMetricPublisher.create()))
                                                                                           .build())).isExactlyInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> S3CrtPojoConversion.toCrtPutObjectRequest(PutObjectRequest.builder()
                                                                                           .overrideConfiguration(b -> b.signer(AwsS3V4Signer.create()))
                                                                                           .build())).isExactlyInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void toCrtGetObjectRequest_withUnsupportedConfigs_shouldThrowException() {
        assertThatThrownBy(() -> S3CrtPojoConversion.toCrtGetObjectRequest(GetObjectRequest.builder()
                                                                                           .overrideConfiguration(b -> b.apiCallAttemptTimeout(Duration.ofMinutes(1)))
                                                                                           .build())).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> S3CrtPojoConversion.toCrtGetObjectRequest(GetObjectRequest.builder()
                                                                                           .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMinutes(1)))
                                                                                           .build())).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> S3CrtPojoConversion.toCrtGetObjectRequest(GetObjectRequest.builder()
                                                                                           .overrideConfiguration(b -> b.credentialsProvider(() ->
                                                                                                                                        AwsBasicCredentials.create("", "")))
                                                                                           .build())).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> S3CrtPojoConversion.toCrtGetObjectRequest(GetObjectRequest.builder()
                                                                                           .overrideConfiguration(b -> b.addApiName(ApiName.builder()
                                                                                                                                  .name("test")
                                                                                                                                  .version("1")
                                                                                                                                  .build()))
                                                                                           .build())).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> S3CrtPojoConversion.toCrtGetObjectRequest(GetObjectRequest.builder()
                                                                                           .overrideConfiguration(b -> b.addMetricPublisher(LoggingMetricPublisher.create()))
                                                                                           .build())).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> S3CrtPojoConversion.toCrtGetObjectRequest(GetObjectRequest.builder()
                                                                                           .overrideConfiguration(b -> b.signer(AwsS3V4Signer.create()))
                                                                                           .build())).isExactlyInstanceOf(UnsupportedOperationException.class);
    }

    private AwsRequestOverrideConfiguration requestOverrideConfigWithCustomHeaders() {
        return AwsRequestOverrideConfiguration.builder()
                                              .putHeader("foo", "bar")
                                              .putRawQueryParameter("hello1", "world1")
                                              .putRawQueryParameter("hello2", "world2")
                                              .build();
    }

    private void verifyHeaders(HttpHeader[] headers) {
        assertThat(headers).hasSize(2);
        verifyUserAgent(headers);
        assertThat(headers[1].getName()).isEqualTo("foo");
        assertThat(headers[1].getValue()).isEqualTo("bar");
    }

    private void verifyUserAgent(HttpHeader[] headers) {
        assertThat(headers[0].getName()).isEqualTo("User-Agent");
        assertThat(headers[0].getValue()).contains("ft/s3-transfer");
        assertThat(headers[0].getValue()).contains(SdkUserAgent.create().userAgent());
    }

    private GetObjectRequest randomGetObjectRequest() {
        GetObjectRequest.Builder builder = GetObjectRequest.builder();
        setSdkFieldsToRandomValues(builder.sdkFields(), builder);
        return builder.build();
    }

    private PutObjectRequest randomPutObjectRequest() {
        PutObjectRequest.Builder builder = PutObjectRequest.builder();
        setSdkFieldsToRandomValues(builder.sdkFields(), builder);
        return builder.build();
    }


    private com.amazonaws.s3.model.GetObjectOutput randomCrtGetObjectOutput() throws IllegalAccessException {
        com.amazonaws.s3.model.GetObjectOutput.Builder builder = com.amazonaws.s3.model.GetObjectOutput.builder();
        Class<?> aClass = builder.getClass();
        setFieldsToRandomValues(Arrays.asList(aClass.getDeclaredFields()), builder);
        return builder.build();
    }

    private com.amazonaws.s3.model.PutObjectOutput randomCrtPutObjectOutput() throws IllegalAccessException {
        com.amazonaws.s3.model.PutObjectOutput.Builder builder = com.amazonaws.s3.model.PutObjectOutput.builder();
        Class<?> aClass = builder.getClass();
        setFieldsToRandomValues(Arrays.asList(aClass.getDeclaredFields()), builder);
        return builder.build();
    }

    private void setFieldsToRandomValues(Collection<Field> fields, Object builder) throws IllegalAccessException {
        for (Field f : fields) {
            setFieldToRandomValue(f, builder);
        }
    }

    private void setFieldToRandomValue(Field field, Object obj) throws IllegalAccessException {
        Class<?> targetClass = field.getType();
        field.setAccessible(true);
        if (targetClass.equals(String.class)) {
            field.set(obj, RandomStringUtils.randomAscii(8));
        } else if (targetClass.equals(Integer.class)) {
            field.set(obj, randomInteger());
        } else if (targetClass.equals(Instant.class)) {
            field.set(obj, randomInstant());
        } else if (targetClass.equals(Long.class)) {
            field.set(obj, RNG.nextLong());
        } else if (targetClass.equals(Map.class)) {
            field.set(obj, new HashMap<>());
        } else if (targetClass.equals(Boolean.class)) {
            field.set(obj, Boolean.TRUE);
        } else if (targetClass.isEnum()) {
            if (targetClass.equals(com.amazonaws.s3.model.ServerSideEncryption.class)) {
                field.set(obj, com.amazonaws.s3.model.ServerSideEncryption.AES256);
            } else if (targetClass.equals(com.amazonaws.s3.model.StorageClass.class)) {
                field.set(obj, com.amazonaws.s3.model.StorageClass.GLACIER);
            } else if (targetClass.equals(com.amazonaws.s3.model.RequestCharged.class)) {
                field.set(obj, com.amazonaws.s3.model.RequestCharged.REQUESTER);
            } else if (targetClass.equals(com.amazonaws.s3.model.ReplicationStatus.class)) {
                field.set(obj, ReplicationStatus.COMPLETE);
            } else if (targetClass.equals(com.amazonaws.s3.model.ObjectLockMode.class)) {
                field.set(obj, com.amazonaws.s3.model.ObjectLockMode.GOVERNANCE);
            } else if (targetClass.equals(com.amazonaws.s3.model.ObjectLockLegalHoldStatus.class)) {
                field.set(obj, com.amazonaws.s3.model.ObjectLockLegalHoldStatus.OFF);
            } else {
                throw new IllegalArgumentException("Unknown enum: " + field.getName());
            }
        } else if (field.getName().equals("body")) {
            log.info(() -> "ignore non s3 fields");
        }  else if (field.isSynthetic()) {
            // ignore jacoco https://github.com/jacoco/jacoco/issues/168
            log.info(() -> "ignore synthetic fields");
        }  else {
            throw new IllegalArgumentException("Unknown Field type: " + field.getName());
        }
    }

    private void setSdkFieldsToRandomValues(Collection<SdkField<?>> fields, Object builder) {
        for (SdkField<?> f : fields) {
            setSdkFieldToRandomValue(f, builder);
        }
    }

    private static void setSdkFieldToRandomValue(SdkField<?> sdkField, Object obj) {
        Class<?> targetClass = sdkField.marshallingType().getTargetClass();
        if (targetClass.equals(String.class)) {
            switch (sdkField.memberName()) {
                case "ACL":
                    sdkField.set(obj, ObjectCannedACL.PUBLIC_READ.toString());
                    break;
                case "ServerSideEncryption":
                    sdkField.set(obj, ServerSideEncryption.AES256.toString());
                    break;
                case "StorageClass":
                    sdkField.set(obj, StorageClass.DEEP_ARCHIVE.toString());
                    break;
                case "RequestPayer":
                    sdkField.set(obj, RequestPayer.UNKNOWN_TO_SDK_VERSION.toString());
                    break;
                case "ObjectLockMode":
                    sdkField.set(obj, ObjectLockMode.COMPLIANCE.toString());
                    break;
                case "ObjectLockLegalHoldStatus":
                    sdkField.set(obj, ObjectLockLegalHoldStatus.OFF.toString());
                    break;
                default:
                    sdkField.set(obj, RandomStringUtils.random(8));
            }
        } else if (targetClass.equals(Integer.class)) {
            sdkField.set(obj, randomInteger());
        } else if (targetClass.equals(Instant.class)) {
            sdkField.set(obj, randomInstant());
        } else if (targetClass.equals(Long.class)) {
            sdkField.set(obj, RNG.nextLong());
        } else if (targetClass.equals(Map.class)) {
            sdkField.set(obj, new HashMap<>());
        } else if (targetClass.equals(Boolean.class)) {
           sdkField.set(obj, Boolean.TRUE);
        } else {
            throw new IllegalArgumentException("Unknown SdkField type: " + targetClass);
        }
    }

    private static Instant randomInstant() {
        return Instant.ofEpochMilli(RNG.nextLong());
    }

    private static Integer randomInteger() {
        return RNG.nextInt();
    }
}