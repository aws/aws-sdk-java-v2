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

package software.amazon.awssdk.services.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Captures wire-level output for DynamoDB to verify the interceptor-to-pipeline migration
 * produces identical requests. Run on both master and feature branch, then diff.
 */
public class WireLevelOutputComparisonTest {

    private static final StaticCredentialsProvider CREDENTIALS =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));

    @Test
    void dynamodb_getItem() {
        WireCapture capture = executeDynamoDbGetItem(null);

        assertThat(capture.host).isEqualTo("dynamodb.us-west-2.amazonaws.com");
        assertThat(capture.protocol).isEqualTo("https");
        assertThat(capture.path).isEqualTo("/");
        assertThat(capture.method).isEqualTo("POST");
        assertThat(capture.authSchemeId).isEqualTo("aws.auth#sigv4");
        assertThat(capture.signingRegion).isEqualTo("us-west-2");
        assertThat(capture.signingName).isEqualTo("dynamodb");
        assertThat(capture.headers.get("X-Amz-Target"))
            .isEqualTo(Collections.singletonList("DynamoDB_20120810.GetItem"));

        System.out.println("=== DynamoDB GetItem ===");
        System.out.println(capture);
    }

    @Test
    void dynamodb_getItem_withEndpointOverride() {
        WireCapture capture = executeDynamoDbGetItem(URI.create("https://localhost:8000"));

        assertThat(capture.host).isEqualTo("localhost");
        assertThat(capture.port).isEqualTo(8000);
        assertThat(capture.authSchemeId).isEqualTo("aws.auth#sigv4");
        assertThat(capture.signingRegion).isEqualTo("us-west-2");

        System.out.println("=== DynamoDB GetItem (endpoint override) ===");
        System.out.println(capture);
    }

    @Test
    void dynamodb_putItem() {
        WireCapture capture = executeDynamoDbPutItem();

        assertThat(capture.host).isEqualTo("dynamodb.us-west-2.amazonaws.com");
        assertThat(capture.method).isEqualTo("POST");
        assertThat(capture.authSchemeId).isEqualTo("aws.auth#sigv4");
        assertThat(capture.headers.get("X-Amz-Target"))
            .isEqualTo(Collections.singletonList("DynamoDB_20120810.PutItem"));

        System.out.println("=== DynamoDB PutItem ===");
        System.out.println(capture);
    }

    private WireCapture executeDynamoDbGetItem(URI endpointOverride) {
        CapturingHttpClient httpClient = new CapturingHttpClient();
        WireCaptureInterceptor interceptor = new WireCaptureInterceptor();

        DynamoDbClientBuilder builder = DynamoDbClient.builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .httpClient(httpClient)
            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor));

        if (endpointOverride != null) {
            builder.endpointOverride(endpointOverride);
        }

        try (DynamoDbClient client = builder.build()) {
            client.getItem(r -> r.tableName("test-table")
                                 .key(Collections.singletonMap("id",
                                     AttributeValue.builder().s("123").build())));
        } catch (Exception e) {
            // Response parsing may fail with mock
        }

        return interceptor.capture;
    }

    private WireCapture executeDynamoDbPutItem() {
        CapturingHttpClient httpClient = new CapturingHttpClient();
        WireCaptureInterceptor interceptor = new WireCaptureInterceptor();

        try (DynamoDbClient client = DynamoDbClient.builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .httpClient(httpClient)
            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
            .build()) {
            client.putItem(r -> r.tableName("test-table")
                                 .item(Collections.singletonMap("id",
                                     AttributeValue.builder().s("123").build())));
        } catch (Exception e) {
            // Response parsing may fail with mock
        }

        return interceptor.capture;
    }

    private static class WireCapture {
        String host;
        String protocol;
        String path;
        String method;
        int port;
        Map<String, List<String>> headers;
        String authSchemeId;
        String signingRegion;
        String signingName;
        String resolvedEndpointUrl;

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Host: ").append(host).append("\n");
            sb.append("Protocol: ").append(protocol).append("\n");
            sb.append("Path: ").append(path).append("\n");
            sb.append("Method: ").append(method).append("\n");
            sb.append("Port: ").append(port).append("\n");
            sb.append("AuthSchemeId: ").append(authSchemeId).append("\n");
            sb.append("SigningRegion: ").append(signingRegion).append("\n");
            sb.append("SigningName: ").append(signingName).append("\n");
            sb.append("ResolvedEndpointUrl: ").append(resolvedEndpointUrl).append("\n");
            if (headers != null) {
                headers.forEach((k, v) -> {
                    if (!k.equalsIgnoreCase("Authorization") && !k.equalsIgnoreCase("X-Amz-Date")) {
                        sb.append("Header[").append(k).append("]: ").append(v).append("\n");
                    }
                });
            }
            return sb.toString();
        }
    }

    private static class WireCaptureInterceptor implements ExecutionInterceptor {
        WireCapture capture = new WireCapture();

        @Override
        public void beforeTransmission(Context.BeforeTransmission context,
                                        ExecutionAttributes executionAttributes) {
            SdkHttpRequest request = context.httpRequest();
            capture.host = request.host();
            capture.protocol = request.protocol();
            capture.path = request.encodedPath();
            capture.method = request.method().name();
            capture.port = request.port();
            capture.headers = request.headers();

            SelectedAuthScheme<?> selectedAuthScheme = executionAttributes.getAttribute(
                SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
            if (selectedAuthScheme != null) {
                AuthSchemeOption option = selectedAuthScheme.authSchemeOption();
                capture.authSchemeId = option.schemeId();
                capture.signingRegion = option.signerProperty(AwsV4HttpSigner.REGION_NAME);
                capture.signingName = option.signerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME);
            }

            Endpoint resolvedEndpoint = executionAttributes.getAttribute(
                SdkInternalExecutionAttribute.RESOLVED_ENDPOINT);
            if (resolvedEndpoint != null) {
                capture.resolvedEndpointUrl = resolvedEndpoint.url().toString();
            }
        }
    }

    private static class CapturingHttpClient implements SdkHttpClient {
        @Override
        public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
            return new ExecutableHttpRequest() {
                @Override
                public HttpExecuteResponse call() {
                    return HttpExecuteResponse.builder()
                        .response(SdkHttpFullResponse.builder().statusCode(200).build())
                        .responseBody(AbortableInputStream.create(
                            new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8))))
                        .build();
                }
                @Override
                public void abort() {}
            };
        }
        @Override
        public void close() {}
    }
}
