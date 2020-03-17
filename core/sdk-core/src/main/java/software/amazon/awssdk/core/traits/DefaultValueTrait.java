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

package software.amazon.awssdk.core.traits;

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.util.IdempotentUtils;

/**
 * Trait that supplies a default value when none is present for a given field.
 */
@SdkProtectedApi
public final class DefaultValueTrait implements Trait {

    private final Supplier<?> defaultValueSupplier;

    private DefaultValueTrait(Supplier<?> defaultValueSupplier) {
        this.defaultValueSupplier = defaultValueSupplier;
    }

    /**
     * If the value is null then the default value supplier is used to get a default
     * value for the field. Otherwise 'val' is returned.
     *
     * @param val Value to resolve.
     * @return Resolved value.
     */
    public Object resolveValue(Object val) {
        return val != null ? val : defaultValueSupplier.get();
    }

    /**
     * Creates a new {@link DefaultValueTrait} with a custom {@link Supplier}.
     *
     * @param supplier Supplier of default value for the field.
     * @return New trait instance.
     */
    public static DefaultValueTrait create(Supplier<?> supplier) {
        return new DefaultValueTrait(supplier);
    }

    /**
     * Creates a precanned {@link DefaultValueTrait} using the idempotency token generation which
     * creates a new UUID if a field is null. This is used when the 'idempotencyToken' trait in the service
     * model is present.
     *
     * @return New trait instance.
     */
    public static DefaultValueTrait idempotencyToken() {
        return new DefaultValueTrait(IdempotentUtils.getGenerator());
    }
}
