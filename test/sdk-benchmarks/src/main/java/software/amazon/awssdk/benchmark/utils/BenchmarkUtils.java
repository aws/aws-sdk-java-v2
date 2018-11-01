/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.benchmark.utils;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.protocolec2.model.AllTypesRequest;

public final class BenchmarkUtils {

    public static final int PORT_NUMBER = 8089;
    public static final String JSON_BODY = "{\"StringMember\":\"foo\",\"IntegerMember\":123,\"BooleanMember\":true,"
                                            + "\"FloatMember\":123.0,\"DoubleMember\":123.9,\"LongMember\":123,"
                                            + "\"SimpleList\":[\"so simple\"],"
                                            + "\"ListOfStructs\":[{\"StringMember\":\"listOfStructs1\"}],"
                                            + "\"TimestampMember\":1540982918.887,"
                                            + "\"StructWithNestedTimestampMember\":{\"NestedTimestamp\":1540982918.908},"
                                            + "\"BlobArg\":\"aGVsbG8gd29ybGQ=\"}";

    public static final String XML_BODY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><AllTypesResponse "
                                           + "xmlns=\"https://restxml/\"><stringMember>foo</stringMember><integerMember>123"
                                           + "</integerMember><booleanMember>true</booleanMember><floatMember>123"
                                           + ".0</floatMember><doubleMember>123"
                                           + ".9</doubleMember><longMember>123</longMember><simpleList><member>so "
                                           + "simple</member></simpleList><listOfStructs><member><StringMember>listOfStructs1"
                                           + "</StringMember></member></listOfStructs><timestampMember>2018-10-31T10:51:12"
                                           + ".302183Z</timestampMember><structWithNestedTimestampMember><NestedTimestamp>2018"
                                           + "-10-31T10:51:12.311305Z</NestedTimestamp></structWithNestedTimestampMember"
                                           + "><blobArg>aGVsbG8gd29ybGQ=</blobArg></AllTypesResponse>";

    private BenchmarkUtils() {
    }

    public static URI getUri() {
        return URI.create(String.format("http://localhost:%s", PORT_NUMBER));
    }

    @SuppressWarnings("unchecked")
    public static AllTypesRequest ec2AllTypeRequest() {
        return AllTypesRequest.builder()
                              .stringMember("foo")
                              .integerMember(123)
                              .booleanMember(true)
                              .floatMember((float) 123.0)
                              .doubleMember(123.9)
                              .longMember(123L)
                              .simpleStructMember(b -> b.stringMember(
                                  "bar"))
                              .simpleList("so simple")
                              .listOfStructs(b -> b.stringMember(
                                  "listOfStructs1").stringMember(
                                  "listOfStructs1"))
                              .timestampMember(LocalDateTime.now().toInstant(ZoneOffset.UTC))
                              .structWithNestedTimestampMember(b -> b.nestedTimestamp(LocalDateTime.now().toInstant(ZoneOffset.UTC)))
                              .blobArg(SdkBytes.fromUtf8String("hello "
                                                               + "world"))
                              .build();
    }

    @SuppressWarnings("unchecked")
    public static software.amazon.awssdk.services.protocolrestxml.model.AllTypesRequest xmlAllTypeRequest() {
        return software.amazon.awssdk.services.protocolrestxml.model.AllTypesRequest.builder()
                                                                                    .stringMember("foo")
                                                                                    .integerMember(123)
                                                                                    .booleanMember(true)
                                                                                    .floatMember((float) 123.0)
                                                                                    .doubleMember(123.9)
                                                                                    .longMember(123L)
                                                                                    .simpleStructMember(b -> b.stringMember(
                                                                                        "bar"))
                                                                                    .simpleList("so simple")
                                                                                    .listOfStructs(b -> b.stringMember(
                                                                                        "listOfStructs1").stringMember(
                                                                                        "listOfStructs1"))
                                                                                    .timestampMember(LocalDateTime.now().toInstant(ZoneOffset.UTC))
                                                                                    .structWithNestedTimestampMember(b -> b.nestedTimestamp(LocalDateTime.now().toInstant(ZoneOffset.UTC)))
                                                                                    .blobArg(SdkBytes.fromUtf8String("hello "
                                                                                                                     + "world"))
                                                                                    .build();
    }

    @SuppressWarnings("unchecked")
    public static software.amazon.awssdk.services.protocolquery.model.AllTypesRequest queryAllTypeRequest() {
        return software.amazon.awssdk.services.protocolquery.model.AllTypesRequest.builder()
                                                                                  .stringMember("foo")
                                                                                  .integerMember(123)
                                                                                  .booleanMember(true)
                                                                                  .floatMember((float) 123.0)
                                                                                  .doubleMember(123.9)
                                                                                  .longMember(123L)
                                                                                  .simpleStructMember(b -> b.stringMember(
                                                                                      "bar"))
                                                                                  .simpleList("so simple")
                                                                                  .listOfStructs(b -> b.stringMember(
                                                                                      "listOfStructs1").stringMember(
                                                                                      "listOfStructs1"))
                                                                                  .timestampMember(LocalDateTime.now().toInstant(ZoneOffset.UTC))
                                                                                  .structWithNestedTimestampMember(b -> b.nestedTimestamp(LocalDateTime.now().toInstant(ZoneOffset.UTC)))
                                                                                  .blobArg(SdkBytes.fromUtf8String("hello " + "world"))
                                                                                  .build();
    }

    @SuppressWarnings("unchecked")
    public static software.amazon.awssdk.services.protocolrestjson.model.AllTypesRequest jsonAllTypeRequest() {
        return software.amazon.awssdk.services.protocolrestjson.model.AllTypesRequest.builder()
                                                                                     .stringMember("foo")
                                                                                     .integerMember(123)
                                                                                     .booleanMember(true)
                                                                                     .floatMember((float) 123.0)
                                                                                     .doubleMember(123.9)
                                                                                     .longMember(123L)
                                                                                     .simpleList("so simple")
                                                                                     .listOfStructs(b -> b.stringMember(
                                                                                         "listOfStructs1").stringMember(
                                                                                         "listOfStructs1"))
                                                                                     .timestampMember(LocalDateTime.now().toInstant(ZoneOffset.UTC))
                                                                                     .structWithNestedTimestampMember(b -> b.nestedTimestamp(LocalDateTime.now().toInstant(ZoneOffset.UTC)))
                                                                                     .blobArg(SdkBytes.fromUtf8String("hello "
                                                                                                                      + "world"))
                                                                                     .build();
    }
}
