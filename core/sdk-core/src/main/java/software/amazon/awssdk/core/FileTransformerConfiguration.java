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

package software.amazon.awssdk.core;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * Configuration options for {@link AsyncResponseTransformer#toFile(Path, FileTransformerConfiguration)}. All values are optional.
 */
@SdkPublicApi
public final class FileTransformerConfiguration implements ToCopyableBuilder<FileTransformerConfiguration.Builder,
    FileTransformerConfiguration> {
    private final FileWriteOption fileWriteOption;
    private final Boolean deleteOnFailure;

    private FileTransformerConfiguration(DefaultBuilder builder) {
        this.fileWriteOption = builder.fileWriteOption;
        this.deleteOnFailure = builder.deleteOnFailure;
    }

    /**
     * The configured {@link FileWriteOption} or {@link Optional#empty()} if not configured
     */
    public Optional<FileWriteOption> fileWriteOption() {
        return Optional.ofNullable(fileWriteOption);
    }

    /**
     * Whether to delete the file in the event of a failure.
     */
    public Optional<Boolean> deleteOnFailure() {
        return Optional.ofNullable(deleteOnFailure);
    }

    /**
     * Create a {@link Builder}, used to create a {@link FileTransformerConfiguration}.
     */
    public static Builder builder() {
        return new DefaultBuilder();
    }

    @Override
    public Builder toBuilder() {
        return new DefaultBuilder().fileWriteOption(fileWriteOption)
                                   .deleteOnFailure(deleteOnFailure);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FileTransformerConfiguration that = (FileTransformerConfiguration) o;

        if (fileWriteOption != that.fileWriteOption) {
            return false;
        }
        return Objects.equals(deleteOnFailure, that.deleteOnFailure);
    }

    @Override
    public int hashCode() {
        int result = fileWriteOption != null ? fileWriteOption.hashCode() : 0;
        result = 31 * result + (deleteOnFailure != null ? deleteOnFailure.hashCode() : 0);
        return result;
    }

    public enum FileWriteOption {
        /**
         * Always create a new file. If the file already exists, {@link FileAlreadyExistsException} will be thrown.
         * In the event of an error, the SDK will attempt to delete the file (whatever has been written to it so far).
         */
        CREATE_NEW,

        /**
         * Create a new file if it doesn't exist, otherwise replace the existing file.
         * <p>
         * In the event of an error, the SDK will NOT attempt to delete the file.
         */
        CREATE_OR_REPLACE_EXISTING,

        /**
         * Create a new file if it doesn't exist, otherwise append to the existing file.
         * <p>
         * In the event of an error, the SDK will NOT attempt to delete the file.
         */
        CREATE_OR_APPEND_EXISTING
    }

    public interface Builder extends CopyableBuilder<Builder, FileTransformerConfiguration> {

        /**
         * Configures the file write option
         *
         * @param fileWriteOption the file write option
         * @return This object for method chaining.
         */
        Builder fileWriteOption(FileWriteOption fileWriteOption);

        /**
         * Configures whether the file should be deleted in the event of an error
         *
         * @param deleteOnFailure whether to delete the file upon failure
         * @return This object for method chaining.
         */
        Builder deleteOnFailure(Boolean deleteOnFailure);
    }

    private static class DefaultBuilder implements Builder {
        private FileWriteOption fileWriteOption;
        private Boolean deleteOnFailure;

        @Override
        public Builder fileWriteOption(FileWriteOption fileWriteOption) {
            this.fileWriteOption = fileWriteOption;
            return this;
        }

        @Override
        public Builder deleteOnFailure(Boolean deleteOnFailure) {
            this.deleteOnFailure = deleteOnFailure;
            return this;
        }

        @Override
        public FileTransformerConfiguration build() {
            return new FileTransformerConfiguration(this);
        }
    }

}