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
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class CompleteMultipartUploadFunctionalTest {
    private static final URI HTTP_LOCALHOST_URI = URI.create("http://localhost:8080/");

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
    public void completeMultipartUpload_syncClient_completeResponse() {
        String location = "http://Example-Bucket.s3.amazonaws.com/Example-Object";
        String bucket = "Example-Bucket";
        String key = "Example-Object";
        String eTag = "\"3858f62230ac3c915f300c664312c11f-9\"";
        String xmlResponseBody = String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<CompleteMultipartUploadResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">\n"
            + "<Location>%s</Location>\n"
            + "<Bucket>%s</Bucket>\n"
            + "<Key>%s</Key>\n"
            + "<ETag>%s</ETag>\n"
            + "</CompleteMultipartUploadResult>", location, bucket, key, eTag);

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(xmlResponseBody)));

        S3Client s3Client = getSyncClientBuilder().build();

        CompleteMultipartUploadResponse response = s3Client.completeMultipartUpload(
            r -> r.bucket(bucket).key(key).uploadId("upload-id"));

        assertThat(response.location()).isEqualTo(location);
        assertThat(response.bucket()).isEqualTo(bucket);
        assertThat(response.key()).isEqualTo(key);
        assertThat(response.eTag()).isEqualTo(eTag);
    }

    @Test
    public void completeMultipartUpload_asyncClient_completeResponse() {
        String location = "http://Example-Bucket.s3.amazonaws.com/Example-Object";
        String bucket = "Example-Bucket";
        String key = "Example-Object";
        String eTag = "\"3858f62230ac3c915f300c664312c11f-9\"";
        String xmlResponseBody = String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<CompleteMultipartUploadResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">\n"
            + "<Location>%s</Location>\n"
            + "<Bucket>%s</Bucket>\n"
            + "<Key>%s</Key>\n"
            + "<ETag>%s</ETag>\n"
            + "</CompleteMultipartUploadResult>", location, bucket, key, eTag);

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(xmlResponseBody)));

        S3AsyncClient s3Client = getAsyncClientBuilder().build();

        CompleteMultipartUploadResponse response = s3Client.completeMultipartUpload(
            r -> r.bucket(bucket).key(key).uploadId("upload-id")).join();

        assertThat(response.location()).isEqualTo(location);
        assertThat(response.bucket()).isEqualTo(bucket);
        assertThat(response.key()).isEqualTo(key);
        assertThat(response.eTag()).isEqualTo(eTag);
    }

    @Test
    public void completeMultipartUpload_syncClient_errorInResponseBody_correctType() {
        String bucket = "Example-Bucket";
        String key = "Example-Object";
        String xmlResponseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                 + "<Error>\n"
                                 + "<Code>InternalError</Code>\n"
                                 + "<Message>We encountered an internal error. Please try again.</Message>\n"
                                 + "<RequestId>656c76696e6727732072657175657374</RequestId>\n"
                                 + "<HostId>Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==</HostId>\n"
                                 + "</Error>";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(xmlResponseBody)));

        S3Client s3Client = getSyncClientBuilder().build();

        assertThatThrownBy(() -> s3Client.completeMultipartUpload(r -> r.bucket(bucket)
                                                                        .key(key)
                                                                        .uploadId("upload-id")))
            .isInstanceOf(S3Exception.class);
    }

    @Test
    public void completeMultipartUpload_asyncClient_errorInResponseBody_correctType() {
        String bucket = "Example-Bucket";
        String key = "Example-Object";
        String xmlResponseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                 + "<Error>\n"
                                 + "<Code>InternalError</Code>\n"
                                 + "<Message>We encountered an internal error. Please try again.</Message>\n"
                                 + "<RequestId>656c76696e6727732072657175657374</RequestId>\n"
                                 + "<HostId>Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==</HostId>\n"
                                 + "</Error>";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(xmlResponseBody)));

        S3AsyncClient s3Client = getAsyncClientBuilder().build();

        assertThatThrownBy(() -> s3Client.completeMultipartUpload(r -> r.bucket(bucket)
                                                                        .key(key)
                                                                        .uploadId("upload-id"))
                                         .join())
            .isInstanceOf(CompletionException.class)
            .hasCauseInstanceOf(S3Exception.class);
    }

    @Test
    public void completeMultipartUpload_syncClient_errorInResponseBody_correctCode() {
        String bucket = "Example-Bucket";
        String key = "Example-Object";
        String xmlResponseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                 + "<Error>\n"
                                 + "<Code>CustomError</Code>\n"
                                 + "<Message>We encountered an internal error. Please try again.</Message>\n"
                                 + "<RequestId>656c76696e6727732072657175657374</RequestId>\n"
                                 + "<HostId>Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==</HostId>\n"
                                 + "</Error>";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(xmlResponseBody)));

        S3Client s3Client = getSyncClientBuilder().build();

        assertThatThrownBy(() -> s3Client.completeMultipartUpload(r -> r.bucket(bucket)
                                                                        .key(key)
                                                                        .uploadId("upload-id")))
            .satisfies(e -> assertThat(((S3Exception)e).awsErrorDetails().errorCode()).isEqualTo("CustomError"));
    }

    @Test
    public void completeMultipartUpload_asyncClient_errorInResponseBody_correctCode() {
        String bucket = "Example-Bucket";
        String key = "Example-Object";
        String xmlResponseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                 + "<Error>\n"
                                 + "<Code>CustomError</Code>\n"
                                 + "<Message>We encountered an internal error. Please try again.</Message>\n"
                                 + "<RequestId>656c76696e6727732072657175657374</RequestId>\n"
                                 + "<HostId>Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==</HostId>\n"
                                 + "</Error>";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(xmlResponseBody)));

        S3AsyncClient s3Client = getAsyncClientBuilder().build();

        assertThatThrownBy(() -> s3Client.completeMultipartUpload(r -> r.bucket(bucket)
                                                                        .key(key)
                                                                        .uploadId("upload-id"))
                                         .join())
            .satisfies(e -> {
                S3Exception s3Exception = (S3Exception) e.getCause();
                assertThat(s3Exception.awsErrorDetails().errorCode()).isEqualTo("CustomError");
            });
    }

    @Test
    public void completeMultipartUpload_syncClient_errorInResponseBody_correctMessage() {
        String bucket = "Example-Bucket";
        String key = "Example-Object";
        String xmlResponseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                 + "<Error>\n"
                                 + "<Code>CustomError</Code>\n"
                                 + "<Message>Foo bar</Message>\n"
                                 + "<RequestId>656c76696e6727732072657175657374</RequestId>\n"
                                 + "<HostId>Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==</HostId>\n"
                                 + "</Error>";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(xmlResponseBody)));

        S3Client s3Client = getSyncClientBuilder().build();

        assertThatThrownBy(() -> s3Client.completeMultipartUpload(r -> r.bucket(bucket)
                                                                        .key(key)
                                                                        .uploadId("upload-id")))
            .satisfies(e -> assertThat(((S3Exception)e).awsErrorDetails().errorMessage()).isEqualTo("Foo bar"));
    }

    @Test
    public void completeMultipartUpload_asyncClient_errorInResponseBody_correctMessage() {
        String bucket = "Example-Bucket";
        String key = "Example-Object";
        String xmlResponseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                 + "<Error>\n"
                                 + "<Code>CustomError</Code>\n"
                                 + "<Message>Foo bar</Message>\n"
                                 + "<RequestId>656c76696e6727732072657175657374</RequestId>\n"
                                 + "<HostId>Uuag1LuByRx9e6j5Onimru9pO4ZVKnJ2Qz7/C1NPcfTWAtRPfTaOFg==</HostId>\n"
                                 + "</Error>";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(xmlResponseBody)));

        S3AsyncClient s3Client = getAsyncClientBuilder().build();

        assertThatThrownBy(() -> s3Client.completeMultipartUpload(r -> r.bucket(bucket)
                                                                        .key(key)
                                                                        .uploadId("upload-id"))
                          .join())
            .satisfies(e -> {
                S3Exception s3Exception = (S3Exception) e.getCause();
                assertThat(s3Exception.awsErrorDetails().errorMessage()).isEqualTo("Foo bar");
            });
    }

    @Test
    public void completeMultipartUpload_syncClient_errorInResponseBody_invalidErrorXml() {
        String bucket = "Example-Bucket";
        String key = "Example-Object";
        String xmlResponseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                 + "<Error>\n"
                                 + "<SomethingWeird></SomethingWeird>"
                                 + "</Error>";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(xmlResponseBody)));

        S3Client s3Client = getSyncClientBuilder().build();

        assertThatThrownBy(() -> s3Client.completeMultipartUpload(r -> r.bucket(bucket)
                                                                        .key(key)
                                                                        .uploadId("upload-id")))
            .isInstanceOf(S3Exception.class);
    }

    @Test
    public void completeMultipartUpload_asyncClient_errorInResponseBody_invalidErrorXml() {
        String bucket = "Example-Bucket";
        String key = "Example-Object";
        String xmlResponseBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                 + "<Error>\n"
                                 + "<SomethingWeird></SomethingWeird>"
                                 + "</Error>";

        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(xmlResponseBody)));

        S3AsyncClient s3Client = getAsyncClientBuilder().build();

        assertThatThrownBy(() -> s3Client.completeMultipartUpload(r -> r.bucket(bucket)
                                                                        .key(key)
                                                                        .uploadId("upload-id"))
                          .join())
            .isInstanceOf(CompletionException.class)
            .hasCauseInstanceOf(S3Exception.class);
    }
}
