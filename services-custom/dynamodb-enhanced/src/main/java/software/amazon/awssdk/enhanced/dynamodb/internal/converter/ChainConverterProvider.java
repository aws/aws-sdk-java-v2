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

package software.amazon.awssdk.enhanced.dynamodb.internal.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;

/**
 * A {@link AttributeConverterProvider} that allows multiple providers to be chained in a specified order
 * to act as a single composite provider. When searching for an attribute converter for a type,
 * the providers will be called in forward/ascending order, attempting to find a converter from the
 * first provider, then the second, and so on, until a match is found or the operation fails.
 */
@SdkInternalApi
public final class ChainConverterProvider implements AttributeConverterProvider {
    private final List<AttributeConverterProvider> providerChain;

    private ChainConverterProvider(List<AttributeConverterProvider> providers) {
        this.providerChain = new ArrayList<>(providers);
    }

    /**
     * Construct a new instance of {@link ChainConverterProvider}.
     * @param providers A list of {@link AttributeConverterProvider} to chain together.
     * @return A constructed {@link ChainConverterProvider} object.
     */
    public static ChainConverterProvider create(AttributeConverterProvider... providers) {
        return new ChainConverterProvider(Arrays.asList(providers));
    }

    /**
     * Construct a new instance of {@link ChainConverterProvider}.
     * @param providers A list of {@link AttributeConverterProvider} to chain together.
     * @return A constructed {@link ChainConverterProvider} object.
     */
    public static ChainConverterProvider create(List<AttributeConverterProvider> providers) {
        return new ChainConverterProvider(providers);
    }

    public List<AttributeConverterProvider> chainedProviders() {
        return Collections.unmodifiableList(this.providerChain);
    }

    @Override
    public <T> AttributeConverter<T> converterFor(EnhancedType<T> enhancedType) {
        return this.providerChain.stream()
                .filter(provider -> provider.converterFor(enhancedType) != null)
                .map(p -> p.converterFor(enhancedType))
                .findFirst().orElse(null);
    }
}
