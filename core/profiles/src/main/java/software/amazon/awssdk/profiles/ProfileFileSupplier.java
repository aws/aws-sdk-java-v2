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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.profiles.internal.ProfileFileRefresher;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Encapsulates the logic for supplying either a single or multiple ProfileFile instances.
 * <p>
 * Each call to the {@link #get()} method will result in either a new or previously supplied profile based on the
 * implementation's rules.
 */
@SdkPublicApi
@FunctionalInterface
public interface ProfileFileSupplier extends Supplier<ProfileFile>, SdkAutoCloseable {

    @Override
    default void close() {
    }

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
    static ProfileFileSupplier reloadDefaultPathsWhenModified() {
        Optional<Path> credentialsFileLocation = ProfileFileLocation.credentialsFileLocation();
        Optional<Path> configurationFileLocation = ProfileFileLocation.configurationFileLocation();

        ProfileFileSupplier supplier = () -> null;
        if (credentialsFileLocation.isPresent() && configurationFileLocation.isPresent()) {
            supplier = reloadWhenModified(credentialsFileLocation.get(), configurationFileLocation.get());
        } else if (credentialsFileLocation.isPresent()) {
            supplier = reloadWhenModified(credentialsFileLocation.get(), ProfileFile.Type.CREDENTIALS);
        } else if (configurationFileLocation.isPresent()) {
            supplier = reloadWhenModified(configurationFileLocation.get(), ProfileFile.Type.CONFIGURATION);
        }

        return supplier;
    }

    /**
     * Creates a {@link ProfileFileSupplier} capable of producing multiple profile objects by aggregating two files. This
     * supplier will return a new ProfileFile instance only once either disk file has been modified. Multiple calls to the
     * supplier while both disk files are unchanged will return the same object.
     *
     * @param credentialsFilePath Path to the credentials file to read from.
     * @param configFilePath Path to the configuration file to read from.
     * @return Implementation of {@link ProfileFileSupplier} that is capable of supplying a new aggregate profile when either file
     *         has been modified.
     */
    static ProfileFileSupplier reloadWhenModified(Path credentialsFilePath, Path configFilePath) {
        return aggregate(reloadWhenModified(credentialsFilePath, ProfileFile.Type.CREDENTIALS),
                         reloadWhenModified(configFilePath, ProfileFile.Type.CONFIGURATION));
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

            @Override
            public void close() {
                refresher.close();
            }
        };
    }

    /**
     * Creates a {@link ProfileFileSupplier} capable of producing a single profile object by aggregating two files.
     *
     * @param credentialsFilePath Path to the credentials file to read from.
     * @param configFilePath Path to the configuration file to read from.
     * @return Implementation of {@link ProfileFileSupplier} that is capable of supplying a single aggregate profile.
     */
    static ProfileFileSupplier fixedProfileFile(Path credentialsFilePath, Path configFilePath) {
        ProfileFile credentialsProfileFile = ProfileFile.builder()
                                                        .content(credentialsFilePath)
                                                        .type(ProfileFile.Type.CREDENTIALS)
                                                        .build();

        ProfileFile configProfileFile = ProfileFile.builder()
                                                   .content(configFilePath)
                                                   .type(ProfileFile.Type.CONFIGURATION)
                                                   .build();

        ProfileFile aggregateProfileFile = ProfileFile.aggregator()
                                                      .addFile(credentialsProfileFile)
                                                      .addFile(configProfileFile)
                                                      .build();

        return () -> aggregateProfileFile;
    }

    /**
     * Creates a {@link ProfileFileSupplier} capable of producing a single profile object from a file.
     *
     * @param path Path to the file to read from.
     * @param type The type of file. See {@link ProfileFile.Type} for possible values.
     * @return Implementation of {@link ProfileFileSupplier} that is capable of supplying a single profile.
     */
    static ProfileFileSupplier fixedProfileFile(Path path, ProfileFile.Type type) {
        ProfileFile profileFile = ProfileFile.builder()
                                             .content(path)
                                             .type(type)
                                             .build();

        return () -> profileFile;
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
     * Creates a {@link ProfileFileSupplier} that produces an existing non-null profile. If the given profile
     * is null, then the created supplier will also be null.
     *
     * @param profileFile Profile object to supply.
     * @return Implementation of {@link ProfileFileSupplier} that is capable of supplying a single profile.
     */
    static ProfileFileSupplier wrapIntoNullableSupplier(ProfileFile profileFile) {
        return Objects.nonNull(profileFile) ? () -> profileFile : null;
    }

    /**
     * Creates a {@link ProfileFileSupplier} by combining the {@link ProfileFile} objects from two {@code ProfileFileSupplier}s.
     * Objects are passed into {@link ProfileFile.Aggregator}.
     *
     * @param suppliers Array of {@code ProfileFileSupplier} objects. {@code ProfileFile} objects are passed to
     *                  {@link ProfileFile.Aggregator#addFile(ProfileFile)} in the same argument order as the supplier that
     *                  generated it.
     * @return Implementation of {@link ProfileFileSupplier} aggregating results from the supplier objects.
     */
    static ProfileFileSupplier aggregate(ProfileFileSupplier... suppliers) {

        return new ProfileFileSupplier() {

            final ConcurrentHashMap<ProfileFileSupplier, ProfileFile> currentValuesBySupplier = new ConcurrentHashMap<>();

            @Override
            public ProfileFile get() {
                boolean refreshAggregate = false;
                ProfileFile.Aggregator aggregator = ProfileFile.aggregator();
                for (ProfileFileSupplier supplier : suppliers) {
                    if (updateCurrentValue(supplier, aggregator::addFile)) {
                        refreshAggregate = true;
                    }
                }

                if (refreshAggregate) {
                    refreshAggregate(aggregator);
                }

                return currentValuesBySupplier.get(this);
            }

            @Override
            public void close() {
                currentValuesBySupplier.remove(this);
                currentValuesBySupplier.keySet().forEach(ProfileFileSupplier::close);
            }

            private boolean updateCurrentValue(ProfileFileSupplier supplier, Consumer<ProfileFile> action) {
                ProfileFile next = supplier.get();
                ProfileFile current = currentValuesBySupplier.put(supplier, next);
                action.accept(next);

                return !Objects.equals(next, current);
            }

            private boolean refreshAggregate(ProfileFile.Aggregator aggregator) {
                ProfileFile next = aggregator.build();
                ProfileFile current = currentValuesBySupplier.get(this);
                if (!Objects.equals(next, current)) {
                    currentValuesBySupplier.put(this, next);
                    return true;
                }

                return false;
            }

        };
    }

}
