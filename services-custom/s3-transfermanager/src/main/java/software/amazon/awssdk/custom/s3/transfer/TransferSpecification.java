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

package software.amazon.awssdk.custom.s3.transfer;

import java.net.URL;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Union of the various ways to specify a transfer.
 */
@SdkProtectedApi
public interface TransferSpecification {

    /**
     * @return {@code true} if this is a presigned URL, {@code false} otherwise.
     */
    default boolean isPresignedUrl() {
        return false;
    }

    /**
     * @return {@code true} if this is an API request, {@code false} otherwise.
     */
    default boolean isApiRequest() {
        return false;
    }

    /**
     * @return This specification as a presigned URL.
     * @throws IllegalStateException If this is not a presigned URL.
     */
    default URL asPresignedUrl() {
        throw new IllegalStateException("Not a presigned URL");
    }
}
