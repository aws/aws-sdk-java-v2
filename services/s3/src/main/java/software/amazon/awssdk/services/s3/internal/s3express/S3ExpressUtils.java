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

import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.spi.identity.AuthSchemeOptionsResolver;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.services.s3.endpoints.internal.KnownS3ExpressEndpointProperty;
import software.amazon.awssdk.services.s3.s3express.S3ExpressAuthScheme;

@SdkInternalApi
public final class S3ExpressUtils {

    public static final String S3_EXPRESS = "S3Express";
    private static final String S3_EXPRESS_BUCKET_SUFFIX = "--x-s3";

    private S3ExpressUtils() {
    }

    /**
     * Determines if this request targets an S3Express bucket by checking the bucket name suffix.
     */
    public static boolean isS3ExpressBucket(SdkRequest request) {
        return request.getValueForField("Bucket", String.class)
                      .map(b -> b.endsWith(S3_EXPRESS_BUCKET_SUFFIX))
                      .orElse(false);
    }

    /**
     * Determines if this request uses S3Express auth by checking the auth scheme options.
     */
    public static boolean isS3ExpressAuthRequest(SdkRequest request, ExecutionAttributes executionAttributes) {
        AuthSchemeOptionsResolver resolver =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.AUTH_SCHEME_OPTIONS_RESOLVER);
        if (resolver != null) {
            List<AuthSchemeOption> options = resolver.resolve(request);
            return options.stream().anyMatch(o -> S3ExpressAuthScheme.SCHEME_ID.equals(o.schemeId()));
        }
        return false;
    }

    /**
     * Whether aws.auth#sigv4-s3express is the selected auth scheme.
     */
    private static boolean useS3ExpressAuthScheme(ExecutionAttributes executionAttributes) {
        SelectedAuthScheme<?> selectedAuthScheme = executionAttributes.getAttribute(SELECTED_AUTH_SCHEME);
        if (selectedAuthScheme != null) {
            AuthSchemeOption authSchemeOption = selectedAuthScheme.authSchemeOption();
            return S3ExpressAuthScheme.SCHEME_ID.equals(authSchemeOption.schemeId());
        }
        return false;
    }

    /**
     * Adds S3 Express business metric if applicable for the current operation.
     */
    public static void addS3ExpressBusinessMetricIfApplicable(Endpoint endpoint, ExecutionAttributes executionAttributes) {
        if (endpoint != null && executionAttributes != null
            && S3_EXPRESS.equals(endpoint.attribute(KnownS3ExpressEndpointProperty.BACKEND))
            && useS3ExpressAuthScheme(executionAttributes)) {
            executionAttributes.getOptionalAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS)
                               .ifPresent(businessMetrics ->
                                              businessMetrics.addMetric(BusinessMetricFeatureId.S3_EXPRESS_BUCKET.value()));
        }
    }
}
