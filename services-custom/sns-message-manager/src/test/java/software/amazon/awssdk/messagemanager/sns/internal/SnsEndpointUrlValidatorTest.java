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
import software.amazon.awssdk.core.exception.SdkClientException;

class SnsEndpointUrlValidatorTest {
    private static final String HOST = "sns.us-east-1.amazonaws.com";
    private static final String URL_NAME = "UnsubscribeURL";
    private static final SnsEndpointUrlValidator VALIDATOR = new SnsEndpointUrlValidator(HOST);

    @Test
    void validate_urlWithQuery_succeeds() {
        URI url = URI.create("https://" + HOST + "/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:us-east-1:123456789012:t:1");
        assertThatNoException().isThrownBy(() -> VALIDATOR.validate(url, URL_NAME));
    }

    @Test
    void validate_urlWithDefaultPort_succeeds() {
        URI url = URI.create("https://" + HOST + ":443/?Action=Unsubscribe");
        assertThatNoException().isThrownBy(() -> VALIDATOR.validate(url, URL_NAME));
    }

    @Test
    void validate_urlNull_throws() {
        assertInvalid(null, "UnsubscribeURL cannot be null");
    }

    @Test
    void validate_schemeNotHttps_throws() {
        assertInvalid(URI.create("http://" + HOST + "/?Action=Unsubscribe"), "UnsubscribeURL must use HTTPS");
    }

    @Test
    void validate_hostMismatch_throws() {
        assertInvalid(URI.create("https://other.example.com/?Action=Unsubscribe"),
                      "UnsubscribeURL does not match expected host: " + HOST);
    }

    @Test
    void validate_userInfo_throws() {
        assertInvalid(URI.create("https://user@" + HOST + "/?Action=Unsubscribe"),
                      "UnsubscribeURL must not contain user info");
    }

    @Test
    void validate_nonDefaultPort_throws() {
        assertInvalid(URI.create("https://" + HOST + ":8443/?Action=Unsubscribe"),
                      "UnsubscribeURL must use the default HTTPS port");
    }

    @Test
    void validate_fragment_throws() {
        assertInvalid(URI.create("https://" + HOST + "/?Action=Unsubscribe#frag"),
                      "UnsubscribeURL must not contain a fragment");
    }

    @Test
    void constructor_blankHost_throws() {
        assertThatThrownBy(() -> new SnsEndpointUrlValidator(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Expected host cannot be null or empty");
        assertThatThrownBy(() -> new SnsEndpointUrlValidator(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Expected host cannot be null or empty");
    }

    private static void assertInvalid(URI url, String message) {
        assertThatThrownBy(() -> VALIDATOR.validate(url, URL_NAME))
            .isInstanceOf(SdkClientException.class)
            .hasMessage(message);
    }
}
