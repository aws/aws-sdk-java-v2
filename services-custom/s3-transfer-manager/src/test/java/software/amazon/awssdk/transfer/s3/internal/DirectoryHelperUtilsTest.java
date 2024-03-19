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

package software.amazon.awssdk.transfer.s3.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

class DirectoryHelperUtilsTest {

    @ParameterizedTest
    @MethodSource("arguments")
    void testNormalizeKey(ListObjectsV2Request listObjectsRequest, String key, String expected) {
        String normalized = DirectoryHelperUtils.normalizeKey(listObjectsRequest, key, "/");
        assertThat(normalized).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "//", "\\", "|", "delim"})
    void testDelimiter(String delimiter) {
        String prefix = String.format("notes%s2021%s", delimiter, delimiter);
        String key = String.format("notes%s2021%s1.txt", delimiter, delimiter);
        String normalized = DirectoryHelperUtils.normalizeKey(
            ListObjectsV2Request.builder().prefix(prefix).build(), key, delimiter);
        assertThat(normalized).isEqualTo("1.txt");
    }

    private static List<Arguments> arguments() {
        return Arrays.asList(
            arg("", "no-delim", "no-delim"),
            arg("", "delim/with/separator", "delim/with/separator"),
            arg("no-delim", "", ""),
            arg("no-delim", "no-delim", "no-delim"),
            arg("delim", "delim/", ""),
            arg("prefix", "not-in-key", "not-in-key"),
            arg("notes/2021", "notes/2021/1.txt", "1.txt"),
            arg("notes/2021/", "notes/2021/1.txt", "1.txt"),
            arg("someInner", "someInnerFolder/another/file1.txt", "Folder/another/file1.txt"),
            arg("someInner", "someInnerF/another/file1.txt", "F/another/file1.txt"),
            arg("someInner", "someInner/another/file1.txt", "another/file1.txt")
        );
    }

    private static Arguments arg(String prefix, String key, String expected) {
        return Arguments.of(ListObjectsV2Request.builder().prefix(prefix).build(), key, expected);
    }

}
