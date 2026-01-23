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

package software.amazon.awssdk.services.s3.checksums;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.CLIENT_TYPE;
import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.REQUEST_CHECKSUM_CALCULATION;
import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.RESPONSE_CHECKSUM_VALIDATION;
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumConstant.CHECKSUM_ENABLED_RESPONSE_HEADER;
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumConstant.CONTENT_LENGTH_HEADER;
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumConstant.ENABLE_MD5_CHECKSUM_HEADER_VALUE;
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumConstant.SERVER_SIDE_CUSTOMER_ENCRYPTION_HEADER;
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumConstant.SERVER_SIDE_ENCRYPTION_HEADER;
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumsEnabledValidator.getObjectChecksumEnabledPerRequest;
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumsEnabledValidator.getObjectChecksumEnabledPerResponse;
import static software.amazon.awssdk.services.s3.internal.checksums.ChecksumsEnabledValidator.responseChecksumIsValid;
import static software.amazon.awssdk.services.s3.model.ServerSideEncryption.AWS_KMS;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.model.ChecksumMode;
import software.amazon.awssdk.services.s3.model.GetObjectAclRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class ChecksumsEnabledValidatorTest {

    @Test
    public void getObjectChecksumEnabledPerRequest_nonGetObjectRequest_returnsFalse() {
        assertThat(getObjectChecksumEnabledPerRequest(GetObjectAclRequest.builder().build(),
                                                      getExecutionAttributesWithChecksumEnabled())).isFalse();
    }

    @Test
    public void getObjectChecksumEnabledPerRequest_checksumEnabledChecksumModeEnabled_returnsFalse() {
        assertThat(getObjectChecksumEnabledPerRequest(GetObjectRequest.builder().checksumMode(ChecksumMode.ENABLED).build(),
                                                      getExecutionAttributesWithChecksumEnabled())).isFalse();
    }

    @Test
    public void getObjectChecksumEnabledPerRequest_checksumDisabledChecksumModeDisabled_returnsFalse() {
        assertThat(getObjectChecksumEnabledPerRequest(GetObjectRequest.builder().build(),
                                                      getExecutionAttributesWithChecksumDisabled())).isFalse();
    }

    @Test
    public void getObjectChecksumEnabledPerRequest_checksumDisabledChecksumModeEnabled_returnsFalse() {
        assertThat(getObjectChecksumEnabledPerRequest(GetObjectRequest.builder().checksumMode(ChecksumMode.ENABLED).build(),
                                                      getExecutionAttributesWithChecksumDisabled())).isFalse();
    }

    @Test
    public void getObjectChecksumEnabledPerResponse_nonGetObjectRequestFalse() {
        assertThat(getObjectChecksumEnabledPerResponse(GetObjectAclRequest.builder().build(),
                                                       getSdkHttpResponseWithChecksumHeader(),
                                                       getExecutionAttributesWithChecksumEnabled())).isFalse();
    }

    @Test
    public void getObjectChecksumEnabledPerResponse_responseContainsChecksumHeader_returnTrue() {
        assertThat(getObjectChecksumEnabledPerResponse(GetObjectRequest.builder().build(),
                                                       getSdkHttpResponseWithChecksumHeader(),
                                                       getExecutionAttributesWithChecksumEnabled())).isTrue();
    }

    @Test
    public void getObjectChecksumEnabledPerResponse_responseNotContainsChecksumHeader_returnFalse() {
        assertThat(getObjectChecksumEnabledPerResponse(GetObjectRequest.builder().build(),
                                                       SdkHttpFullResponse.builder().build(),
                                                       getExecutionAttributesWithChecksumEnabled())).isFalse();
    }

    @Test
    public void responseChecksumIsValid_defaultTrue() {
        assertThat(responseChecksumIsValid(SdkHttpResponse.builder().build())).isTrue();
    }

    @Test
    public void responseChecksumIsValid_serverSideCustomerEncryption_false() {
        SdkHttpResponse response = SdkHttpResponse.builder()
                                                  .putHeader(SERVER_SIDE_CUSTOMER_ENCRYPTION_HEADER, "test")
                                                  .build();

        assertThat(responseChecksumIsValid(response)).isFalse();
    }

    @Test
    public void responseChecksumIsValid_serverSideEncryption_false() {
        SdkHttpResponse response = SdkHttpResponse.builder()
                                                  .putHeader(SERVER_SIDE_ENCRYPTION_HEADER, AWS_KMS.toString())
                                                  .build();

        assertThat(responseChecksumIsValid(response)).isFalse();
    }

    private ExecutionAttributes getExecutionAttributesWithChecksumEnabled() {
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(REQUEST_CHECKSUM_CALCULATION, RequestChecksumCalculation.WHEN_SUPPORTED);
        executionAttributes.putAttribute(RESPONSE_CHECKSUM_VALIDATION, ResponseChecksumValidation.WHEN_SUPPORTED);
        return executionAttributes;
    }

    private ExecutionAttributes getSyncExecutionAttributes() {
        ExecutionAttributes executionAttributes = getExecutionAttributesWithChecksumEnabled();
        executionAttributes.putAttribute(CLIENT_TYPE, ClientType.SYNC);
        return executionAttributes;
    }

    private ExecutionAttributes getExecutionAttributesWithChecksumDisabled() {
        ExecutionAttributes executionAttributes = getSyncExecutionAttributes();
        executionAttributes.putAttribute(REQUEST_CHECKSUM_CALCULATION, RequestChecksumCalculation.WHEN_REQUIRED);
        executionAttributes.putAttribute(RESPONSE_CHECKSUM_VALIDATION, ResponseChecksumValidation.WHEN_REQUIRED);
        return executionAttributes;
    }

    private SdkHttpResponse getSdkHttpResponseWithChecksumHeader() {
        return SdkHttpResponse.builder()
                              .putHeader(CONTENT_LENGTH_HEADER, "100")
                              .putHeader(CHECKSUM_ENABLED_RESPONSE_HEADER, ENABLE_MD5_CHECKSUM_HEADER_VALUE)
                              .build();
    }
}
