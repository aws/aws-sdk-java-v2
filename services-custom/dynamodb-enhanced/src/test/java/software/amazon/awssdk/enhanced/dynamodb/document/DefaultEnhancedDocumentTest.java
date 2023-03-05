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

package software.amazon.awssdk.enhanced.dynamodb.document;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider.defaultProvider;
import static software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocumentTestData.defaultDocBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.enhanced.dynamodb.internal.document.DefaultEnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.utils.Pair;

public class DefaultEnhancedDocumentTest {

    public static final String SIMPLE_NUMBER_KEY = "numberKey";
    public static final String BIG_DECIMAL_NUMBER_KEY = "bigDecimalNumberKey";
    public static final String BOOL_KEY = "boolKey";
    public static final String NULL_KEY = "nullKey";
    public static final String NUMBER_SET_KEY = "numberSet";
    public static final String SDK_BYTES_SET_KEY = "sdkBytesSet";
    public static final String STRING_SET_KEY = "stringSet";
    public static final String[] STRINGS_ARRAY = {"a", "b", "c"};
    public static final SdkBytes[] SDK_BYTES_ARRAY = {SdkBytes.fromUtf8String("a"), SdkBytes.fromUtf8String("b"),
                                                      SdkBytes.fromUtf8String("c")};
    public static final String[] NUMBER_STRING_ARRAY = {"1", "2", "3"};
    public static final AttributeValue NUMBER_STRING_ARRAY_ATTRIBUTES_LISTS =
        AttributeValue.fromL(Arrays.asList(AttributeValue.fromN(NUMBER_STRING_ARRAY[0]),
                                           AttributeValue.fromN(NUMBER_STRING_ARRAY[1]),
                                           AttributeValue.fromN(NUMBER_STRING_ARRAY[2])));

    public static final AttributeValue STRING_ARRAY_ATTRIBUTES_LISTS =
        AttributeValue.fromL(Arrays.asList(AttributeValue.fromS(STRINGS_ARRAY[0]),
                                           AttributeValue.fromS(STRINGS_ARRAY[1]),
                                           AttributeValue.fromS(STRINGS_ARRAY[2])));



    @Test
    void copyCreatedFromToBuilder(){
        DefaultEnhancedDocument originalDoc = (DefaultEnhancedDocument) defaultDocBuilder()
            .putString("stringKey", "stringValue")
            .build();
        DefaultEnhancedDocument copiedDoc = (DefaultEnhancedDocument)  originalDoc.toBuilder().build();
        DefaultEnhancedDocument copyAndAlter =
            (DefaultEnhancedDocument)  originalDoc.toBuilder().putString("keyOne", "valueOne").build();
        assertThat(originalDoc.toMap()).isEqualTo(copiedDoc.toMap());
        assertThat(originalDoc.toMap().keySet().size()).isEqualTo(1);
        assertThat(copyAndAlter.toMap().keySet().size()).isEqualTo(2);
        assertThat(copyAndAlter.getString("stringKey")).isEqualTo("stringValue");
        assertThat(copyAndAlter.getString("keyOne")).isEqualTo("valueOne");
        assertThat(originalDoc).isEqualTo(copiedDoc);
    }

    @Test
    void isNull_inDocumentGet(){
        DefaultEnhancedDocument nullDocument = (DefaultEnhancedDocument) DefaultEnhancedDocument.builder()
            .putNull("nullDocument")
            .putString("nonNull", "stringValue")
            .build();
        assertThat(nullDocument.isNull("nullDocument")).isTrue();
        assertThat(nullDocument.isNull("nonNull")).isFalse();
        assertThat(nullDocument.toMap().get("nullDocument")).isEqualTo(AttributeValue.fromNul(true));
    }

    @Test
    void isNull_when_putObjectWithNullAttribute(){
        DefaultEnhancedDocument.DefaultBuilder builder =
            (DefaultEnhancedDocument.DefaultBuilder) DefaultEnhancedDocument.builder().attributeConverterProviders(defaultProvider());
        builder.putObject("nullAttribute", AttributeValue.fromNul(true));
        DefaultEnhancedDocument document = (DefaultEnhancedDocument) builder.build();
        assertThat(document.isNull("nullAttribute")).isTrue();
    }

}
