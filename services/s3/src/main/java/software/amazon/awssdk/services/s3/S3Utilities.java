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


import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
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
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.awscore.client.config.AwsClientOption;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.awscore.endpoint.AwsClientEndpointProvider;
import software.amazon.awssdk.awscore.endpoint.DualstackEnabledProvider;
import software.amazon.awssdk.awscore.endpoint.FipsEnabledProvider;
import software.amazon.awssdk.awscore.internal.defaultsmode.DefaultsModeConfiguration;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.ClientType;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
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
import software.amazon.awssdk.utils.AttributeMap;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

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
@ThreadSafe
public final class S3Utilities {
    private static final String SERVICE_NAME = "s3";
    private static final Pattern ENDPOINT_PATTERN = Pattern.compile("^(.+\\.)?s3[.-]([a-z0-9-]+)\\.");
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
                                   .orElseGet(() -> ProfileFileSupplier.fixedProfileFile(ProfileFile.defaultProfileFile()));
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

        ClientEndpointProvider clientEndpoint = clientConfiguration.option(SdkClientOption.CLIENT_ENDPOINT_PROVIDER);
        if (clientEndpoint.isEndpointOverridden()) {
            builder.endpoint(clientEndpoint.clientEndpoint());
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
        ClientEndpointProvider clientEndpoint = clientEndpointProvider(endpointOverride, resolvedRegion);

        SdkHttpFullRequest marshalledRequest = createMarshalledRequest(getUrlRequest, clientEndpoint);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                                            .bucket(getUrlRequest.bucket())
                                                            .key(getUrlRequest.key())
                                                            .versionId(getUrlRequest.versionId())
                                                            .build();

        InterceptorContext interceptorContext = InterceptorContext.builder()
                                                                  .httpRequest(marshalledRequest)
                                                                  .request(getObjectRequest)
                                                                  .build();

        ExecutionAttributes executionAttributes = createExecutionAttributes(clientEndpoint, resolvedRegion);

        SdkHttpRequest modifiedRequest = runInterceptors(interceptorContext, executionAttributes).httpRequest();
        try {
            return modifiedRequest.getUri().toURL();
        } catch (MalformedURLException exception) {
            throw SdkException.create("Generated URI is malformed: " + modifiedRequest.getUri(),
                                      exception);
        }
    }

    /**
     * Returns a parsed {@link S3Uri} with which a user can easily retrieve the bucket, key, region, style, and query
     * parameters of the URI. Only path-style and virtual-hosted-style URI parsing is supported, including CLI-style
     * URIs, e.g., "s3://bucket/key". AccessPoints and Outposts URI parsing is not supported. If you work with object keys
     * and/or query parameters with special characters, they must be URL-encoded, e.g., replace " " with "%20". If you work with
     * virtual-hosted-style URIs with bucket names that contain a dot, i.e., ".", the dot must not be URL-encoded. Encoded
     * buckets, keys, and query parameters will be returned decoded.
     *
     * <p>
     * For more information on path-style and virtual-hosted-style URIs, see <a href=
     * "https://docs.aws.amazon.com/AmazonS3/latest/userguide/access-bucket-intro.html"
     * >Methods for accessing a bucket</a>.
     *
     * @param uri The URI to be parsed
     * @return Parsed {@link S3Uri}
     *
     * <p><b>Example Usage</b>
     * <p>
     * {@snippet :
     *     S3Client s3Client = S3Client.create();
     *     S3Utilities s3Utilities = s3Client.utilities();
     *     String uriString = "https://myBucket.s3.us-west-1.amazonaws.com/doc.txt?versionId=abc123";
     *     URI uri = URI.create(uriString);
     *     S3Uri s3Uri = s3Utilities.parseUri(uri);
     *
     *     String bucket = s3Uri.bucket().orElse(null); // "myBucket"
     *     String key = s3Uri.key().orElse(null); // "doc.txt"
     *     Region region = s3Uri.region().orElse(null); // Region.US_WEST_1
     *     boolean isPathStyle = s3Uri.isPathStyle(); // false
     *     String versionId = s3Uri.firstMatchingRawQueryParameter("versionId").orElse(null); // "abc123"
     *}
     */
    public S3Uri parseUri(URI uri) {
        validateUri(uri);

        if ("s3".equalsIgnoreCase(uri.getScheme())) {
            return parseAwsCliStyleUri(uri);
        }

        return parseStandardUri(uri);
    }

    private S3Uri parseStandardUri(URI uri) {

        if (uri.getHost() == null) {
            throw new IllegalArgumentException("Invalid S3 URI: no hostname: " + uri);
        }

        Matcher matcher = ENDPOINT_PATTERN.matcher(uri.getHost());
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid S3 URI: hostname does not appear to be a valid S3 endpoint: " + uri);
        }

        S3Uri.Builder builder = S3Uri.builder().uri(uri);
        addRegionIfNeeded(builder, matcher.group(2));
        addQueryParamsIfNeeded(builder, uri);

        String prefix = matcher.group(1);
        if (StringUtils.isEmpty(prefix)) {
            return parsePathStyleUri(builder, uri);
        }
        return parseVirtualHostedStyleUri(builder, uri, matcher);
    }

    private S3Uri.Builder addRegionIfNeeded(S3Uri.Builder builder, String region) {
        if (!"amazonaws".equals(region)) {
            return builder.region(Region.of(region));
        }
        return builder;
    }

    private S3Uri.Builder addQueryParamsIfNeeded(S3Uri.Builder builder, URI uri) {
        if (uri.getQuery() != null) {
            return builder.queryParams(SdkHttpUtils.uriParams(uri));
        }
        return builder;
    }

    private S3Uri parsePathStyleUri(S3Uri.Builder builder, URI uri) {
        String bucket = null;
        String key = null;
        String path = uri.getPath();

        if (!StringUtils.isEmpty(path) && !"/".equals(path)) {
            int index = path.indexOf('/', 1);

            if (index == -1) {
                // No trailing slash, e.g., "https://s3.amazonaws.com/bucket"
                bucket = path.substring(1);
            } else {
                bucket = path.substring(1, index);
                if (index != path.length() - 1) {
                    key = path.substring(index + 1);
                }
            }
        }
        return builder.key(key)
                      .bucket(bucket)
                      .isPathStyle(true)
                      .build();
    }

    private S3Uri parseVirtualHostedStyleUri(S3Uri.Builder builder, URI uri, Matcher matcher) {
        String bucket;
        String key = null;
        String path = uri.getPath();
        String prefix = matcher.group(1);

        bucket = prefix.substring(0, prefix.length() - 1);
        if (!StringUtils.isEmpty(path) && !"/".equals(path)) {
            key = path.substring(1);
        }

        return builder.key(key)
                      .bucket(bucket)
                      .build();
    }

    private S3Uri parseAwsCliStyleUri(URI uri) {
        String key = null;
        String bucket = uri.getAuthority();
        Region region = null;
        boolean isPathStyle = false;
        Map<String, List<String>> queryParams = new HashMap<>();
        String path = uri.getPath();

        if (bucket == null) {
            throw new IllegalArgumentException("Invalid S3 URI: bucket not included: " + uri);
        }

        if (path.length() > 1) {
            key = path.substring(1);
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

    private void validateUri(URI uri) {
        Validate.paramNotNull(uri, "uri");

        if (uri.toString().contains(".s3-accesspoint")) {
            throw new IllegalArgumentException("AccessPoints URI parsing is not supported: " + uri);
        }

        if (uri.toString().contains(".s3-outposts")) {
            throw new IllegalArgumentException("Outposts URI parsing is not supported: " + uri);
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
    private ClientEndpointProvider clientEndpointProvider(URI overrideEndpoint, Region region) {
        return AwsClientEndpointProvider.builder()
                                        .clientEndpointOverride(overrideEndpoint)
                                        .serviceEndpointOverrideEnvironmentVariable("AWS_ENDPOINT_URL_S3")
                                        .serviceEndpointOverrideSystemProperty("aws.endpointUrlS3")
                                        .serviceProfileProperty("s3")
                                        .serviceEndpointPrefix(SERVICE_NAME)
                                        .defaultProtocol("https")
                                        .region(region)
                                        .profileFile(profileFile)
                                        .profileName(profileName)
                                        .dualstackEnabled(s3Configuration.dualstackEnabled())
                                        .fipsEnabled(fipsEnabled)
                                        .build();
    }

    private URI getEndpointOverride(GetUrlRequest request) {
        URI requestOverrideEndpoint = request.endpoint();
        return requestOverrideEndpoint != null ? requestOverrideEndpoint : endpoint;
    }

    /**
     * Create a {@link SdkHttpFullRequest} object with the bucket and key values marshalled into the path params.
     */
    private SdkHttpFullRequest createMarshalledRequest(GetUrlRequest getUrlRequest,
                                                       ClientEndpointProvider clientEndpoint) {
        OperationInfo operationInfo = OperationInfo.builder()
                                                   .requestUri("/{Key+}")
                                                   .httpMethod(SdkHttpMethod.HEAD)
                                                   .build();

        SdkHttpFullRequest.Builder builder = ProtocolUtils.createSdkHttpRequest(operationInfo,
                                                                                clientEndpoint.clientEndpoint());

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
    private ExecutionAttributes createExecutionAttributes(ClientEndpointProvider clientEndpointProvider, Region region) {
        return new ExecutionAttributes()
            .putAttribute(AwsExecutionAttribute.AWS_REGION, region)
            .putAttribute(SdkExecutionAttribute.CLIENT_TYPE, ClientType.SYNC)
            .putAttribute(SdkExecutionAttribute.SERVICE_NAME, SERVICE_NAME)
            .putAttribute(SdkExecutionAttribute.OPERATION_NAME, "GetObject")
            .putAttribute(SdkExecutionAttribute.SERVICE_CONFIG, s3Configuration)
            .putAttribute(AwsExecutionAttribute.FIPS_ENDPOINT_ENABLED, fipsEnabled)
            .putAttribute(AwsExecutionAttribute.DUALSTACK_ENDPOINT_ENABLED, s3Configuration.dualstackEnabled())
            .putAttribute(SdkInternalExecutionAttribute.ENDPOINT_PROVIDER, S3EndpointProvider.defaultProvider())
            .putAttribute(SdkInternalExecutionAttribute.CLIENT_CONTEXT_PARAMS, createClientContextParams())
            .putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER, clientEndpointProvider)
            .putAttribute(AwsExecutionAttribute.USE_GLOBAL_ENDPOINT, useGlobalEndpointResolver.resolve(region));
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
}
