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

package software.amazon.awssdk.http;

import java.net.URI;
import java.util.List;
import software.amazon.awssdk.AmazonWebServiceClient;
import software.amazon.awssdk.annotation.NotThreadSafe;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.Signer;
import software.amazon.awssdk.handlers.RequestHandler;
import software.amazon.awssdk.internal.auth.NoOpSignerProvider;
import software.amazon.awssdk.internal.http.timers.client.ClientExecutionAbortTrackerTask;
import software.amazon.awssdk.metrics.spi.AwsRequestMetrics;
import software.amazon.awssdk.runtime.auth.SignerProvider;
import software.amazon.awssdk.runtime.auth.SignerProviderContext;
import software.amazon.awssdk.util.AwsRequestMetricsFullSupport;

/**
 * @NotThreadSafe This class should only be accessed by a single thread and be used throughout
 *                a single request lifecycle.
 */
@NotThreadSafe
@SdkProtectedApi
public class ExecutionContext {
    private final AwsRequestMetrics awsRequestMetrics;
    private final List<RequestHandler> requestHandlers;
    private final AmazonWebServiceClient awsClient;
    private final SignerProvider signerProvider;

    private boolean retryCapacityConsumed;

    /**
     * Optional credentials to enable the runtime layer to handle signing requests (and resigning on
     * retries).
     */
    private AwsCredentialsProvider credentialsProvider;

    private ClientExecutionAbortTrackerTask clientExecutionTrackerTask;

    /** For testing purposes. */
    public ExecutionContext(boolean isMetricEnabled) {
        this(builder().withUseRequestMetrics(isMetricEnabled).withSignerProvider(new NoOpSignerProvider()));
    }

    /** For testing purposes. */
    public ExecutionContext() {
        this(builder().withSignerProvider(new NoOpSignerProvider()));
    }

    @Deprecated
    public ExecutionContext(List<RequestHandler> requestHandlers, boolean isMetricEnabled,
                            AmazonWebServiceClient awsClient) {
        this.requestHandlers = requestHandlers;
        awsRequestMetrics = isMetricEnabled ? new AwsRequestMetricsFullSupport() : new AwsRequestMetrics();
        this.awsClient = awsClient;
        this.signerProvider = new SignerProvider() {
            @Override
            public Signer getSigner(SignerProviderContext context) {
                return getSignerByUri(context.getUri());
            }
        };
    }

    private ExecutionContext(final Builder builder) {
        this.requestHandlers = builder.requestHandlers;
        this.awsRequestMetrics = builder.useRequestMetrics ? new AwsRequestMetricsFullSupport() : new AwsRequestMetrics();
        this.awsClient = builder.awsClient;
        this.signerProvider = builder.signerProvider;
    }

    public static ExecutionContext.Builder builder() {
        return new ExecutionContext.Builder();
    }

    public List<RequestHandler> getRequestHandlers() {
        return requestHandlers;
    }

    public AwsRequestMetrics getAwsRequestMetrics() {
        return awsRequestMetrics;
    }

    protected AmazonWebServiceClient getAwsClient() {
        return awsClient;
    }

    /**
     * There is in general no need to set the signer in the execution context, since the signer for
     * each request may differ depending on the URI of the request. The exception is S3 where the
     * signer is currently determined only when the S3 client is constructed. Hence the need for
     * this method. We may consider supporting a per request level signer determination for S3 later
     * on.
     */
    @Deprecated
    public void setSigner(Signer signer) {
    }

    /**
     * Returns whether retry capacity was consumed during this request lifecycle.
     * This can be inspected to determine whether capacity should be released if a retry succeeds.
     *
     * @return true if retry capacity was consumed
     */
    public boolean retryCapacityConsumed() {
        return retryCapacityConsumed;
    }

    /**
     * Marks that a retry during this request lifecycle has consumed retry capacity.  This is inspected
     * when determining if capacity should be released if a retry succeeds.
     */
    public void markRetryCapacityConsumed() {
        this.retryCapacityConsumed = true;
    }

    /**
     * Passes in the provided {@link SignerProviderContext} into a {@link SignerProvider} and returns
     * a {@link Signer} instance.
     */
    public Signer getSigner(SignerProviderContext context) {
        return signerProvider.getSigner(context);
    }

    /**
     * Returns the signer for the given uri. Note S3 in particular overrides this method.
     */
    @Deprecated
    public Signer getSignerByUri(URI uri) {
        return awsClient == null ? null : awsClient.getSignerByUri(uri);
    }

    /**
     * Returns the credentials provider used for fetching the credentials. The credentials fetched
     * is used for signing the request. If there is no credential provider, then the runtime will
     * not attempt to sign (or resign on retries) requests.
     *
     * @return the credentials provider to fetch {@link AwsCredentials}
     */
    public AwsCredentialsProvider getCredentialsProvider() {
        return this.credentialsProvider;
    }

    /**
     * Sets the credentials provider used for fetching the credentials. The credentials fetched is
     * used for signing the request. If there is no credential provider, then the runtime will not
     * attempt to sign (or resign on retries) requests.
     *
     * @param credentialsProvider
     *            the credentials provider to fetch {@link AwsCredentials}
     */
    public void setCredentialsProvider(AwsCredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    public ClientExecutionAbortTrackerTask getClientExecutionTrackerTask() {
        return clientExecutionTrackerTask;
    }

    public void setClientExecutionTrackerTask(ClientExecutionAbortTrackerTask clientExecutionTrackerTask) {
        this.clientExecutionTrackerTask = clientExecutionTrackerTask;
    }

    public SignerProvider getSignerProvider() {
        return signerProvider;
    }

    public static class Builder {

        private boolean useRequestMetrics;
        private List<RequestHandler> requestHandlers;
        private AmazonWebServiceClient awsClient;
        private SignerProvider signerProvider = new NoOpSignerProvider();

        private Builder() {
        }

        public boolean useRequestMetrics() {
            return useRequestMetrics;
        }

        public void setUseRequestMetrics(final boolean useRequestMetrics) {
            this.useRequestMetrics = useRequestMetrics;
        }

        public Builder withUseRequestMetrics(final boolean withUseRequestMetrics) {
            setUseRequestMetrics(withUseRequestMetrics);
            return this;
        }

        public List<RequestHandler> getRequestHandlers() {
            return requestHandlers;
        }

        public void setRequestHandlers(final List<RequestHandler> requestHandlers) {
            this.requestHandlers = requestHandlers;
        }

        public Builder withRequestHandlers(final List<RequestHandler> requestHandlers) {
            setRequestHandlers(requestHandlers);
            return this;
        }

        public AmazonWebServiceClient getAwsClient() {
            return awsClient;
        }

        public void setAwsClient(final AmazonWebServiceClient awsClient) {
            this.awsClient = awsClient;
        }

        public Builder withAwsClient(final AmazonWebServiceClient awsClient) {
            setAwsClient(awsClient);
            return this;
        }

        public SignerProvider getSignerProvider() {
            return signerProvider;
        }

        public void setSignerProvider(final SignerProvider signerProvider) {
            this.signerProvider = signerProvider;
        }

        public Builder withSignerProvider(final SignerProvider signerProvider) {
            setSignerProvider(signerProvider);
            return this;
        }

        public ExecutionContext build() {
            return new ExecutionContext(this);
        }

    }

}
