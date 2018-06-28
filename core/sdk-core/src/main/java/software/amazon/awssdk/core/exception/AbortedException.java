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

package software.amazon.awssdk.core.exception;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Extension of {@link SdkClientException} that is thrown whenever an
 * operation has been aborted by the SDK.
 *
 * This exception is not meant to be retried.
 */
@SdkPublicApi
public final class AbortedException extends SdkClientException {

    public AbortedException() {
        this("Aborted.");
    }

    public AbortedException(String message) {
        super(message);
    }

    public AbortedException(Throwable cause) {
        super("Aborted.", cause);
    }

    public AbortedException(String message, Throwable cause) {
        super(message, cause);
    }
}
