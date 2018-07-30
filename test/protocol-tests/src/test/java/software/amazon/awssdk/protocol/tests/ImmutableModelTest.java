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

package software.amazon.awssdk.protocol.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.protocolrestjson.model.AllTypesRequest;
import software.amazon.awssdk.services.protocolrestjson.model.SimpleStruct;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * Verifies that the models are actually immutable.
 */
public class ImmutableModelTest {
    @Test
    public void mapsAreImmutable() {
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");

        AllTypesRequest request = AllTypesRequest.builder().mapOfStringToString(map).build();
        map.put("key2", "value2");

        assertThat(request.mapOfStringToString()).doesNotContainKey("key2");
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> request.mapOfStringToString().put("key2", "value2"));
    }

    @Test
    public void listsAreImmutable() {
        SimpleStruct struct = SimpleStruct.builder().stringMember("value").build();
        SimpleStruct struct2 = SimpleStruct.builder().stringMember("value2").build();

        List<SimpleStruct> list = new ArrayList<>();
        list.add(struct);

        AllTypesRequest request = AllTypesRequest.builder().listOfStructs(list).build();
        list.add(struct2);

        assertThat(request.listOfStructs()).doesNotContain(struct2);
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> request.listOfStructs().add(struct2));
    }

    @Test
    public void mapsOfListsAreImmutable() {
        List<Integer> list = new ArrayList<>();
        list.add(1);

        Map<String, List<Integer>> map = Collections.singletonMap("key", list);

        AllTypesRequest request = AllTypesRequest.builder().mapOfStringToIntegerList(map).build();

        list.add(2);

        assertThat(request.mapOfStringToIntegerList().get("key")).doesNotContain(2);
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> request.mapOfStringToIntegerList().get("key").add(2));
    }

    @Test
    public void byteBuffersAreImmutable() {
        ByteBuffer buffer = ByteBuffer.wrap("Hello".getBytes(StandardCharsets.UTF_8));

        buffer.position(1);

        AllTypesRequest request = AllTypesRequest.builder().blobArg(SdkBytes.fromByteBuffer(buffer)).build();

        buffer.array()[1] = ' ';

        assertThat(request.blobArg().asByteBuffer()).as("Check new read-only blob each time")
                                                    .isNotSameAs(request.blobArg().asByteBuffer());
        assertThat(request.blobArg().asByteBuffer().isReadOnly()).as("Check read-only").isTrue();
        assertThat(BinaryUtils.copyAllBytesFrom(request.blobArg().asByteBuffer()))
                .as("Check copy contents").containsExactly('e', 'l', 'l', 'o');
    }
}
