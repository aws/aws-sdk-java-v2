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

package software.amazon.awssdk.core.internal.http.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ClasspathHttpWarmupInvokerTest {

    @Test
    void invokeAll_whenMultipleWarmers_invokesEachOnce() {
        CountingWarmer first = new CountingWarmer();
        CountingWarmer second = new CountingWarmer();

        new ClasspathHttpWarmupInvoker(Arrays.asList(first, second)).invokeAll();

        assertThat(first.invocations()).isEqualTo(1);
        assertThat(second.invocations()).isEqualTo(1);
    }

    @Test
    void invokeAll_whenNoWarmers_isNoOp() {
        assertThatCode(() -> new ClasspathHttpWarmupInvoker(Collections.emptyList()).invokeAll())
            .doesNotThrowAnyException();
    }

    private static final class CountingWarmer implements HttpClientWarmer {
        private final AtomicInteger invocations = new AtomicInteger();

        @Override
        public void warmAll() {
            invocations.incrementAndGet();
        }

        int invocations() {
            return invocations.get();
        }
    }
}
