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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.protocolec2.model.AllTypesRequest;

/**
 * Class javadoc
 */
public class ApiCallUtils {

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
