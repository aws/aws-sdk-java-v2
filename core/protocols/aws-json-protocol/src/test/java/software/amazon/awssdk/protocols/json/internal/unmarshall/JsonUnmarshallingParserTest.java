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

package software.amazon.awssdk.protocols.json.internal.unmarshall;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.thirdparty.jackson.core.JsonParseException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonUnmarshallingParserTest {

    @Test
    public void parsingAMapFieldThrowsOnNumberFoundInstead() {
        JsonUnmarshallingParser parser = parser();
        UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> {
            parser.parse(TestRequest.builder(), from("{\"mapOfStringToStringMember\": 123}"));
        });
        assertNotNull(e.getCause());
        assertInstanceOf(JsonParseException.class, e.getCause());
    }

    @Test
    public void parsingAListFieldThrowsOnNumberFoundInstead() {
        JsonUnmarshallingParser parser = parser();
        UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> {
            parser.parse(TestRequest.builder(), from("{\"listOfStringsMember\": 123}"));
        });
        assertNotNull(e.getCause());
        assertInstanceOf(JsonParseException.class, e.getCause());
    }

    @Test
    public void parseOnEmptyInputReturnsAValidPojo() {
        JsonUnmarshallingParser parser = parser();
        TestRequest req = (TestRequest) parser.parse(TestRequest.builder(), from(""));
        assertNotNull(req);
    }

    @Test
    public void parseOnJsonNullLiteralReturnsNull() {
        JsonUnmarshallingParser parser = parser();
        TestRequest req = (TestRequest) parser.parse(TestRequest.builder(), from("null"));
        assertNull(req);
    }

    static JsonUnmarshallingParser parser() {
        ProtocolUnmarshallDependencies dependencies = JsonProtocolUnmarshaller.defaultProtocolUnmarshallDependencies();
        JsonUnmarshallingParser parser = JsonUnmarshallingParser
            .builder()
            .jsonFactory(dependencies.jsonFactory())
            .unmarshallerRegistry(dependencies.jsonUnmarshallerRegistry())
            .build();
        return parser;
    }

    static InputStream from(String source) {
        return new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8));
    }


}