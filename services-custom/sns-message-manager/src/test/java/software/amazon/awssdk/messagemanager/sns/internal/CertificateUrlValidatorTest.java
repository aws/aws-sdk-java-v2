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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;

public class CertificateUrlValidatorTest {
    private static final String CERT_HOST = "my-test-service.amazonaws.com";

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
