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

package software.amazon.awssdk.services;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.time.Instant;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolrestjson.model.BaseType;
import software.amazon.awssdk.services.protocolrestjson.model.RecursiveStructType;
import software.amazon.awssdk.services.protocolrestjson.model.SimpleStruct;
import software.amazon.awssdk.services.protocolrestjson.model.StructWithNestedBlobType;
import software.amazon.awssdk.services.protocolrestjson.model.StructWithTimestamp;
import software.amazon.awssdk.services.protocolrestjson.model.SubTypeOne;

/**
 * Verify that modeled objects can be marshalled using Jackson.
 */
public class ModelSerializationTest {
    @Test
    public void jacksonSerializationWorksForEmptyRequestObjects() throws IOException {
        validateJacksonSerialization(AllTypesRequest.builder().build());
    }

    @Test
    public void jacksonSerializationWorksForPopulatedRequestModels() throws IOException {
        SdkBytes blob = SdkBytes.fromUtf8String("foo");

        SimpleStruct simpleStruct = SimpleStruct.builder().stringMember("foo").build();
        StructWithTimestamp structWithTimestamp = StructWithTimestamp.builder().nestedTimestamp(Instant.EPOCH).build();
        StructWithNestedBlobType structWithNestedBlob = StructWithNestedBlobType.builder().nestedBlob(blob).build();
        RecursiveStructType recursiveStruct = RecursiveStructType.builder()
                                                                 .recursiveStruct(RecursiveStructType.builder().build())
                                                                 .build();
        BaseType baseType = BaseType.builder().baseMember("foo").build();
        SubTypeOne subtypeOne = SubTypeOne.builder().subTypeOneMember("foo").build();

        validateJacksonSerialization(AllTypesRequest.builder()
                                                    .stringMember("foo")
                                                    .integerMember(5)
                                                    .booleanMember(true)
                                                    .floatMember(5F)
                                                    .doubleMember(5D)
                                                    .longMember(5L)
                                                    .simpleList("foo", "bar")
                                                    .listOfMaps(singletonList(singletonMap("foo", "bar")))
                                                    .listOfStructs(simpleStruct)
                                                    .mapOfStringToIntegerList(singletonMap("foo", singletonList(5)))
                                                    .mapOfStringToStruct(singletonMap("foo", simpleStruct))
                                                    .timestampMember(Instant.EPOCH)
                                                    .structWithNestedTimestampMember(structWithTimestamp)
                                                    .blobArg(blob)
                                                    .structWithNestedBlob(structWithNestedBlob)
                                                    .blobMap(singletonMap("foo", blob))
                                                    .listOfBlobs(blob, blob)
                                                    .recursiveStruct(recursiveStruct)
                                                    .polymorphicTypeWithSubTypes(baseType)
                                                    .polymorphicTypeWithoutSubTypes(subtypeOne)
                                                    .enumMember("foo")
                                                    .listOfEnumsWithStrings("foo", "bar")
                                                    .mapOfEnumToEnumWithStrings(singletonMap("foo", "bar"))
                                                    .build());
    }

    private void validateJacksonSerialization(AllTypesRequest original) throws IOException {
        SimpleModule instantModule = new SimpleModule();
        instantModule.addSerializer(Instant.class, new InstantSerializer());
        instantModule.addDeserializer(Instant.class, new InstantDeserializer());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(instantModule);

        String serialized = mapper.writeValueAsString(original.toBuilder());
        AllTypesRequest deserialized = mapper.readValue(serialized, AllTypesRequest.serializableBuilderClass()).build();
        assertThat(deserialized).isEqualTo(original);

    }

    private class InstantSerializer extends JsonSerializer<Instant> {
        @Override
        public void serialize(Instant t, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
                throws IOException {
            jsonGenerator.writeString(t.toString());
        }
    }

    private class InstantDeserializer extends JsonDeserializer<Instant> {
        @Override
        public Instant deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            return Instant.parse(jsonParser.getText());
        }
    }
}
