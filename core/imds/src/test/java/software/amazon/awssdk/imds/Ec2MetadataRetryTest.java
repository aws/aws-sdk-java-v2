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

import static org.assertj.core.api.Assertions.assertThat;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;

/**
 * Unit Tests to test the Ec2MetadataRetryTest functionality.
 */
public class Ec2MetadataRetryTest {

    @Test
    public void verifyEqualsAndHashcode_when_defaultCaseTested(){

        EqualsVerifier.forClass(Ec2MetadataRetryPolicy.class).usingGetClass().verify();
    }

    @Test
    public void verifyToString_when_defaultCaseTested(){

        BackoffStrategy backoffStrategy = BackoffStrategy.defaultStrategy(RetryMode.STANDARD);
        Ec2MetadataRetryPolicy ec2MetadataRetryPolicy = Ec2MetadataRetryPolicy.builder().numRetries(3).backoffStrategy(backoffStrategy).build();
        String output = "Ec2MetadataRetryPolicy{backoffStrategy=FullJitterBackoffStrategy(baseDelay=PT0.1S, maxBackoffTime=PT20S)"
                        + ", numRetries=3}";
        assertThat(ec2MetadataRetryPolicy.toString()).isEqualTo(output);
    }

    @Test
    public void verifyToBuilder_when_defaultCaseTested(){

        BackoffStrategy backoffStrategy = BackoffStrategy.defaultStrategy(RetryMode.STANDARD);
        Ec2MetadataRetryPolicy ec2MetadataRetryPolicy = Ec2MetadataRetryPolicy.builder().numRetries(3).backoffStrategy(backoffStrategy)
                                                                              .build();
        Ec2MetadataRetryPolicy ec2MetadataRetryPolicyNew = ec2MetadataRetryPolicy.toBuilder().numRetries(4).build();
        String output = "Ec2MetadataRetryPolicy{backoffStrategy=FullJitterBackoffStrategy(baseDelay=PT0.1S, maxBackoffTime=PT20S)"
                        + ", numRetries=4}";
        assertThat(ec2MetadataRetryPolicyNew.toString()).isEqualTo(output);
    }
}
