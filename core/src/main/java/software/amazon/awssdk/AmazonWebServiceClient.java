/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.annotation.SdkInternalApi;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.auth.RegionAwareSigner;
import software.amazon.awssdk.auth.Signer;
import software.amazon.awssdk.auth.SignerFactory;
import software.amazon.awssdk.client.AwsSyncClientParams;
import software.amazon.awssdk.client.builder.AwsClientBuilder;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.http.AmazonHttpClient;
import software.amazon.awssdk.http.ExecutionContext;
import software.amazon.awssdk.internal.auth.DefaultSignerProvider;
import software.amazon.awssdk.log.CommonsLogFactory;
import software.amazon.awssdk.log.InternalLogFactory;
import software.amazon.awssdk.metrics.AwsSdkMetrics;
import software.amazon.awssdk.metrics.RequestMetricCollector;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.runtime.auth.SignerProvider;
import software.amazon.awssdk.runtime.auth.SignerProviderContext;
import software.amazon.awssdk.runtime.endpoint.DefaultServiceEndpointBuilder;
import software.amazon.awssdk.util.AwsHostNameUtils;
import software.amazon.awssdk.util.Classes;
import software.amazon.awssdk.util.RuntimeHttpUtils;
import software.amazon.awssdk.util.StringUtils;

/**
 * Abstract base class for Amazon Web Service Java clients.
 * <p>
 * Responsible for basic client capabilities that are the same across all AWS
 * SDK Java clients (ex: setting the client endpoint).
 */
public abstract class AmazonWebServiceClient {

    /**
     * @deprecated No longer used.
     */
    @Deprecated
    public static final boolean LOGGING_AWS_REQUEST_METRIC = true;

    private static final String AMAZON = "Amazon";
    private static final String AWS = "AWS";

    private static final Log LOG =
            LogFactory.getLog(AmazonWebServiceClient.class);

    static {
        // Configures the internal logging of the signers and core
        // classes to use Jakarta Commons Logging to stay consistent with the
        // rest of the library.
        boolean success = InternalLogFactory.configureFactory(
                new CommonsLogFactory());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Internal logging successfully configured to commons logger: "
                    + success);
        }
    }

    /**
     * Optional request handlers for additional request processing.
     */
    protected final List<RequestHandler> requestHandlers;
    /**
     * The service endpoint to which this client will send requests.
     * <p>
     * Subclass should only read but not assign to this field, at least not
     * without synchronization on the enclosing object for thread-safety
     * reason.
     */
    protected volatile URI endpoint;
    /**
     * The client configuration.
     */
    protected LegacyClientConfiguration clientConfiguration;
    /**
     * Low level client for sending requests to AWS services.
     */
    protected AmazonHttpClient client;
    /**
     * Optional offset (in seconds) to use when signing requests.
     */
    protected int timeOffset;
    /**
     * Flag indicating whether a client is mutable or not. Legacy clients built via the constructors
     * are mutable. Clients built with the fluent builders are immutable.
     */
    private volatile boolean isImmutable = false;
    /**
     * Used to explicitly override the internal signer region computed by the
     * default implementation. This field is typically null.
     */
    private volatile String signerRegionOverride;
    private volatile SignerProvider signerProvider;

    /**
     * The cached service abbreviation for this service, used for identifying
     * service endpoints by region, identifying the necessary signer, etc.
     * Thread safe so it's backward compatible.
     */
    private volatile String serviceName;

    /**
     * The service name in region metadata, i.e. the prefix of endpoint.
     */
    private volatile String endpointPrefix;

    /**
     * Constructs a new AmazonWebServiceClient object using the specified
     * configuration.
     *
     * @param clientConfiguration The client configuration for this client.
     */
    public AmazonWebServiceClient(LegacyClientConfiguration clientConfiguration) {
        this(clientConfiguration, null);
    }

    /**
     * Constructs a new AmazonWebServiceClient object using the specified
     * configuration and request metric collector.
     *
     * @param clientConfiguration    The client configuration for this client.
     * @param requestMetricCollector optional request metric collector to be used at the http
     *                               client level; can be null.
     */
    public AmazonWebServiceClient(LegacyClientConfiguration clientConfiguration,
                                  RequestMetricCollector requestMetricCollector) {
        this(clientConfiguration, requestMetricCollector, false);
    }

    @SdkProtectedApi
    protected AmazonWebServiceClient(LegacyClientConfiguration clientConfiguration,
                                     RequestMetricCollector requestMetricCollector,
                                     boolean disableStrictHostNameVerification) {
        this.clientConfiguration = clientConfiguration;
        requestHandlers = new CopyOnWriteArrayList<>();
        client = AmazonHttpClient.builder()
                .clientConfiguration(clientConfiguration)
                .requestMetricCollector(requestMetricCollector)
                .calculateCrc32FromCompressedData(calculateCrc32FromCompressedData())
                .build();
    }

    protected AmazonWebServiceClient(AwsSyncClientParams clientParams) {
        this.clientConfiguration = clientParams.getClientConfiguration();
        requestHandlers = clientParams.getRequestHandlers();
        client = AmazonHttpClient.builder()
                                 .sdkHttpClient(clientParams.sdkHttpClient())
                                 .clientConfiguration(clientConfiguration)
                                 .requestMetricCollector(clientParams.getRequestMetricCollector())
                                 .calculateCrc32FromCompressedData(calculateCrc32FromCompressedData())
                                 .build();
    }

    /**
     * Returns the signer.
     * <p>
     * Note, however, the signer configured for S3 is incomplete at this stage
     * as the information on the S3 bucket and key is not yet known.
     */
    @Deprecated
    protected Signer getSigner() {
        return signerProvider.getSigner(SignerProviderContext.builder().build());
    }

    /**
     * @return Current SignerProvider instance.
     */
    @SdkProtectedApi
    protected SignerProvider getSignerProvider() {
        return signerProvider;
    }

    /**
     * Overrides the default endpoint for this client. Callers can use this
     * method to control which AWS region they want to work with.
     * <p>
     * <b>This method is not threadsafe. Endpoints should be configured when the
     * client is created and before any service requests are made. Changing it
     * afterwards creates inevitable race conditions for any service requests in
     * transit.</b>
     * <p>
     * Callers can pass in just the endpoint (ex: "ec2.amazonaws.com") or a full
     * URL, including the protocol (ex: "https://ec2.amazonaws.com"). If the
     * protocol is not specified here, the default protocol from this client's
     * {@link LegacyClientConfiguration} will be used, which by default is HTTPS.
     * <p>
     * For more information on using AWS regions with the AWS SDK for Java, and
     * a complete list of all available endpoints for all AWS services, see:
     * <a href="http://developer.amazonwebservices.com/connect/entry.jspa?externalID=3912">
     * http://developer.amazonwebservices.com/connect/entry.jspa?externalID=3912</a>
     *
     * @param endpoint The endpoint (ex: "ec2.amazonaws.com") or a full URL,
     *                 including the protocol (ex: "https://ec2.amazonaws.com") of
     *                 the region specific AWS endpoint this client will communicate
     *                 with.
     * @throws IllegalArgumentException If any problems are detected with the specified endpoint.
     * @deprecated use {@link AwsClientBuilder#setEndpointConfiguration(AwsClientBuilder.EndpointConfiguration)} for example:
     * {@code builder.setEndpointConfiguration(new EndpointConfiguration(endpoint, signingRegion));}
     */
    @Deprecated
    public void setEndpoint(String endpoint) throws IllegalArgumentException {
        checkMutability();
        URI uri = toUri(endpoint);
        Signer signer = computeSignerByUri(uri, signerRegionOverride, false);
        synchronized (this) {
            this.endpoint = uri;
            this.signerProvider = createSignerProvider(signer);
        }
    }

    /**
     * Returns the endpoint as a URI.
     */
    private URI toUri(String endpoint) throws IllegalArgumentException {
        return RuntimeHttpUtils.toUri(endpoint, clientConfiguration);
    }

    /**
     * Returns the signer based on the given URI and the current AWS client
     * configuration. Currently only the SQS client can have different region on
     * a per request basis. For other AWS clients, the region remains the same
     * on a per AWS client level.
     * <p>
     * Note, however, the signer returned for S3 is incomplete at this stage as
     * the information on the S3 bucket and key is not yet known.
     */
    public Signer getSignerByUri(URI uri) {
        return computeSignerByUri(uri, signerRegionOverride, true);
    }

    /**
     * Returns the signer for the given uri and the current client
     * configuration.
     * <p>
     * Note, however, the signer returned for S3 is incomplete at this stage as
     * the information on the S3 bucket and key is not yet known.
     *
     * @param signerRegionOverride    the overriding signer region; or null if there is none.
     * @param isRegionIdAsSignerParam true if the "regionId" is used to configure the signer if
     *                                applicable; false if this method is called for the purpose of
     *                                purely setting the communication end point of this AWS client,
     *                                and therefore the "regionId" parameter will not be used
     *                                directly for configuring the signer.
     */
    private Signer computeSignerByUri(URI uri, String signerRegionOverride,
                                      boolean isRegionIdAsSignerParam) {
        if (uri == null) {
            throw new IllegalArgumentException(
                    "Endpoint is not set. Use setEndpoint to set an endpoint before performing any request.");
        }
        String service = getServiceNameIntern();
        String region = AwsHostNameUtils.parseRegionName(uri.getHost(), service);
        return computeSignerByServiceRegion(
                service, region, signerRegionOverride, isRegionIdAsSignerParam);
    }

    /**
     * Returns the signer for the given service name, region id, and the current
     * client configuration.
     * <p>
     * Note, however, the signer returned for S3 is incomplete at this stage as
     * the information on the S3 bucket and key is not yet known.
     *
     * @param regionId                the region for sending AWS requests
     * @param signerRegionOverride    the overriding signer region; or null if there is none.
     * @param isRegionIdAsSignerParam true if the "regionId" is used to configure the signer if
     *                                applicable; false if this method is called for the purpose of
     *                                purely setting the communication end point of this AWS client,
     *                                and therefore the "regionId" parameter will not be used
     *                                directly for configuring the signer.
     */
    private Signer computeSignerByServiceRegion(
            String serviceName, String regionId,
            String signerRegionOverride,
            boolean isRegionIdAsSignerParam) {
        String signerType = clientConfiguration.getSignerOverride();
        Signer signer = signerType == null
                ? SignerFactory.getSigner(serviceName, regionId)
                : SignerFactory.getSignerByTypeAndService(signerType, serviceName);
        if (signer instanceof RegionAwareSigner) {
            // Overrides the default region computed
            RegionAwareSigner regionAwareSigner = (RegionAwareSigner) signer;
            // (signerRegionOverride != null) means that it is likely to be AWS
            // internal dev work, as "signerRegionOverride" is typically null
            // when used in the external release
            if (signerRegionOverride != null) {
                regionAwareSigner.setRegionName(signerRegionOverride);
            } else if (regionId != null && isRegionIdAsSignerParam) {
                regionAwareSigner.setRegionName(regionId);
            }
        }
        return signer;
    }

    /**
     * An alternative to {@link AmazonWebServiceClient#setEndpoint(String)}, sets the regional
     * endpoint for this client's service calls. Callers can use this method to control which AWS
     * region they want to work with.
     * <p>
     * <b>This method is not threadsafe. A region should be configured when the client is created
     * and before any service requests are made. Changing it afterwards creates inevitable race
     * conditions for any service requests in transit or retrying.</b>
     * <p>
     * By default, all service endpoints in all regions use the https protocol. To use http instead,
     * specify it in the {@link LegacyClientConfiguration} supplied at construction.
     *
     * @param region The of this client will communicate with. See
     *               {@link Region#of(String)} for accessing a given
     *               of.
     * @throws java.lang.IllegalArgumentException If the given of is null, or if this service isn't available in the given
     *                                            of. See {@link Region#getRegionMetadata()} isServiceSupported(String)}
     * @see Region#of(String)
     * LegacyClientConfiguration)
     * @deprecated use {@link AwsClientBuilder#setRegion(Region)}
     */
    @Deprecated
    public void setRegion(Region region) throws IllegalArgumentException {
        checkMutability();
        if (region == null) {
            throw new IllegalArgumentException("No region provided");
        }
        final String serviceNameForEndpoint = getEndpointPrefix();
        final String serviceNameForSigner = getServiceNameIntern();
        String protocol = clientConfiguration.getProtocol().toString();
        URI uri = new DefaultServiceEndpointBuilder(serviceNameForEndpoint, protocol).withRegion(region).getServiceEndpoint();

        Signer signer = computeSignerByServiceRegion(serviceNameForSigner, region.value(), signerRegionOverride, false);

        synchronized (this) {
            this.endpoint = uri;
            this.signerProvider = createSignerProvider(signer);
        }
    }

    /**
     * Shuts down this client object, releasing any resources that might be held
     * open. This is an optional method, and callers are not expected to call
     * it, but can if they want to explicitly release any open resources. Once a
     * client has been shutdown, it should not be used to make any more
     * requests.
     */
    public void shutdown() {
        invokeSafely(client::close);
    }

    /**
     * Runs the {@code beforeMarshalling} method of any
     * {@code RequestHandler2}s associated with this client.
     *
     * @param request the request passed in from the user
     * @return the (possibly different) request to marshal
     */
    @SuppressWarnings("unchecked")
    protected final <T extends AmazonWebServiceRequest> T beforeMarshalling(
            T request) {

        T local = request;
        for (RequestHandler handler : requestHandlers) {
            local = (T) handler.beforeMarshalling(local);
        }
        return local;
    }

    protected ExecutionContext createExecutionContext(AmazonWebServiceRequest req) {
        return createExecutionContext(req, signerProvider);
    }

    protected ExecutionContext createExecutionContext(AmazonWebServiceRequest req,
                                                      SignerProvider signerProvider) {
        boolean isMetricsEnabled = isRequestMetricsEnabled(req);
        return ExecutionContext.builder()
                .withRequestHandlers(requestHandlers)
                .withUseRequestMetrics(isMetricsEnabled)
                .withAwsClient(this)
                .withSignerProvider(signerProvider).build();
    }

    protected final ExecutionContext createExecutionContext(Request<?> req) {
        return createExecutionContext(req.getOriginalRequest());
    }

    protected SignerProvider createSignerProvider(Signer signer) {
        return new DefaultSignerProvider(signer);
    }

    /**
     * Returns true if request metric collection is applicable to the given
     * request; false otherwise.
     */
    protected final boolean isRequestMetricsEnabled(AmazonWebServiceRequest req) {
        RequestMetricCollector c = req.getRequestMetricCollector(); // request level collector
        if (c != null && c.isEnabled()) {
            return true;
        }
        return isRmcEnabledAtClientOrSdkLevel();
    }

    /**
     * Returns true if request metric collection is enabled at the service
     * client or AWS SDK level request; false otherwise.
     */
    private boolean isRmcEnabledAtClientOrSdkLevel() {
        RequestMetricCollector c = requestMetricCollector();
        return c != null && c.isEnabled();
    }

    /**
     * Sets the optional value for time offset for this client.  This
     * value will be applied to all requests processed through this client.
     * Value is in seconds, positive values imply the current clock is "fast",
     * negative values imply clock is slow.
     *
     * @param timeOffset The optional value for time offset (in seconds) for this client.
     * @return the updated web service client
     */
    public AmazonWebServiceClient withTimeOffset(int timeOffset) {
        checkMutability();
        setTimeOffset(timeOffset);
        return this;
    }

    /**
     * Returns the optional value for time offset for this client.  This
     * value will be applied to all requests processed through this client.
     * Value is in seconds, positive values imply the current clock is "fast",
     * negative values imply clock is slow.
     *
     * @return The optional value for time offset (in seconds) for this client.
     */
    public int getTimeOffset() {
        return timeOffset;
    }

    /**
     * Sets the optional value for time offset for this client.  This
     * value will be applied to all requests processed through this client.
     * Value is in seconds, positive values imply the current clock is "fast",
     * negative values imply clock is slow.
     *
     * @param timeOffset The optional value for time offset (in seconds) for this client.
     */
    public void setTimeOffset(int timeOffset) {
        checkMutability();
        this.timeOffset = timeOffset;
    }

    /**
     * Returns the client specific {@link RequestMetricCollector}; or null if
     * there is none.
     */
    public RequestMetricCollector getRequestMetricsCollector() {
        return client.getRequestMetricCollector();
    }

    /**
     * Returns the client specific request metric collector if there is one; or
     * the one at the AWS SDK level otherwise.
     */
    protected RequestMetricCollector requestMetricCollector() {
        RequestMetricCollector mc = client.getRequestMetricCollector();
        return mc == null ? AwsSdkMetrics.getRequestMetricCollector() : mc;
    }

    /**
     * Returns the most specific request metric collector, starting from the request level, then
     * client level, then finally the AWS SDK level.
     */
    private final RequestMetricCollector findRequestMetricCollector(
            RequestMetricCollector reqLevelMetricsCollector) {
        if (reqLevelMetricsCollector != null) {
            return reqLevelMetricsCollector;
        } else if (getRequestMetricsCollector() != null) {
            return getRequestMetricsCollector();
        } else {
            return AwsSdkMetrics.getRequestMetricCollector();
        }
    }

    /**
     * Convenient method to end the client execution without logging the
     * awsRequestMetrics.
     */
    protected final void endClientExecution(AwsRequestMetrics awsRequestMetrics, Request<?> request) {
        this.endClientExecution(awsRequestMetrics, request, null, !LOGGING_AWS_REQUEST_METRIC);
    }

    /**
     * Common routine to end a client AWS request/response execution and collect
     * the request metrics.  Caller of this routine is responsible for starting
     * the event for {@link AwsRequestMetrics.Field#ClientExecuteTime} and call this method
     * in a try-finally block.
     *
     * @param loggingAwsRequestMetrics deprecated and ignored
     */
    protected final void endClientExecution(
            AwsRequestMetrics awsRequestMetrics, Request<?> request,
            Response<?> response, @Deprecated boolean loggingAwsRequestMetrics) {
        if (request != null) {
            awsRequestMetrics.endEvent(AwsRequestMetrics.Field.ClientExecuteTime);
            awsRequestMetrics.getTimingInfo().endTiming();
            RequestMetricCollector c = findRequestMetricCollector(
                    request.getOriginalRequest().getRequestMetricCollector());
            c.collectMetrics(request, response.getAwsResponse());
            awsRequestMetrics.log();
        }
    }

    /**
     * @deprecated by {@link #getServiceName()}.
     */
    @Deprecated
    protected String getServiceAbbreviation() {
        return getServiceNameIntern();
    }

    /**
     * Returns the service abbreviation for this service, used for identifying
     * service endpoints by region, identifying the necessary signer, etc.
     * Used to be call "getServiceAbbreviation".
     */
    public String getServiceName() {
        return getServiceNameIntern();
    }

    /**
     * Returns the service name that should be used when computing the region
     * endpoints. This is the values of the regionMetadataServiceName
     * configuration in the internal config file if such configuration is
     * specified for the current client, otherwise it returns the same
     * service name that is used for request signing.
     *
     * @return the service name
     */
    public String getEndpointPrefix() {
        if (endpointPrefix != null) {
            return endpointPrefix;
        }

        String httpClientName = getHttpClientName();
        String serviceNameInRegionMetadata = ServiceNameFactory.getServiceNameInRegionMetadata(httpClientName);

        synchronized (this) {
            if (endpointPrefix == null) {
                if (serviceNameInRegionMetadata != null) {
                    endpointPrefix = serviceNameInRegionMetadata;
                } else {
                    endpointPrefix = getServiceNameIntern();
                }
            }

            return endpointPrefix;
        }
    }

    /**
     * An internal method used to explicitly override the service name for region metadata.
     * This service name is used to compute the region endpoints.
     */
    protected void setEndpointPrefix(String endpointPrefix) {
        if (endpointPrefix == null) {
            throw new IllegalArgumentException(
                    "The parameter endpointPrefix must be specified!");
        }
        this.endpointPrefix = endpointPrefix;
    }

    /**
     * Internal method for implementing {@link #getServiceName()}. Method is
     * protected by intent so peculiar subclass that don't follow the class
     * naming convention can choose to return whatever service name as needed.
     */
    protected String getServiceNameIntern() {
        if (serviceName == null) {
            synchronized (this) {
                if (serviceName == null) {
                    serviceName = computeServiceName();
                }
            }
        }
        return serviceName;
    }

    /**
     * An internal method used to explicitly override the service name
     * computed by the default implementation. This method is not expected to be
     * normally called except for AWS internal development purposes.
     */
    public final void setServiceNameIntern(String serviceName) {
        if (serviceName == null) {
            throw new IllegalArgumentException(
                    "The parameter serviceName must be specified!");
        }
        this.serviceName = serviceName;
    }

    /**
     * Returns the service name of this AWS http client by first looking it up from the SDK internal
     * configuration, and if not found, derive it from the class name of the immediate subclass of
     * {@link AmazonWebServiceClient}. No configuration is necessary if the simple class name of the
     * http client follows the convention of <code>(Amazon|AWS).*(JavaClient|Client)</code>.
     */
    private String computeServiceName() {
        final String httpClientName = getHttpClientName();
        String service = ServiceNameFactory.getServiceName(httpClientName);
        if (service != null) {
            return service; // only if it is so explicitly configured
        }
        // Otherwise, make use of convention over configuration
        int j = httpClientName.indexOf("JavaClient");
        if (j == -1) {
            j = httpClientName.indexOf("Client");
            if (j == -1) {
                throw new IllegalStateException(
                        "Unrecognized suffix for the AWS http client class name " + httpClientName);
            }
        }
        int i = httpClientName.indexOf(AMAZON);
        int len;
        if (i == -1) {
            i = httpClientName.indexOf(AWS);
            if (i == -1) {
                throw new IllegalStateException(
                        "Unrecognized prefix for the AWS http client class name " + httpClientName);
            }
            len = AWS.length();
        } else {
            len = AMAZON.length();
        }
        if (i >= j) {
            throw new IllegalStateException(
                    "Unrecognized AWS http client class name " + httpClientName);
        }
        String serviceName = httpClientName.substring(i + len, j);
        return StringUtils.lowerCase(serviceName);
    }

    private String getHttpClientName() {
        Class<?> httpClientClass = Classes.childClassOf(AmazonWebServiceClient.class, this);
        return httpClientClass.getSimpleName();
    }

    /**
     * Returns the signer region override.
     *
     * @see #setSignerRegionOverride(String).
     */
    public final String getSignerRegionOverride() {
        return signerRegionOverride;
    }

    /**
     * An internal method used to explicitly override the internal signer region
     * computed by the default implementation. This method is not expected to be
     * normally called except for AWS internal development purposes.
     */
    public final void setSignerRegionOverride(String signerRegionOverride) {
        checkMutability();
        Signer signer = computeSignerByUri(endpoint, signerRegionOverride, true);
        synchronized (this) {
            this.signerRegionOverride = signerRegionOverride;
            this.signerProvider = createSignerProvider(signer);
        }
    }

    /**
     * Fluent method for {@link #setRegion(Region)}.
     * <p>
     * Example: {@code AmazonDynamoDBClient client = new AmazonDynamoDBClient(...).<AmazonDynamoDBClient>withRegion(...);}
     *
     * @see #setRegion(Region)
     * @deprecated use {@link AwsClientBuilder#withRegion(Region)} for example:
     * {@code AmazonSNSClient.builder().withRegion(region).build();}
     */
    @Deprecated
    public <T extends AmazonWebServiceClient> T withRegion(Region region) {
        setRegion(region);
        @SuppressWarnings("unchecked")
        T t = (T) this;
        return t;
    }

    /**
     * Fluent method for {@link #setEndpoint(String)}.
     * <p>
     * Example: {@code AmazonDynamoDBClient client = new AmazonDynamoDBClient(...).<AmazonDynamoDBClient>withEndPoint(...);}
     *
     * @see #setEndpoint(String)
     * @deprecated use {@link AwsClientBuilder#withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration)} for example:
     * {@code AmazonSNSClient.builder()
     * .withEndpointConfiguration(new EndpointConfiguration(endpoint, signingRegion)).build();}
     */
    @Deprecated
    public <T extends AmazonWebServiceClient> T withEndpoint(String endpoint) {
        setEndpoint(endpoint);
        @SuppressWarnings("unchecked")
        T t = (T) this;
        return t;
    }

    /**
     * Internal only API to lock a client's mutable methods. Only intended for use by the fluent
     * builders.
     */
    @Deprecated
    @SdkInternalApi
    public final void makeImmutable() {
        this.isImmutable = true;
    }

    /**
     * If the client has been marked as immutable then throw an {@link
     * UnsupportedOperationException}, otherwise do nothing. Should be called by each mutating
     * method.
     */
    @SdkProtectedApi
    protected final void checkMutability() {
        if (isImmutable) {
            throw new UnsupportedOperationException(
                    "Client is immutable when created with the builder.");
        }
    }

    /**
     * Hook to allow S3 client to disable strict hostname verification since it uses wildcard
     * certificates.
     *
     * @return True if strict hostname verification should be used, false otherwise.
     */
    protected boolean useStrictHostNameVerification() {
        return true;
    }

    /**
     * Hook to allow clients to override CRC32 calculation behavior. Currently, only exercised by DynamoDB.
     *
     * @return True if the service returns CRC32 checksum from the compressed data, false otherwise.
     */
    protected boolean calculateCrc32FromCompressedData() {
        return false;
    }
}
