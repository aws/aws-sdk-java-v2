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

package software.amazon.awssdk.transfer.s3.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.s3.model.S3Object;

public class DownloadFilterTest {

    public static Stream<Arguments> s3Objects() {
        return Stream.of(
            Arguments.of(S3Object.builder().key("no-slash-zero-content").size(0L).build(), true),
            Arguments.of(S3Object.builder().key("slash-zero-content/").size(0L).build(), false),
            Arguments.of(S3Object.builder().key("key").size(10L).build(), true)
        );
    }

    @ParameterizedTest
    @MethodSource("s3Objects")
    void allObjectsFilter_shouldWork(S3Object s3Object, boolean result) {
        assertThat(DownloadFilter.allObjects().test(s3Object)).isEqualTo(result);
    }

    private static Stream<Arguments> basicOrFilterTestCases() {
        return Stream.of(
            Arguments.of(
                "File in folder1 - should match",
                S3Object.builder().key("folder1/test.txt").size(2000L).build(),
                true
            ),
            Arguments.of(
                "File in folder3 - should match",
                S3Object.builder().key("folder3/test.txt").size(2000L).build(),
                true
            ),
            Arguments.of(
                "File in folder2 - should not match",
                S3Object.builder().key("folder2/test.txt").size(2000L).build(),
                false
            )
        );
    }

    @ParameterizedTest
    @MethodSource("basicOrFilterTestCases")
    @DisplayName("Test OR filter combinations")
    void testBasicOrFilter(String testName, S3Object s3Object, boolean result) {
        DownloadFilter folder1Filter = s3Obj -> s3Obj.key().startsWith("folder1");
        DownloadFilter folder3Filter = s3Obj -> s3Obj.key().startsWith("folder3");
        DownloadFilter combinedFilter = folder1Filter.or(folder3Filter);
        assertThat(combinedFilter.test(s3Object))
            .as(testName)
            .isEqualTo(result);
    }

    private static Stream<Arguments> basicAndFilterTestCases() {
        return Stream.of(
            Arguments.of(
                "Large text file - should match",
                S3Object.builder().key("folder1/test.txt").size(2000L).build(),
                true
            ),
            Arguments.of(
                "Small text file - should not match",
                S3Object.builder().key("folder1/test.txt").size(500L).build(),
                false
            ),
            Arguments.of(
                "Large non-text file - should not match",
                S3Object.builder().key("folder1/test.pdf").size(2000L).build(),
                false
            )
        );
    }

    @ParameterizedTest
    @MethodSource("basicAndFilterTestCases")
    @DisplayName("Test AND filter combinations")
    void testBasicAndFilter(String testName, S3Object s3Object, boolean result) {
        DownloadFilter txtFilter = s3Obj -> s3Obj.key().endsWith(".txt");
        DownloadFilter sizeFilter = s3Obj -> s3Obj.size() > 1000L;
        DownloadFilter combinedFilter = txtFilter.and(sizeFilter);
        assertThat(combinedFilter.test(s3Object))
            .as(testName)
            .isEqualTo(result);
    }

    private static Stream<Arguments> basicNegateFilterTestCases() {
        return Stream.of(
            Arguments.of(
                "File in folder1 - should not match",
                "FOLDER",
                S3Object.builder().key("folder1/test.txt").size(1000L).build(),
                false
            ),
            Arguments.of(
                "File not in folder1 - should match",
                "FOLDER",
                S3Object.builder().key("folder2/test.txt").size(1000L).build(),
                true
            ),
            Arguments.of(
                "Large file - should not match",
                "SIZE",
                S3Object.builder().key("test.txt").size(2000L).build(),
                false
            ),
            Arguments.of(
                "Small file - should match",
                "SIZE",
                S3Object.builder().key("test.txt").size(500L).build(),
                true
            )
        );
    }

    @ParameterizedTest
    @MethodSource("basicNegateFilterTestCases")
    @DisplayName("Test NEGATE filter operations")
    void testBasicNegateFilter(String testName, String filterType, S3Object s3Object, boolean result) {
        DownloadFilter baseFilter;

        switch (filterType) {
            case "FOLDER":
                baseFilter = s3Obj -> s3Obj.key().startsWith("folder1");
                break;
            case "SIZE":
                baseFilter = s3Obj -> s3Obj.size() > 1000L;
                break;
            default:
                throw new IllegalArgumentException("Unknown filter type: " + filterType);
        }

        DownloadFilter negatedFilter = baseFilter.negate();
        assertThat(negatedFilter.test(s3Object))
            .as(testName)
            .isEqualTo(result);
    }

    private static Stream<Arguments> combinedFilterTestCases() {
        return Stream.of(
            Arguments.of(
                "Large file in folder1 - should match",
                S3Object.builder().key("folder1/test.txt").size(2000L).build(),
                true,
                "folder1 OR folder3 AND size > 1000"
            ),
            Arguments.of(
                "Small file in folder1 - should not match",
                S3Object.builder().key("folder1/test.txt").size(500L).build(),
                false,
                "folder1 OR folder3 AND size > 1000"
            ),
            Arguments.of(
                "Large file in folder2 - should not match",
                S3Object.builder().key("folder2/test.txt").size(2000L).build(),
                false,
                "folder1 OR folder3 AND size > 1000"
            )
        );
    }

    @ParameterizedTest
    @MethodSource("combinedFilterTestCases")
    @DisplayName("Test combined filter operations")
    void testCombinedFilters(String testName, S3Object s3Object, boolean result, String description) {
        DownloadFilter folder1Filter = s3Obj -> s3Obj.key().startsWith("folder1");
        DownloadFilter folder3Filter = s3Obj -> s3Obj.key().startsWith("folder3");
        DownloadFilter sizeFilter = s3Obj -> s3Obj.size() > 1000L;

        DownloadFilter chainedFilter = folder1Filter
            .or(folder3Filter)
            .and(sizeFilter);

        assertThat(chainedFilter.test(s3Object))
            .as("%s: %s", testName, description)
            .isEqualTo(result);
    }
}
