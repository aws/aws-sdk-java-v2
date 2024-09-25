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

package software.amazon.awssdk.benchmark.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.protocolec2.model.AllTypesRequest;

/**
 * Contains constants used by the benchmarks
 */
@SuppressWarnings("unchecked")
public final class BenchmarkConstant {

    public static final String DEFAULT_JDK_SSL_PROVIDER = "jdk";
    public static final String OPEN_SSL_PROVIDER = "openssl";

    public static final int CONCURRENT_CALLS = 50;

    public static final Instant TIMESTAMP_MEMBER = LocalDateTime.now().toInstant(ZoneOffset.UTC);

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final String ERROR_JSON_BODY = "{}";

    public static final String ENCODED_SMITHY_RPCV2_BODY = ""
                                                           + "bf6c537472696e674d656d62657263666f6f6d49"
                                                           + "6e74656765724d656d626572187b6d426f6f6c65"
                                                           + "616e4d656d626572f56b466c6f61744d656d6265"
                                                           + "72187b6c446f75626c654d656d626572fb405ef9"
                                                           + "999999999a6a4c6f6e674d656d626572187b6a53"
                                                           + "696d706c654c6973748169736f2073696d706c65"
                                                           + "6d4c6973744f665374727563747381bf6c537472"
                                                           + "696e674d656d6265726e6c6973744f6653747275"
                                                           + "63747331ff6f54696d657374616d704d656d6265"
                                                           + "72c1fb41d6f66221b8c49c781f53747275637457"
                                                           + "6974684e657374656454696d657374616d704d65"
                                                           + "6d626572bf6f4e657374656454696d657374616d"
                                                           + "70c1fb41d6f66221ba1cacff67426c6f62417267"
                                                           + "4b68656c6c6f20776f726c64ff";

    public static final String ERROR_ENCODED_SMITHY_RPCV2_BODY = "bfff";

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

    public static final String ERROR_XML_BODY = "<ErrorResponse>"
                                                + "   <Error>"
                                                + "      <Code>ImplicitPayloadException</Code>"
                                                + "      <Message>this is the service message</Message>"
                                                + "      <StringMember>foo</StringMember>"
                                                + "      <IntegerMember>42</IntegerMember>"
                                                + "      <LongMember>9001</LongMember>"
                                                + "      <DoubleMember>1234.56</DoubleMember>"
                                                + "      <FloatMember>789.10</FloatMember>"
                                                + "      <TimestampMember>2015-01-25T08:00:12Z</TimestampMember>"
                                                + "      <BooleanMember>true</BooleanMember>"
                                                + "      <BlobMember>dGhlcmUh</BlobMember>"
                                                + "      <ListMember>"
                                                + "         <member>valOne</member>"
                                                + "         <member>valTwo</member>"
                                                + "      </ListMember>"
                                                + "      <MapMember>"
                                                + "         <entry>"
                                                + "            <key>keyOne</key>"
                                                + "            <value>valOne</value>"
                                                + "         </entry>"
                                                + "         <entry>"
                                                + "            <key>keyTwo</key>"
                                                + "            <value>valTwo</value>"
                                                + "         </entry>"
                                                + "      </MapMember>"
                                                + "      <SimpleStructMember>"
                                                + "         <StringMember>foobar</StringMember>"
                                                + "      </SimpleStructMember>"
                                                + "   </Error>"
                                                + "</ErrorResponse>";

    public static final software.amazon.awssdk.services.protocolrestxml.model.AllTypesRequest XML_ALL_TYPES_REQUEST =
        software.amazon.awssdk.services.protocolrestxml.model.AllTypesRequest.builder()
                                                                             .stringMember("foo")
                                                                             .integerMember(123)
                                                                             .booleanMember(true)
                                                                             .floatMember(123.0f)
                                                                             .doubleMember(123.9)
                                                                             .longMember(123L)
                                                                             .simpleStructMember(b -> b.stringMember(
                                                                                 "bar"))
                                                                             .simpleList("so simple")
                                                                             .listOfStructs(b -> b.stringMember(
                                                                                 "listOfStructs1").stringMember(
                                                                                 "listOfStructs1"))
                                                                             .timestampMember(
                                                                                 TIMESTAMP_MEMBER)
                                                                             .structWithNestedTimestampMember(
                                                                                 b -> b.nestedTimestamp(TIMESTAMP_MEMBER))
                                                                             .blobArg(SdkBytes.fromUtf8String("hello "
                                                                                                              + "world"))
                                                                             .build();
    public static final software.amazon.awssdk.services.protocolquery.model.AllTypesRequest QUERY_ALL_TYPES_REQUEST =
        software.amazon.awssdk.services.protocolquery.model.AllTypesRequest.builder()
                                                                           .stringMember("foo")
                                                                           .integerMember(123)
                                                                           .booleanMember(true)
                                                                           .floatMember(123.0f)
                                                                           .doubleMember(123.9)
                                                                           .longMember(123L)
                                                                           .simpleStructMember(b -> b.stringMember(
                                                                               "bar"))
                                                                           .simpleList("so simple")
                                                                           .listOfStructs(b -> b.stringMember(
                                                                               "listOfStructs1").stringMember(
                                                                               "listOfStructs1"))
                                                                           .timestampMember(
                                                                               TIMESTAMP_MEMBER)
                                                                           .structWithNestedTimestampMember(
                                                                               b -> b.nestedTimestamp(
                                                                                   TIMESTAMP_MEMBER))
                                                                           .blobArg(SdkBytes.fromUtf8String("hello " +
                                                                                                            "world"))
                                                                           .build();


    public static final software.amazon.awssdk.services.protocolrestjson.model.AllTypesRequest JSON_ALL_TYPES_REQUEST =
        software.amazon.awssdk.services.protocolrestjson.model.AllTypesRequest.builder()
                                                                              .stringMember("foo")
                                                                              .integerMember(123)
                                                                              .booleanMember(true)
                                                                              .floatMember(123.0f)
                                                                              .doubleMember(123.9)
                                                                              .longMember(123L)
                                                                              .simpleList("so simple")
                                                                              .listOfStructs(b -> b.stringMember(
                                                                                  "listOfStructs1").stringMember(
                                                                                  "listOfStructs1"))
                                                                              .timestampMember(
                                                                                  TIMESTAMP_MEMBER)
                                                                              .structWithNestedTimestampMember(
                                                                                  b -> b.nestedTimestamp(
                                                                                      TIMESTAMP_MEMBER))
                                                                              .blobArg(SdkBytes.fromUtf8String("hello "
                                                                                                               + "world"))
                                                                              .build();

    public static final software.amazon.awssdk.services.protocolsmithyrpcv2.model.AllTypesRequest RPCV2_ALL_TYPES_REQUEST =
        software.amazon.awssdk.services.protocolsmithyrpcv2.model.AllTypesRequest.builder()
                                                                                 .stringMember("foo")
                                                                                 .integerMember(123)
                                                                                 .booleanMember(true)
                                                                                 .floatMember(123.0f)
                                                                                 .doubleMember(123.9)
                                                                                 .longMember(123L)
                                                                                 .simpleList("so simple")
                                                                                 .listOfStructs(b -> b.stringMember(
                                                                                     "listOfStructs1").stringMember(
                                                                                     "listOfStructs1"))
                                                                                 .timestampMember(
                                                                                     TIMESTAMP_MEMBER)
                                                                                 .structWithNestedTimestampMember(
                                                                                     b -> b.nestedTimestamp(
                                                                                         TIMESTAMP_MEMBER))
                                                                                 .blobArg(SdkBytes.fromUtf8String("hello "
                                                                                                                  + "world"))
                                                                                 .build();

    public static final AllTypesRequest EC2_ALL_TYPES_REQUEST =
        AllTypesRequest.builder()
                       .stringMember("foo")
                       .integerMember(123)
                       .booleanMember(true)
                       .floatMember(123.0f)
                       .doubleMember(123.9)
                       .longMember(123L)
                       .simpleStructMember(b -> b.stringMember(
                           "bar"))
                       .simpleList("so simple")
                       .listOfStructs(b -> b.stringMember(
                           "listOfStructs1").stringMember(
                           "listOfStructs1"))
                       .timestampMember(TIMESTAMP_MEMBER)
                       .structWithNestedTimestampMember(b -> b.nestedTimestamp(
                           TIMESTAMP_MEMBER))
                       .blobArg(SdkBytes.fromUtf8String("hello "
                                                        + "world"))
                       .build();

    private BenchmarkConstant() {
    }
}
