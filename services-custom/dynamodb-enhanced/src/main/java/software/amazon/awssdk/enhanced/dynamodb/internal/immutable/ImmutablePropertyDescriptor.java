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

package software.amazon.awssdk.enhanced.dynamodb.internal.immutable;

import java.lang.reflect.Method;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class ImmutablePropertyDescriptor {
    private final String name;
    private final Method getter;
    private final Method setter;

    private ImmutablePropertyDescriptor(String name, Method getter, Method setter) {
        this.name = name;
        this.getter = getter;
        this.setter = setter;
    }

    public static ImmutablePropertyDescriptor create(String name, Method getter, Method setter) {
        return new ImmutablePropertyDescriptor(name, getter, setter);
    }

    public String name() {
        return name;
    }

    public Method getter() {
        return getter;
    }

    public Method setter() {
        return setter;
    }
}
