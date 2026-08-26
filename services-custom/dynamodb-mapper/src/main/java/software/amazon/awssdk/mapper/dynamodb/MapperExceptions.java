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
package software.amazon.awssdk.mapper.dynamodb;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * Port of v1 {@code com.amazonaws.util.Throwables.failure}, using the v2 exception
 * hierarchy. A {@link RuntimeException} is returned unchanged (the message argument is
 * discarded, matching v1); an {@link Error} is rethrown; an {@link InterruptedException}
 * becomes an {@link AbortedException}; any other checked exception becomes an
 * {@link SdkClientException} (the v2 analog of v1's {@code AmazonClientException}).
 */
@SdkInternalApi
final class MapperExceptions {

    private MapperExceptions() {
    }

    static RuntimeException failure(Throwable t, String message) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        if (t instanceof Error) {
            throw (Error) t;
        }
        if (t instanceof InterruptedException) {
            return AbortedException.create(message, t);
        }
        return SdkClientException.create(message, t);
    }
}
