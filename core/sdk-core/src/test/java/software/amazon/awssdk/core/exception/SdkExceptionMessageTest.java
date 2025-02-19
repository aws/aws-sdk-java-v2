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

package software.amazon.awssdk.core.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Verifies the ways in which a message in an {@link SdkException} can be populated.
 */
public class SdkExceptionMessageTest {
    @Test
    public void noMessage_noCause_implies_noMessage() {
        assertThat(SdkException.builder().build().getMessage()).isEqualTo(null);
    }

    @Test
    public void message_noCause_implies_messageFromMessage() {
        assertThat(SdkException.builder().message("foo").build().getMessage()).isEqualTo("foo");
    }

    @Test
    public void message_cause_implies_messageFromMessage() {
        assertThat(SdkException.builder().message("foo").cause(new Exception("bar")).build().getMessage()).isEqualTo("foo");
    }

    @Test
    public void noMessage_causeWithoutMessage_implies_noMessage() {
        assertThat(SdkException.builder().cause(new Exception()).build().getMessage()).isEqualTo(null);
    }

    @Test
    public void noMessage_causeWithMessage_implies_messageFromCause() {
        assertThat(SdkException.builder().cause(new Exception("bar")).build().getMessage()).isEqualTo("bar");
    }

    @Test
    public void numAttempts_WithExplicitAttemptCount_ReturnsOneBased() {
        assertThat(SdkException.builder().message("foo").numAttempts(2).build().numAttempts()).isEqualTo(2);
    }

    @Test
    public void toBuilder_CopiesException_PreservesAttemptCount() {
        SdkException original = SdkException.builder().numAttempts(2).build();
        SdkException copy = original.toBuilder().build();
        assertThat(copy.numAttempts()).isEqualTo(original.numAttempts());
    }

    @Test
    public void create_WithoutAttemptCount_DefaultsToNull() {
        SdkException exception = SdkException.builder().message("message").cause(new RuntimeException()).build();
        assertThat(exception.numAttempts()).isEqualTo(null);
    }
}