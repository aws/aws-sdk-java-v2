/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.auth;

import java.util.Date;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Clock interface to prevent static coupling to {@link System#currentTimeMillis()}.
 */
@SdkProtectedApi
public interface SdkClock {

    /**
     * Standard implementation that calls out to {@link System#currentTimeMillis()}. Used in production code.
     */
    SdkClock STANDARD = new SdkClock() {
        @Override
        public long currentTimeMillis() {
            return System.currentTimeMillis();
        }
    };

    long currentTimeMillis();

    /**
     * Mock implementation used in tests.
     */
    final class MockClock implements SdkClock {
        private final long mockedTime;

        public MockClock(Date mockedTime) {
            this(mockedTime.getTime());
        }

        public MockClock(long mockedTime) {
            this.mockedTime = mockedTime;
        }

        @Override
        public long currentTimeMillis() {
            return mockedTime;
        }
    }
}
