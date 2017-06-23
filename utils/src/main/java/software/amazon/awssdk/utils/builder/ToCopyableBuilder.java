/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

/**
 * Implementors of this interface provide a way to get from an instance of T to a {@link CopyableBuilder}. This allows
 * modification of an otherwise immutable object using the source object as a base.
 *
 * @param <T> the type that the builder will build (this)
 * @param <B> the builder type
 */
public interface ToCopyableBuilder<B extends CopyableBuilder<B, T>, T extends ToCopyableBuilder<B, T>> {
    /**
     * Take this object and create a builder that contains all of the current property values of this object.
     *
     * @return a builder for type T
     */
    B toBuilder();
}
