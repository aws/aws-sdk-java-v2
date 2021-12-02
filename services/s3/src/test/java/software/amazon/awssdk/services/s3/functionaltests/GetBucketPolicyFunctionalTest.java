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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.concurrent.CompletionException;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.junit.Rule;
import org.junit.Test;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class GetBucketPolicyFunctionalTest {
    private static final URI HTTP_LOCALHOST_URI = URI.create("http://localhost:8080/");
    private static final String EXAMPLE_BUCKET = "Example-Bucket";
    private static final String EXAMPLE_POLICY =
        "{\"Version\":\"2012-10-17\",\"Id\":\"Policy1234\","
        + "\"Statement\":[{\"Sid\":\"Stmt1578431058575\",\"Effect\":\"Allow\","
        + "\"Principal\":{\"AWS\":\"arn:aws:iam::1234567890:root\"},\"Action\":\"s3:*\","
        + "\"Resource\":\"arn:aws:s3:::dummy-resource/*\"}]}";

    @Rule
    public WireMockRule wireMock = new WireMockRule();

    private S3ClientBuilder getSyncClientBuilder() {

        return S3Client.builder()
                       .region(Region.US_EAST_1)
                       .endpointOverride(HTTP_LOCALHOST_URI)
                       .credentialsProvider(
                           StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")));
    }

    private S3AsyncClientBuilder getAsyncClientBuilder() {
        return S3AsyncClient.builder()
                            .region(Region.US_EAST_1)
                            .endpointOverride(HTTP_LOCALHOST_URI)
                            .credentialsProvider(
                                StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")));

    }

    @Test
    public void getBucketPolicy_syncClient() {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(EXAMPLE_POLICY)));

        S3Client s3Client = getSyncClientBuilder().build();

        GetBucketPolicyResponse response = s3Client.getBucketPolicy(r -> r.bucket(EXAMPLE_BUCKET));
        assertThat(response.policy()).isEqualTo(EXAMPLE_POLICY);
    }

    @Test
    public void getBucketPolicy_asyncClient() {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(EXAMPLE_POLICY)));

        S3AsyncClient s3Client = getAsyncClientBuilder().build();

        GetBucketPolicyResponse response = s3Client.getBucketPolicy(r -> r.bucket(EXAMPLE_BUCKET)).join();
        assertThat(response.policy()).isEqualTo(EXAMPLE_POLICY);
    }
}
