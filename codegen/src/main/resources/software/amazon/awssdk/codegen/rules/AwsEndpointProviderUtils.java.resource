import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.net.URI;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.aws.AwsV4aHttpSigner;
import software.amazon.awssdk.http.auth.spi.AuthSchemeOption;
import software.amazon.awssdk.identity.spi.Identity;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.HostnameValidator;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

@SdkInternalApi
public final class AwsEndpointProviderUtils {
    private static final Logger LOG = Logger.loggerFor(AwsEndpointProviderUtils.class);

    private AwsEndpointProviderUtils() {
    }

    public static Region regionBuiltIn(ExecutionAttributes executionAttributes) {
        return executionAttributes.getAttribute(AwsExecutionAttribute.AWS_REGION);
    }

    public static Boolean dualStackEnabledBuiltIn(ExecutionAttributes executionAttributes) {
        return executionAttributes.getAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED);
    }

    public static Boolean fipsEnabledBuiltIn(ExecutionAttributes executionAttributes) {
        return executionAttributes.getAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED);
    }

    /**
     * Returns the endpoint set on the client. Note that this strips off the query part of the URI because the endpoint
     * rules library, e.g. {@code ParseURL} will return an exception if the URI it parses has query parameters.
     */
    public static String endpointBuiltIn(ExecutionAttributes executionAttributes) {
        if (endpointIsOverridden(executionAttributes)) {
            return invokeSafely(() -> {
                URI endpointOverride = executionAttributes.getAttribute(SdkExecutionAttribute.CLIENT_ENDPOINT);
                return new URI(endpointOverride.getScheme(), null, endpointOverride.getHost(), endpointOverride.getPort(),
                        endpointOverride.getPath(), null, endpointOverride.getFragment()).toString();
            });
        }
        return null;
    }

    public static Boolean useGlobalEndpointBuiltIn(ExecutionAttributes executionAttributes) {
        return executionAttributes.getAttribute(AwsExecutionAttribute.USE_GLOBAL_ENDPOINT);
    }

    /**
     * True if the the {@link SdkExecutionAttribute#ENDPOINT_OVERRIDDEN} attribute is present and its value is
     * {@code true}, {@code false} otherwise.
     */
    public static boolean endpointIsOverridden(ExecutionAttributes attrs) {
        return attrs.getOptionalAttribute(SdkExecutionAttribute.ENDPOINT_OVERRIDDEN).orElse(false);
    }

    /**
     * True if the the {@link SdkInternalExecutionAttribute#IS_DISCOVERED_ENDPOINT} attribute is present and its value
     * is {@code true}, {@code false} otherwise.
     */
    public static boolean endpointIsDiscovered(ExecutionAttributes attrs) {
        return attrs.getOptionalAttribute(SdkInternalExecutionAttribute.IS_DISCOVERED_ENDPOINT).orElse(false);
    }

    /**
     * True if the the {@link SdkInternalExecutionAttribute#DISABLE_HOST_PREFIX_INJECTION} attribute is present and its
     * value is {@code true}, {@code false} otherwise.
     */
    public static boolean disableHostPrefixInjection(ExecutionAttributes attrs) {
        return attrs.getOptionalAttribute(SdkInternalExecutionAttribute.DISABLE_HOST_PREFIX_INJECTION).orElse(false);
    }

    /**
     * Apply the given endpoint prefix to the endpoint.
     */
    public static Endpoint addHostPrefix(Endpoint endpoint, String prefix) {
        if (StringUtils.isBlank(prefix)) {
            return endpoint;
        }

        validatePrefixIsHostNameCompliant(prefix);

        URI originalUrl = endpoint.url();
        String newHost = prefix + endpoint.url().getHost();
        URI newUrl = invokeSafely(() -> new URI(originalUrl.getScheme(), null, newHost, originalUrl.getPort(),
                originalUrl.getPath(), originalUrl.getQuery(), originalUrl.getFragment()));

        return endpoint.toBuilder().url(newUrl).build();
    }

    public static Endpoint valueAsEndpointOrThrow(Value value) {
        if (value instanceof Value.Endpoint) {
            Value.Endpoint endpoint = value.expectEndpoint();
            Endpoint.Builder builder = Endpoint.builder();
            builder.url(URI.create(endpoint.getUrl()));

            Map<String, List<String>> headers = endpoint.getHeaders();
            if (headers != null) {
                headers.forEach((name, values) -> values.forEach(v -> builder.putHeader(name, v)));
            }

            addKnownProperties(builder, endpoint.getProperties());

            return builder.build();
        } else if (value instanceof Value.Str) {
            String errorMsg = value.expectString();
            throw SdkClientException.create(errorMsg);
        } else {
            throw SdkClientException.create("Rule engine return neither an endpoint result or error value. Returned value was:"
                    + value);
        }
    }

    /**
     * This sets the request URI to the resolved URI returned by the endpoint provider. There are some things to be
     * careful about to make this work properly:
     * <p>
     * If the client endpoint is an endpoint override, it may contain a path. In addition, the request marshaller itself
     * may add components to the path if it's modeled for the operation. Unfortunately,
     * {@link SdkHttpRequest#encodedPath()} returns the combined path from both the endpoint and the request. There is
     * no way to know, just from the HTTP request object, where the override path ends (if it's even there) and where
     * the request path starts. Additionally, the rule itself may also append other parts to the endpoint override path.
     * <p>
     * To solve this issue, we pass in the endpoint set on the path, which allows us to the strip the path from the
     * endpoint override from the request path, and then correctly combine the paths.
     * <p>
     * For example, let's suppose the endpoint override on the client is {@code https://example.com/a}. Then we call an
     * operation {@code Foo()}, that marshalls {@code /c} to the path. The resulting request path is {@code /a/c}.
     * However, we also pass the endpoint to provider as a parameter, and the resolver returns
     * {@code https://example.com/a/b}. This method takes care of combining the paths correctly so that the resulting
     * path is {@code https://example.com/a/b/c}.
     */
    public static SdkHttpRequest setUri(SdkHttpRequest request, URI clientEndpoint, URI resolvedUri) {
        // [client endpoint path]
        String clientEndpointPath = clientEndpoint.getRawPath();

        // [client endpoint path]/[request path]
        String requestPath = request.encodedPath();

        // [client endpoint path]/[additional path added by resolver]
        String resolvedUriPath = resolvedUri.getRawPath();

        String finalPath = requestPath;

        // If there is an additional path added by resolver, i.e., [additional path added by resolver] not null,
        // we need to combine the path
        if (!resolvedUriPath.equals(clientEndpointPath)) {
            finalPath = combinePath(clientEndpointPath, requestPath, resolvedUriPath);
        }

        return request.toBuilder().protocol(resolvedUri.getScheme()).host(resolvedUri.getHost()).port(resolvedUri.getPort())
                .encodedPath(finalPath).build();
    }

    /**
     * Our goal is to construct [client endpoint path]/[additional path added by resolver]/[request path], so we just
     * need to strip the client endpoint path from the marshalled request path to isolate just the part added by the
     * marshaller. Trailing slash is removed from client endpoint path before stripping because it could cause the
     * leading slash to be removed from the request path: e.g., StringUtils.replaceOnce("/", "//test", "") generates
     * "/test" and the expected result is "//test"
     */
    private static String combinePath(String clientEndpointPath, String requestPath, String resolvedUriPath) {
        String requestPathWithClientPathRemoved = StringUtils.replaceOnce(requestPath, clientEndpointPath, "");
        String finalPath = SdkHttpUtils.appendUri(resolvedUriPath, requestPathWithClientPathRemoved);
        return finalPath;
    }

    /**
     * Merge the resolved endpoint auth scheme settings into the selected auth scheme. Endpoint auth schemes settings
     * take priority over settings already set in the selected auth scheme.
     */
    public static <T extends Identity> SelectedAuthScheme<T> mergeIntoResolvedAuthScheme(
            List<EndpointAuthScheme> endpointAuthSchemes, SelectedAuthScheme<T> selectedAuthScheme) {
        for (EndpointAuthScheme endpointAuthScheme : endpointAuthSchemes) {
            if (!endpointAuthScheme.schemeId().equals(selectedAuthScheme.authSchemeOption().schemeId())) {
                // Don't include signer properties for auth options that don't match our selected auth scheme
                continue;
            }

            if (endpointAuthScheme instanceof SigV4AuthScheme) {
                SigV4AuthScheme v4AuthScheme = (SigV4AuthScheme) endpointAuthScheme;
                AuthSchemeOption authSchemeOption = selectedAuthScheme.authSchemeOption().toBuilder()
                        .applyMutation(o -> overrideV4SignerProperties(o, v4AuthScheme)).build();
                return new SelectedAuthScheme<>(selectedAuthScheme.identity(), selectedAuthScheme.signer(), authSchemeOption);
            }

            if (endpointAuthScheme instanceof SigV4aAuthScheme) {
                SigV4aAuthScheme v4aAuthScheme = (SigV4aAuthScheme) endpointAuthScheme;
                AuthSchemeOption authSchemeOption = selectedAuthScheme.authSchemeOption().toBuilder()
                        .applyMutation(o -> overrideV4aSignerProperties(o, v4aAuthScheme)).build();
                return new SelectedAuthScheme<>(selectedAuthScheme.identity(), selectedAuthScheme.signer(), authSchemeOption);
            }
        }

        return selectedAuthScheme;
    }

    private static void overrideV4SignerProperties(AuthSchemeOption.Builder option, SigV4AuthScheme endpointAuthScheme) {
        if (endpointAuthScheme.isDisableDoubleEncodingSet()) {
            option.putSignerProperty(AwsV4HttpSigner.DOUBLE_URL_ENCODE, !endpointAuthScheme.disableDoubleEncoding());
        }
        if (endpointAuthScheme.signingRegion() != null) {
            option.putSignerProperty(AwsV4HttpSigner.REGION_NAME, endpointAuthScheme.signingRegion());
        }
        if (endpointAuthScheme.signingName() != null) {
            option.putSignerProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, endpointAuthScheme.signingName());
        }
    }

    private static void overrideV4aSignerProperties(AuthSchemeOption.Builder option, SigV4aAuthScheme endpointAuthScheme) {
        if (endpointAuthScheme.isDisableDoubleEncodingSet()) {
            option.putSignerProperty(AwsV4aHttpSigner.DOUBLE_URL_ENCODE, !endpointAuthScheme.disableDoubleEncoding());
        }
        if (endpointAuthScheme.signingRegionSet() != null) {
            option.putSignerProperty(AwsV4aHttpSigner.REGION_NAME, String.join(",", endpointAuthScheme.signingRegionSet()));
        }
        if (endpointAuthScheme.signingName() != null) {
            option.putSignerProperty(AwsV4aHttpSigner.SERVICE_SIGNING_NAME, endpointAuthScheme.signingName());
        }
    }

    private static void addKnownProperties(Endpoint.Builder builder, Map<String, Value> properties) {
        properties.forEach((n, v) -> {
            switch (n) {
            case "authSchemes":
                builder.putAttribute(AwsEndpointAttribute.AUTH_SCHEMES, AuthSchemeUtils.createAuthSchemes(v));
                break;
            default:
                LOG.debug(() -> "Ignoring unknown endpoint property: " + n);
                break;
            }
        });
    }

    private static void validatePrefixIsHostNameCompliant(String prefix) {
        String[] components = splitHostLabelOnDots(prefix);
        for (String component : components) {
            HostnameValidator.validateHostnameCompliant(component, component, "request");
        }
    }

    private static String[] splitHostLabelOnDots(String label) {
        return label.split("\\.");
    }
}
