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

package software.amazon.awssdk.core.internal.util;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import org.junit.Test;

public class AwsDateUtilsTest {
    /**
     * Tests the Date marshalling and unmarshalling. Asserts that the value is
     * same before and after marshalling/unmarshalling
     */
    @Test
    public void testAwsFormatDateUtils() throws Exception {
        testInstant(System.currentTimeMillis());
        testInstant(1L);
        testInstant(0L);
    }

    private void testInstant(long dateInMilliSeconds) {
        Instant instant = Instant.ofEpochMilli(dateInMilliSeconds);
        String serverSpecificDateFormat = AwsDateUtils.formatServiceSpecificDate(instant);
        Instant parsed = AwsDateUtils.parseServiceSpecificInstant(String.valueOf(serverSpecificDateFormat));

        assertEquals(instant, parsed);
    }
}
