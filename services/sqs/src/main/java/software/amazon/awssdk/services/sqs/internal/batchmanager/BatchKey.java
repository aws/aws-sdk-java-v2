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

package software.amazon.awssdk.services.sqs.internal.batchmanager;

import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;

/**
 * Identifies one batching buffer: requests may only be coalesced into the same batch API call when their batch keys are
 * {@link #equals(Object)}.
 *
 * <p>Grouping is by the queue URL together with {@link AwsRequestOverrideConfiguration#equals(Object)} - never by the
 * configuration's {@code hashCode()}, which is a lossy 32-bit digest: two configurations that are not equal but whose
 * hash codes collide must not share a buffer, because a batch is sent with a single configuration (and therefore a
 * single set of credentials, signer, endpoint provider and plugins) for every request in it.
 *
 * <p>The queue URL and the override configuration carried by this key - not any individual buffered request - are what
 * the batch call is dispatched with.
 */
@SdkInternalApi
public final class BatchKey {

    private final String queueUrl;
    private final AwsRequestOverrideConfiguration overrideConfiguration;
    private final int hash;

    private BatchKey(String queueUrl, AwsRequestOverrideConfiguration overrideConfiguration) {
        this.queueUrl = Validate.paramNotNull(queueUrl, "queueUrl");
        this.overrideConfiguration = overrideConfiguration;
        this.hash = 31 * (31 + this.queueUrl.hashCode()) + Objects.hashCode(overrideConfiguration);
    }

    /**
     * Creates a batch key for the given queue URL and request override configuration. A null {@code overrideConfiguration}
     * means "this request carries no override configuration"; it is deliberately not equal to a key holding an empty
     * configuration, since the two are not interchangeable when the batch is signed and dispatched.
     */
    public static BatchKey create(String queueUrl, AwsRequestOverrideConfiguration overrideConfiguration) {
        return new BatchKey(queueUrl, overrideConfiguration);
    }

    public String queueUrl() {
        return queueUrl;
    }

    public Optional<AwsRequestOverrideConfiguration> overrideConfiguration() {
        return Optional.ofNullable(overrideConfiguration);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BatchKey that = (BatchKey) o;

        if (!queueUrl.equals(that.queueUrl)) {
            return false;
        }
        return Objects.equals(overrideConfiguration, that.overrideConfiguration);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        // The override configuration itself must never be printed: it may hold credentials providers.
        return ToString.builder("BatchKey")
                       .add("queueUrl", queueUrl)
                       .add("hasOverrideConfiguration", overrideConfiguration != null)
                       .build();
    }
}
