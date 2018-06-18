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

package software.amazon.awssdk.utils.builder;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A special type of {@link SdkBuilder} that can be used when the built type implements {@link ToCopyableBuilder}.
 */
@SdkPublicApi
public interface CopyableBuilder<B extends CopyableBuilder<B, T>, T extends ToCopyableBuilder<B, T>> extends SdkBuilder<B, T> {
    /**
     * A shallow copy of this object created by building an immutable T and then transforming it back to a builder.
     *
     * @return a copy of this object
     */
    default B copy() {
        return build().toBuilder();
    }
}
