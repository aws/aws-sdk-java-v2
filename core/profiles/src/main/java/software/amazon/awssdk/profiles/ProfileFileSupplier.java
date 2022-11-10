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
     * Creates a {@link ProfileFileSupplier} capable of producing multiple profile objects from a file. This supplier will
     * return a new ProfileFile instance only once the disk file has been modified. Multiple calls to the supplier while the
     * disk file is unchanged will return the same object.
     *
     * @param path Path to the file to read from.
     * @return Implementation of {@link ProfileFileSupplier} that is capable of supplying a new profile when the file
     *         has been modified.
     */
    static ProfileFileSupplier reloadWhenModified(Path path) {
        return new ProfileFileSupplier() {

            final ProfileFile.Builder builder = ProfileFile.builder()
                                                           .content(path)
                                                           .type(ProfileFile.Type.CREDENTIALS);

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
     * Creates a {@link ProfileFileSupplier} capable of producing a single profile object from a file.
     *
     * @param path Path to the file to read from.
     * @return Implementation of {@link ProfileFileSupplier} that is capable of supplying a single profile.
     */
    static ProfileFileSupplier fixedProfileFile(Path path) {
        ProfileFile profileFile = ProfileFile.builder()
                                             .content(path)
                                             .type(ProfileFile.Type.CREDENTIALS)
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
     * creates a {@link ProfileFileSupplier} that produces an existing non-null profile. If the given profile
     * is null, then the created supplier will also be null.
     *
     * @param profileFile Profile object to supply.
     * @return Implementation of {@link ProfileFileSupplier} that is capable of supplying a single profile.
     */
    static ProfileFileSupplier wrapIntoNullableSupplier(ProfileFile profileFile) {
        return Objects.nonNull(profileFile) ? () -> profileFile : null;
    }

    /**
     * A builder for {@link ProfileFileSupplier}.
     */
    interface Builder {

        /**
         * Sets a supplier for reloading the contents of a file.
         *
         * <p>Calling {@link #fixedProfileFile(Path)} or {@link #fixedProfileFile(ProfileFile)} will remove the reloading
         * functionality</p>
         *
         * @param path Path to the file to read from.
         * @return This builder for method chaining.
         */
        Builder reloadWhenModified(Path path);

        /**
         * Sets a supplier for loading the contents of a file once.
         *
         * <p>Calling {@link #reloadWhenModified(Path)} will remove the fixed functionality</p>
         *
         * @param path Path to the file to read from.
         * @return This builder for method chaining.
         * @see #fixedProfileFile(ProfileFile)
         */
        Builder fixedProfileFile(Path path);

        /**
         * Sets a supplier for returning a specific profile instance.
         *
         * <p>Calling {@link #reloadWhenModified(Path)} will remove the fixed functionality</p>
         *
         * @param profileFile Profile object to supply.
         * @return This builder for method chaining.
         * @see #fixedProfileFile(Path)
         */
        Builder fixedProfileFile(ProfileFile profileFile);

        /**
         * Sets an action to execute whenever a new profile object is supplied. This action may be called zero, one, or many
         * times depending on the {@link ProfileFileSupplier} implementation.
         *
         * @param action The block to execute.
         * @return This builder for method chaining.
         */
        Builder onProfileFileLoad(Consumer<ProfileFile> action);

        ProfileFileSupplier build();
    }

}
