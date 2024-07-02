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

package software.amazon.awssdk.services.s3.internal.s3express;

import static software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.services.s3.endpoints.internal.KnownS3ExpressEndpointProperty;
import software.amazon.awssdk.services.s3.s3express.S3ExpressAuthScheme;

@SdkInternalApi
public final class S3ExpressUtils {

    public static final String S3_EXPRESS = "S3Express";

    private S3ExpressUtils() {
    }

    /**
     * Returns true if the resolved endpoint contains S3Express, else false.
     */
    public static boolean useS3Express(ExecutionAttributes executionAttributes) {
        Endpoint endpoint = executionAttributes.getAttribute(SdkInternalExecutionAttribute.RESOLVED_ENDPOINT);
        if (endpoint != null) {
            String useS3Express = endpoint.attribute(KnownS3ExpressEndpointProperty.BACKEND);
            return S3_EXPRESS.equals(useS3Express);
        }
        return false;
    }

    /**
     * Whether aws.auth#sigv4-s3express is used or not
     */
    public static boolean useS3ExpressAuthScheme(ExecutionAttributes executionAttributes) {
        SelectedAuthScheme<?> selectedAuthScheme = executionAttributes.getAttribute(SELECTED_AUTH_SCHEME);
        if (selectedAuthScheme != null) {
            AuthSchemeOption authSchemeOption = selectedAuthScheme.authSchemeOption();
            return S3ExpressAuthScheme.SCHEME_ID.equals(authSchemeOption.schemeId());
        }
        return false;
    }
}
