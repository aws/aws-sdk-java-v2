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

package software.amazon.awssdk.protocols.xml.internal.marshall;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class S3XmlWriterTest {

    private static final S3XmlWriter s3XmlWriter = new S3XmlWriter(new StringWriter(), "namespace");

    private static Stream<Arguments> testKeys() {
        return Stream.of(
            Arguments.of("<Key>objectId</Key>", "&lt;Key&gt;objectId&lt;/Key&gt;"),
            Arguments.of("&lt;Key&gt;objectId&lt;/Key&gt;", "&amp;lt;Key&amp;gt;objectId&amp;lt;/Key&amp;gt;"),
            Arguments.of("&lt;<", "&amp;lt;&lt;")
        );
    }

    @ParameterizedTest
    @MethodSource("testKeys")
    public void escapeXmlEntities_properlyEncodesKey(String key, String encodedKey) {
        String escaped = s3XmlWriter.escapeXmlEntities(key);
        assertThat(escaped).isEqualTo(encodedKey);
    }
}
