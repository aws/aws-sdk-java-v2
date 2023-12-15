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

package software.amazon.awssdk.thirdparty.org.slf4j.impl.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Utility for creating and invoke {@link MethodHandle}s.
 */
@SdkInternalApi
public final class InvocationHelper {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private InvocationHelper() {
    }

    public static Optional<MethodHandle> staticHandle(Class<?> refc, String name, MethodType mt) {
        try {
            return Optional.of(LOOKUP.findStatic(refc, name, mt));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return Optional.empty();
        }
    }

    public static Optional<MethodHandle> virtualHandle(Class<?> refc, String name, MethodType mt) {
        try {
            return Optional.of(LOOKUP.findVirtual(refc, name, mt));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return Optional.empty();
        }
    }

    public static MethodHandle cachedGetHandle(AtomicReference<MethodHandle> ref, Supplier<MethodHandle> creator) {
        if (ref.get() != null) {
            return ref.get();
        }

        return ref.updateAndGet(mh -> {
            if (mh != null) {
                return mh;
            }
            return creator.get();
        });
    }
}
