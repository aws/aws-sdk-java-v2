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

package software.amazon.awssdk.utils;


import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

public class ThreadFactoryBuilderTest {

    @Before
    public void setup() {
        ThreadFactoryBuilder.resetPoolNumber();
    }

    @Test
    public void poolNumberWrapsAround() {
        for (int i = 0; i < 9_9999; i++) {
            new ThreadFactoryBuilder().build();
        }
        Thread threadBeforeWrap = new ThreadFactoryBuilder().build().newThread(this::doNothing);
        assertThat(threadBeforeWrap.getName()).isEqualTo("aws-java-sdk-9999-0");

        // Next factory should cause the pool number to wrap
        Thread threadAfterWrap = new ThreadFactoryBuilder().build().newThread(this::doNothing);
        assertThat(threadAfterWrap.getName()).isEqualTo("aws-java-sdk-0-0");
    }

    @Test
    public void customPrefixAppendsPoolNumber() {
        Thread thread = new ThreadFactoryBuilder()
                .threadNamePrefix("custom-name")
                .build()
                .newThread(this::doNothing);
         assertThat(thread.getName()).isEqualTo("custom-name-0-0");
    }

    @Test
    public void daemonThreadRespected() {
        Thread thread = new ThreadFactoryBuilder()
                .daemonThreads(true)
                .build()
                .newThread(this::doNothing);
        assertThat(thread.isDaemon()).isTrue();
    }

    /**
     * To use as a {@link Runnable} method reference.
     */
    private void doNothing() {
    }

}
