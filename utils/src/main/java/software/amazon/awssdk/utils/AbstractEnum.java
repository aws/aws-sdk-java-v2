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

package software.amazon.awssdk.utils;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import software.amazon.awssdk.annotation.SdkInternalApi;

@SdkInternalApi
public abstract class AbstractEnum {

    private static final ConcurrentHashMap<Entry<Class, String>, Object> VALUES = new ConcurrentHashMap<>();

    private final String value;

    protected AbstractEnum(String value) {
        this.value = value;
    }

    @SuppressWarnings("unchecked")
    protected static <T extends AbstractEnum> T value(String value, Class<T> clz, Function<String, T> creator) {
        return (T) VALUES.computeIfAbsent(new SimpleImmutableEntry<>(clz, value), ignored -> creator.apply(value));
    }

    public String value() {
        return this.value;
    }

    @Override
    public int hashCode() {
        return 73 * getClass().hashCode() * value.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }

        return other.getClass().equals(this.getClass()) && ((AbstractEnum) other).value.equals(this.value);
    }
}