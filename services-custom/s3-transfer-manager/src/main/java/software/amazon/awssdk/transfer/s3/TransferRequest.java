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

package software.amazon.awssdk.transfer.s3;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Base class for all transfer requests.
 */
@SdkPublicApi
public interface TransferRequest {

    /**
     * The bucket name containing the object.
     *
     * @return the bucket name
     */
    String bucket();

    /**
     * The Key of the object.
     *
     * @return the key of the object
     */
    String key();

    interface Builder<TypeToBuildT, BuilderT extends Builder> {

        /**
         * The bucket name containing the object.
         *
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        BuilderT bucket(String bucket);

        /**
         * The Key of the object to transfer.
         *
         * @return Returns a reference to this object so that method calls can be chained together.
         */
        BuilderT key(String key);

        TypeToBuildT build();
    }
}
