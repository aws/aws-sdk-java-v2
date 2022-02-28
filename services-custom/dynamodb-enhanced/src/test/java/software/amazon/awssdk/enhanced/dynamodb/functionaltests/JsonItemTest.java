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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.enhanced.dynamodb.model.JsonItem;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class JsonItemTest {

    String COMPLEX_JSON_STRING = "{\"binary\":\"AQIDBA==\",\"binarySet\":[\"BQY=\",\"Bwg=\"],\"booleanTrue\":true,"
                                 + "\"booleanFalse\":false,\"intAttr\":1234,\"listAtr\":[\"abc\",\"123\"],"
                                 + "\"mapAttr\":{\"key1\":\"value1\",\"key2\":999},\"nullAttr\":null,\"numberAttr\":999.1234,"
                                 + "\"stringAttr\":\"bla\",\"stringSetAttr\":[\"da\",\"di\",\"foo\",\"bar\",\"bazz\"]}";

    static String SIMPLE_STRING_JSON = "{\"key\":\"AQIDBA==\"}";
    static String SIMPLE_NUMBER_JSON = "{\"key\":99}";
    static String SIMPLE_BOOLEAN_JSON = "{\"key\":true}";
    static String SIMPLE_ARRAY_JSON = "{\"key\":[\"BQY=\",\"Bwg=\"]}";

    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void jsonItem_from_JsonString() throws JsonProcessingException {
        JsonItem jsonItem = JsonItem.fromJson(COMPLEX_JSON_STRING);
        assertEquals(mapper.readTree(COMPLEX_JSON_STRING), mapper.readTree(jsonItem.toJson()));
    }

    @ParameterizedTest
    @MethodSource("provideJsonToItemMapValues")
    public void jsonItem_To_MapConversion(String jsonString , Map<String, AttributeValue> attributeValue){
        JsonItem jsonItem = JsonItem.fromJson(jsonString);
        assertThat(jsonItem.itemToMap()).isEqualTo(attributeValue);
    }

    private static Stream<Arguments> provideJsonToItemMapValues() {
        Map<String, AttributeValue> stringMap = new HashMap<String, AttributeValue>();
        stringMap.put("key", AttributeValue.builder().s("AQIDBA==").build());

        Map<String, AttributeValue> numberMap = new HashMap<String, AttributeValue>();
        numberMap.put("key", AttributeValue.builder().n("99").build());

        Map<String, AttributeValue> boolMap = new HashMap<String, AttributeValue>();
        boolMap.put("key", AttributeValue.builder().bool(true).build());


        Map<String, AttributeValue> stringListMap = new HashMap<String, AttributeValue>();
        stringListMap.put("key", AttributeValue.builder().l(AttributeValue.builder().s("BQY=").build(),
                                                                             AttributeValue.builder().s("Bwg=").build()).build());
        return Stream.of(
            Arguments.of(SIMPLE_STRING_JSON, stringMap),
            Arguments.of(SIMPLE_NUMBER_JSON,numberMap),
            Arguments.of(SIMPLE_BOOLEAN_JSON,boolMap),
            Arguments.of(SIMPLE_ARRAY_JSON, stringListMap)
        );
    }



}
