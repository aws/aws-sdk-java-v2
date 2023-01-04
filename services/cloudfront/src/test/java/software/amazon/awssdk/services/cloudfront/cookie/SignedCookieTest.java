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

package software.amazon.awssdk.services.cloudfront.cookie;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Map;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.cloudfront.internal.cookie.DefaultCookiesForCannedPolicy;
import software.amazon.awssdk.services.cloudfront.internal.cookie.DefaultCookiesForCustomPolicy;

class SignedCookieTest {

    private static final String KEY_PAIR_ID_KEY = "CloudFront-Key-Pair-Id";
    private static final String SIGNATURE_KEY = "CloudFront-Signature";
    private static final String EXPIRES_KEY = "CloudFront-Expires";
    private static final String POLICY_KEY = "CloudFront-Policy";
    private static final String KEY_PAIR_ID_VALUE = "keyPairIdValue";
    private static final String SIGNATURE_VALUE = "signatureValue";
    private static final String EXPIRES_VALUE = "expiresValue";
    private static final String POLICY_VALUE = "policyValue";
    private static final String RESOURCE_URL = "https://d1npcfkc2mojrf.cloudfront.net/resourcePath";
    private static final String EXPECTED_KEY_PAIR_ID_HEADER = KEY_PAIR_ID_KEY + "=" + KEY_PAIR_ID_VALUE;
    private static final String EXPECTED_SIGNATURE_HEADER = SIGNATURE_KEY + "=" + SIGNATURE_VALUE;
    private static final String EXPECTED_EXPIRES_HEADER = EXPIRES_KEY + "=" + EXPIRES_VALUE;
    private static final String EXPECTED_POLICY_HEADER = POLICY_KEY + "=" + POLICY_VALUE;

    @Test
    void cookiesForCannedPolicy_producesValidCookies() {
        CookiesForCannedPolicy cookiesForCannedPolicy = DefaultCookiesForCannedPolicy.builder()
                                                                                     .resourceUrl(RESOURCE_URL)
                                                                                     .keyPairIdHeaderValue(KEY_PAIR_ID_KEY +
                                                                                                           "=" + KEY_PAIR_ID_VALUE)
                                                                                     .signatureHeaderValue(SIGNATURE_KEY +
                                                                                                           "=" + SIGNATURE_VALUE)
                                                                                     .expiresHeaderValue(EXPIRES_KEY +
                                                                                                         "=" + EXPIRES_VALUE)
                                                                                     .build();

        assertThat(cookiesForCannedPolicy.resourceUrl()).isEqualTo(RESOURCE_URL);
        assertThat(cookiesForCannedPolicy.keyPairIdHeaderValue()).isEqualTo(EXPECTED_KEY_PAIR_ID_HEADER);
        assertThat(cookiesForCannedPolicy.signatureHeaderValue()).isEqualTo(EXPECTED_SIGNATURE_HEADER);
        assertThat(cookiesForCannedPolicy.expiresHeaderValue()).isEqualTo(EXPECTED_EXPIRES_HEADER);
    }

    @Test
    void cookiesForCustomPolicy_producesValidCookies() {
        CookiesForCustomPolicy cookiesForCustomPolicy = DefaultCookiesForCustomPolicy.builder()
                                                                                     .resourceUrl(RESOURCE_URL)
                                                                                     .keyPairIdHeaderValue(KEY_PAIR_ID_KEY +
                                                                                                           "=" + KEY_PAIR_ID_VALUE)
                                                                                     .signatureHeaderValue(SIGNATURE_KEY +
                                                                                                           "=" + SIGNATURE_VALUE)
                                                                                     .policyHeaderValue(POLICY_KEY +
                                                                                                        "=" + POLICY_VALUE)
                                                                                     .build();

        assertThat(cookiesForCustomPolicy.resourceUrl()).isEqualTo(RESOURCE_URL);
        assertThat(cookiesForCustomPolicy.keyPairIdHeaderValue()).isEqualTo(EXPECTED_KEY_PAIR_ID_HEADER);
        assertThat(cookiesForCustomPolicy.signatureHeaderValue()).isEqualTo(EXPECTED_SIGNATURE_HEADER);
        assertThat(cookiesForCustomPolicy.policyHeaderValue()).isEqualTo(EXPECTED_POLICY_HEADER);
    }

    @Test
    void generateHttpGetRequest_producesValidCookies() {
        CookiesForCannedPolicy cookiesForCannedPolicy = DefaultCookiesForCannedPolicy.builder()
                                                                                     .resourceUrl(RESOURCE_URL)
                                                                                     .keyPairIdHeaderValue(KEY_PAIR_ID_KEY +
                                                                                                           "=" + KEY_PAIR_ID_VALUE)
                                                                                     .signatureHeaderValue(SIGNATURE_KEY +
                                                                                                           "=" + SIGNATURE_VALUE)
                                                                                     .expiresHeaderValue(EXPIRES_KEY +
                                                                                                         "=" + EXPIRES_VALUE)
                                                                                     .build();
        SdkHttpRequest httpRequestCannedPolicy = cookiesForCannedPolicy.createHttpGetRequest();
        Map<String, List<String>> headersCannedPolicy = httpRequestCannedPolicy.headers();
        List<String> headerValuesCannedPolicy = headersCannedPolicy.get("Cookie");
        assertThat(httpRequestCannedPolicy.getUri()).isEqualTo(URI.create(RESOURCE_URL));
        assertThat(headerValuesCannedPolicy.get(0)).isEqualTo(EXPECTED_SIGNATURE_HEADER);
        assertThat(headerValuesCannedPolicy.get(1)).isEqualTo(EXPECTED_KEY_PAIR_ID_HEADER);
        assertThat(headerValuesCannedPolicy.get(2)).isEqualTo(EXPECTED_EXPIRES_HEADER);

        CookiesForCustomPolicy cookiesForCustomPolicy = DefaultCookiesForCustomPolicy.builder()
                                                                                     .resourceUrl(RESOURCE_URL)
                                                                                     .keyPairIdHeaderValue(KEY_PAIR_ID_KEY +
                                                                                                           "=" + KEY_PAIR_ID_VALUE)
                                                                                     .signatureHeaderValue(SIGNATURE_KEY +
                                                                                                           "=" + SIGNATURE_VALUE)
                                                                                     .policyHeaderValue(POLICY_KEY +
                                                                                                        "=" + POLICY_VALUE)
                                                                                     .build();
        SdkHttpRequest httpRequestCustomPolicy = cookiesForCustomPolicy.createHttpGetRequest();
        Map<String, List<String>> headersCustomPolicy = httpRequestCustomPolicy.headers();
        List<String> headerValuesCustomPolicy = headersCustomPolicy.get("Cookie");

        assertThat(httpRequestCustomPolicy.getUri()).isEqualTo(URI.create(RESOURCE_URL));
        assertThat(headerValuesCustomPolicy.get(0)).isEqualTo(EXPECTED_POLICY_HEADER);
        assertThat(headerValuesCustomPolicy.get(1)).isEqualTo(EXPECTED_SIGNATURE_HEADER);
        assertThat(headerValuesCustomPolicy.get(2)).isEqualTo(EXPECTED_KEY_PAIR_ID_HEADER);
    }

    @Test
    void testEqualsAndHashCodeContract() {
        EqualsVerifier.forClass(DefaultCookiesForCannedPolicy.class).verify();
        EqualsVerifier.forClass(DefaultCookiesForCustomPolicy.class).verify();
    }

}
