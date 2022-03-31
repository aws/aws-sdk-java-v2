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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructMap;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesUnionStructure;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesUnionStructure.Type;
import software.amazon.awssdk.services.protocolrestjson.model.BaseType;
import software.amazon.awssdk.services.protocolrestjson.model.EnumType;
import software.amazon.awssdk.services.protocolrestjson.model.RecursiveStructType;
import software.amazon.awssdk.services.protocolrestjson.model.StructWithNestedBlobType;
import software.amazon.awssdk.services.protocolrestjson.model.StructWithTimestamp;
import software.amazon.awssdk.services.protocolrestjson.model.SubTypeOne;

public class UnionTypeTest {
    public static List<TestCase<?>> testCases() {
        List<TestCase<?>> tests = new ArrayList<>();
        tests.add(new TestCase<>(Type.STRING_MEMBER,
                                 "foo",
                                 AllTypesUnionStructure::fromStringMember,
                                 AllTypesUnionStructure::stringMember,
                                 AllTypesUnionStructure.Builder::stringMember));
        tests.add(new TestCase<>(Type.INTEGER_MEMBER,
                                 5,
                                 AllTypesUnionStructure::fromIntegerMember,
                                 AllTypesUnionStructure::integerMember,
                                 AllTypesUnionStructure.Builder::integerMember));
        tests.add(new TestCase<>(Type.BOOLEAN_MEMBER,
                                 true,
                                 AllTypesUnionStructure::fromBooleanMember,
                                 AllTypesUnionStructure::booleanMember,
                                 AllTypesUnionStructure.Builder::booleanMember));
        tests.add(new TestCase<>(Type.FLOAT_MEMBER,
                                 0.1f,
                                 AllTypesUnionStructure::fromFloatMember,
                                 AllTypesUnionStructure::floatMember,
                                 AllTypesUnionStructure.Builder::floatMember));
        tests.add(new TestCase<>(Type.DOUBLE_MEMBER,
                                 0.1d,
                                 AllTypesUnionStructure::fromDoubleMember,
                                 AllTypesUnionStructure::doubleMember,
                                 AllTypesUnionStructure.Builder::doubleMember));
        tests.add(new TestCase<>(Type.LONG_MEMBER,
                                 5L,
                                 AllTypesUnionStructure::fromLongMember,
                                 AllTypesUnionStructure::longMember,
                                 AllTypesUnionStructure.Builder::longMember));
        tests.add(new TestCase<>(Type.SHORT_MEMBER,
                                 (short) 5,
                                 AllTypesUnionStructure::fromShortMember,
                                 AllTypesUnionStructure::shortMember,
                                 AllTypesUnionStructure.Builder::shortMember));
        tests.add(new TestCase<>(Type.ENUM_MEMBER,
                                 EnumType.ENUM_VALUE1,
                                 AllTypesUnionStructure::fromEnumMember,
                                 AllTypesUnionStructure::enumMember,
                                 AllTypesUnionStructure.Builder::enumMember));
        tests.add(new TestCase<>(Type.SIMPLE_LIST,
                                 emptyList(),
                                 AllTypesUnionStructure::fromSimpleList,
                                 AllTypesUnionStructure::simpleList,
                                 AllTypesUnionStructure.Builder::simpleList));
        tests.add(new TestCase<>(Type.LIST_OF_ENUMS,
                                 emptyList(),
                                 AllTypesUnionStructure::fromListOfEnums,
                                 AllTypesUnionStructure::listOfEnums,
                                 AllTypesUnionStructure.Builder::listOfEnums));
        tests.add(new TestCase<>(Type.LIST_OF_MAPS,
                                 emptyList(),
                                 AllTypesUnionStructure::fromListOfMaps,
                                 AllTypesUnionStructure::listOfMaps,
                                 AllTypesUnionStructure.Builder::listOfMaps));
        tests.add(new TestCase<>(Type.LIST_OF_LIST_OF_MAPS_OF_STRING_TO_STRUCT,
                                 emptyList(),
                                 AllTypesUnionStructure::fromListOfListOfMapsOfStringToStruct,
                                 AllTypesUnionStructure::listOfListOfMapsOfStringToStruct,
                                 AllTypesUnionStructure.Builder::listOfListOfMapsOfStringToStruct));
        tests.add(new TestCase<>(Type.LIST_OF_MAPS_OF_STRING_TO_STRUCT,
                                 emptyList(),
                                 AllTypesUnionStructure::fromListOfMapsOfStringToStruct,
                                 AllTypesUnionStructure::listOfMapsOfStringToStruct,
                                 AllTypesUnionStructure.Builder::listOfMapsOfStringToStruct));
        tests.add(new TestCase<>(Type.LIST_OF_STRUCTS,
                                 emptyList(),
                                 AllTypesUnionStructure::fromListOfStructs,
                                 AllTypesUnionStructure::listOfStructs,
                                 AllTypesUnionStructure.Builder::listOfStructs));
        tests.add(new TestCase<>(Type.MAP_OF_STRING_TO_INTEGER_LIST,
                                 emptyMap(),
                                 AllTypesUnionStructure::fromMapOfStringToIntegerList,
                                 AllTypesUnionStructure::mapOfStringToIntegerList,
                                 AllTypesUnionStructure.Builder::mapOfStringToIntegerList));
        tests.add(new TestCase<>(Type.MAP_OF_STRING_TO_STRING,
                                 emptyMap(),
                                 AllTypesUnionStructure::fromMapOfStringToString,
                                 AllTypesUnionStructure::mapOfStringToString,
                                 AllTypesUnionStructure.Builder::mapOfStringToString));
        tests.add(new TestCase<>(Type.MAP_OF_STRING_TO_STRUCT,
                                 emptyMap(),
                                 AllTypesUnionStructure::fromMapOfStringToStruct,
                                 AllTypesUnionStructure::mapOfStringToStruct,
                                 AllTypesUnionStructure.Builder::mapOfStringToStruct));
        tests.add(new TestCase<>(Type.MAP_OF_ENUM_TO_ENUM,
                                 emptyMap(),
                                 AllTypesUnionStructure::fromMapOfEnumToEnum,
                                 AllTypesUnionStructure::mapOfEnumToEnum,
                                 AllTypesUnionStructure.Builder::mapOfEnumToEnum));
        tests.add(new TestCase<>(Type.TIMESTAMP_MEMBER,
                                 Instant.now(),
                                 AllTypesUnionStructure::fromTimestampMember,
                                 AllTypesUnionStructure::timestampMember,
                                 AllTypesUnionStructure.Builder::timestampMember));
        tests.add(new TestCase<>(Type.STRUCT_WITH_NESTED_TIMESTAMP_MEMBER,
                                 StructWithTimestamp.builder().build(),
                                 AllTypesUnionStructure::fromStructWithNestedTimestampMember,
                                 AllTypesUnionStructure::structWithNestedTimestampMember,
                                 AllTypesUnionStructure.Builder::structWithNestedTimestampMember));
        tests.add(new TestCase<>(Type.BLOB_ARG,
                                 SdkBytes.fromUtf8String(""),
                                 AllTypesUnionStructure::fromBlobArg,
                                 AllTypesUnionStructure::blobArg,
                                 AllTypesUnionStructure.Builder::blobArg));
        tests.add(new TestCase<>(Type.STRUCT_WITH_NESTED_BLOB,
                                 StructWithNestedBlobType.builder().build(),
                                 AllTypesUnionStructure::fromStructWithNestedBlob,
                                 AllTypesUnionStructure::structWithNestedBlob,
                                 AllTypesUnionStructure.Builder::structWithNestedBlob));
        tests.add(new TestCase<>(Type.BLOB_MAP,
                                 emptyMap(),
                                 AllTypesUnionStructure::fromBlobMap,
                                 AllTypesUnionStructure::blobMap,
                                 AllTypesUnionStructure.Builder::blobMap));
        tests.add(new TestCase<>(Type.LIST_OF_BLOBS,
                                 emptyList(),
                                 AllTypesUnionStructure::fromListOfBlobs,
                                 AllTypesUnionStructure::listOfBlobs,
                                 AllTypesUnionStructure.Builder::listOfBlobs));
        tests.add(new TestCase<>(Type.RECURSIVE_STRUCT,
                                 RecursiveStructType.builder().build(),
                                 AllTypesUnionStructure::fromRecursiveStruct,
                                 AllTypesUnionStructure::recursiveStruct,
                                 AllTypesUnionStructure.Builder::recursiveStruct));
        tests.add(new TestCase<>(Type.POLYMORPHIC_TYPE_WITH_SUB_TYPES,
                                 BaseType.builder().build(),
                                 AllTypesUnionStructure::fromPolymorphicTypeWithSubTypes,
                                 AllTypesUnionStructure::polymorphicTypeWithSubTypes,
                                 AllTypesUnionStructure.Builder::polymorphicTypeWithSubTypes));
        tests.add(new TestCase<>(Type.POLYMORPHIC_TYPE_WITHOUT_SUB_TYPES,
                                 SubTypeOne.builder().build(),
                                 AllTypesUnionStructure::fromPolymorphicTypeWithoutSubTypes,
                                 AllTypesUnionStructure::polymorphicTypeWithoutSubTypes,
                                 AllTypesUnionStructure.Builder::polymorphicTypeWithoutSubTypes));
        tests.add(new TestCase<>(Type.SET_PREFIXED_MEMBER,
                                 "foo",
                                 AllTypesUnionStructure::fromSetPrefixedMember,
                                 AllTypesUnionStructure::setPrefixedMember,
                                 AllTypesUnionStructure.Builder::setPrefixedMember));
        tests.add(new TestCase<>(Type.UNION_MEMBER,
                                 AllTypesUnionStructure.builder().build(),
                                 AllTypesUnionStructure::fromUnionMember,
                                 AllTypesUnionStructure::unionMember,
                                 AllTypesUnionStructure.Builder::unionMember));
        return tests;
    }

    @Test
    public void allTypesTested() {
        Set<Type> types = EnumSet.allOf(Type.class);
        types.remove(Type.UNKNOWN_TO_SDK_VERSION);
        Set<Type> testedTypes = testCases().stream().map(t -> t.type).collect(Collectors.toSet());

        assertThat(testedTypes).isEqualTo(types); // If this fails, it means you need to add a test case above
    }

    @Test
    public void noMembersIsUnknownType() {
        assertThat(AllTypesUnionStructure.builder().build().type()).isEqualTo(Type.UNKNOWN_TO_SDK_VERSION);
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public <T> void twoMembersIsNull(TestCase<T> testCase) {
        AllTypesUnionStructure.Builder builder = AllTypesUnionStructure.builder();

        testCase.setter.apply(builder, testCase.value);
        if (testCase.type == Type.STRING_MEMBER) {
            builder.integerMember(5);
        } else {
            builder.stringMember("foo");
        }

        assertThat(builder.build().type()).isNull();
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public <T> void twoMinusOneMemberIsKnownType(TestCase<T> testCase) {
        AllTypesUnionStructure.Builder builder = AllTypesUnionStructure.builder();

        if (testCase.type == Type.STRING_MEMBER) {
            builder.integerMember(5);
        } else {
            builder.stringMember("foo");
        }

        testCase.setter.apply(builder, testCase.value);

        if (testCase.type == Type.STRING_MEMBER) {
            builder.integerMember(null);
        } else {
            builder.stringMember(null);
        }

        assertThat(builder.build().type()).isEqualTo(testCase.type);
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public <T> void staticConstructorsWork(TestCase<T> testCase) {
        AllTypesUnionStructure structure = testCase.constructor.apply(testCase.value);
        T valuePassedThrough = testCase.getter.apply(structure);
        assertThat(valuePassedThrough).isEqualTo(testCase.value);
        assertThat(structure.type()).isEqualTo(testCase.type);
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public <T> void typeIsPreservedThroughToBuilder(TestCase<T> testCase) {
        AllTypesUnionStructure structure = testCase.constructor.apply(testCase.value);
        assertThat(structure.toBuilder().build().type()).isEqualTo(testCase.type);
    }

    @Test
    public void autoConstructListsArentCountedForType() {
        assertThat(AllTypesUnionStructure.builder()
                                         .stringMember("foo")
                                         .listOfMaps(DefaultSdkAutoConstructList.getInstance())
                                         .build()
                                         .type())
            .isEqualTo(Type.STRING_MEMBER);
        assertThat(AllTypesUnionStructure.builder()
                                         .listOfMaps(DefaultSdkAutoConstructList.getInstance())
                                         .stringMember("foo")
                                         .build()
                                         .type())
            .isEqualTo(Type.STRING_MEMBER);
    }

    @Test
    public void autoConstructMapsArentCountedForType() {
        assertThat(AllTypesUnionStructure.builder()
                                         .stringMember("foo")
                                         .mapOfEnumToEnum(DefaultSdkAutoConstructMap.getInstance())
                                         .build()
                                         .type())
            .isEqualTo(Type.STRING_MEMBER);
        assertThat(AllTypesUnionStructure.builder()
                                         .mapOfEnumToEnum(DefaultSdkAutoConstructMap.getInstance())
                                         .stringMember("foo")
                                         .build()
                                         .type())
            .isEqualTo(Type.STRING_MEMBER);
    }

    private static class TestCase<T> {
        private final Type type;
        private final T value;
        private final Function<T, AllTypesUnionStructure> constructor;
        private final Function<AllTypesUnionStructure, T> getter;
        private final BiFunction<AllTypesUnionStructure.Builder, T, AllTypesUnionStructure.Builder> setter;

        public TestCase(Type type,
                        T value,
                        Function<T, AllTypesUnionStructure> constructor,
                        Function<AllTypesUnionStructure, T> getter,
                        BiFunction<AllTypesUnionStructure.Builder, T, AllTypesUnionStructure.Builder> setter) {
            this.type = type;
            this.value = value;
            this.constructor = constructor;
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        public String toString() {
            return type + " test";
        }
    }
}
