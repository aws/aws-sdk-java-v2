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

package software.amazon.awssdk.utils;

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * An implementation of {@link AutoCloseable} that does not throw any checked exceptions. The SDK does not throw checked
 * exceptions in its close() methods, so users of the SDK should not need to handle them.
 */
@SdkProtectedApi
// CHECKSTYLE:OFF - This is the only place we're allowed to use AutoCloseable
public interface SdkAutoCloseable extends AutoCloseable {
    // CHECKSTYLE:ON
    /**
     * {@inheritDoc}
     */
    @Override
    void close();
}
