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

package software.amazon.awssdk.profiles;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.profiles.internal.ProfileFileRefresher;

/**
 * Encapsulates the logic for supplying either a single or multiple ProfileFile instances.
 * <p>
 * Each call to the {@link #get()} method will result in either a new or previously supplied profile based on the
 * implementation's rules.
 */
@SdkPublicApi
@FunctionalInterface
public interface ProfileFileSupplier extends Supplier<ProfileFile> {

    /**
     * Creates a {@link ProfileFileSupplier} capable of producing multiple profile objects by aggregating the default
     * credentials and configuration files as determined by {@link ProfileFileLocation#credentialsFileLocation()} abd
     * {@link ProfileFileLocation#configurationFileLocation()}. This supplier will return a new ProfileFile instance only once
     * either disk file has been modified. Multiple calls to the supplier while both disk files are unchanged will return the
     * same object.
     *
     * @return Implementation of {@link ProfileFileSupplier} that is capable of supplying a new aggregate profile when either file
     *         has been modified.
     */
    static ProfileFileSupplier defaultSupplier() {
        Optional<ProfileFileSupplier> credentialsSupplierOptional
            = ProfileFileLocation.credentialsFileLocation()
                                 .map(path -> reloadWhenModified(path, ProfileFile.Type.CREDENTIALS));

        Optional<ProfileFileSupplier> configurationSupplierOptional
            = ProfileFileLocation.configurationFileLocation()
                                 .map(path -> reloadWhenModified(path, ProfileFile.Type.CONFIGURATION));

        ProfileFileSupplier supplier = () -> ProfileFile.builder().build();
        if (credentialsSupplierOptional.isPresent() && configurationSupplierOptional.isPresent()) {
            supplier = aggregate(credentialsSupplierOptional.get(), configurationSupplierOptional.get());
        } else if (credentialsSupplierOptional.isPresent()) {
            supplier = credentialsSupplierOptional.get();
        } else if (configurationSupplierOptional.isPresent()) {
            supplier = configurationSupplierOptional.get();
        }

        return supplier;
    }

    /**
     * Creates a {@link ProfileFileSupplier} capable of producing multiple profile objects from a file. This supplier will
     * return a new ProfileFile instance only once the disk file has been modified. Multiple calls to the supplier while the
     * disk file is unchanged will return the same object.
     *
     * @param path Path to the file to read from.
     * @param type The type of file. See {@link ProfileFile.Type} for possible values.
     * @return Implementation of {@link ProfileFileSupplier} that is capable of supplying a new profile when the file
     *         has been modified.
     */
    static ProfileFileSupplier reloadWhenModified(Path path, ProfileFile.Type type) {
        return new ProfileFileSupplier() {

            final ProfileFile.Builder builder = ProfileFile.builder()
                                                           .content(path)
                                                           .type(type);

            final ProfileFileRefresher refresher = ProfileFileRefresher.builder()
                                                                       .profileFile(builder::build)
                                                                       .profileFilePath(path)
                                                                       .build();

            @Override
            public ProfileFile get() {
                return refresher.refreshIfStale();
            }

        };
    }

    /**
     * Creates a {@link ProfileFileSupplier} that produces an existing profile.
     *
     * @param profileFile Profile object to supply.
     * @return Implementation of {@link ProfileFileSupplier} that is capable of supplying a single profile.
     */
    static ProfileFileSupplier fixedProfileFile(ProfileFile profileFile) {
        return () -> profileFile;
    }

    /**
     * Creates a {@link ProfileFileSupplier} by combining the {@link ProfileFile} objects from multiple {@code
     * ProfileFileSupplier}s. Objects are passed into {@link ProfileFile.Aggregator}.
     *
     * @param suppliers Array of {@code ProfileFileSupplier} objects. {@code ProfileFile} objects are passed to
     *                  {@link ProfileFile.Aggregator#addFile(ProfileFile)} in the same argument order as the supplier that
     *                  generated it.
     * @return Implementation of {@link ProfileFileSupplier} aggregating results from the supplier objects.
     */
    static ProfileFileSupplier aggregate(ProfileFileSupplier... suppliers) {

        return new ProfileFileSupplier() {

            final AtomicReference<ProfileFile> currentAggregateProfileFile = new AtomicReference<>();
            final Map<Supplier<ProfileFile>, ProfileFile> currentValuesBySupplier
                = Collections.synchronizedMap(new LinkedHashMap<>());

            @Override
            public ProfileFile get() {
                boolean refreshAggregate = false;
                for (ProfileFileSupplier supplier : suppliers) {
                    if (didSuppliedValueChange(supplier)) {
                        refreshAggregate = true;
                    }
                }

                if (refreshAggregate) {
                    refreshCurrentAggregate();
                }

                return  currentAggregateProfileFile.get();
            }

            private boolean didSuppliedValueChange(Supplier<ProfileFile> supplier) {
                ProfileFile next = supplier.get();
                ProfileFile current = currentValuesBySupplier.put(supplier, next);

                return !Objects.equals(next, current);
            }

            private void refreshCurrentAggregate() {
                ProfileFile.Aggregator aggregator = ProfileFile.aggregator();
                currentValuesBySupplier.values().forEach(aggregator::addFile);
                ProfileFile current = currentAggregateProfileFile.get();
                ProfileFile next = aggregator.build();
                if (!Objects.equals(current, next)) {
                    currentAggregateProfileFile.compareAndSet(current, next);
                }
            }

        };
    }

}
