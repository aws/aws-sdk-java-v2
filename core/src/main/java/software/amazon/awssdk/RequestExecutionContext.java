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

import static software.amazon.awssdk.utils.Validate.notNull;

import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.auth.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.MetricsReportingCredentialsProvider;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.http.AmazonHttpClient;
import software.amazon.awssdk.http.ExecutionContext;
import software.amazon.awssdk.http.async.SdkHttpRequestProvider;
import software.amazon.awssdk.internal.http.timers.client.ClientExecutionAbortTrackerTask;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;
import software.amazon.awssdk.runtime.auth.SignerProvider;
import software.amazon.awssdk.utils.Validate;

/**
 * Request scoped dependencies and context for an execution of a request by {@link AmazonHttpClient}. Provided to the
 * {@link software.amazon.awssdk.http.pipeline.RequestPipeline#execute(Object, RequestExecutionContext)} method.
 */
public final class RequestExecutionContext {

    private final SdkHttpRequestProvider requestProvider;
    private final RequestConfig requestConfig;
    private final AwsRequestMetrics awsRequestMetrics;
    private final AwsCredentialsProvider credentialsProvider;
    private final List<RequestHandler> requestHandlers;
    private final SignerProvider signerProvider;

    private ClientExecutionAbortTrackerTask clientExecutionTrackerTask;

    private RequestExecutionContext(Builder builder) {
        this.requestProvider = builder.requestProvider;
        this.requestConfig = notNull(builder.requestConfig, "RequestConfig must not be null");
        this.requestHandlers = builder.resolveRequestHandlers();
        this.awsRequestMetrics = builder.executionContext.getAwsRequestMetrics();
        this.signerProvider = builder.executionContext.getSignerProvider();

        AwsCredentialsProvider contextCredentialsProvider = builder.executionContext.getCredentialsProvider();
        this.credentialsProvider = contextCredentialsProvider != null
                                   ? new MetricsReportingCredentialsProvider(contextCredentialsProvider, awsRequestMetrics)
                                   : new AnonymousCredentialsProvider();
    }

    /**
     * Create a {@link Builder}, used to create a {@link RequestExecutionContext}.
     */
    public static Builder builder() {
        return new Builder();
    }

    public SdkHttpRequestProvider requestProvider() {
        return requestProvider;
    }

    /**
     * @return Request level configuration.
     */
    public RequestConfig requestConfig() {
        return requestConfig;
    }

    /**
     * @return Request handlers to hook into request lifecycle.
     */
    public List<RequestHandler> requestHandlers() {
        return Collections.unmodifiableList(requestHandlers);
    }

    /**
     * @return AwsRequestMetrics object to report timing and events.
     */
    public AwsRequestMetrics awsRequestMetrics() {
        return awsRequestMetrics;
    }

    /**
     * @return Credentials provider to sign with. Will be non-null.
     */
    public AwsCredentialsProvider credentialsProvider() {
        return credentialsProvider;
    }

    /**
     * @return SignerProvider used to obtain an instance of a {@link software.amazon.awssdk.auth.Signer}.
     */
    public SignerProvider signerProvider() {
        return signerProvider;
    }

    /**
     * @return Tracker task for the {@link software.amazon.awssdk.internal.http.timers.client.ClientExecutionTimer}.
     */
    public ClientExecutionAbortTrackerTask getClientExecutionTrackerTask() {
        return clientExecutionTrackerTask;
    }

    /**
     * Sets the tracker task for the {@link software.amazon.awssdk.internal.http.timers.client.ClientExecutionTimer}. Should
     * be called once per request lifecycle.
     */
    public void setClientExecutionTrackerTask(ClientExecutionAbortTrackerTask clientExecutionTrackerTask) {
        this.clientExecutionTrackerTask = clientExecutionTrackerTask;
    }

    /**
     * An SDK-internal implementation of {@link Builder}.
     */
    public static final class Builder {

        private SdkHttpRequestProvider requestProvider;
        private RequestConfig requestConfig;
        private ExecutionContext executionContext;

        public Builder requestProvider(SdkHttpRequestProvider requestProvider) {
            this.requestProvider = requestProvider;
            return this;
        }

        public Builder requestConfig(RequestConfig requestConfig) {
            this.requestConfig = requestConfig;
            return this;
        }

        public Builder executionContext(ExecutionContext executionContext) {
            this.executionContext = executionContext;
            return this;
        }

        private List<RequestHandler> resolveRequestHandlers() {
            Validate.notNull(executionContext, "Execution context must be initialized before resolving request handlers.");

            List<RequestHandler> requestHandlers = executionContext.getRequestHandlers();
            if (requestHandlers == null) {
                return Collections.emptyList();
            }
            return requestHandlers;
        }

        public RequestExecutionContext build() {
            notNull(executionContext, "executionContext must not be null");
            return new RequestExecutionContext(this);
        }

    }
}
