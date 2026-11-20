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

package software.amazon.awssdk.mapper.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.mapper.dynamodb.jsonfixtures.JsonModelFixtures;
import software.amazon.awssdk.mapper.dynamodb.jsonfixtures.JsonModelFixtures.ReadCase;

/**
 * Checks {@link DynamoDBTypeConvertedJson.Converter} against JSON recorded from the v1 mapper.
 *
 * <p>{@code type_converted_json_fixture.json} holds the output of v1's
 * {@code com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConvertedJson.Converter} from
 * {@code aws-java-sdk-dynamodb} 1.12.797, running on jackson-databind 2.17.2, the version that release depends on,
 * for every case in {@link JsonModelFixtures}. Write cases hold the JSON v1 wrote; read cases hold the object v1 read,
 * serialized again with a default {@code ObjectMapper}.
 */
class DynamoDBTypeConvertedJsonV1CompatibilityTest {

    private static final String FIXTURE = "type_converted_json_fixture.json";

    /**
     * Classes with two or more properties that have a getter but no field. jackson-databind writes those properties
     * after the ones with fields, ordered among themselves by {@code Class.getDeclaredMethods()}, which the JVM doesn't
     * specify, so their JSON can differ between JVMs even in v1. Cases that include them are compared without regard
     * to property order. A class with a single getter-only property has a stable order and is compared byte for byte.
     */
    private static final Set<String> GETTER_ONLY_CLASSES = new HashSet<>(Arrays.asList(
        "SubclassWithFieldsAndGetters", "GetterOnly", "ManglesGetterNames"));

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final Map<String, String> RECORDED = load();

    private static final DynamoDBTypeConvertedJson ANNOTATION = annotation();

    @DynamoDBTypeConvertedJson
    private Object holder;

    static Stream<Arguments> writeCases() {
        List<Object> writes = JsonModelFixtures.writeCorpus();
        return Stream.iterate(0, i -> i + 1).limit(writes.size())
                     .map(i -> Arguments.of(writeName(i, writes.get(i)), writes.get(i)));
    }

    static Stream<Arguments> readCases() {
        List<ReadCase> reads = JsonModelFixtures.readCorpus();
        return Stream.iterate(0, i -> i + 1).limit(reads.size())
                     .map(i -> Arguments.of(readName(i, reads.get(i)), reads.get(i)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("writeCases")
    void convert_matchesV1(String name, Object value) throws IOException {
        assertMatchesRecorded(name, converter(value == null ? Object.class : value.getClass()).convert(value));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("readCases")
    void unconvert_matchesV1(String name, ReadCase readCase) throws IOException {
        Object read = converter(readCase.targetType()).unconvert(readCase.json());

        assertMatchesRecorded(name, JSON.writeValueAsString(read));
    }

    @Test
    void fixture_hasExactlyOneEntryPerCase() {
        Map<String, String> expected = new LinkedHashMap<>();
        writeCases().forEach(a -> expected.put((String) a.get()[0], ""));
        readCases().forEach(a -> expected.put((String) a.get()[0], ""));

        assertThat(RECORDED.keySet()).containsExactlyInAnyOrderElementsOf(expected.keySet());
    }

    private static void assertMatchesRecorded(String name, String actual) throws IOException {
        String recorded = RECORDED.get(name);
        assertThat(recorded).as("recorded v1 output for %s", name).isNotNull();
        if (GETTER_ONLY_CLASSES.contains(name.substring(name.indexOf('-') + 1))) {
            assertThat(JSON.readTree(actual)).as(name).isEqualTo(JSON.readTree(recorded));
        } else {
            assertThat(actual).as(name).isEqualTo(recorded);
        }
    }

    private static String writeName(int index, Object value) {
        return String.format("write/%03d-%s", index, value == null ? "null" : value.getClass().getSimpleName());
    }

    private static String readName(int index, ReadCase readCase) {
        return String.format("read/%03d-%s", index, readCase.targetType().getSimpleName());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static DynamoDBTypeConvertedJson.Converter<Object> converter(Class<?> type) {
        return new DynamoDBTypeConvertedJson.Converter(type, ANNOTATION);
    }

    private static Map<String, String> load() {
        try (InputStream in = DynamoDBTypeConvertedJsonV1CompatibilityTest.class.getClassLoader()
                                                                                .getResourceAsStream(FIXTURE)) {
            return JSON.readValue(in, new TypeReference<LinkedHashMap<String, String>>() { });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static DynamoDBTypeConvertedJson annotation() {
        try {
            return DynamoDBTypeConvertedJsonV1CompatibilityTest.class.getDeclaredField("holder")
                                                                     .getAnnotation(DynamoDBTypeConvertedJson.class);
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }
}
