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

import software.amazon.awssdk.config.SyncClientConfiguration;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Validate;

public class HttpSyncClientDependencies extends HttpClientDependencies {
    private final SyncClientConfiguration syncClientConfiguration;

    private HttpSyncClientDependencies(Builder builder) {
        super(builder.syncClientConfiguration, builder);
        this.syncClientConfiguration = Validate.paramNotNull(builder.syncClientConfiguration, "syncClientConfiguration");
    }

    /**
     * Create a {@link Builder}, used to create a {@link HttpSyncClientDependencies}.
     */
    public static Builder builder() {
        return new Builder();
    }

    public SyncClientConfiguration syncClientConfiguration() {
        return syncClientConfiguration;
    }

    @Override
    public void close() throws Exception {
        super.close();
        IoUtils.closeQuietly(syncClientConfiguration.httpClient(), null);
    }

    public static final class Builder extends HttpClientDependencies.Builder<Builder> {
        private SyncClientConfiguration syncClientConfiguration;

        public Builder syncClientConfiguration(SyncClientConfiguration syncClientConfiguration) {
            this.syncClientConfiguration = syncClientConfiguration;
            return this;
        }

        public HttpSyncClientDependencies build() {
            return new HttpSyncClientDependencies(this);
        }
    }
}
