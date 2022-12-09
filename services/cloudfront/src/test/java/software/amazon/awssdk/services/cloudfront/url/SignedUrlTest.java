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

package software.amazon.awssdk.services.cloudfront.url;

import static org.assertj.core.api.Assertions.assertThat;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.cloudfront.internal.url.DefaultSignedUrl;

class SignedUrlTest {

    private static final String PROTOCOL = "https";
    private static final String DOMAIN = "d1npcfkc2mojrf.cloudfront.net";
    private static final String ENCODED_PATH = "/encodedPath";
    private static final String URL = PROTOCOL + "://" + DOMAIN + ENCODED_PATH;

    @Test
    void signedUrl_producesValidUrl() {
        SignedUrl signedUrl = DefaultSignedUrl.builder()
                                              .protocol(PROTOCOL).
                                              domain(DOMAIN).
                                              encodedPath(ENCODED_PATH).
                                              url(URL).build();

        assertThat(signedUrl.protocol()).isEqualTo(PROTOCOL);
        assertThat(signedUrl.domain()).isEqualTo(DOMAIN);
        assertThat(signedUrl.encodedPath()).isEqualTo(ENCODED_PATH);
        assertThat(signedUrl.url()).isEqualTo(URL);
    }

    @Test
    void generateHttpGetRequest_producesValidRequest() {
        SignedUrl signedUrl = DefaultSignedUrl.builder()
                                              .protocol(PROTOCOL)
                                              .domain(DOMAIN)
                                              .encodedPath(ENCODED_PATH).build();
        SdkHttpRequest httpRequest = signedUrl.createHttpGetRequest();

        assertThat(httpRequest.protocol()).isEqualTo(PROTOCOL);
        assertThat(httpRequest.host()).isEqualTo(DOMAIN);
        assertThat(httpRequest.encodedPath()).isEqualTo(ENCODED_PATH);
    }

    @Test
    void testEqualsAndHashCodeContract() {
        EqualsVerifier.forClass(DefaultSignedUrl.class).verify();
    }

}
