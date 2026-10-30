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

import java.net.URI;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.Validate;

/**
 * Validates that a URL carried in an SNS message points at the expected regional SNS endpoint.
 * <p>
 * A valid URL uses HTTPS, the expected host and the default HTTPS port, and has no user info or fragment. The path and
 * query are not constrained, because URLs such as {@code UnsubscribeURL} and {@code SubscribeURL} carry a query string.
 */
@SdkInternalApi
public final class SnsEndpointUrlValidator {
    private static final int HTTPS_PORT = 443;

    private final String expectedHost;

    public SnsEndpointUrlValidator(String expectedHost) {
        Validate.notBlank(expectedHost, "Expected host cannot be null or empty");
        this.expectedHost = expectedHost;
    }

    /**
     * Validates the URL, throwing {@link SdkClientException} when it does not point at the expected SNS endpoint.
     *
     * @param url the URL to validate
     * @param urlName the name of the URL used in error messages, for example {@code "Certificate URL"}
     */
    public void validate(URI url, String urlName) {
        if (url == null) {
            throw SdkClientException.create(urlName + " cannot be null");
        }

        if (!"https".equals(url.getScheme())) {
            throw SdkClientException.create(urlName + " must use HTTPS");
        }

        if (!expectedHost.equals(url.getHost())) {
            throw SdkClientException.create(urlName + " does not match expected host: " + expectedHost);
        }

        if (url.getRawUserInfo() != null) {
            throw SdkClientException.create(urlName + " must not contain user info");
        }

        int port = url.getPort();
        if (port != -1 && port != HTTPS_PORT) {
            throw SdkClientException.create(urlName + " must use the default HTTPS port");
        }

        if (url.getRawFragment() != null) {
            throw SdkClientException.create(urlName + " must not contain a fragment");
        }
    }
}
