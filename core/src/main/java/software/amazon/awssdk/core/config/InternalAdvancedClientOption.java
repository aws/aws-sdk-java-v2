/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.config;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Client options that are internal to the SDK. Customers should not rely on these settings, and they are subject to change
 * without notice.
 */
@SdkInternalApi
public class InternalAdvancedClientOption<T> extends AdvancedClientOption<T> {
    /**
     * Whether to calculate the CRC 32 checksum of a message based on the uncompressed data. By default, this is false.
     */
    public static final InternalAdvancedClientOption<Boolean> CRC32_FROM_COMPRESSED_DATA_ENABLED =
            new InternalAdvancedClientOption<>(Boolean.class);

    private InternalAdvancedClientOption(Class<T> valueClass) {
        super(valueClass);
    }
}
