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

/**
 * Identifies the buffer a request is batched into. Two requests share a batch only when their queue URLs are equal and
 * their override configurations are equal (or both absent). Equality, not the hash code, decides grouping, so two
 * different override configurations whose hash codes collide never share a batch.
 */
@SdkInternalApi
public final class SqsBatchKey {

    private final String queueUrl;
    private final AwsRequestOverrideConfiguration overrideConfiguration;

    private SqsBatchKey(String queueUrl, AwsRequestOverrideConfiguration overrideConfiguration) {
        this.queueUrl = queueUrl;
        this.overrideConfiguration = overrideConfiguration;
    }

    public static SqsBatchKey create(String queueUrl, Optional<AwsRequestOverrideConfiguration> overrideConfiguration) {
        return new SqsBatchKey(queueUrl, overrideConfiguration.orElse(null));
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
        SqsBatchKey that = (SqsBatchKey) o;
        return Objects.equals(queueUrl, that.queueUrl)
               && Objects.equals(overrideConfiguration, that.overrideConfiguration);
    }

    @Override
    public int hashCode() {
        int hashCode = Objects.hashCode(queueUrl);
        hashCode = 31 * hashCode + Objects.hashCode(overrideConfiguration);
        return hashCode;
    }

    @Override
    public String toString() {
        return ToString.builder("SqsBatchKey")
                       .add("queueUrl", queueUrl)
                       .add("hasOverrideConfiguration", overrideConfiguration != null)
                       .build();
    }
}
