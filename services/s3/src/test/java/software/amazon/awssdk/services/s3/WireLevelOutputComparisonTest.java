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

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpFullResponse;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

/**
 * Captures wire-level output (endpoint URL, headers, signing properties, auth scheme) to verify
 * the interceptor-to-pipeline migration produces identical requests.
 *
 * Run on both master and feature branch, then diff the output.
 */
public class WireLevelOutputComparisonTest {

    private static final StaticCredentialsProvider CREDENTIALS =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));

    @Test
    void s3_getObject_virtualHostedStyle() {
        WireCapture capture = executeS3GetObject(null);

        assertThat(capture.host).isEqualTo("my-bucket.s3.us-west-2.amazonaws.com");
        assertThat(capture.protocol).isEqualTo("https");
        assertThat(capture.path).isEqualTo("/my-key");
        assertThat(capture.authSchemeId).isEqualTo("aws.auth#sigv4");
        assertThat(capture.signingRegion).isEqualTo("us-west-2");
        assertThat(capture.signingName).isNotEmpty();
        assertThat(capture.resolvedEndpointUrl).isNotNull();

        // Print for manual diff between branches
        System.out.println("=== S3 GetObject (virtual hosted) ===");
        System.out.println(capture);
    }

    @Test
    void s3_getObject_withEndpointOverride() {
        WireCapture capture = executeS3GetObject(URI.create("https://custom-endpoint.example.com"));

        assertThat(capture.host).isEqualTo("my-bucket.custom-endpoint.example.com");
        assertThat(capture.protocol).isEqualTo("https");
        assertThat(capture.authSchemeId).isEqualTo("aws.auth#sigv4");
        assertThat(capture.signingRegion).isEqualTo("us-west-2");

        System.out.println("=== S3 GetObject (endpoint override) ===");
        System.out.println(capture);
    }

    @Test
    void s3_getObject_pathStyle() {
        WireCapture capture = executeS3GetObjectPathStyle();

        assertThat(capture.host).isEqualTo("s3.us-west-2.amazonaws.com");
        assertThat(capture.path).startsWith("/my-bucket/my-key");
        assertThat(capture.authSchemeId).isEqualTo("aws.auth#sigv4");

        System.out.println("=== S3 GetObject (path style) ===");
        System.out.println(capture);
    }

    @Test
    void s3_putObject() {
        WireCapture capture = executeS3PutObject();

        assertThat(capture.host).isEqualTo("my-bucket.s3.us-west-2.amazonaws.com");
        assertThat(capture.path).isEqualTo("/my-key");
        assertThat(capture.method).isEqualTo("PUT");
        assertThat(capture.authSchemeId).isEqualTo("aws.auth#sigv4");

        System.out.println("=== S3 PutObject ===");
        System.out.println(capture);
    }

    @Test
    void s3_getObject_mrapBucket() {
        MockSyncHttpClient httpClient = new MockSyncHttpClient();
        httpClient.stubResponses(successResponse());

        WireCaptureInterceptor interceptor = new WireCaptureInterceptor();

        try (S3Client client = S3Client.builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .httpClient(httpClient)
            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
            .build()) {
            client.getObject(r -> r.bucket("arn:aws:s3::123456789012:accesspoint/mfzwi23gnjvgw.mrap")
                                   .key("my-key"));
        } catch (Exception e) {
            // Response parsing may fail with mock
        }

        WireCapture capture = interceptor.capture;
        assertThat(capture.host).contains(".mrap.");
        assertThat(capture.authSchemeId).isEqualTo("aws.auth#sigv4a");

        System.out.println("=== S3 GetObject (MRAP) ===");
        System.out.println(capture);
    }

    @Test
    void s3_presignGetObject() throws Exception {
        S3Presigner presigner = S3Presigner.builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .build();

        PresignedGetObjectRequest presigned = presigner.presignGetObject(r -> r
            .signatureDuration(Duration.ofMinutes(10))
            .getObjectRequest(g -> g.bucket("my-bucket").key("my-key")));

        URI url = presigned.url().toURI();

        assertThat(url.getHost()).isEqualTo("my-bucket.s3.us-west-2.amazonaws.com");
        assertThat(url.getPath()).isEqualTo("/my-key");
        assertThat(url.getQuery()).contains("X-Amz-Algorithm=AWS4-HMAC-SHA256");
        assertThat(url.getQuery()).contains("X-Amz-SignedHeaders=host");

        System.out.println("=== S3 Presign GetObject ===");
        System.out.println("URL: " + url);
        System.out.println("Host: " + url.getHost());
        System.out.println("Path: " + url.getPath());
        // Print query params individually (exclude signature which changes)
        for (String param : url.getQuery().split("&")) {
            if (!param.startsWith("X-Amz-Signature=") && !param.startsWith("X-Amz-Date=")) {
                System.out.println("QueryParam: " + param);
            }
        }
    }

    private WireCapture executeS3GetObject(URI endpointOverride) {
        MockSyncHttpClient httpClient = new MockSyncHttpClient();
        httpClient.stubResponses(successResponse());

        WireCaptureInterceptor interceptor = new WireCaptureInterceptor();

        S3ClientBuilder builder = S3Client.builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .httpClient(httpClient)
            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor));

        if (endpointOverride != null) {
            builder.endpointOverride(endpointOverride);
        }

        try (S3Client client = builder.build()) {
            client.getObject(r -> r.bucket("my-bucket").key("my-key"));
        } catch (Exception e) {
            // Response parsing may fail with mock, that's fine — we captured the request
        }

        return interceptor.capture;
    }

    private WireCapture executeS3GetObjectPathStyle() {
        MockSyncHttpClient httpClient = new MockSyncHttpClient();
        httpClient.stubResponses(successResponse());

        WireCaptureInterceptor interceptor = new WireCaptureInterceptor();

        try (S3Client client = S3Client.builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .httpClient(httpClient)
            .forcePathStyle(true)
            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
            .build()) {
            client.getObject(r -> r.bucket("my-bucket").key("my-key"));
        } catch (Exception e) {
            // Response parsing may fail with mock
        }

        return interceptor.capture;
    }

    private WireCapture executeS3PutObject() {
        MockSyncHttpClient httpClient = new MockSyncHttpClient();
        httpClient.stubResponses(successResponse());

        WireCaptureInterceptor interceptor = new WireCaptureInterceptor();

        try (S3Client client = S3Client.builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(CREDENTIALS)
            .httpClient(httpClient)
            .overrideConfiguration(o -> o.addExecutionInterceptor(interceptor))
            .build()) {
            client.putObject(r -> r.bucket("my-bucket").key("my-key"),
                             RequestBody.fromString("hello"));
        } catch (Exception e) {
            // Response parsing may fail with mock
        }

        return interceptor.capture;
    }

    private static HttpExecuteResponse successResponse() {
        return HttpExecuteResponse.builder()
            .response(SdkHttpFullResponse.builder().statusCode(200).build())
            .build();
    }

    private static class WireCapture {
        String host;
        String protocol;
        String path;
        String method;
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
            capture.headers = request.headers();

            // Auth scheme details
            SelectedAuthScheme<?> selectedAuthScheme = executionAttributes.getAttribute(
                SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
            if (selectedAuthScheme != null) {
                AuthSchemeOption option = selectedAuthScheme.authSchemeOption();
                capture.authSchemeId = option.schemeId();
                capture.signingRegion = option.signerProperty(AwsV4HttpSigner.REGION_NAME);
                capture.signingName = option.signerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME);
            }

            // Resolved endpoint
            Endpoint resolvedEndpoint = executionAttributes.getAttribute(
                SdkInternalExecutionAttribute.RESOLVED_ENDPOINT);
            if (resolvedEndpoint != null) {
                capture.resolvedEndpointUrl = resolvedEndpoint.url().toString();
            }
        }
    }
}
