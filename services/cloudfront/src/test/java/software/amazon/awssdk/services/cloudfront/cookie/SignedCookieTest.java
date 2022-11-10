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

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
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
    private static final String RESOURCE_URL = "URL";

    @Test
    void cookiesForCannedPolicy_shouldWork() {
        CookiesForCannedPolicy cookies = DefaultCookiesForCannedPolicy.builder()
                                                                      .keyPairId(KEY_PAIR_ID_VALUE)
                                                                      .signature(SIGNATURE_VALUE)
                                                                      .resourceUrl(RESOURCE_URL)
                                                                      .expires(EXPIRES_VALUE).build();

        assertThat(cookies.keyPairIdValue()).isEqualTo(KEY_PAIR_ID_VALUE);
        assertThat(cookies.signatureValue()).isEqualTo(SIGNATURE_VALUE);
        assertThat(cookies.resourceUrl()).isEqualTo(RESOURCE_URL);
        assertThat(cookies.expiresValue()).isEqualTo(EXPIRES_VALUE);
        assertThat(cookies.keyPairIdKey()).isEqualTo(KEY_PAIR_ID_KEY);
        assertThat(cookies.signatureKey()).isEqualTo(SIGNATURE_KEY);
        assertThat(cookies.expiresKey()).isEqualTo(EXPIRES_KEY);
    }

    @Test
    void cookiesForCustomPolicy_shouldWork() {
        CookiesForCustomPolicy cookies = DefaultCookiesForCustomPolicy.builder()
                                                                      .keyPairId(KEY_PAIR_ID_VALUE)
                                                                      .signature(SIGNATURE_VALUE)
                                                                      .resourceUrl(RESOURCE_URL)
                                                                      .policy(POLICY_VALUE).build();

        assertThat(cookies.keyPairIdValue()).isEqualTo(KEY_PAIR_ID_VALUE);
        assertThat(cookies.signatureValue()).isEqualTo(SIGNATURE_VALUE);
        assertThat(cookies.resourceUrl()).isEqualTo(RESOURCE_URL);
        assertThat(cookies.policyValue()).isEqualTo(POLICY_VALUE);
        assertThat(cookies.keyPairIdKey()).isEqualTo(KEY_PAIR_ID_KEY);
        assertThat(cookies.signatureKey()).isEqualTo(SIGNATURE_KEY);
        assertThat(cookies.policyKey()).isEqualTo(POLICY_KEY);
    }

    @Test
    void cookieHeaderValue_shouldWork() {
        CookiesForCannedPolicy cookiesForCannedPolicy = DefaultCookiesForCannedPolicy.builder()
                                                                      .keyPairId(KEY_PAIR_ID_VALUE)
                                                                      .signature(SIGNATURE_VALUE)
                                                                      .resourceUrl(RESOURCE_URL)
                                                                      .expires(EXPIRES_VALUE).build();
        String expectedKeyPairIdHeader = KEY_PAIR_ID_KEY + "=" + KEY_PAIR_ID_VALUE;
        String expectedSignatureHeader = SIGNATURE_KEY + "=" + SIGNATURE_VALUE;
        String expectedExpiresHeader = EXPIRES_KEY + "=" + EXPIRES_VALUE;

        assertThat(cookiesForCannedPolicy.cookieHeaderValue(SignedCookie.CookieType.KEY_PAIR_ID)).isEqualTo(expectedKeyPairIdHeader);
        assertThat(cookiesForCannedPolicy.cookieHeaderValue(SignedCookie.CookieType.SIGNATURE)).isEqualTo(expectedSignatureHeader);
        assertThat(cookiesForCannedPolicy.cookieHeaderValue(SignedCookie.CookieType.EXPIRES)).isEqualTo(expectedExpiresHeader);

        CookiesForCustomPolicy cookiesForCustomPolicy = DefaultCookiesForCustomPolicy.builder()
                                                                      .keyPairId(KEY_PAIR_ID_VALUE)
                                                                      .signature(SIGNATURE_VALUE)
                                                                      .resourceUrl(RESOURCE_URL)
                                                                      .policy(POLICY_VALUE).build();
        String expectedPolicyHeader = POLICY_KEY + "=" + POLICY_VALUE;
        assertThat(cookiesForCustomPolicy.cookieHeaderValue(SignedCookie.CookieType.KEY_PAIR_ID)).isEqualTo(expectedKeyPairIdHeader);
        assertThat(cookiesForCustomPolicy.cookieHeaderValue(SignedCookie.CookieType.SIGNATURE)).isEqualTo(expectedSignatureHeader);
        assertThat(cookiesForCustomPolicy.cookieHeaderValue(SignedCookie.CookieType.POLICY)).isEqualTo(expectedPolicyHeader);
    }

    @Test
    void testEqualsAndHashCodeContract() {
        EqualsVerifier.forClass(DefaultCookiesForCannedPolicy.class).verify();
        EqualsVerifier.forClass(DefaultCookiesForCustomPolicy.class).verify();
    }

}
