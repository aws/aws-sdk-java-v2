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

package software.amazon.awssdk.profiles.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSupplier;

/**
 * A {@link ProfileFileSupplier} that combines the {@link ProfileFile} objects from multiple
 * {@code ProfileFileSupplier}s. Objects are passed into {@link ProfileFile.Aggregator}.
 */
@SdkInternalApi
public class AggregateProfileFileSupplier implements  ProfileFileSupplier {
    final List<ProfileFileSupplier> suppliers;

    // supplier values and the resulting aggregate must always be updated atomically together
    final AtomicReference<SupplierState> state =
        new AtomicReference<>(new SupplierState(Collections.emptyMap(), null));

    public AggregateProfileFileSupplier(ProfileFileSupplier... suppliers) {
        this.suppliers = Collections.unmodifiableList(Arrays.asList(suppliers));
    }

    @Override
    public ProfileFile get() {
        SupplierState currentState = state.get();
        Map<Supplier<ProfileFile>, ProfileFile> currentValues = currentState.values;
        Map<Supplier<ProfileFile>, ProfileFile> changedValues = changedSupplierValues(currentValues);

        if (changedValues == null) {
            // no suppliers have changed values, return the current aggregate
            return currentState.aggregate;
        }

        // one or more supplier values have changed, we need to update the aggregate (and the state)
        // the order of the suppliers matters so we MUST preserve it using LinkedHashMap with insertion ordering
        Map<Supplier<ProfileFile>, ProfileFile> nextValues = new LinkedHashMap<>(currentValues);
        nextValues.putAll(changedValues);

        ProfileFile.Aggregator aggregator = ProfileFile.aggregator();
        nextValues.values().forEach(aggregator::addFile);
        ProfileFile nextAggregate = aggregator.build();

        SupplierState nextState = new SupplierState(nextValues, nextAggregate);
        if (state.compareAndSet(currentState, nextState)) {
            return nextAggregate;
        }
        // else: another thread has modified the state in between, assume it is up to date and use the new state
        return state.get().aggregate;
    }

    // return the suppliers with changed values.  Returns null if no values have changed
    private Map<Supplier<ProfileFile>, ProfileFile> changedSupplierValues(Map<Supplier<ProfileFile>, ProfileFile> currentValues) {
        Map<Supplier<ProfileFile>, ProfileFile> changedValues = null;
        for (ProfileFileSupplier supplier : suppliers) {
            ProfileFile next = supplier.get();
            ProfileFile prev = currentValues.get(supplier);
            // we ONLY care about if the reference has changed, we don't care about object equality here
            if (prev != next) {
                if (changedValues == null) {
                    // changed values must also preserve supplier order
                    changedValues = new LinkedHashMap<>();
                }
                changedValues.put(supplier, next);
            }
        }
        return changedValues;
    }

    private static final class SupplierState {
        final Map<Supplier<ProfileFile>, ProfileFile> values;
        final ProfileFile aggregate;

        private SupplierState(Map<Supplier<ProfileFile>, ProfileFile> values, ProfileFile aggregate) {
            this.values = values;
            this.aggregate = aggregate;
        }
    }
}
