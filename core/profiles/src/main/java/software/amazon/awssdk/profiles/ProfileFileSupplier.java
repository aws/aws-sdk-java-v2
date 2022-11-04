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
import java.time.Clock;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.profiles.internal.ProfileFileRefresher;
import software.amazon.awssdk.utils.SdkAutoCloseable;

/**
 * Encapsulates the logic for supplying either a single or multiple ProfileFile instances.
 * <p>
 * Each call to the {@link #getProfileFile()} method will result in either a new or previously supplied profile based on the
 * implementation's rules.
 */
@SdkPublicApi
@FunctionalInterface
public interface ProfileFileSupplier extends SdkAutoCloseable {

    /**
     * @return A ProfileFile instance.
     */
    ProfileFile getProfileFile();

    @Override
    default void close() {
    }

    /**
     * Creates a {@link ProfileFileSupplier} capable of producing multiple profile objects from a file. See
     * {@link ProfileFileSupplier#builder()} to create a customized implementation.
     *
     * @param path Path to the file read from.
     * @return Implementation of {@link ProfileFileSupplier} that is capable of supplying  a new profile when the file
     *         has been modified.
     */
    static ProfileFileSupplier reloadWhenModified(Path path) {
        return new ProfileFileSupplier() {

            ProfileFile.Builder builder = ProfileFile.builder()
                                                     .content(path)
                                                     .type(ProfileFile.Type.CREDENTIALS);

            ProfileFileRefresher refresher = ProfileFileRefresher.builder()
                                                                 .profileFile(builder::build)
                                                                 .profileFilePath(path)
                                                                 .build();

            @Override
            public ProfileFile getProfileFile() {
                return refresher.refreshIfStale();
            }

            @Override
            public void close() {
                refresher.close();
            }
        };
    }

    /**
     * Creates a {@link ProfileFileSupplier} capable of producing a single profile object from a file. See
     * {@link ProfileFileSupplier#builder()} to create a customized implementation.
     *
     * @param path Path to the file read from.
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
     * Creates a {@link ProfileFileSupplier} that produces an existing profile. See
     * {@link ProfileFileSupplier#builder()} to create a customized implementation.
     *
     * @param profileFile Profile object to supply.
     * @return Implementation of {@link ProfileFileSupplier} that is capable of supplying a single profile.
     */
    static ProfileFileSupplier fixedProfileFile(ProfileFile profileFile) {
        return () -> profileFile;
    }

    /**
     * @return Builder instance to construct a {@link ProfileFileSupplier}.
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Completes {@link ProfileFileSupplier} build.
     * @param builder Object to complete build.
     * @return Implementation of {@link ProfileFileSupplier}.
     */
    static ProfileFileSupplier fromBuilder(Builder builder) {
        if (builder.reloadingSupplier) {

            ProfileFileRefresher.Builder refresherBuilder = ProfileFileRefresher.builder()
                                                                                .profileFile(builder.profileFile)
                                                                                .profileFilePath(builder.profileFilePath);

            if (Objects.nonNull(builder.clock)) {
                refresherBuilder.clock(builder.clock);
            }
            if (Objects.nonNull(builder.onProfileFileLoad)) {
                refresherBuilder.onProfileFileReload(builder.onProfileFileLoad);
            }

            ProfileFileRefresher refresher = refresherBuilder.build();

            return new ProfileFileSupplier() {

                @Override
                public ProfileFile getProfileFile() {
                    return refresher.refreshIfStale();
                }

                @Override
                public void close() {
                    refresher.close();
                }
            };
        } else {
            return () -> builder.profileFile.get();
        }
    }

    /**
     * A builder for {@link ProfileFileSupplier}.
     */
    final class Builder {

        private boolean reloadingSupplier = false;
        private Supplier<ProfileFile> profileFile;
        private Path profileFilePath;
        private Clock clock;
        private Consumer<ProfileFile> onProfileFileLoad;

        private Builder() {
        }

        /**
         * Sets a supplier for reloading the contents of a file.
         *
         * <p>Calling {@link #fixedProfileFile(Path)} or {@link #fixedProfileFile(ProfileFile)} will remove rhe reloading
         * functionality</p>
         *
         * @param path Path to the file read from.
         * @return This builder for method chaining.
         */
        public Builder reloadWhenModified(Path path) {
            ProfileFile.Builder builder = ProfileFile.builder()
                                                     .content(path)
                                                     .type(ProfileFile.Type.CREDENTIALS);
            this.profileFile = builder::build;
            this.profileFilePath = path;
            this.reloadingSupplier = true;
            return this;
        }

        /**
         * Sets a supplier for loading the contents of a file once.
         *
         * <p>Calling {@link #reloadWhenModified(Path)} will remove the fixed functionality</p>
         *
         * @param path Path to the file read from.
         * @return This builder for method chaining.
         * @see #fixedProfileFile(ProfileFile)
         */
        public Builder fixedProfileFile(Path path) {
            ProfileFile profileFileInstance = ProfileFile.builder()
                                                         .content(path)
                                                         .type(ProfileFile.Type.CREDENTIALS)
                                                         .build();
            return fixedProfileFile(profileFileInstance);
        }

        /**
         * Sets a supplier for returning a specific profile instance.
         *
         * <p>Calling {@link #reloadWhenModified(Path)} will remove the fixed functionality</p>
         *
         * @param profileFile Profile object to supply.
         * @return This builder for method chaining.
         * @see #fixedProfileFile(Path)
         */
        public Builder fixedProfileFile(ProfileFile profileFile) {
            this.profileFile = () -> profileFile;
            this.profileFilePath = null;
            this.reloadingSupplier = false;
            return this;
        }

        /**
         * Sets an action to execute whenever a new profile object is supplied. This action may be called zere, one, or many
         * times depending on the {@link ProfileFileSupplier} implementation.
         *
         * @param action The block to execute.
         * @return This builder for method chaining.
         */
        public Builder onProfileFileLoad(Consumer<ProfileFile> action) {
            this.onProfileFileLoad = action;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public ProfileFileSupplier build() {
            return fromBuilder(this);
        }
    }

}
