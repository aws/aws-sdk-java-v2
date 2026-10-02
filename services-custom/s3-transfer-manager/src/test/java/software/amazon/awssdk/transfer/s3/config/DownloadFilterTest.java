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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Function;
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

    private static Stream<Arguments> filterOperationTestCases() {
        Function<S3Object, DownloadFilter> folder1OrFolder3Filter = s3Object -> {
            DownloadFilter folder1 = obj -> obj.key().startsWith("folder1");
            DownloadFilter folder3 = obj -> obj.key().startsWith("folder3");
            return folder1.or(folder3);
        };

        Function<S3Object, DownloadFilter> txtAndLargeSizeFilter = s3Object -> {
            DownloadFilter txtFilter = obj -> obj.key().endsWith(".txt");
            DownloadFilter sizeFilter = obj -> obj.size() > 1000L;
            return txtFilter.and(sizeFilter);
        };

        Function<S3Object, DownloadFilter> notFolder1Filter = s3Object -> {
            DownloadFilter folder1 = obj -> obj.key().startsWith("folder1");
            return folder1.negate();
        };

        Function<S3Object, DownloadFilter> notLargeSizeFilter = s3Object -> {
            DownloadFilter largeSize = obj -> obj.size() > 1000L;
            return largeSize.negate();
        };

        Function<S3Object, DownloadFilter> complexFilter = s3Object -> {
            DownloadFilter folder1 = obj -> obj.key().startsWith("folder1");
            DownloadFilter folder3 = obj -> obj.key().startsWith("folder3");
            DownloadFilter sizeFilter = obj -> obj.size() > 1000L;
            return folder1.or(folder3).and(sizeFilter);
        };
        Function<S3Object, DownloadFilter> nullParameterFilter = s3Object -> {
            DownloadFilter baseFilter = obj -> obj.key().startsWith("folder1");
            return s -> {
                assertThrows(NullPointerException.class,
                             () -> baseFilter.or(null),
                             "or() should throw NullPointerException when other is null");
                assertThrows(NullPointerException.class,
                             () -> baseFilter.and(null),
                             "and() should throw NullPointerException when other is null");
                return true;  // Return value doesn't matter as we're testing for exceptions
            };
        };


        return Stream.of(
            // OR operation tests
            Arguments.of(
                "OR: folder1/test.txt matches (folder1 OR folder3)",
                S3Object.builder().key("folder1/test.txt").size(2000L).build(),
                folder1OrFolder3Filter,
                true
            ),
            Arguments.of(
                "OR: folder3/test.txt matches (folder1 OR folder3)",
                S3Object.builder().key("folder3/test.txt").size(2000L).build(),
                folder1OrFolder3Filter,
                true
            ),
            Arguments.of(
                "OR: folder2/test.txt does not match (folder1 OR folder3)",
                S3Object.builder().key("folder2/test.txt").size(2000L).build(),
                folder1OrFolder3Filter,
                false
            ),

            // AND operation tests
            Arguments.of(
                "AND: large .txt file matches (.txt AND size > 1000)",
                S3Object.builder().key("folder1/test.txt").size(2000L).build(),
                txtAndLargeSizeFilter,
                true
            ),
            Arguments.of(
                "AND: small .txt file does not match (.txt AND size > 1000)",
                S3Object.builder().key("folder1/test.txt").size(500L).build(),
                txtAndLargeSizeFilter,
                false
            ),
            Arguments.of(
                "AND: large .pdf file does not match (.txt AND size > 1000)",
                S3Object.builder().key("folder1/test.pdf").size(2000L).build(),
                txtAndLargeSizeFilter,
                false
            ),

            // NEGATE operation tests
            Arguments.of(
                "NEGATE: folder1 file does not match NOT(folder1)",
                S3Object.builder().key("folder1/test.txt").size(1000L).build(),
                notFolder1Filter,
                false
            ),
            Arguments.of(
                "NEGATE: folder2 file matches NOT(folder1)",
                S3Object.builder().key("folder2/test.txt").size(1000L).build(),
                notFolder1Filter,
                true
            ),
            Arguments.of(
                "NEGATE: large file does not match NOT(size > 1000)",
                S3Object.builder().key("test.txt").size(2000L).build(),
                notLargeSizeFilter,
                false
            ),
            Arguments.of(
                "NEGATE: small file matches NOT(size > 1000)",
                S3Object.builder().key("test.txt").size(500L).build(),
                notLargeSizeFilter,
                true
            ),

            // Complex chained operations
            Arguments.of(
                "COMPLEX: large file in folder1 matches ((folder1 OR folder3) AND size > 1000)",
                S3Object.builder().key("folder1/test.txt").size(2000L).build(),
                complexFilter,
                true
            ),
            Arguments.of(
                "COMPLEX: small file in folder1 does not match ((folder1 OR folder3) AND size > 1000)",
                S3Object.builder().key("folder1/test.txt").size(500L).build(),
                complexFilter,
                false
            ),
            Arguments.of(
                "COMPLEX: large file in folder2 does not match ((folder1 OR folder3) AND size > 1000)",
                S3Object.builder().key("folder2/test.txt").size(2000L).build(),
                complexFilter,
                false
            ),
            Arguments.of(
                "COMPLEX: large file in folder3 matches ((folder1 OR folder3) AND size > 1000)",
                S3Object.builder().key("folder3/test.txt").size(2000L).build(),
                complexFilter,
                true
            ),
            // NullPointerException
            Arguments.of(
                "NULL: or/and with null parameter should throw NullPointerException",
                S3Object.builder().key("folder1/test.txt").size(1000L).build(),
                nullParameterFilter,
                true
            )

        );
    }

    @ParameterizedTest
    @MethodSource("filterOperationTestCases")
    @DisplayName("Test DownloadFilter operations (AND, OR, NEGATE)")
    void testFilterOperations(String scenario, S3Object s3Object,
                              Function<S3Object, DownloadFilter> filterFactory,
                              boolean expectedResult) {
        // Given
        DownloadFilter filter = filterFactory.apply(s3Object);

        // When
        boolean actualResult = filter.test(s3Object);

        // Then
        assertThat(actualResult)
            .as(scenario)
            .isEqualTo(expectedResult);
    }
}
