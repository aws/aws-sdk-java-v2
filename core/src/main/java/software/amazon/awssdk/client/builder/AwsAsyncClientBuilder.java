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

package software.amazon.awssdk.client.builder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import software.amazon.awssdk.LegacyClientConfiguration;
import software.amazon.awssdk.annotation.NotThreadSafe;
import software.amazon.awssdk.annotation.SdkProtectedApi;
import software.amazon.awssdk.annotation.SdkTestInternalApi;
import software.amazon.awssdk.client.AwsAsyncClientParams;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;

/**
 * Base class for all service specific async client builders.
 *
 * @param <SubclassT> Concrete builder type, used for better fluent methods.
 */
@NotThreadSafe
@SdkProtectedApi
public abstract class AwsAsyncClientBuilder<SubclassT extends AwsAsyncClientBuilder, TypeToBuildT>
        extends AwsClientBuilder<SubclassT, TypeToBuildT> {
    private ExecutorFactory executorFactory;

    protected AwsAsyncClientBuilder() {
        super();
    }

    @SdkTestInternalApi
    protected AwsAsyncClientBuilder(
            AwsRegionProvider regionProvider) {
        super(regionProvider);
    }


    /**
     * @return The {@link ExecutorFactory} currently configured by the client.
     */
    public final ExecutorFactory getExecutorFactory() {
        return executorFactory;
    }

    /**
     * Sets a custom executor service factory to use for the async clients. The factory will be
     * called for each async client created through the builder.
     *
     * @param executorFactory Factory supplying new instances of {@link ExecutorService}
     */
    public final void setExecutorFactory(ExecutorFactory executorFactory) {
        this.executorFactory = executorFactory;
    }

    /**
     * Sets a custom executor service factory to use for the async clients. The factory will be
     * called for each async client created through the builder.
     *
     * @param executorFactory Factory supplying new instances of {@link ExecutorService}
     * @return This object for method chaining.
     */
    public final SubclassT withExecutorFactory(ExecutorFactory executorFactory) {
        setExecutorFactory(executorFactory);
        return getSubclass();
    }

    @Override
    public final TypeToBuildT build() {
        return build(getAsyncClientParams());
    }

    protected abstract TypeToBuildT build(AwsAsyncClientParams asyncClientParams);

    /**
     * @return An instance of AwsAsyncClientParams that has all params to be used in the async
     * client constructor.
     */
    protected final AwsAsyncClientParams getAsyncClientParams() {
        return new AsyncBuilderParams(executorFactory);
    }

    /**
     * Presents a view of the builder to be used in the async client constructor.
     */
    protected class AsyncBuilderParams extends SyncBuilderParams {

        private final ScheduledExecutorService executorService;

        protected AsyncBuilderParams(ExecutorFactory executorFactory) {
            this.executorService = Executors.newScheduledThreadPool(5);
        }

        @Override
        public ScheduledExecutorService getExecutor() {
            return this.executorService;
        }

        /**
         * @return Default async Executor to use if none is explicitly provided by user.
         */
        private ExecutorService defaultExecutor() {
            return Executors.newFixedThreadPool(LegacyClientConfiguration.DEFAULT_MAX_CONNECTIONS);
        }
    }

}
