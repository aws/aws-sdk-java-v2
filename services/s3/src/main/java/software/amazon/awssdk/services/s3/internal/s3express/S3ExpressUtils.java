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

    /**
     * Adds S3 Express business metric if applicable for the current operation.
     */
    public static void addS3ExpressBusinessMetricIfApplicable(ExecutionAttributes executionAttributes) {
        if (executionAttributes != null && useS3Express(executionAttributes) && useS3ExpressAuthScheme(executionAttributes)) {
            executionAttributes.getOptionalAttribute(SdkInternalExecutionAttribute.BUSINESS_METRICS)
                               .ifPresent(businessMetrics ->
                                              businessMetrics.addMetric(BusinessMetricFeatureId.S3_EXPRESS_BUCKET.value()));
        }
    }

    /**
     * Adds S3 Express business metric using the provided endpoint directly. Use this overload when
     * {@code RESOLVED_ENDPOINT} is not yet set in execution attributes (e.g., inside the endpoint resolution callback).
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

    /**
     * Determines if this request targets an S3Express bucket by checking the bucket name suffix.
     * This is safe to call from interceptors at any point — it does not depend on auth scheme resolution
     * or endpoint resolution.
     */
    public static boolean isS3ExpressBucket(SdkRequest request) {
        return request.getValueForField("Bucket", String.class)
                      .map(b -> b.endsWith("--x-s3"))
                      .orElse(false);
    }

    /**
     * Determines if this request uses S3Express auth by checking the auth scheme options resolved for the request.
     * Safe to call from interceptors before pipeline stages have set {@code SELECTED_AUTH_SCHEME} or
     * {@code RESOLVED_ENDPOINT}.
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
}
