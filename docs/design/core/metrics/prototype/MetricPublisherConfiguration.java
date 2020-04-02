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

package software.amazon.awssdk.metrics.publisher;

import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configure the options to publish the metrics.
 * <p>
 * By default, SDK creates and uses only CloudWatch publisher with default options (Default credential chain
 * and region chain).
 * To use CloudWatch publisher with custom options or any other publishers, create a
 * #PublisherConfiguration object and set it in the ClientOverrideConfiguration on the client.
 * </p>
 *
 * <p>
 *     SDK exposes the CloudWatch and CSM publisher implementation, so instances of these classes with
 *     different configuration can be set in this class.
 * </p>
 */
public final class MetricPublisherConfiguration implements
                                    ToCopyableBuilder<MetricPublisherConfiguration.Builder, MetricPublisherConfiguration> {

    private final List<MetricPublisher> publishers = Collections.emptyList();

    public MetricPublisherConfiguration(Builder builder) {
        this.publishers.addAll(builder.publishers);
    }

    /**
     * @return the list of #MetricPublisher to be used for publishing the metrics
     */
    public List<MetricPublisher> publishers() {
        return publishers;
    }

    /**
     * @return a {@link Builder} object to construct a PublisherConfiguration instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return new Builder();
    }

    public static final class Builder implements CopyableBuilder<Builder, MetricPublisherConfiguration> {

        private final List<MetricPublisher> publishers = Collections.emptyList();

        private Builder() {
        }

        /**
         * Sets the list of publishers used for publishing the metrics.
         */
        public Builder publishers(List<MetricPublisher> publishers) {
            this.publishers.addAll(publishers);
            return this;
        }

        /**
         * Add a publisher to the list of publishers used for publishing the metrics.
         */
        public Builder addPublisher(MetricPublisher publisher) {
            this.publishers.add(publisher);
            return this;
        }

        public MetricPublisherConfiguration build() {
            return new MetricPublisherConfiguration(this);
        }
    }
}
