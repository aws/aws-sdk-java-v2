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

package software.amazon.awssdk.protocols.json.internal;

import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.protocols.jsoncore.JsonNodeParser;
import software.amazon.awssdk.thirdparty.jackson.core.JsonFactory;

class AwsStructuredPlainJsonFactoryTest {

    private static final JsonFactory JSON_FACTORY = AwsStructuredPlainJsonFactory.SDK_JSON_FACTORY.getJsonFactory();

    @Test
    void parse_fieldNameAtMaxDynamoDbLength_succeeds() {
        char[] chars = new char[65_535];
        Arrays.fill(chars, 'a');
        String name = new String(chars);
        String json = "{\"" + name + "\": \"value\"}";

        assertThatNoException().isThrownBy(() ->
            JsonNodeParser.builder().jsonFactory(JSON_FACTORY).build().parse(json));
    }
}
