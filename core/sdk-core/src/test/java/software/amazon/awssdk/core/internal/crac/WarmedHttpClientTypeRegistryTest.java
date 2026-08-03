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

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ClientType;

/**
 * Unit tests for {@link WarmedHttpClientTypeRegistry}.
 */
class WarmedHttpClientTypeRegistryTest {

    @Test
    void isWarmed_freshRegistry_returnsFalseForAllTypes() {
        WarmedHttpClientTypeRegistry registry = new WarmedHttpClientTypeRegistry();

        assertThat(registry.isWarmed(ClientType.SYNC)).isFalse();
        assertThat(registry.isWarmed(ClientType.ASYNC)).isFalse();
    }

    @Test
    void isWarmed_afterMarkingSync_asyncStaysUnwarmed() {
        WarmedHttpClientTypeRegistry registry = new WarmedHttpClientTypeRegistry();
        registry.markWarmed(ClientType.SYNC);

        assertThat(registry.isWarmed(ClientType.SYNC)).isTrue();
        assertThat(registry.isWarmed(ClientType.ASYNC)).isFalse();
    }

    @Test
    void isWarmed_afterMarkingBoth_returnsTrueForEach() {
        WarmedHttpClientTypeRegistry registry = new WarmedHttpClientTypeRegistry();
        registry.markWarmed(ClientType.SYNC);
        registry.markWarmed(ClientType.ASYNC);

        assertThat(registry.isWarmed(ClientType.SYNC)).isTrue();
        assertThat(registry.isWarmed(ClientType.ASYNC)).isTrue();
    }

    @Test
    void unknownType_isNeverWarmedAndMarkingIsNoOp() {
        WarmedHttpClientTypeRegistry registry = new WarmedHttpClientTypeRegistry();
        registry.markWarmed(ClientType.UNKNOWN);

        assertThat(registry.isWarmed(ClientType.UNKNOWN)).isFalse();
        assertThat(registry.isWarmed(ClientType.SYNC)).isFalse();
        assertThat(registry.isWarmed(ClientType.ASYNC)).isFalse();
    }
}
