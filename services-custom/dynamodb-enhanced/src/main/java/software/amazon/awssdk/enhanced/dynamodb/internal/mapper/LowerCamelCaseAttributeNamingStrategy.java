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

package software.amazon.awssdk.enhanced.dynamodb.internal.mapper;

import java.util.List;
import java.util.Locale;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.annotations.ThreadSafe;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeNamingStrategy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeNamingWords;

/**
 * Capitalizes every word after the first and joins the words without a separator.
 */
@SdkInternalApi
@ThreadSafe
public final class LowerCamelCaseAttributeNamingStrategy implements AttributeNamingStrategy {
    private static final LowerCamelCaseAttributeNamingStrategy INSTANCE = new LowerCamelCaseAttributeNamingStrategy();

    private LowerCamelCaseAttributeNamingStrategy() {
    }

    /**
     * Returns the shared lower-camel-case strategy.
     */
    public static AttributeNamingStrategy create() {
        return INSTANCE;
    }

    @Override
    public String transform(String sourceName) {
        List<String> words = AttributeNamingWords.split(sourceName);
        StringBuilder result = new StringBuilder(words.get(0));
        for (int i = 1; i < words.size(); i++) {
            result.append(capitalize(words.get(i)));
        }
        return result.toString();
    }

    private static String capitalize(String word) {
        return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
    }
}
