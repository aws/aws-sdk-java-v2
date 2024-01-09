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

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.InputStream;
import java.net.URI;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;

@WireMockTest
public class DeleteObjectsFunctionalTest {

    private static S3AsyncClient s3Client;
    private static final CapturingInterceptor CAPTURING_INTERCEPTOR = new CapturingInterceptor();

    @BeforeEach
    public void init(WireMockRuntimeInfo wm) {
        s3Client = S3AsyncClient.builder()
                                .region(Region.US_EAST_1)
                                .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                .overrideConfiguration(c -> c.addExecutionInterceptor(CAPTURING_INTERCEPTOR))
                                .credentialsProvider(
                                    StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")))
                                .build();
    }

    private static Stream<Arguments> testKeys() {
        return Stream.of(
            Arguments.of("<Key>objectId</Key>", "&lt;Key&gt;objectId&lt;/Key&gt;"),
            Arguments.of("&lt;Key&gt;objectId&lt;/Key&gt;", "&amp;lt;Key&amp;gt;objectId&amp;lt;/Key&amp;gt;"),
            Arguments.of("&lt;<", "&amp;lt;&lt;")
        );
    }

    @ParameterizedTest
    @MethodSource("testKeys")
    public void deleteObjects_shouldProperlyEncodeKeysInPayload(String key, String encodedKey) {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)));

        Delete delete = Delete.builder().objects(o -> o.key(key)).build();
        DeleteObjectsRequest request = DeleteObjectsRequest.builder().bucket("bucket").delete(delete).build();
        s3Client.deleteObjects(request);

        verifyPayload(encodedKey);
    }

    private void verifyPayload(String encodedKey) {
        String expectedPayload = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Delete xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\"><Object><Key>"
                                 + encodedKey
                                 + "</Key></Object></Delete>";

        assertThat(CAPTURING_INTERCEPTOR.payload).contains(expectedPayload);
    }

    private static final class CapturingInterceptor implements ExecutionInterceptor {
        private String payload;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            SdkHttpFullRequest request = (SdkHttpFullRequest) context.httpRequest();
            InputStream is = request.contentStreamProvider().get().newStream();
            byte[] buf = new byte[200];
            try {
                is.read(buf);
            } catch (Exception e) {
                throw SdkClientException.create(e.getMessage(), e);
            }
            payload = new String(buf);
        }
    }
}
