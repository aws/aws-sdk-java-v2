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

package software.amazon.awssdk.messagemanager.sns.internal;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.core.exception.SdkClientException;

public class CertificateUrlValidatorTest {
    private static final String CERT_HOST = "my-test-service.amazonaws.com";

    @Test
    void validate_urlValid_succeeds() {
        CertificateUrlValidator validator = new CertificateUrlValidator(CERT_HOST);
        assertThatNoException().isThrownBy(() -> validator.validate(URI.create("https://" + CERT_HOST
                                                                                    + "/SimpleNotificationService-abc123.pem")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://" + CERT_HOST + ":443/SimpleNotificationService-abc123.pem",
        "https://" + CERT_HOST + "/cert.pem",
        "https://" + CERT_HOST + "/cert.pem?x=1",
        "https://" + CERT_HOST + "/a/b/cert.pem",
        "https://" + CERT_HOST + "/cert"
    })
    void validate_otherValidUrls_succeeds(String url) {
        CertificateUrlValidator validator = new CertificateUrlValidator(CERT_HOST);
        assertThatNoException().isThrownBy(() -> validator.validate(URI.create(url)));
    }

    @Test
    void validate_urlHasFragment_throws() {
        assertInvalid("https://" + CERT_HOST + "/cert.pem#x", "Certificate URL must not contain a fragment");
    }

    @Test
    void validate_urlHasUserInfo_throws() {
        assertInvalid("https://***@" + CERT_HOST + "/cert.pem", "Certificate URL must not contain user info");
    }

    @Test
    void validate_urlHasNonDefaultPort_throws() {
        assertInvalid("https://" + CERT_HOST + ":8443/cert.pem", "Certificate URL must use the default HTTPS port");
    }

    private static void assertInvalid(String url, String message) {
        CertificateUrlValidator validator = new CertificateUrlValidator(CERT_HOST);
        assertThatThrownBy(() -> validator.validate(URI.create(url)))
            .isInstanceOf(SdkClientException.class)
            .hasMessage(message);
    }

    @Test
    void validate_urlNull_throws() {
        CertificateUrlValidator validator = new CertificateUrlValidator(CERT_HOST);
        assertThatThrownBy(() -> validator.validate(null))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Certificate URL cannot be null");
    }

    @Test
    void validate_schemeNotHttps_throws() {
        CertificateUrlValidator validator = new CertificateUrlValidator(CERT_HOST);
        assertThatThrownBy(() -> validator.validate(URI.create("http://" + CERT_HOST)))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Certificate URL must use HTTPS");
    }

    @Test
    void validate_urlHostDoesNotMatchExpectedHost_throws() {
        CertificateUrlValidator validator = new CertificateUrlValidator(CERT_HOST);
        assertThatThrownBy(() -> validator.validate(URI.create("https://my-other-test-service.amazonaws.com")))
            .isInstanceOf(SdkClientException.class)
            .hasMessage("Certificate URL does not match expected host: " + CERT_HOST);
    }

}
