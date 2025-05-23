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

package software.amazon.awssdk.enhanced.dynamodb.internal.converter.string;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.StringConverter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.StringConverterProvider;

/**
 * A {@link StringConverterProvider} that delegates to a default provider and
 * falls back to a {@link GenericObjectStringConverter} when no specific converter is found.
 *
 * <p>Fallback converters are cached per raw class for performance.</p>
 */
@SdkInternalApi
@ThreadSafe
@Immutable
public class FallbackStringConverterProvider implements StringConverterProvider {

    private final StringConverterProvider delegate;
    private final Map<Class<?>, StringConverter<?>> cache = new ConcurrentHashMap<>();

    public FallbackStringConverterProvider(StringConverterProvider delegate) {
        this.delegate = delegate;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> StringConverter<T> converterFor(EnhancedType<T> type) {
        try {
            return delegate.converterFor(type);
        } catch (IllegalArgumentException e) {
            return (StringConverter<T>) cache.computeIfAbsent(
                type.rawClass(),
                cls -> GenericObjectStringConverter.create((EnhancedType<Object>) type)
            );
        }
    }
}



