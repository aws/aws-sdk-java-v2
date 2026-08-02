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

package software.amazon.awssdk.core.internal.crac;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ClientType;

/**
 * Unit tests for {@link WarmedHttpClientRegistry}.
 */
class WarmedHttpClientRegistryTest {

    @Test
    void selectUnwarmed_whenRepeated_returnsTypesOnlyOnce() {
        WarmedHttpClientRegistry registry = new WarmedHttpClientRegistry();
        List<ClientType> requested = Arrays.asList(ClientType.SYNC, ClientType.ASYNC);

        // First call selects both types; the caller warms them and marks them warmed.
        Set<ClientType> firstCall = registry.selectUnwarmed(requested);
        assertThat(firstCall).containsExactlyInAnyOrder(ClientType.SYNC, ClientType.ASYNC);
        registry.markWarmed(firstCall);

        // Repeat call selects nothing, so the caller warms nothing.
        assertThat(registry.selectUnwarmed(requested)).isEmpty();
    }

    @Test
    void selectUnwarmed_whenOnlySyncWarmed_stillReturnsAsync() {
        WarmedHttpClientRegistry registry = new WarmedHttpClientRegistry();
        registry.markWarmed(Collections.singletonList(ClientType.SYNC));

        assertThat(registry.selectUnwarmed(Arrays.asList(ClientType.SYNC, ClientType.ASYNC)))
            .containsExactly(ClientType.ASYNC);
    }
}
