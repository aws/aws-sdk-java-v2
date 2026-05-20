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
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.spi.identity.AuthSchemeOptionsResolver;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointProvider;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.endpoints.internal.KnownS3ExpressEndpointProperty;
import software.amazon.awssdk.services.s3.endpoints.internal.S3EndpointResolverUtils;
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
     * Resolves the signing service name from the auth scheme options. This is needed because the endpoint rules may
     * specify a different signing name (e.g., "s3express" for S3Express control plane APIs) than the client-level
     * default ("s3"). Returns the fallback value if the signing name cannot be resolved from auth scheme options.
     */
    public static String resolveSigningName(SdkRequest request, ExecutionAttributes executionAttributes, String fallback) {
        Endpoint endpoint = resolveEndpoint(request, executionAttributes);
        if (endpoint != null) {
            List<EndpointAuthScheme> authSchemes = endpoint.attribute(AwsEndpointAttribute.AUTH_SCHEMES);
            if (authSchemes != null && !authSchemes.isEmpty()) {
                EndpointAuthScheme authScheme = authSchemes.get(0);
                if (authScheme instanceof SigV4AuthScheme) {
                    return ((SigV4AuthScheme) authScheme).signingName();
                }
            }
        }
        return fallback;
    }

    /**
     * Resolves the signing region from the endpoint resolution. This is needed because the endpoint rules may
     * specify a different region (e.g., for cross-region access) than the client-level default.
     * Returns the fallback value if the region cannot be resolved.
     */
    public static Region resolveSigningRegion(SdkRequest request, ExecutionAttributes executionAttributes, Region fallback) {
        Endpoint endpoint = resolveEndpoint(request, executionAttributes);
        if (endpoint != null) {
            List<EndpointAuthScheme> authSchemes = endpoint.attribute(AwsEndpointAttribute.AUTH_SCHEMES);
            if (authSchemes != null && !authSchemes.isEmpty()) {
                EndpointAuthScheme authScheme = authSchemes.get(0);
                if (authScheme instanceof SigV4AuthScheme) {
                    String region = ((SigV4AuthScheme) authScheme).signingRegion();
                    if (region != null) {
                        return Region.of(region);
                    }
                }
            }
        }
        return fallback;
    }

    private static Endpoint resolveEndpoint(SdkRequest request, ExecutionAttributes executionAttributes) {
        EndpointProvider endpointProvider =
            executionAttributes.getAttribute(SdkInternalExecutionAttribute.ENDPOINT_PROVIDER);
        if (endpointProvider instanceof S3EndpointProvider) {
            try {
                S3EndpointParams endpointParams = S3EndpointResolverUtils.ruleParams(request, executionAttributes);
                return ((S3EndpointProvider) endpointProvider).resolveEndpoint(endpointParams).join();
            } catch (Exception e) {
                // If resolution fails, fall back to defaults
            }
        }
        return null;
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
