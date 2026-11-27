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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.annotations.ThreadSafe;

/**
 * Splits a source name into lowercase words using the built-in boundaries.
 *
 * <p>
 * {@code _}, {@code -}, {@code .}, and whitespace separate words. A lowercase letter or a digit followed by an
 * uppercase letter starts a word. Two uppercase letters followed by a lowercase letter start a word before the second
 * uppercase letter. Digits stay with the preceding word unless a separator divides them.
 *
 * <p>
 * Each built-in strategy calls {@link #split(String)}. A custom strategy may call it for the same boundaries, or skip
 * it and define its own.
 *
 * <p>
 * Proof of concept: case classification uses {@link Character} and {@link String#toLowerCase(Locale)} with
 * {@link Locale#ROOT}. Fixed Unicode case tables are not included. A null or empty source name is outside this
 * proof of concept. A name that contains no word, such as {@code "___"}, is rejected.
 */
@SdkPublicApi
@ThreadSafe
public final class AttributeNamingWordSplitter {
    private AttributeNamingWordSplitter() {
    }

    /**
     * Returns the lowercase words in the source name.
     *
     * @param sourceName the Java property name, or the static {@code name(...)} value
     * @return the lowercase words, in order
     * @throws IllegalArgumentException when the source name contains no word
     */
    public static List<String> split(String sourceName) {
        List<String> words = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int length = sourceName.length();

        for (int i = 0; i < length; i++) {
            char character = sourceName.charAt(i);
            if (isSeparator(character)) {
                finishWord(words, current);
                continue;
            }
            if (current.length() > 0 && startsNewWord(current.charAt(current.length() - 1), character, sourceName, i)) {
                finishWord(words, current);
            }
            current.append(character);
        }
        finishWord(words, current);

        if (words.isEmpty()) {
            throw new IllegalArgumentException("Built-in attribute naming failed for source name '" + sourceName
                                               + "' because it contains no word.");
        }

        List<String> lowercaseWords = new ArrayList<>(words.size());
        for (String word : words) {
            lowercaseWords.add(word.toLowerCase(Locale.ROOT));
        }
        return Collections.unmodifiableList(lowercaseWords);
    }

    private static boolean startsNewWord(char previous, char current, String sourceName, int index) {
        if (Character.isLowerCase(previous) && Character.isUpperCase(current)) {
            return true;
        }
        if (Character.isDigit(previous) && Character.isUpperCase(current)) {
            return true;
        }
        return Character.isUpperCase(previous)
               && Character.isUpperCase(current)
               && index + 1 < sourceName.length()
               && Character.isLowerCase(sourceName.charAt(index + 1));
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
}
