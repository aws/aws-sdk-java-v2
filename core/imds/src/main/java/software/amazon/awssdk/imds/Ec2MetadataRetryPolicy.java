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

package software.amazon.awssdk.imds;

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Interface for specifying a retry policy to use when evaluating whether or not a request should be retried , and the gap
 * between each retry. The {@link #builder()} can be used to construct a retry policy with numRetries and backoffStrategy.
 * <p>
 * When using the {@link #builder()} the SDK will use default values for fields that are not provided.A custom BackoffStrategy
 * can be used to construct a policy or a default {@link BackoffStrategy} is used.
 *
 * @see BackoffStrategy for a list of SDK provided backoff strategies
 */
@SdkPublicApi
public class Ec2MetadataRetryPolicy implements ToCopyableBuilder<Ec2MetadataRetryPolicy.Builder, Ec2MetadataRetryPolicy> {

    private final BackoffStrategy backoffStrategy;
    private final int numRetries;

    private Ec2MetadataRetryPolicy(BuilderImpl builder) {

        this.numRetries = builder.numRetries != null ? builder.numRetries : 3;

        this.backoffStrategy = builder.backoffStrategy != null ? builder.backoffStrategy :
                               BackoffStrategy.defaultStrategy(RetryMode.STANDARD);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Ec2MetadataRetryPolicy ec2MetadataRetryPolicy = (Ec2MetadataRetryPolicy) obj;

        if (numRetries != ec2MetadataRetryPolicy.numRetries) {
            return false;
        }
        return Objects.equals(backoffStrategy, ec2MetadataRetryPolicy.backoffStrategy);
    }

    @Override
    public int hashCode() {

        int result = Math.max(numRetries, 0);
        result = 31 * result + (backoffStrategy != null ? backoffStrategy.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Ec2MetadataRetryPolicy{" +
               "backoffStrategy=" + backoffStrategy.toString() +
               ", numRetries=" + numRetries +
               '}';
    }

    /**
     * Method to return the number of retries allowed.
     * @return The number of retries allowed.
     */
    public int numRetries() {
        return numRetries;
    }

    /**
     * Method to return the BackoffStrategy used.
     * @return The backoff Strategy used.
     */
    public BackoffStrategy backoffStrategy() {
        return backoffStrategy;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    @Override
    public Builder toBuilder() {
        return builder().numRetries(numRetries)
                        .backoffStrategy(backoffStrategy);
    }

    public interface Builder extends CopyableBuilder<Ec2MetadataRetryPolicy.Builder, Ec2MetadataRetryPolicy> {

        /**
         * Configure the backoff strategy that should be used for waiting in between retry attempts.
         */
        Builder backoffStrategy(BackoffStrategy backoffStrategy);

        /**
         * Configure the maximum number of times that a single request should be retried, assuming it fails for a retryable error.
         */
        Builder numRetries(Integer numRetries);

        @Override
        Ec2MetadataRetryPolicy build();
    }

    private static final class BuilderImpl implements Builder {

        private Integer numRetries;
        private BackoffStrategy backoffStrategy;

        private BuilderImpl() {
        }

        @Override
        public Builder numRetries(Integer numRetries) {
            this.numRetries = numRetries;
            return this;
        }

        public void setNumRetries(Integer numRetries) {
            numRetries(numRetries);
        }

        @Override
        public Builder backoffStrategy(BackoffStrategy backoffStrategy) {
            this.backoffStrategy = backoffStrategy;
            return this;
        }

        public void setBackoffStrategy(BackoffStrategy backoffStrategy) {
            backoffStrategy(backoffStrategy);
        }

        @Override
        public Ec2MetadataRetryPolicy build() {
            return new Ec2MetadataRetryPolicy(this);
        }
    }
}