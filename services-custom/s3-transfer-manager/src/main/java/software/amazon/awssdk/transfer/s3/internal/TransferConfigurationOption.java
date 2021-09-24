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

package software.amazon.awssdk.transfer.s3.internal;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.AttributeMap;

@SdkInternalApi
public final class TransferConfigurationOption<T> extends AttributeMap.Key<T> {
    public static final TransferConfigurationOption<Integer> UPLOAD_DIRECTORY_MAX_DEPTH =
        new TransferConfigurationOption<>("UploadDirectoryMaxDepth", Integer.class);

    public static final TransferConfigurationOption<Boolean> UPLOAD_DIRECTORY_RECURSIVE =
        new TransferConfigurationOption<>("UploadDirectoryRecursive", Boolean.class);

    public static final TransferConfigurationOption<Boolean> UPLOAD_DIRECTORY_FOLLOW_SYMBOLIC_LINKS =
        new TransferConfigurationOption<>("UploadDirectoryFileVisitOption", Boolean.class);


    private static final int DEFAULT_UPLOAD_DIRECTORY_MAX_DEPTH = Integer.MAX_VALUE;
    private static final Boolean DEFAULT_UPLOAD_DIRECTORY_RECURSIVE = Boolean.TRUE;
    // TODO: revisit
    private static final Boolean DEFAULT_UPLOAD_DIRECTORY_FOLLOW_SYMBOLIC_LINKS = Boolean.FALSE;

    public static final AttributeMap TRANSFER_DEFAULTS = AttributeMap
        .builder()
        .put(UPLOAD_DIRECTORY_MAX_DEPTH, DEFAULT_UPLOAD_DIRECTORY_MAX_DEPTH)
        .put(UPLOAD_DIRECTORY_RECURSIVE, DEFAULT_UPLOAD_DIRECTORY_RECURSIVE)
        .put(UPLOAD_DIRECTORY_FOLLOW_SYMBOLIC_LINKS, DEFAULT_UPLOAD_DIRECTORY_FOLLOW_SYMBOLIC_LINKS)
        .build();

    private final String name;

    private TransferConfigurationOption(String name, Class<T> clzz) {
        super(clzz);
        this.name = name;
    }

    /**
     * Note that the name is mainly used for debugging purposes. Two option key objects with the same name do not represent
     * the same option. Option keys are compared by reference when obtaining a value from an {@link AttributeMap}.
     *
     * @return Name of this option key.
     */
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}

