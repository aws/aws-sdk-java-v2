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

import static org.assertj.core.api.Assertions.assertThat;
import java.util.HashMap;
import org.junit.Test;

public class DefaultSdkAutoConstructMapTest {
    private static final DefaultSdkAutoConstructMap<String, String> AUTO_CONSTRUCT_MAP = DefaultSdkAutoConstructMap.getInstance();

    @Test
    public void equal_emptyMap() {
        assertThat(AUTO_CONSTRUCT_MAP.equals(new HashMap<>())).isTrue();
    }

    @Test
    public void hashCode_sameAsEmptyMap() {
        assertThat(AUTO_CONSTRUCT_MAP.hashCode()).isEqualTo(new HashMap<>().hashCode());

        // The hashCode is defined by the Map interface to be the hashCodes of
        // all the entries in the Map, so this should be 0.
        assertThat(AUTO_CONSTRUCT_MAP.hashCode()).isEqualTo(0);
    }

    @Test
    public void toString_emptyMap() {
        assertThat(AUTO_CONSTRUCT_MAP.toString()).isEqualTo("{}");
    }
}
