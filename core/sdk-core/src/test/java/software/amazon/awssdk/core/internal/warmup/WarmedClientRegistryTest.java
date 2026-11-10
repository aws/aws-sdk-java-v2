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

package software.amazon.awssdk.core.internal.warmup;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WarmedClientRegistry}. Each test uses a fresh instance, so the dedup state is isolated without
 * any static reset hook or reflection.
 */
class WarmedClientRegistryTest {

    private static final String SERVICE1_SYNC = "software.amazon.awssdk.services.service1.Service1Client";
    private static final String SERVICE1_ASYNC = "software.amazon.awssdk.services.service1.Service1AsyncClient";

    @Test
    void selectUnwarmed_freshRegistry_returnsAllRequested() {
        WarmedClientRegistry registry = new WarmedClientRegistry();

        assertThat(registry.selectUnwarmed(Arrays.asList(SERVICE1_SYNC, SERVICE1_ASYNC)))
            .containsExactly(SERVICE1_SYNC, SERVICE1_ASYNC);
    }

    @Test
    void selectUnwarmed_afterMarkWarmed_skipsAlreadyWarmed() {
        WarmedClientRegistry registry = new WarmedClientRegistry();
        registry.markWarmed(Collections.singletonList(SERVICE1_SYNC));

        assertThat(registry.selectUnwarmed(Arrays.asList(SERVICE1_SYNC, SERVICE1_ASYNC)))
            .containsExactly(SERVICE1_ASYNC);
    }

    @Test
    void selectUnwarmed_allAlreadyWarmed_returnsEmpty() {
        WarmedClientRegistry registry = new WarmedClientRegistry();
        registry.markWarmed(Arrays.asList(SERVICE1_SYNC, SERVICE1_ASYNC));

        assertThat(registry.selectUnwarmed(Arrays.asList(SERVICE1_SYNC, SERVICE1_ASYNC))).isEmpty();
    }

    @Test
    void selectUnwarmed_preservesEncounterOrderAndDeduplicates() {
        WarmedClientRegistry registry = new WarmedClientRegistry();

        assertThat(registry.selectUnwarmed(Arrays.asList(SERVICE1_ASYNC, SERVICE1_SYNC, SERVICE1_ASYNC)))
            .containsExactly(SERVICE1_ASYNC, SERVICE1_SYNC);
    }

    @Test
    void selectUnwarmed_ignoresNullNames() {
        WarmedClientRegistry registry = new WarmedClientRegistry();

        assertThat(registry.selectUnwarmed(Arrays.asList(SERVICE1_SYNC, null, SERVICE1_ASYNC)))
            .containsExactly(SERVICE1_SYNC, SERVICE1_ASYNC);
    }

    @Test
    void selectUnwarmed_emptyInput_returnsEmpty() {
        WarmedClientRegistry registry = new WarmedClientRegistry();

        assertThat(registry.selectUnwarmed(Collections.emptyList())).isEmpty();
    }

    @Test
    void separateInstances_doNotShareState() {
        WarmedClientRegistry first = new WarmedClientRegistry();
        first.markWarmed(Collections.singletonList(SERVICE1_SYNC));

        WarmedClientRegistry second = new WarmedClientRegistry();

        assertThat(second.selectUnwarmed(Collections.singletonList(SERVICE1_SYNC)))
            .containsExactly(SERVICE1_SYNC);
    }
}
