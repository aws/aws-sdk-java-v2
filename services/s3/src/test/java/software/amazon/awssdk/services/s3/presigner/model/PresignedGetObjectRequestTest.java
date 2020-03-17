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

package software.amazon.awssdk.services.s3.presigner.model;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;

@RunWith(MockitoJUnitRunner.class)
public class PresignedGetObjectRequestTest {
    private static final Map<String, List<String>> FAKE_SIGNED_HEADERS;
    private static final URL FAKE_URL;
    private static final SdkBytes FAKE_SIGNED_PAYLOAD = SdkBytes.fromString("fake-payload", StandardCharsets.UTF_8);

    static {
        Map<String, List<String>> map = new HashMap<>();
        map.put("fake-key", Collections.unmodifiableList(Arrays.asList("one", "two")));
        FAKE_SIGNED_HEADERS = Collections.unmodifiableMap(map);


        try {
            FAKE_URL = new URL("https://localhost");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Mock
    private SdkHttpRequest mockSdkHttpRequest;

    @Before
    public void setup() throws URISyntaxException {
        when(mockSdkHttpRequest.getUri()).thenReturn(FAKE_URL.toURI());
    }

    private PresignedGetObjectRequest generateMaximal() {
        return PresignedGetObjectRequest.builder()
                                        .expiration(Instant.MAX)
                                        .httpRequest(mockSdkHttpRequest)
                                        .signedHeaders(FAKE_SIGNED_HEADERS)
                                        .signedPayload(FAKE_SIGNED_PAYLOAD)
                                        .isBrowserExecutable(false)
                                        .build();
    }

    private PresignedGetObjectRequest generateMinimal() {
        return PresignedGetObjectRequest.builder()
                                        .expiration(Instant.MAX)
                                        .httpRequest(mockSdkHttpRequest)
                                        .signedHeaders(FAKE_SIGNED_HEADERS)
                                        .isBrowserExecutable(false)
                                        .build();
    }

    @Test
    public void build_allProperties() {
        PresignedGetObjectRequest presignedGetObjectRequest = generateMaximal();

        assertThat(presignedGetObjectRequest.expiration()).isEqualTo(Instant.MAX);
        assertThat(presignedGetObjectRequest.httpRequest()).isEqualTo(mockSdkHttpRequest);
        assertThat(presignedGetObjectRequest.signedHeaders()).isEqualTo(FAKE_SIGNED_HEADERS);
        assertThat(presignedGetObjectRequest.signedPayload()).isEqualTo(Optional.of(FAKE_SIGNED_PAYLOAD));
        assertThat(presignedGetObjectRequest.url()).isEqualTo(FAKE_URL);
    }

    @Test
    public void build_minimalProperties() {
        PresignedGetObjectRequest presignedGetObjectRequest = generateMinimal();

        assertThat(presignedGetObjectRequest.expiration()).isEqualTo(Instant.MAX);
        assertThat(presignedGetObjectRequest.httpRequest()).isEqualTo(mockSdkHttpRequest);
        assertThat(presignedGetObjectRequest.url()).isEqualTo(FAKE_URL);
        assertThat(presignedGetObjectRequest.signedHeaders()).isEqualTo(FAKE_SIGNED_HEADERS);
        assertThat(presignedGetObjectRequest.signedPayload()).isEmpty();
    }

    @Test
    public void build_missingProperty_expiration() {
        assertThatThrownBy(() -> generateMinimal().toBuilder().expiration(null).build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("expiration");
    }

    @Test
    public void build_missingProperty_httpRequest() {
        assertThatThrownBy(() -> generateMinimal().toBuilder().httpRequest(null).build())
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("httpRequest");
    }

    @Test
    public void hasSignedPayload_false() {
        PresignedGetObjectRequest presignedGetObjectRequest = generateMinimal();

        assertThat(presignedGetObjectRequest.signedPayload()).isNotPresent();
    }

    @Test
    public void hasSignedPayload_true() {
        PresignedGetObjectRequest presignedGetObjectRequest = generateMaximal();

        assertThat(presignedGetObjectRequest.signedPayload()).isPresent();
    }

    @Test
    public void equalsAndHashCode_maximal() {
        PresignedGetObjectRequest request = generateMaximal();
        PresignedGetObjectRequest otherRequest = generateMaximal();

        assertThat(request).isEqualTo(otherRequest);
        assertThat(request.hashCode()).isEqualTo(otherRequest.hashCode());
    }

    @Test
    public void equalsAndHashCode_minimal() {
        PresignedGetObjectRequest request = generateMinimal();
        PresignedGetObjectRequest otherRequest = generateMinimal();

        assertThat(request).isEqualTo(otherRequest);
        assertThat(request.hashCode()).isEqualTo(otherRequest.hashCode());
    }

    @Test
    public void equalsAndHashCode_differentProperty_httpRequest() throws URISyntaxException {
        SdkHttpRequest otherHttpRequest = mock(SdkHttpRequest.class);
        when(otherHttpRequest.getUri()).thenReturn(FAKE_URL.toURI());

        PresignedGetObjectRequest request = generateMaximal();
        PresignedGetObjectRequest otherRequest = request.toBuilder().httpRequest(otherHttpRequest).build();

        assertThat(request).isNotEqualTo(otherRequest);
        assertThat(request.hashCode()).isNotEqualTo(otherRequest.hashCode());
    }

    @Test
    public void equalsAndHashCode_differentProperty_expiration() {
        PresignedGetObjectRequest request = generateMaximal();
        PresignedGetObjectRequest otherRequest = request.toBuilder().expiration(Instant.MIN).build();

        assertThat(request).isNotEqualTo(otherRequest);
        assertThat(request.hashCode()).isNotEqualTo(otherRequest.hashCode());
    }

    @Test
    public void equalsAndHashCode_differentProperty_signedPayload() {
        SdkBytes otherSignedPayload = SdkBytes.fromString("other-payload", StandardCharsets.UTF_8);

        PresignedGetObjectRequest request = generateMaximal();
        PresignedGetObjectRequest otherRequest = request.toBuilder().signedPayload(otherSignedPayload).build();

        assertThat(request).isNotEqualTo(otherRequest);
        assertThat(request.hashCode()).isNotEqualTo(otherRequest.hashCode());
    }

    @Test
    public void equalsAndHashCode_differentProperty_signedHeaders() {
        Map<String, List<String>> otherSignedHeaders = new HashMap<>();
        otherSignedHeaders.put("fake-key", Collections.unmodifiableList(Arrays.asList("other-one", "other-two")));

        PresignedGetObjectRequest request = generateMaximal();
        PresignedGetObjectRequest otherRequest = request.toBuilder().signedHeaders(otherSignedHeaders).build();

        assertThat(request).isNotEqualTo(otherRequest);
        assertThat(request.hashCode()).isNotEqualTo(otherRequest.hashCode());
    }
}