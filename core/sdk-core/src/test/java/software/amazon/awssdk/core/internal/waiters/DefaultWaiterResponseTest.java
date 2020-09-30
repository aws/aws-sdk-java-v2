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

package software.amazon.awssdk.core.internal.waiters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.testng.annotations.Test;
import software.amazon.awssdk.core.waiters.WaiterResponse;

public class DefaultWaiterResponseTest {
    @Test
    public void bothResponseAndExceptionPresent_shouldThrowException() {
        assertThatThrownBy(() -> DefaultWaiterResponse.<String>builder().exception(new RuntimeException(""))
                                                                        .response("foobar")
                                                                        .attemptsExecuted(1)
                                                                        .build()).hasMessageContaining("mutually exclusive");
    }

    @Test
    public void missingAttemptsExecuted_shouldThrowException() {
        assertThatThrownBy(() -> DefaultWaiterResponse.<String>builder().response("foobar")
                                                                        .build()).hasMessageContaining("attemptsExecuted");
    }

    @Test
    public void equalsHashcode() {
        WaiterResponse<String> response1 = DefaultWaiterResponse.<String>builder()
            .response("response")
            .attemptsExecuted(1)
            .build();

        WaiterResponse<String> response2 = DefaultWaiterResponse.<String>builder()
            .response("response")
            .attemptsExecuted(1)
            .build();

        WaiterResponse<String> response3 = DefaultWaiterResponse.<String>builder()
            .exception(new RuntimeException("test"))
            .attemptsExecuted(1)
            .build();

        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
        assertNotEquals(response1.hashCode(), response3.hashCode());
        assertNotEquals(response1, response3);
    }

    @Test
    public void exceptionPresent() {
        RuntimeException exception = new RuntimeException("test");
        WaiterResponse<String> response = DefaultWaiterResponse.<String>builder()
            .exception(exception)
            .attemptsExecuted(1)
            .build();

        assertThat(response.matched().exception()).contains(exception);
        assertThat(response.matched().response()).isEmpty();
        assertThat(response.attemptsExecuted()).isEqualTo(1);
    }

    @Test
    public void responsePresent() {
        WaiterResponse<String> response = DefaultWaiterResponse.<String>builder()
            .response("helloworld")
            .attemptsExecuted(2)
            .build();


        assertThat(response.matched().response()).contains("helloworld");
        assertThat(response.matched().exception()).isEmpty();
        assertThat(response.attemptsExecuted()).isEqualTo(2);
    }
}
