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

package software.amazon.awssdk.imds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.isA;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.thirdparty.jackson.core.JsonParseException;

/**
 * The class tests the utility methods provided by MetadataResponse Class .
 */
public class MetadataResponseTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void check_asString_success() {

        String response = "foobar";

        MetadataResponse metadataResponse = MetadataResponse.create(response);
        String result = metadataResponse.asString();
        assertThat(result).isEqualTo(response);

    }

    @Test
    public void check_asString_failure() {

        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Metadata is null");

        MetadataResponse metadataResponse = MetadataResponse.create(null);
        metadataResponse.asString();
    }

    @Test
    public void check_asList_success_with_delimiter() {

        String response = "sai\ntest";

        MetadataResponse metadataResponse = MetadataResponse.create(response);
        List<String> result = metadataResponse.asList();
        assertThat(result).hasSize(2);
    }

    @Test
    public void check_asList_success_without_delimiter() {

        String response = "test1-test2";

        MetadataResponse metadataResponse = MetadataResponse.create(response);
        List<String> result = metadataResponse.asList();
        assertThat(result).hasSize(1);
    }

    @Test
    public void check_asList_failure() {

        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Metadata is null");

        MetadataResponse metadataResponse = MetadataResponse.create(null);
        metadataResponse.asList();
    }

    @Test
    public void check_asDocument_failure() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Metadata is null");

        MetadataResponse metadataResponse = MetadataResponse.create(null);
        metadataResponse.asDocument();
    }

    @Test
    public void check_asDocument_success() {
        String jsonResponse = "{"
                              + "\"instanceType\":\"m1.small\","
                              + "\"devpayProductCodes\":[\"bar\",\"foo\"]"
                              + "}";

        MetadataResponse metadataResponse = MetadataResponse.create(jsonResponse);
        Document document = metadataResponse.asDocument();
        Map<String, Document> expectedMap = new LinkedHashMap<>();

        List<Document> documentList = new ArrayList<>();
        documentList.add(Document.fromString("bar"));
        documentList.add(Document.fromString("foo"));

        expectedMap.put("instanceType", Document.fromString("m1.small"));
        expectedMap.put("devpayProductCodes", Document.fromList(documentList));
        Document expectedDocumentMap = Document.fromMap(expectedMap);
        assertThat(document).isEqualTo(expectedDocumentMap);
    }

    @Test
    public void toDocument_nonJsonFormat_ExpectIllegalArgument() {
        thrown.expectCause(isA(JsonParseException.class));
        String malformed = "this is not json";
        MetadataResponse metadataResponse = MetadataResponse.create(malformed);
        metadataResponse.asDocument();
    }

}
