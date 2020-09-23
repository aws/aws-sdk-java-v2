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

package software.amazon.awssdk.services.s3control.functionaltests;

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
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3control.S3ControlAsyncClient;
import software.amazon.awssdk.services.s3control.S3ControlAsyncClientBuilder;
import software.amazon.awssdk.services.s3control.S3ControlClient;
import software.amazon.awssdk.services.s3control.S3ControlClientBuilder;
import software.amazon.awssdk.services.s3control.model.InvalidRequestException;
import software.amazon.awssdk.services.s3control.model.NoSuchPublicAccessBlockConfigurationException;
import software.amazon.awssdk.services.s3control.model.S3ControlException;

public class XMLErrorTypesTranslationFunctionalTest {

    private static final URI HTTP_LOCALHOST_URI = URI.create("http://localhost:8080/");

    @Rule
    public WireMockRule wireMock = new WireMockRule();

    private S3ControlClientBuilder getSyncClientBuilder() {
        return S3ControlClient.builder()
                              .region(Region.US_EAST_1)
                              .overrideConfiguration(c -> c.addExecutionInterceptor(new LocalhostEndpointAddressInterceptor()))
                              .credentialsProvider(
                                  StaticCredentialsProvider.create(
                                      AwsBasicCredentials.create("key", "secret")));
    }

    private S3ControlAsyncClientBuilder getAsyncClientBuilder() {
        return S3ControlAsyncClient.builder()
                                   .region(Region.US_EAST_1)
                                   .overrideConfiguration(c -> c.addExecutionInterceptor(new LocalhostEndpointAddressInterceptor()))
                                   .credentialsProvider(
                                       StaticCredentialsProvider.create(
                                           AwsBasicCredentials.create("key", "secret")));
    }

    @Test
    public void standardErrorXML_translated_correctly_with_syncClient() {
        String accountId = "Account-Id";
        String xmlResponseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                 + "<ErrorResponse>\n"
                                 + "<Error>\n"
                                 + "<AccountId>Account-Id</AccountId>\n"
                                 + "<Code>NoSuchPublicAccessBlockConfiguration</Code>\n"
                                 + "<Message>The public access block configuration was not found</Message>\n"
                                 + "</Error>\n"
                                 + "<RequestId>656c76696e6727732072657175657374</RequestId>\n"
                                 + "<HostId>Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==</HostId>\n"
                                 + "</ErrorResponse>";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(400).withBody(xmlResponseBody)));

        S3ControlClient s3Client = getSyncClientBuilder().build();

        assertThatThrownBy(() -> s3Client.getPublicAccessBlock(r -> r.accountId(accountId)))
            .isInstanceOf(S3ControlException.class)
            .isInstanceOf(NoSuchPublicAccessBlockConfigurationException.class)
            .satisfies(e -> assertThat(((S3ControlException) e).awsErrorDetails().errorCode())
                .isEqualTo("NoSuchPublicAccessBlockConfiguration"))
            .satisfies(e -> assertThat(((S3ControlException) e).awsErrorDetails().errorMessage()).contains("block"));
    }

    @Test
    public void standardErrorXML_translated_correctly_with_asyncClient() {
        String accountId = "Account-Id";
        String xmlResponseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                 + "<ErrorResponse>\n"
                                 + "<Error>\n"
                                 + "<AccountId>Account-Id</AccountId>\n"
                                 + "<Code>NoSuchPublicAccessBlockConfiguration</Code>\n"
                                 + "<Message>The public access block configuration was not found</Message>\n"
                                 + "</Error>\n"
                                 + "<RequestId>656c76696e6727732072657175657374</RequestId>\n"
                                 + "<HostId>Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==</HostId>\n"
                                 + "</ErrorResponse>";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(400).withBody(xmlResponseBody)));

        S3ControlAsyncClient s3Client = getAsyncClientBuilder().build();

        assertThatThrownBy(() -> s3Client.createJob(r -> r.accountId(accountId)).join())
            .isInstanceOf(CompletionException.class)
            .hasCauseExactlyInstanceOf(NoSuchPublicAccessBlockConfigurationException.class)
            .satisfies(e -> {
                S3ControlException s3ControlException = (S3ControlException) e.getCause();
                assertThat(s3ControlException.awsErrorDetails().errorCode())
                    .isEqualTo("NoSuchPublicAccessBlockConfiguration");
                assertThat(s3ControlException.awsErrorDetails().errorMessage()).contains("block");
            });
    }

    @Test
    public void xmlRootError_specificException_translated_correctly_with_syncClient() {
        String accountId = "Account-Id";
        String xmlResponseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                 + "<Error>\n"
                                 + "<Code>InvalidRequest</Code>\n"
                                 + "<Message>Missing role arn</Message>\n"
                                 + "<RequestId>656c76696e6727732072657175657374</RequestId>\n"
                                 + "<HostId>Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==</HostId>\n"
                                 + "</Error>";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(400).withBody(xmlResponseBody)));

        S3ControlClient s3Client = getSyncClientBuilder().build();

        assertThatThrownBy(() -> s3Client.createJob(r -> r.accountId(accountId)))
            .isInstanceOf(S3ControlException.class)
            .isInstanceOf(InvalidRequestException.class)
            .satisfies(e -> assertThat(((S3ControlException) e).awsErrorDetails().errorCode()).isEqualTo("InvalidRequest"))
            .satisfies(e -> assertThat(((S3ControlException) e).awsErrorDetails().errorMessage()).isEqualTo("Missing role arn"));
    }

    @Test
    public void xmlRootError_specificException_translated_correctly_with_asyncClient() {
        String accountId = "Account-Id";
        String xmlResponseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                 + "<Error>\n"
                                 + "<Code>InvalidRequest</Code>\n"
                                 + "<Message>Missing role arn</Message>\n"
                                 + "<RequestId>656c76696e6727732072657175657374</RequestId>\n"
                                 + "<HostId>Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==</HostId>\n"
                                 + "</Error>";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(400).withBody(xmlResponseBody)));

        S3ControlAsyncClient s3Client = getAsyncClientBuilder().build();

        assertThatThrownBy(() -> s3Client.createJob(r -> r.accountId(accountId)).join())
            .isInstanceOf(CompletionException.class)
            .hasCauseInstanceOf(S3ControlException.class)
            .hasCauseInstanceOf(InvalidRequestException.class)
            .satisfies(e -> {
                S3ControlException s3ControlException = (S3ControlException) e.getCause();
                assertThat(s3ControlException.awsErrorDetails().errorCode()).isEqualTo("InvalidRequest");
                assertThat(s3ControlException.awsErrorDetails().errorMessage()).isEqualTo("Missing role arn");
            });
    }

    @Test
    public void xmlRootError_unknownException_translated_correctly_with_syncClient() {
        String accountId = "Account-Id";
        String xmlResponseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                 + "<Error>\n"
                                 + "<Code>UnrecognizedCode</Code>\n"
                                 + "<Message>Error message</Message>\n"
                                 + "<RequestId>656c76696e6727732072657175657374</RequestId>\n"
                                 + "<HostId>Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==</HostId>\n"
                                 + "</Error>";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(400).withBody(xmlResponseBody)));

        S3ControlClient s3Client = getSyncClientBuilder().build();

        assertThatThrownBy(() -> s3Client.createJob(r -> r.accountId(accountId)))
            .isInstanceOf(S3ControlException.class)
            .isNotInstanceOf(InvalidRequestException.class)
            .satisfies(e -> assertThat(((S3ControlException) e).awsErrorDetails().errorCode()).isEqualTo("UnrecognizedCode"))
            .satisfies(e -> assertThat(((S3ControlException) e).awsErrorDetails().errorMessage()).isEqualTo("Error message"));
    }

    @Test
    public void xmlRootError_unknownException_translated_correctly_with_asyncClient() {
        String accountId = "Account-Id";
        String xmlResponseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                 + "<Error>\n"
                                 + "<Code>UnrecognizedCode</Code>\n"
                                 + "<Message>Error message</Message>\n"
                                 + "<RequestId>656c76696e6727732072657175657374</RequestId>\n"
                                 + "<HostId>Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==</HostId>\n"
                                 + "</Error>";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(400).withBody(xmlResponseBody)));

        S3ControlAsyncClient s3Client = getAsyncClientBuilder().build();

        assertThatThrownBy(() -> s3Client.createJob(r -> r.accountId(accountId)).join())
            .isInstanceOf(CompletionException.class)
            .hasCauseExactlyInstanceOf(S3ControlException.class)
            .satisfies(e -> {
                S3ControlException s3ControlException = (S3ControlException) e.getCause();
                assertThat(s3ControlException.awsErrorDetails().errorCode()).isEqualTo("UnrecognizedCode");
                assertThat(s3ControlException.awsErrorDetails().errorMessage()).isEqualTo("Error message");
            });
    }

    private static final class LocalhostEndpointAddressInterceptor implements ExecutionInterceptor {

        @Override
        public SdkHttpRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
            return context.httpRequest()
                          .toBuilder()
                          .protocol(HTTP_LOCALHOST_URI.getScheme())
                          .host(HTTP_LOCALHOST_URI.getHost())
                          .port(HTTP_LOCALHOST_URI.getPort())
                          .build();
        }
    }
}
