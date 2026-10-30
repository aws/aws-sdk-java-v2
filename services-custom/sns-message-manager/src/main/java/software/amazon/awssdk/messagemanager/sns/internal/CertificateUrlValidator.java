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
import software.amazon.awssdk.utils.Validate;

/**
 * Validates that the signing certificate URL is valid.
 * <p>
 * A valid URL uses HTTPS, the expected host and the default HTTPS port, and has no user info or fragment. The path and
 * query are not constrained, because SNS may use other forms for them in the future.
 */
@SdkInternalApi
public class CertificateUrlValidator {
    private static final String URL_NAME = "Certificate URL";

    private final SnsEndpointUrlValidator endpointUrlValidator;

    public CertificateUrlValidator(String certificateHost) {
        Validate.notBlank(certificateHost, "Expected certificate host cannot be null or empty");
        this.endpointUrlValidator = new SnsEndpointUrlValidator(certificateHost);
    }

    public void validate(URI certificateUrl) {
        endpointUrlValidator.validate(certificateUrl, URL_NAME);
    }
}
