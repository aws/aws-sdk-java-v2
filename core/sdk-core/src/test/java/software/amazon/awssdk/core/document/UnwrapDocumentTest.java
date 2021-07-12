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

package software.amazon.awssdk.core.document;

import org.testng.annotations.Test;
import software.amazon.awssdk.core.SdkNumber;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class UnwrapDocumentTest {

    @Test
    public void testMapDocumentUnwrap() {
        final Document mapDocument = Document.mapBuilder().putString("key", "stringValue")
                .putDocument("documentKey", Document.fromString("documentValue"))
                .build();
        final Object unwrappedMapObject = mapDocument.unwrap();
        final long unwrappedMapCountAsString = ((Map<?, ?>) unwrappedMapObject).entrySet().stream()
                .filter(k -> k.getKey() instanceof String && k.getValue() instanceof String).count();
        final long unwrappedMapCountAsDocument = ((Map<?, ?>) unwrappedMapObject).entrySet().stream()
                .filter(k -> k.getKey() instanceof String && k.getValue() instanceof Document).count();
        assertThat(unwrappedMapCountAsString).isEqualTo(2);
        assertThat(unwrappedMapCountAsDocument).isZero();
    }

    @Test
    public void testListDocumentUnwrap() {
        final Document documentList = Document.fromList(Arrays.asList(Document.fromNumber(SdkNumber.fromLong(1)), Document.fromNumber(SdkNumber.fromLong(2))));
        final Object documentListAsObjects = documentList.unwrap();
        final Optional strippedAsSDKNumber = ((List) documentListAsObjects)
                .stream().filter(e -> e instanceof String).findAny();
        final Optional strippedAsDocuments = ((List) documentListAsObjects)
                .stream().filter(e -> e instanceof Document).findAny();
        assertThat(strippedAsSDKNumber).isPresent();
        assertThat(strippedAsDocuments).isNotPresent();
    }

    @Test
    public void testStringDocumentUnwrap() {
        final Document testDocument = Document.fromString("testDocument");
        assertThat(testDocument.unwrap()).isEqualTo("testDocument");
    }

    @Test
    public void testNumberDocumentUnwrap() {
        final Document testDocument = Document.fromNumber(SdkNumber.fromLong(2));
        assertThat(testDocument.unwrap()).isEqualTo(SdkNumber.fromLong(2).stringValue());
    }

    @Test
    public void testBoolanDocumentUnwrap() {
        final Document testDocument = Document.fromBoolean(true);
        assertThat(testDocument.unwrap()).isEqualTo(true);
    }

    @Test
    public void testNullDocumentUnwrap() {
        final Document testDocument = Document.fromNull();
        assertThat(testDocument.unwrap()).isNull();
    }


}
