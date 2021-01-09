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

import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.Buildable;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A builder for an immutable {@link SdkPojo} with no fields.
 *
 * <p>
 * This is useful for {@code SdkPojo} implementations that don't have their own builders, but need to be passed to something
 * that assumes they already have a builder. For example, marshallers expect all {@code SdkPojo} implementations to have a
 * builder. In the cases that they do not, this can be used as their builder.
 *
 * <p>
 * This currently only supports {@code SdkPojo}s without any fields (because it has no way to set them). It also does not support
 * {@code SdkPojo}s that already have or are a builder (that builder should be used instead).
 */
@SdkProtectedApi
public final class SdkPojoBuilder<T extends SdkPojo> implements SdkPojo, Buildable {
    private final T delegate;

    public SdkPojoBuilder(T delegate) {
        Validate.isTrue(delegate.sdkFields().isEmpty(), "Delegate must be empty.");
        Validate.isTrue(!(delegate instanceof ToCopyableBuilder), "Delegate already has a builder.");
        Validate.isTrue(!(delegate instanceof Buildable), "Delegate is already a builder.");
        this.delegate = delegate;
    }

    @Override
    public List<SdkField<?>> sdkFields() {
        return Collections.emptyList();
    }

    @Override
    public boolean equalsBySdkFields(Object other) {
        return delegate.equalsBySdkFields(other);
    }

    @Override
    public T build() {
        return delegate;
    }
}
