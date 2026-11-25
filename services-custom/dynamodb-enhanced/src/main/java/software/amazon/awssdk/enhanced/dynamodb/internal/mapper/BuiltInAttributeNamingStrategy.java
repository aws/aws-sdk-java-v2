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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.enhanced.dynamodb.mapper.AttributeNamingStrategy;

/**
 * Built-in naming strategies. Each strategy validates the source name, splits it into words, lowercases those words,
 * and then applies its own format.
 *
 * <p>
 * Word boundaries are mechanical. {@code _}, {@code -}, {@code .}, and whitespace separate words. A lowercase letter
 * or a digit followed by an uppercase letter starts a word. Two uppercase letters followed by a lowercase letter start
 * a word before the second uppercase letter. Digits stay with the preceding word unless a separator divides them.
 *
 * <p>
 * Proof of concept: case classification uses {@link Character} and {@link String#toLowerCase(Locale)} with
 * {@link Locale#ROOT}. Fixed Unicode case tables are not included. A null or empty source name is outside this
 * proof of concept. A name that contains no word, such as {@code "___"}, is rejected. The full set of rejected
 * characters is not defined yet.
 */
@SdkInternalApi
public enum BuiltInAttributeNamingStrategy implements AttributeNamingStrategy {
    LOWER_SNAKE_CASE {
        @Override
        protected String format(List<String> words) {
            return String.join("_", words);
        }
    },
    UPPER_SNAKE_CASE {
        @Override
        protected String format(List<String> words) {
            return words.stream()
                        .map(word -> word.toUpperCase(Locale.ROOT))
                        .collect(Collectors.joining("_"));
        }
    },
    LOWER_CAMEL_CASE {
        @Override
        protected String format(List<String> words) {
            StringBuilder result = new StringBuilder(words.get(0));
            for (int i = 1; i < words.size(); i++) {
                result.append(capitalize(words.get(i)));
            }
            return result.toString();
        }
    },
    UPPER_CAMEL_CASE {
        @Override
        protected String format(List<String> words) {
            StringBuilder result = new StringBuilder();
            for (String word : words) {
                result.append(capitalize(word));
            }
            return result.toString();
        }
    };

    @Override
    public String transform(String attributeName) {
        List<String> words = splitWords(attributeName);
        if (words.isEmpty()) {
            throw new IllegalArgumentException("Built-in attribute naming failed for source name '" + attributeName
                                               + "' because it contains no word.");
        }

        List<String> lowercaseWords = words.stream()
                                           .map(word -> word.toLowerCase(Locale.ROOT))
                                           .collect(Collectors.toList());
        return format(lowercaseWords);
    }

    /**
     * Formats words that are already lowercase.
     */
    protected abstract String format(List<String> words);

    private static List<String> splitWords(String attributeName) {
        List<String> words = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int length = attributeName.length();

        for (int i = 0; i < length; i++) {
            char character = attributeName.charAt(i);
            if (isSeparator(character)) {
                finishWord(words, current);
                continue;
            }
            if (current.length() > 0 && startsNewWord(current.charAt(current.length() - 1), character, attributeName, i)) {
                finishWord(words, current);
            }
            current.append(character);
        }
        finishWord(words, current);
        return words;
    }

    private static boolean startsNewWord(char previous, char current, String attributeName, int index) {
        if (Character.isLowerCase(previous) && Character.isUpperCase(current)) {
            return true;
        }
        if (Character.isDigit(previous) && Character.isUpperCase(current)) {
            return true;
        }
        return Character.isUpperCase(previous)
               && Character.isUpperCase(current)
               && index + 1 < attributeName.length()
               && Character.isLowerCase(attributeName.charAt(index + 1));
    }

    private static boolean isSeparator(char character) {
        return character == '_'
               || character == '-'
               || character == '.'
               || Character.isWhitespace(character);
    }

    private static void finishWord(List<String> words, StringBuilder current) {
        if (current.length() > 0) {
            words.add(current.toString());
            current.setLength(0);
        }
    }

    private static String capitalize(String word) {
        return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
    }
}
