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

package software.amazon.awssdk.services.stringarray;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.stringarray.endpoints.StringArrayEndpointParams;
import software.amazon.awssdk.services.stringarray.endpoints.StringArrayEndpointProvider;
import software.amazon.awssdk.services.stringarray.model.ObjectMember;
import software.amazon.awssdk.services.stringarray.model.UnionMember;

class StringArrayBindingsTest {
    private static final AwsCredentialsProvider CREDENTIALS = StaticCredentialsProvider.create(
        AwsBasicCredentials.create("akid", "skid"));

    private static final Region REGION = Region.of("us-east-1");

    private StringArrayEndpointProvider mockEndpointProvider;

    @BeforeEach
    public void setup() {
        mockEndpointProvider = mock(StringArrayEndpointProvider.class);
        when(mockEndpointProvider.resolveEndpoint(any(StringArrayEndpointParams.class)))
            .thenThrow(new RuntimeException("boom"));
    }

    @Test
    void noBindingsOperation_usesDefaultValues() {
        assertThatThrownBy(() -> createClient().noBindingsOperation(r -> {}))
            .isInstanceOf(RuntimeException.class);

        StringArrayEndpointParams params = captureEndpointParams();

        assertThat(params.stringArrayParam()).isNotEmpty()
                                             .hasSize(2)
                                             .containsExactly("defaultValue1", "defaultValue2");
    }

    @Test
    void emptyStaticContextOperation_hasEmptyArray() {
        assertThatThrownBy(() -> createClient().emptyStaticContextOperation(r -> {}))
            .isInstanceOf(RuntimeException.class);

        StringArrayEndpointParams params = captureEndpointParams();

        assertThat(params.stringArrayParam()).isEmpty();
    }

    @Test
    void staticContextOperation_hasStaticValue() {
        assertThatThrownBy(() -> createClient().staticContextOperation(r -> {}))
            .isInstanceOf(RuntimeException.class);

        StringArrayEndpointParams params = captureEndpointParams();

        assertThat(params.stringArrayParam()).isNotEmpty()
                                             .hasSize(1)
                                             .containsExactly("staticValue1");
    }

    @Test
    void listOfObjectsOperation_extractsSingleKeyFromNestedList() {
        assertThatThrownBy(() -> createClient().listOfObjectsOperation(r -> r.nested(n -> n.listOfObjects(
            ObjectMember.builder().key("key1").build()
        )))).isInstanceOf(RuntimeException.class);

        StringArrayEndpointParams params = captureEndpointParams();

        assertThat(params.stringArrayParam()).isNotEmpty()
                                             .hasSize(1)
                                             .containsExactly("key1");
    }

    @Test
    void listOfObjectsOperation_extractsMultipleKeysFromNestedList() {
        assertThatThrownBy(() -> createClient().listOfObjectsOperation(r -> r.nested(n -> n.listOfObjects(
            ObjectMember.builder().key("key1").build(),
            ObjectMember.builder().key("key2").build(),
            ObjectMember.builder().key("key3").build()
        )))).isInstanceOf(RuntimeException.class);

        StringArrayEndpointParams params = captureEndpointParams();

        assertThat(params.stringArrayParam()).isNotEmpty()
                                             .hasSize(3)
                                             .containsExactly("key1", "key2", "key3");
    }

    @Test
    void listOfObjectsOperation_withNullNested_returnsEmptyArray() {
        assertThatThrownBy(() -> createClient().listOfObjectsOperation(r -> {}))
            .isInstanceOf(RuntimeException.class);

        StringArrayEndpointParams params = captureEndpointParams();

        assertThat(params.stringArrayParam()).isEmpty();
    }

    @Test
    void listOfObjectsOperation_withEmptyList_returnsEmptyArray() {
        assertThatThrownBy(() -> createClient().listOfObjectsOperation(r -> r.nested(n -> n.listOfObjects(Collections.emptyList()))))
            .isInstanceOf(RuntimeException.class);

        StringArrayEndpointParams params = captureEndpointParams();

        assertThat(params.stringArrayParam()).isEmpty();
    }

    @Test
    void listOfObjectsOperation_filtersOutNullKeys() {
        assertThatThrownBy(() -> createClient().listOfObjectsOperation(r -> r.nested(n -> n.listOfObjects(
            ObjectMember.builder().key("key1").build(),
            ObjectMember.builder().build(), // null key
            ObjectMember.builder().key("key2").build()
        )))).isInstanceOf(RuntimeException.class);

        StringArrayEndpointParams params = captureEndpointParams();

        assertThat(params.stringArrayParam()).isNotEmpty()
                                             .hasSize(2)
                                             .containsExactly("key1", "key2");
    }

    @Test
    void listOfUnionsOperation_extractsStringValuesFromUnions() {
        assertThatThrownBy(() -> createClient().listOfUnionsOperation(r -> r.listOfUnions(
            UnionMember.builder().string("value1").build(),
            UnionMember.builder().string("value2").build()
        ))).isInstanceOf(RuntimeException.class);

        StringArrayEndpointParams params = captureEndpointParams();

        assertThat(params.stringArrayParam()).isNotEmpty()
                                             .hasSize(2)
                                             .containsExactly("value1", "value2");
    }

    @Test
    void listOfUnionsOperation_extractsKeysFromObjectMembers() {
        assertThatThrownBy(() -> createClient().listOfUnionsOperation(r -> r.listOfUnions(
            UnionMember.builder().object(o -> o.key("objKey1")).build(),
            UnionMember.builder().object(o -> o.key("objKey2")).build()
        ))).isInstanceOf(RuntimeException.class);

        StringArrayEndpointParams params = captureEndpointParams();

        assertThat(params.stringArrayParam()).isNotEmpty()
                                             .hasSize(2)
                                             .containsExactly("objKey1", "objKey2");
    }

    @Test
    void listOfUnionsOperation_extractsBothStringsAndObjectKeys() {
        assertThatThrownBy(() -> createClient().listOfUnionsOperation(r -> r.listOfUnions(
            UnionMember.builder().string("stringValue").build(),
            UnionMember.builder().object(o -> o.key("objectKey")).build()
        ))).isInstanceOf(RuntimeException.class);

        StringArrayEndpointParams params = captureEndpointParams();

        assertThat(params.stringArrayParam()).isNotEmpty()
                                             .hasSize(2)
                                             .containsExactly("stringValue", "objectKey");
    }

    @Test
    void listOfUnionsOperation_withNullList_returnsEmptyArray() {
        assertThatThrownBy(() -> createClient().listOfUnionsOperation(r -> {}))
            .isInstanceOf(RuntimeException.class);

        StringArrayEndpointParams params = captureEndpointParams();

        assertThat(params.stringArrayParam()).isEmpty();
    }

    @Test
    void listOfUnionsOperation_withEmptyList_returnsEmptyArray() {
        assertThatThrownBy(() -> createClient().listOfUnionsOperation(r -> r.listOfUnions(Collections.emptyList())))
            .isInstanceOf(RuntimeException.class);

        StringArrayEndpointParams params = captureEndpointParams();

        assertThat(params.stringArrayParam()).isEmpty();
    }

    @Test
    void listOfUnionsOperation_filtersOutUnionsWithNoValues() {
        assertThatThrownBy(() -> createClient().listOfUnionsOperation(r -> r.listOfUnions(
            UnionMember.builder().string("value1").build(),
            UnionMember.builder().build(), // no string or object
            UnionMember.builder().string("value2").build()
        ))).isInstanceOf(RuntimeException.class);

        StringArrayEndpointParams params = captureEndpointParams();

        assertThat(params.stringArrayParam()).isNotEmpty()
                                             .hasSize(2)
                                             .containsExactly("value1", "value2");
    }

    @Test
    void listOfUnionsOperation_filtersOutObjectsWithNullKeys() {
        assertThatThrownBy(() -> createClient().listOfUnionsOperation(r -> r.listOfUnions(
            UnionMember.builder().string("value1").build(),
            UnionMember.builder().object(o -> {}).build(), // null key
            UnionMember.builder().object(o -> o.key("value2")).build()
        ))).isInstanceOf(RuntimeException.class);

        StringArrayEndpointParams params = captureEndpointParams();

        assertThat(params.stringArrayParam()).isNotEmpty()
                                             .hasSize(2)
                                             .containsExactly("value1", "value2");
    }

    @Test
    void mapOperation_extractsSingleKeyFromMap() {
        assertThatThrownBy(() -> createClient().mapOperation(r -> r.map(Collections.singletonMap("key1", "value1"))))
            .isInstanceOf(RuntimeException.class);

        StringArrayEndpointParams params = captureEndpointParams();

        assertThat(params.stringArrayParam()).isNotEmpty()
                                             .hasSize(1)
                                             .containsExactly("key1");
    }

    @Test
    void mapOperation_extractsMultipleKeysFromMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");

        assertThatThrownBy(() -> createClient().mapOperation(r -> r.map(map)))
            .isInstanceOf(RuntimeException.class);

        StringArrayEndpointParams params = captureEndpointParams();

        assertThat(params.stringArrayParam()).isNotEmpty()
                                             .hasSize(3)
                                             .containsExactly("key1", "key2", "key3");
    }

    @Test
    void mapOperation_withNullMap_returnsEmptyArray() {
        assertThatThrownBy(() -> createClient().mapOperation(r -> {}))
            .isInstanceOf(RuntimeException.class);

        StringArrayEndpointParams params = captureEndpointParams();

        assertThat(params.stringArrayParam()).isEmpty();
    }

    @Test
    void mapOperation_withEmptyMap_returnsEmptyArray() {
        assertThatThrownBy(() -> createClient().mapOperation(r -> r.map(Collections.emptyMap())))
            .isInstanceOf(RuntimeException.class);

        StringArrayEndpointParams params = captureEndpointParams();

        assertThat(params.stringArrayParam()).isEmpty();
    }

    private StringArrayClient createClient() {
        return StringArrayClient.builder()
                                .region(REGION)
                                .credentialsProvider(CREDENTIALS)
                                .endpointProvider(mockEndpointProvider)
                                .build();
    }

    private StringArrayEndpointParams captureEndpointParams() {
        ArgumentCaptor<StringArrayEndpointParams> paramsCaptor =
            ArgumentCaptor.forClass(StringArrayEndpointParams.class);
        verify(mockEndpointProvider).resolveEndpoint(paramsCaptor.capture());
        return paramsCaptor.getValue();
    }
}
