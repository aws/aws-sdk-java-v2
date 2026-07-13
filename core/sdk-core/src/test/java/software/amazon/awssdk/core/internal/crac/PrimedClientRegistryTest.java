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
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PrimedClientRegistry}. Each test uses a fresh instance, so the dedup state is isolated without
 * any static reset hook or reflection.
 */
class PrimedClientRegistryTest {

    private static final String SERVICE1_SYNC = "software.amazon.awssdk.services.service1.Service1Client";
    private static final String SERVICE1_ASYNC = "software.amazon.awssdk.services.service1.Service1AsyncClient";

    @Test
    void selectUnprimed_freshRegistry_returnsAllRequested() {
        PrimedClientRegistry registry = new PrimedClientRegistry();

        assertThat(registry.selectUnprimed(Arrays.asList(SERVICE1_SYNC, SERVICE1_ASYNC)))
            .containsExactly(SERVICE1_SYNC, SERVICE1_ASYNC);
    }

    @Test
    void selectUnprimed_afterMarkPrimed_skipsAlreadyPrimed() {
        PrimedClientRegistry registry = new PrimedClientRegistry();
        registry.markPrimed(Collections.singletonList(SERVICE1_SYNC));

        assertThat(registry.selectUnprimed(Arrays.asList(SERVICE1_SYNC, SERVICE1_ASYNC)))
            .containsExactly(SERVICE1_ASYNC);
    }

    @Test
    void selectUnprimed_allAlreadyPrimed_returnsEmpty() {
        PrimedClientRegistry registry = new PrimedClientRegistry();
        registry.markPrimed(Arrays.asList(SERVICE1_SYNC, SERVICE1_ASYNC));

        assertThat(registry.selectUnprimed(Arrays.asList(SERVICE1_SYNC, SERVICE1_ASYNC))).isEmpty();
    }

    @Test
    void selectUnprimed_preservesEncounterOrderAndDeduplicates() {
        PrimedClientRegistry registry = new PrimedClientRegistry();

        assertThat(registry.selectUnprimed(Arrays.asList(SERVICE1_ASYNC, SERVICE1_SYNC, SERVICE1_ASYNC)))
            .containsExactly(SERVICE1_ASYNC, SERVICE1_SYNC);
    }

    @Test
    void selectUnprimed_ignoresNullNames() {
        PrimedClientRegistry registry = new PrimedClientRegistry();

        assertThat(registry.selectUnprimed(Arrays.asList(SERVICE1_SYNC, null, SERVICE1_ASYNC)))
            .containsExactly(SERVICE1_SYNC, SERVICE1_ASYNC);
    }

    @Test
    void selectUnprimed_emptyInput_returnsEmpty() {
        PrimedClientRegistry registry = new PrimedClientRegistry();

        assertThat(registry.selectUnprimed(Collections.emptyList())).isEmpty();
    }

    @Test
    void separateInstances_doNotShareState() {
        PrimedClientRegistry first = new PrimedClientRegistry();
        first.markPrimed(Collections.singletonList(SERVICE1_SYNC));

        PrimedClientRegistry second = new PrimedClientRegistry();

        assertThat(second.selectUnprimed(Collections.singletonList(SERVICE1_SYNC)))
            .containsExactly(SERVICE1_SYNC);
    }
}
