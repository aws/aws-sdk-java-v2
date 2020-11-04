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

package software.amazon.awssdk.services.s3.functionaltests;


import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.services.s3.S3MockUtils.mockListObjectsResponse;

import java.io.UnsupportedEncodingException;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.auth.signer.internal.SignerConstant;
import software.amazon.awssdk.authcrt.signer.AwsS3V4aSigner;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.testutils.service.http.MockHttpClient;

/**
 * Functional tests for multi-region access point ARN signing
 */
public class MultiRegionAccessPointSigningFunctionalTest {

    private static final String MRAP_ARN = "arn:aws:s3::123456789012:accesspoint:mfzwi23gnjvgw.mrap";
    private static final String AWS4A_SIGNING_ALGORITHM = "AWS4-ECDSA-P256-SHA256";

    private MockHttpClient mockHttpClient;

    @Before
    public void setup() throws UnsupportedEncodingException {
        mockHttpClient = new MockHttpClient();
        mockHttpClient.stubNextResponse(mockListObjectsResponse());
    }

    @Test
    public void multiRegionArn_noSignerOverride_usesInterceptorSigner() {
        S3Client s3Client = clientBuilder().build();
        s3Client.listObjects(ListObjectsRequest.builder()
                                               .bucket(MRAP_ARN)
                                               .build());
        assertThat(mockHttpClient.getLastRequest().headers().get("Authorization").get(0))
            .contains(AWS4A_SIGNING_ALGORITHM);
    }

    @Test
    public void multiRegionArn_clientSignerOverride_usesOverrideSigner() {
        S3Client s3Client = clientBuilderWithOverrideSigner(AwsS3V4Signer.create()).build();
        s3Client.listObjects(ListObjectsRequest.builder()
                                               .bucket(MRAP_ARN)
                                               .build());
        assertThat(mockHttpClient.getLastRequest().headers().get("Authorization").get(0))
            .contains(SignerConstant.AWS4_SIGNING_ALGORITHM);
    }

    @Test
    public void multiRegionArn_requestSignerOverride_usesOverrideSigner() {
        S3Client s3Client = clientBuilder().build();
        s3Client.listObjects(ListObjectsRequest.builder()
                                               .bucket(MRAP_ARN)
                                               .overrideConfiguration(s -> s.signer(AwsS3V4Signer.create()))
                                               .build());
        assertThat(mockHttpClient.getLastRequest().headers().get("Authorization").get(0))
            .contains(SignerConstant.AWS4_SIGNING_ALGORITHM);
    }

    @Test
    public void multiRegionArn_requestAndClientSignerOverride_usesRequestOverrideSigner() {
        S3Client s3Client = clientBuilderWithOverrideSigner(AwsS3V4aSigner.create()).build();
        s3Client.listObjects(ListObjectsRequest.builder()
                                               .bucket(MRAP_ARN)
                                               .overrideConfiguration(s -> s.signer(AwsS3V4Signer.create()))
                                               .build());
        assertThat(mockHttpClient.getLastRequest().headers().get("Authorization").get(0))
            .contains(SignerConstant.AWS4_SIGNING_ALGORITHM);
    }

    private S3ClientBuilder clientBuilder() {
        return S3Client.builder()
                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid")))
                       .region(Region.AP_SOUTH_1)
                       .httpClient(mockHttpClient)
                       .serviceConfiguration(S3Configuration.builder()
                                                            .useArnRegionEnabled(true)
                                                            .build());
    }

    private S3ClientBuilder clientBuilderWithOverrideSigner(Signer signer) {
        return clientBuilder().overrideConfiguration(ClientOverrideConfiguration.builder()
                                                                                .putAdvancedOption(SdkAdvancedClientOption.SIGNER, signer)
                                                                                .build());
    }
}
