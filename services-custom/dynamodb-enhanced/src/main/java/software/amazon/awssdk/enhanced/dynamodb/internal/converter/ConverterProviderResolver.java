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

import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.DefaultAttributeConverterProvider;

/**
 * Static module to assist with the initialization of attribute converter providers for a StaticTableSchema.
 */
@SdkInternalApi
public final class ConverterProviderResolver {

    private static final AttributeConverterProvider DEFAULT_ATTRIBUTE_CONVERTER =
        DefaultAttributeConverterProvider.create();

    private ConverterProviderResolver() {
    }

    /**
     * Static provider for the default attribute converters that are bundled with the DynamoDB Enhanced Client.
     * This provider will be used by default unless overridden in the static table schema builder or using bean
     * annotations.
     */
    public static AttributeConverterProvider defaultConverterProvider() {
        return DEFAULT_ATTRIBUTE_CONVERTER;
    }

    /**
     * Resolves a list of attribute converter providers into a single provider. If the list is a singleton,
     * it will just return that provider, otherwise it will combine them into a
     * {@link ChainConverterProvider} using the order provided in the list.
     *
     * @param providers A list of providers to be combined in strict order
     * @return A single provider that combines all the supplied providers or null if no providers were supplied
     */
    public static AttributeConverterProvider resolveProviders(List<AttributeConverterProvider> providers) {
        if (providers == null || providers.isEmpty()) {
            return null;
        }

        if (providers.size() == 1) {
            return providers.get(0);
        }

        return ChainConverterProvider.create(providers);
    }
}
