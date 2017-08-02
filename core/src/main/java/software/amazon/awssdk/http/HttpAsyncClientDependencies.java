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

import software.amazon.awssdk.config.AsyncClientConfiguration;
import software.amazon.awssdk.utils.IoUtils;

public class HttpAsyncClientDependencies extends HttpClientDependencies {
    private final AsyncClientConfiguration asyncClientConfiguration;

    private HttpAsyncClientDependencies(Builder builder) {
        super(builder.asyncClientConfiguration, builder);
        this.asyncClientConfiguration = builder.asyncClientConfiguration;
    }

    /**
     * Create a {@link Builder}, used to create a {@link HttpAsyncClientDependencies}.
     */
    public static Builder builder() {
        return new Builder();
    }

    public AsyncClientConfiguration asyncClientConfiguration() {
        return asyncClientConfiguration;
    }

    @Override
    public void close() throws Exception {
        super.close();
        IoUtils.closeQuietly(asyncClientConfiguration.asyncHttpClient(), null);
        asyncClientConfiguration.asyncExecutorService().shutdown();
    }

    public static final class Builder extends HttpClientDependencies.Builder<Builder> {
        private AsyncClientConfiguration asyncClientConfiguration;

        public Builder asyncClientConfiguration(AsyncClientConfiguration asyncClientConfiguration) {
            this.asyncClientConfiguration = asyncClientConfiguration;
            return this;
        }

        public HttpAsyncClientDependencies build() {
            return new HttpAsyncClientDependencies(this);
        }
    }
}
