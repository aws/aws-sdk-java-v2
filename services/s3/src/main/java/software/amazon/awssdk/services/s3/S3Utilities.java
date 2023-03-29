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

package software.amazon.awssdk.services.s3;


import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.awscore.endpoint.DefaultServiceEndpointBuilder;
import software.amazon.awssdk.awscore.endpoint.DualstackEnabledProvider;
import software.amazon.awssdk.awscore.endpoint.FipsEnabledProvider;
import software.amazon.awssdk.awscore.internal.defaultsmode.DefaultsModeConfiguration;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptorChain;
import software.amazon.awssdk.core.interceptor.InterceptorContext;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSupplier;
import software.amazon.awssdk.protocols.core.OperationInfo;
import software.amazon.awssdk.protocols.core.PathMarshaller;
import software.amazon.awssdk.protocols.core.ProtocolUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.ServiceMetadataAdvancedOption;
import software.amazon.awssdk.services.s3.endpoints.S3ClientContextParams;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider;
import software.amazon.awssdk.services.s3.endpoints.internal.S3RequestSetEndpointInterceptor;
import software.amazon.awssdk.services.s3.endpoints.internal.S3ResolveEndpointInterceptor;
import software.amazon.awssdk.services.s3.internal.endpoints.UseGlobalEndpointResolver;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.parsing.S3Uri;
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.Validate;

/**
 * Utilities for working with Amazon S3 objects. An instance of this class can be created by:
 * <p>
 * 1) Directly using the {@link #builder()} method. You have to manually specify the configuration params like region,
 * s3Configuration on the builder.
 *
 * <pre>
 * S3Utilities utilities = S3Utilities.builder().region(Region.US_WEST_2).build()
 * GetUrlRequest request = GetUrlRequest.builder().bucket("foo-bucket").key("key-without-spaces").build();
 * URL url = utilities.getUrl(request);
 * </pre>
 *
 * <p>
 * 2) Using the low-level client {@link S3Client#utilities()} method. This is recommended as SDK will use the same
 * configuration from the {@link S3Client} object to create the {@link S3Utilities} object.
 *
 * <pre>
 * S3Client s3client = S3Client.create();
 * S3Utilities utilities = s3client.utilities();
 * GetUrlRequest request = GetUrlRequest.builder().bucket("foo-bucket").key("key-without-spaces").build();
 * URL url = utilities.getUrl(request);
 * </pre>
 *
 * Note: This class does not make network calls.
 */
@Immutable
@SdkPublicApi
public final class S3Utilities {
    private static final String SERVICE_NAME = "s3";

    private final Region region;
    private final URI endpoint;
    private final S3Configuration s3Configuration;
    private final Supplier<ProfileFile> profileFile;
    private final String profileName;
    private final boolean fipsEnabled;
    private final ExecutionInterceptorChain interceptorChain;
    private final UseGlobalEndpointResolver useGlobalEndpointResolver;

    /**
     * SDK currently validates that region is present while constructing {@link S3Utilities} object.
     * This can be relaxed in the future when more methods are added that don't use region.
     */
    private S3Utilities(Builder builder) {
        this.region = Validate.paramNotNull(builder.region, "Region");
        this.endpoint = builder.endpoint;
        this.profileFile = Optional.ofNullable(builder.profileFile)
                                   .orElse(ProfileFile::defaultProfileFile);
        this.profileName = builder.profileName;

        if (builder.s3Configuration == null) {
            this.s3Configuration = S3Configuration.builder().dualstackEnabled(builder.dualstackEnabled).build();
        } else {
            this.s3Configuration = builder.s3Configuration.toBuilder()
                                                          .applyMutation(b -> resolveDualstackSetting(b, builder))
                                                          .build();
        }

        this.fipsEnabled = builder.fipsEnabled != null ? builder.fipsEnabled
                                                       : FipsEnabledProvider.builder()
                                                                            .profileFile(profileFile)
                                                                            .profileName(profileName)
                                                                            .build()
                                                                            .isFipsEnabled()
                                                                            .orElse(false);

        this.interceptorChain = createEndpointInterceptorChain();

        this.useGlobalEndpointResolver = createUseGlobalEndpointResolver();
    }

    private void resolveDualstackSetting(S3Configuration.Builder s3ConfigBuilder, Builder s3UtiltiesBuilder) {
        Validate.validState(s3ConfigBuilder.dualstackEnabled() == null || s3UtiltiesBuilder.dualstackEnabled == null,
                            "Only one of S3Configuration.Builder's dualstackEnabled or S3Utilities.Builder's dualstackEnabled "
                            + "should be set.");

        if (s3ConfigBuilder.dualstackEnabled() != null) {
            return;
        }

        if (s3UtiltiesBuilder.dualstackEnabled != null) {
            s3ConfigBuilder.dualstackEnabled(s3UtiltiesBuilder.dualstackEnabled);
            return;
        }

        s3ConfigBuilder.dualstackEnabled(DualstackEnabledProvider.builder()
                                                                 .profileFile(profileFile)
                                                                 .profileName(profileName)
                                                                 .build()
                                                                 .isDualstackEnabled()
                                                                 .orElse(false));
    }

    /**
     * Creates a builder for {@link S3Utilities}.
     */
    public static Builder builder() {
        return new Builder();
    }

    // Used by low-level client
    @SdkInternalApi
    static S3Utilities create(SdkClientConfiguration clientConfiguration) {
        S3Utilities.Builder builder = builder()
                          .region(clientConfiguration.option(AwsClientOption.AWS_REGION))
                          .s3Configuration((S3Configuration) clientConfiguration.option(SdkClientOption.SERVICE_CONFIGURATION))
                          .profileFile(clientConfiguration.option(SdkClientOption.PROFILE_FILE_SUPPLIER))
                          .profileName(clientConfiguration.option(SdkClientOption.PROFILE_NAME));

        if (Boolean.TRUE.equals(clientConfiguration.option(SdkClientOption.ENDPOINT_OVERRIDDEN))) {
            builder.endpoint(clientConfiguration.option(SdkClientOption.ENDPOINT));
        }

        return builder.build();
    }

    /**
     * Returns the URL for an object stored in Amazon S3.
     *
     * If the object identified by the given bucket and key has public read permissions,
     * then this URL can be directly accessed to retrieve the object's data.
     *
     * <p>
     *     If same configuration options are set on both #GetUrlRequest and #S3Utilities objects (for example: region),
     *     the configuration set on the #GetUrlRequest takes precedence.
     * </p>
     *
     * <p>
     *     This is a convenience which creates an instance of the {@link GetUrlRequest.Builder} avoiding the need to
     *     create one manually via {@link GetUrlRequest#builder()}
     * </p>
     *
     * @param getUrlRequest A {@link Consumer} that will call methods on {@link GetUrlRequest.Builder} to create a request.
     * @return A URL for an object stored in Amazon S3.
     * @throws SdkException Generated Url is malformed
     */
    public URL getUrl(Consumer<GetUrlRequest.Builder> getUrlRequest) {
        return getUrl(GetUrlRequest.builder().applyMutation(getUrlRequest).build());
    }

    /**
     * Returns the URL for an object stored in Amazon S3.
     *
     * If the object identified by the given bucket and key has public read permissions,
     * then this URL can be directly accessed to retrieve the object's data.
     *
     * <p>
     *     If same configuration options are set on both #GetUrlRequest and #S3Utilities objects (for example: region),
     *     the configuration set on the #GetUrlRequest takes precedence.
     * </p>
     *
     * @param getUrlRequest request to construct url
     * @return A URL for an object stored in Amazon S3.
     * @throws SdkException Generated Url is malformed
     */
    public URL getUrl(GetUrlRequest getUrlRequest) {
        Region resolvedRegion = resolveRegionForGetUrl(getUrlRequest);
        URI endpointOverride = getEndpointOverride(getUrlRequest);
        URI resolvedEndpoint = resolveEndpoint(endpointOverride, resolvedRegion);

        SdkHttpFullRequest marshalledRequest = createMarshalledRequest(getUrlRequest, resolvedEndpoint);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                            .bucket(getUrlRequest.bucket())
                                                            .key(getUrlRequest.key())
                                                            .versionId(getUrlRequest.versionId())
                                                            .build();

        InterceptorContext interceptorContext = InterceptorContext.builder()
                                                                  .httpRequest(marshalledRequest)
                                                                  .request(getObjectRequest)
                                                                  .build();

        ExecutionAttributes executionAttributes = createExecutionAttributes(resolvedEndpoint,
                                                                            resolvedRegion,
                                                                            endpointOverride != null);

        SdkHttpRequest modifiedRequest = runInterceptors(interceptorContext, executionAttributes).httpRequest();
        try {
            return modifiedRequest.getUri().toURL();
        } catch (MalformedURLException exception) {
            throw SdkException.create("Generated URI is malformed: " + modifiedRequest.getUri(),
                                      exception);
        }
    }

    private Region resolveRegionForGetUrl(GetUrlRequest getUrlRequest) {
        if (getUrlRequest.region() == null && this.region == null) {
            throw new IllegalArgumentException("Region should be provided either in GetUrlRequest object or S3Utilities object");
        }

        return getUrlRequest.region() != null ? getUrlRequest.region() : this.region;
    }

    /**
     * If endpoint is not present, construct a default endpoint using the region information.
     */
    private URI resolveEndpoint(URI overrideEndpoint, Region region) {
        return overrideEndpoint != null
               ? overrideEndpoint
               : new DefaultServiceEndpointBuilder("s3", "https").withRegion(region)
                                                                 .withProfileFile(profileFile)
                                                                 .withProfileName(profileName)
                                                                 .withDualstackEnabled(s3Configuration.dualstackEnabled())
                                                                 .withFipsEnabled(fipsEnabled)
                                                                 .getServiceEndpoint();
    }

    private URI getEndpointOverride(GetUrlRequest request) {
        URI requestOverrideEndpoint = request.endpoint();
        return requestOverrideEndpoint != null ? requestOverrideEndpoint : endpoint;
    }

    /**
     * Create a {@link SdkHttpFullRequest} object with the bucket and key values marshalled into the path params.
     */
    private SdkHttpFullRequest createMarshalledRequest(GetUrlRequest getUrlRequest, URI endpoint) {
        OperationInfo operationInfo = OperationInfo.builder()
                                                   .requestUri("/{Key+}")
                                                   .httpMethod(SdkHttpMethod.HEAD)
                                                   .build();

        SdkHttpFullRequest.Builder builder = ProtocolUtils.createSdkHttpRequest(operationInfo, endpoint);

        // encode bucket
        builder.encodedPath(PathMarshaller.NON_GREEDY.marshall(builder.encodedPath(),
                                                               "Bucket",
                                                               getUrlRequest.bucket()));

        // encode key
        builder.encodedPath(PathMarshaller.GREEDY.marshall(builder.encodedPath(), "Key", getUrlRequest.key()));

        if (getUrlRequest.versionId() != null) {
            builder.appendRawQueryParameter("versionId", getUrlRequest.versionId());
        }

        return builder.build();
    }

    /**
     * Create the execution attributes to provide to the endpoint interceptors.
     * @return
     */
    private ExecutionAttributes createExecutionAttributes(URI clientEndpoint, Region region, boolean isEndpointOverridden) {
        ExecutionAttributes executionAttributes = new ExecutionAttributes()
            .putAttribute(AwsExecutionAttribute.AWS_REGION, region)
            .putAttribute(SdkExecutionAttribute.CLIENT_TYPE, ClientType.SYNC)
            .putAttribute(SdkExecutionAttribute.SERVICE_NAME, SERVICE_NAME)
            .putAttribute(SdkExecutionAttribute.OPERATION_NAME, "GetObject")
            .putAttribute(SdkExecutionAttribute.SERVICE_CONFIG, s3Configuration)
            .putAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED, fipsEnabled)
            .putAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED, s3Configuration.dualstackEnabled())
            .putAttribute(SdkInternalExecutionAttribute.ENDPOINT_PROVIDER, S3EndpointProvider.defaultProvider())
            .putAttribute(SdkInternalExecutionAttribute.CLIENT_CONTEXT_PARAMS, createClientContextParams())
            .putAttribute(SdkExecutionAttribute.CLIENT_ENDPOINT, clientEndpoint)
            .putAttribute(AwsExecutionAttribute.USE_GLOBAL_ENDPOINT, useGlobalEndpointResolver.resolve(region));

        if (isEndpointOverridden) {
            executionAttributes.putAttribute(SdkExecutionAttribute.ENDPOINT_OVERRIDDEN, true);
        }

        return executionAttributes;
    }

    private AttributeMap createClientContextParams() {
        AttributeMap.Builder params = AttributeMap.builder();

        params.put(S3ClientContextParams.USE_ARN_REGION, s3Configuration.useArnRegionEnabled());
        params.put(S3ClientContextParams.DISABLE_MULTI_REGION_ACCESS_POINTS,
                   !s3Configuration.multiRegionEnabled());
        params.put(S3ClientContextParams.FORCE_PATH_STYLE, s3Configuration.pathStyleAccessEnabled());
        params.put(S3ClientContextParams.ACCELERATE, s3Configuration.accelerateModeEnabled());

        return params.build();
    }

    private InterceptorContext runInterceptors(InterceptorContext context, ExecutionAttributes executionAttributes) {
        context = interceptorChain.modifyRequest(context, executionAttributes);
        return interceptorChain.modifyHttpRequestAndHttpContent(context, executionAttributes);
    }

    private ExecutionInterceptorChain createEndpointInterceptorChain() {
        List<ExecutionInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new S3ResolveEndpointInterceptor());
        interceptors.add(new S3RequestSetEndpointInterceptor());
        return new ExecutionInterceptorChain(interceptors);
    }

    private UseGlobalEndpointResolver createUseGlobalEndpointResolver() {
        String standardOption =
            DefaultsModeConfiguration.defaultConfig(DefaultsMode.LEGACY)
                                     .get(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT);

        SdkClientConfiguration config =
            SdkClientConfiguration.builder()
                                  .option(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT, standardOption)
                                  .option(SdkClientOption.PROFILE_FILE_SUPPLIER, profileFile)
                                  .option(SdkClientOption.PROFILE_NAME, profileName)
                                  .build();

        return new UseGlobalEndpointResolver(config);
    }

    /**
     * Builder class to construct {@link S3Utilities} object
     */
    public static final class Builder {
        private Region region;
        private URI endpoint;

        private S3Configuration s3Configuration;
        private Supplier<ProfileFile> profileFile;
        private String profileName;
        private Boolean dualstackEnabled;
        private Boolean fipsEnabled;

        private Builder() {
        }

        /**
         * The default region to use when working with the methods in {@link S3Utilities} class.
         *
         * There can be methods in {@link S3Utilities} that don't need the region info.
         * In that case, this option will be ignored when using those methods.
         *
         * @return This object for method chaining
         */
        public Builder region(Region region) {
            this.region = region;
            return this;
        }

        /**
         * The default endpoint to use when working with the methods in {@link S3Utilities} class.
         *
         * There can be methods in {@link S3Utilities} that don't need the endpoint info.
         * In that case, this option will be ignored when using those methods.
         *
         * @return This object for method chaining
         */
        public Builder endpoint(URI endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        /**
         * Configure whether the SDK should use the AWS dualstack endpoint.
         *
         * <p>If this is not specified, the SDK will attempt to determine whether the dualstack endpoint should be used
         * automatically using the following logic:
         * <ol>
         *     <li>Check the 'aws.useDualstackEndpoint' system property for 'true' or 'false'.</li>
         *     <li>Check the 'AWS_USE_DUALSTACK_ENDPOINT' environment variable for 'true' or 'false'.</li>
         *     <li>Check the {user.home}/.aws/credentials and {user.home}/.aws/config files for the 'use_dualstack_endpoint'
         *     property set to 'true' or 'false'.</li>
         * </ol>
         *
         * <p>If the setting is not found in any of the locations above, 'false' will be used.
         */
        public Builder dualstackEnabled(Boolean dualstackEnabled) {
            this.dualstackEnabled = dualstackEnabled;
            return this;
        }

        /**
         * Configure whether the SDK should use the AWS fips endpoint.
         *
         * <p>If this is not specified, the SDK will attempt to determine whether the fips endpoint should be used
         * automatically using the following logic:
         * <ol>
         *     <li>Check the 'aws.useFipsEndpoint' system property for 'true' or 'false'.</li>
         *     <li>Check the 'AWS_USE_FIPS_ENDPOINT' environment variable for 'true' or 'false'.</li>
         *     <li>Check the {user.home}/.aws/credentials and {user.home}/.aws/config files for the 'use_fips_endpoint'
         *     property set to 'true' or 'false'.</li>
         * </ol>
         *
         * <p>If the setting is not found in any of the locations above, 'false' will be used.
         */
        public Builder fipsEnabled(Boolean fipsEnabled) {
            this.fipsEnabled = fipsEnabled;
            return this;
        }

        /**
         * Sets the S3 configuration to enable options like path style access, dual stack, accelerate mode etc.
         *
         * There can be methods in {@link S3Utilities} that don't need the region info.
         * In that case, this option will be ignored when using those methods.
         *
         * @return This object for method chaining
         */
        public Builder s3Configuration(S3Configuration s3Configuration) {
            this.s3Configuration = s3Configuration;
            return this;
        }

        /**
         * The profile file from the {@link ClientOverrideConfiguration#defaultProfileFile()}. This is private and only used
         * when the utilities is created via {@link S3Client#utilities()}. This is not currently public because it may be less
         * confusing to support the full {@link ClientOverrideConfiguration} object in the future.
         */
        private Builder profileFile(ProfileFile profileFile) {
            return profileFile(Optional.ofNullable(profileFile)
                                       .map(ProfileFileSupplier::fixedProfileFile)
                                       .orElse(null));
        }

        private Builder profileFile(Supplier<ProfileFile> profileFileSupplier) {
            this.profileFile = profileFileSupplier;
            return this;
        }

        /**
         * The profile name from the {@link ClientOverrideConfiguration#defaultProfileFile()}. This is private and only used
         * when the utilities is created via {@link S3Client#utilities()}. This is not currently public because it may be less
         * confusing to support the full {@link ClientOverrideConfiguration} object in the future.
         */
        private Builder profileName(String profileName) {
            this.profileName = profileName;
            return this;
        }

        /**
         * Construct a {@link S3Utilities} object.
         */
        public S3Utilities build() {
            return new S3Utilities(this);
        }
    }

    public S3Uri parseS3Uri(URI uri) {
        return parseS3Uri(uri, true);
    }

    public S3Uri parseS3Uri(URI uri, boolean urlEncode) {
        if (uri == null) {
            throw SdkClientException.create("URI must not be null");
        }

        Pattern accessPointPattern = Pattern.compile("^([a-zA-Z0-9\\-]+)\\.s3-accesspoint(-fips)?(\\.dualstack)?"
                                                     + "\\.([a-zA-Z0-9\\-]+)\\.amazonaws\\.com(.cn)?$");
        if (accessPointPattern.matcher(uri.toString()).find()) {
            throw SdkClientException.create("AccessPoints URI parsing is not supported");
        }

        Pattern outpostPattern = Pattern.compile("^([a-zA-Z0-9\\-]+)\\.op\\-[0-9]+\\.s3-outposts\\.([a-zA-Z0-9\\-]+)"
                                                 + "\\.amazonaws\\.com(.cn)?$");
        if (outpostPattern.matcher(uri.toString()).find()) {
            throw SdkClientException.create("Outposts URI parsing is not supported");
        }

        String bucket = null;
        String key = null;
        String region = null;
        boolean isPathStyle = false;
        Map<String, String> queryParams = new HashMap<>();

        if ("s3".equalsIgnoreCase(uri.getScheme())) {
            if (uri.getAuthority() == null) {
                throw SdkClientException.create("Invalid S3 URI: bucket not included");
            }
            bucket = uri.getAuthority();

            String path = uri.getPath();
            if (path.length() > 1) {
                key = uri.getPath().substring(1);
            }

        } else {
            if (uri.getHost() == null) {
                throw SdkClientException.create("Invalid S3 URI: hostname not included");
            }

            Pattern endpointPattern = Pattern.compile("^(.+\\.)?s3[.-]([a-z0-9-]+)\\.");
            Matcher matcher = endpointPattern.matcher(uri.getHost());
            if (!matcher.find()) {
                throw SdkClientException.create("Invalid S3 URI: hostname does not appear to be a valid S3 endpoint");
            }

            String prefix = matcher.group(1);
            if (prefix == null || prefix.isEmpty()) {
                isPathStyle = true;
                String path = urlEncode ? uri.getPath() : uri.getRawPath();

                if (!"".equals(path) && !"/".equals(path)) {
                    int index = path.indexOf('/', 1);

                    if (index == -1) {
                        bucket = decode(path.substring(1));
                    } else if (index == (path.length() - 1)) {
                        bucket = decode(path.substring(1, index));
                    } else {
                        bucket = decode(path.substring(1, index));
                        key = decode(path.substring(index + 1));
                    }
                }
            } else {
                bucket = prefix.substring(0, prefix.length() - 1);
                String path = uri.getPath();
                if (path != null && !path.isEmpty() && !"/".equals(uri.getPath())) {
                    key = uri.getPath().substring(1);
                }
            }

            if (!"amazonaws".equals(matcher.group(2))) {
                region = matcher.group(2);
            }
        }

        String queryPart = uri.getRawQuery();
        if (queryPart != null) {
            parseQuery(queryParams, queryPart);
        }

        return S3Uri.builder()
                .uri(uri)
                .bucket(bucket)
                .key(key)
                .region(region)
                .isPathStyle(isPathStyle)
                .queryParams(queryParams)
                .build();
    }

    private void parseQuery(Map<String, String> queryParams, String queryPart) {
        String[] params = queryPart.split("&");
        for (String param: params) {
            try {
                String[] keyValuePair = param.split("=", 2);
                String key = URLDecoder.decode(keyValuePair[0], "UTF-8");
                if (key.isEmpty()) {
                    continue;
                }
                String value = URLDecoder.decode(keyValuePair[1], "UTF-8");
                queryParams.put(key, value);
            } catch (UnsupportedEncodingException e) {
                // Param could not be decoded
            }

        }
    }

    /**
     * Percent-decodes the given string, with a fast path for strings that
     * are not percent-encoded.
     *
     * @param str the string to decode
     * @return the decoded string
     */
    private static String decode(final String str) {
        if (str == null) {
            return null;
        }

        for (int i = 0; i < str.length(); ++i) {
            if (str.charAt(i) == '%') {
                return decode(str, i);
            }
        }

        return str;
    }

    /**
     * Percent-decodes the given string.
     *
     * @param str the string to decode
     * @param firstPercent the index of the first '%' character in the string
     * @return the decoded string
     */
    private static String decode(final String str, final int firstPercent) {
        StringBuilder builder = new StringBuilder();
        builder.append(str.substring(0, firstPercent));

        appendDecoded(builder, str, firstPercent);

        for (int i = firstPercent + 3; i < str.length(); ++i) {
            if (str.charAt(i) == '%') {
                appendDecoded(builder, str, i);
                i += 2;
            } else {
                builder.append(str.charAt(i));
            }
        }

        return builder.toString();
    }

    /**
     * Decodes the percent-encoded character at the given index in the string
     * and appends the decoded value to the given {@code StringBuilder}.
     *
     * @param builder the string builder to append to
     * @param str the string being decoded
     * @param index the index of the '%' character in the string
     */
    private static void appendDecoded(final StringBuilder builder,
                                      final String str,
                                      final int index) {

        if (index > str.length() - 3) {
            throw new IllegalStateException("Invalid percent-encoded string:"
                                            + "\"" + str + "\".");
        }

        char first = str.charAt(index + 1);
        char second = str.charAt(index + 2);

        char decoded = (char) ((fromHex(first) << 4) | fromHex(second));
        builder.append(decoded);
    }

    /**
     * Converts a hex character (0-9A-Fa-f) into its corresponding quad value.
     *
     * @param c the hex character
     * @return the quad value
     */
    private static int fromHex(final char c) {
        if (c < '0') {
            throw new IllegalStateException(
                "Invalid percent-encoded string: bad character '" + c + "' in "
                + "escape sequence.");
        }
        if (c <= '9') {
            return (c - '0');
        }

        if (c < 'A') {
            throw new IllegalStateException(
                "Invalid percent-encoded string: bad character '" + c + "' in "
                + "escape sequence.");
        }
        if (c <= 'F') {
            return (c - 'A') + 10;
        }

        if (c < 'a') {
            throw new IllegalStateException(
                "Invalid percent-encoded string: bad character '" + c + "' in "
                + "escape sequence.");
        }
        if (c <= 'f') {
            return (c - 'a') + 10;
        }

        throw new IllegalStateException(
            "Invalid percent-encoded string: bad character '" + c + "' in "
            + "escape sequence.");
    }

}
