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

package software.amazon.awssdk.services.tostring;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.services.tostring.model.InputShape;

public class SensitiveDataRedactedTest {
    @Test
    public void stringIncluded() {
        assertThat(InputShape.builder().string("Value").build().toString())
                .contains("Value");
    }

    @Test
    public void sensitiveStringRedacted() {
        assertThat(InputShape.builder().sensitiveString("Value").build().toString())
                .doesNotContain("Value");
    }

    @Test
    public void recursiveRedactionWorks() {
        assertThat(InputShape.builder()
                             .recursiveShape(InputShape.builder().sensitiveString("Value").build())
                             .build()
                             .toString())
                .doesNotContain("Value");
    }

    @Test
    public void stringMarkedSensitiveRedacted() {
        assertThat(InputShape.builder().stringMemberMarkedSensitive("Value").build().toString())
                .doesNotContain("Value");
    }

    @Test
    public void sensitiveListOfStringRedacted() {
        assertThat(InputShape.builder().listOfSensitiveString("Value").build().toString())
                   .doesNotContain("Value");
    }

    @Test
    public void sensitiveListOfListOfStringRedacted() {
        assertThat(InputShape.builder().listOfListOfSensitiveString(singletonList(singletonList("Value"))).build().toString())
                   .doesNotContain("Value");
    }

    @Test
    public void sensitiveMapOfSensitiveStringToStringRedacted() {
        assertThat(InputShape.builder().mapOfSensitiveStringToString(singletonMap("Value", "Value")).build().toString())
                   .doesNotContain("Value");
    }

    @Test
    public void sensitiveMapOfStringToSensitiveStringRedacted() {
        assertThat(InputShape.builder().mapOfStringToSensitiveString(singletonMap("Value", "Value")).build().toString())
                   .doesNotContain("Value");
    }

    @Test
    public void sensitiveMapOfStringToListOfListOfSensitiveStringRedacted() {
        Map<String, List<List<String>>> value = singletonMap("Value", singletonList(singletonList("Value")));
        assertThat(InputShape.builder().mapOfStringToListOfListOfSensitiveString(value).toString())
                   .doesNotContain("Value");
    }
}
