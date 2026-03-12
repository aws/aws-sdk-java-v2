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

/**
 * Validates that the signing certificate URL is valid.
 */
@SdkInternalApi
public class CertificateUrlValidator {
    private final String expectedCommonName;

    public CertificateUrlValidator(String expectedCommonName) {
        this.expectedCommonName = expectedCommonName;
    }

    public void validate(URI certificateUrl) {
        if (certificateUrl == null) {
            throw SdkClientException.create("Certificate URL cannot be null");
        }

        if (!"https".equals(certificateUrl.getScheme())) {
            throw SdkClientException.create("Certificate URL must use HTTPS");
        }

        if (!expectedCommonName.equals(certificateUrl.getHost())) {
            throw SdkClientException.create("Certificate URL does not match expected host: " + expectedCommonName);
        }
    }
}
