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

package software.amazon.awssdk.core.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.junit.Test;

public class PaginatorUtilsTest {

    @Test
    public void nullOutputToken_shouldReturnFalse() {
       assertFalse(PaginatorUtils.isOutputTokenAvailable(null));
    }

    @Test
    public void nonNullString_shouldReturnTrue() {
        assertTrue(PaginatorUtils.isOutputTokenAvailable("next"));
    }

    @Test
    public void nonNullInteger_shouldReturnTrue() {
        assertTrue(PaginatorUtils.isOutputTokenAvailable(12));
    }

    @Test
    public void emptyCollection_shouldReturnFalse() {
        assertFalse(PaginatorUtils.isOutputTokenAvailable(new ArrayList<>()));
    }

    @Test
    public void nonEmptyCollection_shouldReturnTrue() {
        assertTrue(PaginatorUtils.isOutputTokenAvailable(Arrays.asList("foo", "bar")));
    }

    @Test
    public void emptyMap_shouldReturnFalse() {
        assertFalse(PaginatorUtils.isOutputTokenAvailable(new HashMap<>()));
    }

    @Test
    public void nonEmptyMap_shouldReturnTrue() {
        HashMap<String, String> outputTokens = new HashMap<>();
        outputTokens.put("foo", "bar");
        assertTrue(PaginatorUtils.isOutputTokenAvailable(outputTokens));
    }

    @Test
    public void sdkAutoConstructList_shouldReturnFalse() {
        assertFalse(PaginatorUtils.isOutputTokenAvailable(DefaultSdkAutoConstructList.getInstance()));
    }

    @Test
    public void sdkAutoConstructMap_shouldReturnFalse() {
        assertFalse(PaginatorUtils.isOutputTokenAvailable(DefaultSdkAutoConstructMap.getInstance()));
    }
}
