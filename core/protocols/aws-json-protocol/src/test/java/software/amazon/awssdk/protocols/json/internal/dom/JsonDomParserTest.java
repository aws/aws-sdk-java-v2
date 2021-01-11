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

package software.amazon.awssdk.protocols.json.internal.dom;


import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonFactory;
import java.io.IOException;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.utils.StringInputStream;

public class JsonDomParserTest {

    private JsonDomParser parser;

    @Before
    public void setup() {
        parser = JsonDomParser.create(new JsonFactory());
    }

    @Test
    public void simpleString_ParsedCorrectly() throws IOException {
        SdkJsonNode node = parse("\"foo\"");
        assertThat(node)
            .isInstanceOf(SdkScalarNode.class)
            .matches(s -> ((SdkScalarNode) s).value().equals("foo"));
    }

    @Test
    public void simpleNumber_ParsedCorrectly() throws IOException {
        SdkJsonNode node = parse("42");
        assertThat(node)
            .isInstanceOf(SdkScalarNode.class)
            .matches(s -> ((SdkScalarNode) s).value().equals("42"));
    }

    @Test
    public void decimalNumber_ParsedCorrectly() throws IOException {
        SdkJsonNode node = parse("1234.56");
        assertThat(node)
            .isInstanceOf(SdkScalarNode.class)
            .matches(s -> ((SdkScalarNode) s).value().equals("1234.56"));
    }

    @Test
    public void falseBoolean_ParsedCorrectly() throws IOException {
        SdkJsonNode node = parse("false");
        assertThat(node)
            .isInstanceOf(SdkScalarNode.class)
            .matches(s -> ((SdkScalarNode) s).value().equals("false"));
    }

    @Test
    public void trueBoolean_ParsedCorrectly() throws IOException {
        SdkJsonNode node = parse("true");
        assertThat(node)
            .isInstanceOf(SdkScalarNode.class)
            .matches(s -> ((SdkScalarNode) s).value().equals("true"));
    }

    @Test
    public void jsonNull_ParsedCorrectly() throws IOException {
        SdkJsonNode node = parse("null");
        assertThat(node).isInstanceOf(SdkNullNode.class);
    }

    @Test
    public void emptyObject_ParsedCorrecty() throws IOException {
        SdkJsonNode node = parse("{}");
        SdkObjectNode expected = SdkObjectNode.builder().build();
        assertThat(node).isInstanceOf(SdkObjectNode.class)
                        .isEqualTo(expected);
    }

    @Test
    public void simpleObjectOfScalars_ParsedCorrectly() throws IOException {
        SdkJsonNode node = parse("{"
                                 + "    \"stringMember\": \"foo\","
                                 + "    \"integerMember\": 42,"
                                 + "    \"floatMember\": 1234.56,"
                                 + "    \"booleanMember\": true,"
                                 + "    \"nullMember\": null"
                                 + "}");

        SdkObjectNode expected = SdkObjectNode.builder()
                                              .putField("stringMember", scalar("foo"))
                                              .putField("integerMember", scalar("42"))
                                              .putField("floatMember", scalar("1234.56"))
                                              .putField("booleanMember", scalar("true"))
                                              .putField("nullMember", nullNode())
                                              .build();
        assertThat(node).isInstanceOf(SdkObjectNode.class)
                        .isEqualTo(expected);
    }

    @Test
    public void nestedObject_ParsedCorrectly() throws IOException {
        SdkJsonNode node = parse("{"
                                 + "    \"structMember\": {"
                                 + "        \"floatMember\": 1234.56,"
                                 + "        \"booleanMember\": true,"
                                 + "        \"nullMember\": null"
                                 + "    },"
                                 + "    \"integerMember\": 42"
                                 + "}");

        SdkObjectNode expected = SdkObjectNode.builder()
                                              .putField("structMember",
                                                        SdkObjectNode.builder()
                                                                     .putField("floatMember", scalar("1234.56"))
                                                                     .putField("booleanMember", scalar("true"))
                                                                     .putField("nullMember", nullNode())
                                                                     .build())
                                              .putField("integerMember", scalar("42"))
                                              .build();
        assertThat(node).isInstanceOf(SdkObjectNode.class)
                        .isEqualTo(expected);
    }

    @Test
    public void emptyArray_ParsedCorrectly() throws IOException {
        SdkJsonNode node = parse("[]");
        SdkArrayNode expected = SdkArrayNode.builder().build();
        assertThat(node).isInstanceOf(SdkArrayNode.class)
                        .isEqualTo(expected);
    }

    @Test
    public void arrayOfScalars_ParsedCorrectly() throws IOException {
        SdkJsonNode node = parse("[\"foo\", 42, null, false, 1234.56]");
        SdkArrayNode expected = SdkArrayNode.builder()
                                            .addItem(scalar("foo"))
                                            .addItem(scalar("42"))
                                            .addItem(nullNode())
                                            .addItem(scalar("false"))
                                            .addItem(scalar("1234.56"))
                                            .build();
        assertThat(node).isInstanceOf(SdkArrayNode.class)
                        .isEqualTo(expected);
    }

    @Test
    public void nestedArray_ParsedCorrectly() throws IOException {
        SdkJsonNode node = parse("[[\"valOne\", \"valTwo\"], [\"valThree\", \"valFour\"]]");
        SdkArrayNode expected = SdkArrayNode.builder()
                                            .addItem(array(scalar("valOne"), scalar("valTwo")))
                                            .addItem(array(scalar("valThree"), scalar("valFour")))
                                            .build();
        assertThat(node).isInstanceOf(SdkArrayNode.class)
                        .isEqualTo(expected);
    }

    @Test
    public void complexObject_ParsedCorrectly() throws IOException {
        SdkJsonNode node = parse("{"
                                 + "   \"stringMember\":\"foo\","
                                 + "   \"deeplyNestedArray\":["
                                 + "      [\"valOne\", 42, null],"
                                 + "      \"valTwo\","
                                 + "      ["
                                 + "         [],"
                                 + "         [\"valThree\"]"
                                 + "      ]"
                                 + "   ],"
                                 + "   \"deeplyNestedObject\":{\n"
                                 + "      \"deeplyNestedArray\":["
                                 + "         [\"valOne\", 42, null],"
                                 + "         \"valTwo\","
                                 + "         ["
                                 + "            [],"
                                 + "            [\"valThree\"]"
                                 + "         ]"
                                 + "      ],"
                                 + "      \"nestedObject\":{"
                                 + "         \"stringMember\":\"foo\","
                                 + "         \"integerMember\":42,"
                                 + "         \"floatMember\":1234.56,"
                                 + "         \"booleanMember\":true,"
                                 + "         \"furtherNestedObject\":{"
                                 + "            \"stringMember\":\"foo\","
                                 + "            \"arrayMember\":["
                                 + "               \"valOne\","
                                 + "               \"valTwo\""
                                 + "            ],\n"
                                 + "            \"nullMember\":null"
                                 + "         }"
                                 + "      }"
                                 + "   }"
                                 + "}");
        SdkArrayNode deeplyNestedArray = array(
            array(scalar("valOne"), scalar("42"), nullNode()),
            scalar("valTwo"),
            array(array(), array(scalar("valThree")))
        );

        SdkObjectNode furtherNestedObject =
            SdkObjectNode.builder()
                         .putField("stringMember", scalar("foo"))
                         .putField("arrayMember", array(scalar("valOne"), scalar("valTwo")))
                         .putField("nullMember", nullNode())
                         .build();

        SdkObjectNode deeplyNestedObject =
            SdkObjectNode.builder()
                         .putField("deeplyNestedArray", deeplyNestedArray)
                         .putField("nestedObject",
                                   SdkObjectNode.builder()
                                                .putField("stringMember", scalar("foo"))
                                                .putField("integerMember", scalar("42"))
                                                .putField("floatMember", scalar("1234.56"))
                                                .putField("booleanMember", scalar("true"))
                                                .putField("furtherNestedObject", furtherNestedObject)
                                                .build())
                         .build();

        SdkObjectNode expected = SdkObjectNode.builder()
                                              .putField("stringMember", scalar("foo"))
                                              .putField("deeplyNestedArray", deeplyNestedArray)
                                              .putField("deeplyNestedObject",
                                                        deeplyNestedObject)
                                              .build();
        assertThat(node).isInstanceOf(SdkObjectNode.class)
                        .isEqualTo(expected);
    }

    private SdkNullNode nullNode() {
        return SdkNullNode.instance();
    }

    private SdkScalarNode scalar(String value) {
        return SdkScalarNode.create(value);
    }

    private SdkArrayNode array(SdkJsonNode... nodes) {
        SdkArrayNode.Builder builder = SdkArrayNode.builder();
        Arrays.stream(nodes).forEach(builder::addItem);
        return builder.build();
    }

    private SdkJsonNode parse(String json) throws IOException {
        return parser.parse(new StringInputStream(json));
    }


}
