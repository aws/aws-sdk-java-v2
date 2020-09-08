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

package software.amazon.awssdk.core.waiters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;
import software.amazon.awssdk.core.exception.SdkException;

/**
 * Verify the accuracy of {@link WaitersRuntime#DEFAULT_ACCEPTORS}.
 */
public class WaitersRuntimeDefaultAcceptorsTest {
    @Test
    public void defaultAcceptorsFailOnRuntimeExceptionThrowsException() {
        RuntimeException runtimeException = new RuntimeException();
        assertThatThrownBy(() -> WaitersRuntime.DEFAULT_ACCEPTORS.forEach(acceptor -> acceptor.matches(runtimeException)))
            .isEqualTo(runtimeException);
    }

    @Test
    public void defaultAcceptorsFailOnThrowableThrowsSdkException() {
        Throwable throwable = new Throwable();
        assertThatThrownBy(() -> WaitersRuntime.DEFAULT_ACCEPTORS.forEach(acceptor -> acceptor.matches(throwable)))
            .isInstanceOf(SdkException.class)
            .hasCause(throwable);
    }

    @Test
    public void defaultAcceptorsRetryOnUnrecognizedResponse() {
        assertThat(WaitersRuntime.DEFAULT_ACCEPTORS.stream().filter(acceptor -> acceptor.matches(new Object())).findFirst())
            .hasValueSatisfying(v -> assertThat(v.waiterState()).isEqualTo(WaiterState.RETRY));
    }
}