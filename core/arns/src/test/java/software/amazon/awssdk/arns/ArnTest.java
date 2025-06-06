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

package software.amazon.awssdk.arns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ArnTest {

    @Test
    public void arnWithBasicResource_ParsesCorrectly() {
        String arnString = "arn:aws:s3:us-east-1:12345678910:myresource";
        Arn arn = Arn.fromString(arnString);
        assertThat(arn.partition()).isEqualTo("aws");
        assertThat(arn.service()).isEqualTo("s3");
        assertThat(arn.region()).hasValue("us-east-1");
        assertThat(arn.accountId()).hasValue("12345678910");
        assertThat(arn.resourceAsString()).isEqualTo("myresource");
        System.out.println(arn.resource());
    }

    @Test
    public void arnWithMinimalRequirementFromString() {
        Arn arn = Arn.fromString("arn:aws:foobar:::myresource");
        assertThat(arn.partition()).isEqualTo("aws");
        assertThat(arn.service()).isEqualTo("foobar");
        assertThat(arn.resourceAsString()).isEqualTo("myresource");
    }

    @Test
    public void arn_ParsesBackToString() {
        String arnString = "arn:aws:s3:us-east-1:12345678910:myresource";
        Arn arn = Arn.fromString(arnString);
        assertThat(arn.toString()).isEqualTo(arnString);
    }

    @Test
    public void arnWithQualifiedResource_ParsesBackToString() {
        String arnString = "arn:aws:s3:us-east-1:12345678910:myresource:foobar:1";
        Arn arn = Arn.fromString(arnString);
        assertThat(arn.toString()).isEqualTo(arnString);
        assertThat(arn.resourceAsString()).isEqualTo("myresource:foobar:1");
    }

    @Test
    public void arnWithMinimalResources_ParsesBackToString() {
        String arnString = "arn:aws:s3:::bucket";
        Arn arn = Arn.fromString(arnString);
        assertThat(arn.toString()).isEqualTo(arnString);
        assertThat(arn.resourceAsString()).isEqualTo("bucket");
    }

    @Test
    public void arnWithoutRegion_ParsesBackToString() {
        String arnString = "arn:aws:iam::123456789012:root";
        Arn arn = Arn.fromString(arnString);
        assertThat(arn.toString()).isEqualTo(arnString);
        assertThat(arn.resourceAsString()).isEqualTo("root");
    }

    @Test
    public void arnWithResourceTypeAndResource_ParsesCorrectly() {
        String arnString = "arn:aws:s3:us-east-1:12345678910:bucket:foobar";
        Arn arn = Arn.fromString(arnString);
        assertThat(arn.partition()).isEqualTo("aws");
        assertThat(arn.service()).isEqualTo("s3");
        assertThat(arn.region()).hasValue("us-east-1");
        assertThat(arn.resourceAsString()).isEqualTo("bucket:foobar");

        verifyArnResource(arn.resource());
    }

    private void verifyArnResource(ArnResource arnResource) {
        assertThat(arnResource.resource()).isEqualTo("foobar");
        assertThat(arnResource.resourceType()).isPresent();
        assertThat(arnResource.resourceType().get()).isEqualTo("bucket");
    }

    @Test
    public void arnWithResourceTypeAndResourceAndQualifier_ParsesCorrectly() {
        String arnString = "arn:aws:s3:us-east-1:12345678910:bucket:foobar:1";
        Arn arn = Arn.fromString(arnString);
        assertThat(arn.partition()).isEqualTo("aws");
        assertThat(arn.service()).isEqualTo("s3");
        assertThat(arn.region()).hasValue("us-east-1");
        assertThat(arn.resourceAsString()).isEqualTo("bucket:foobar:1");


        ArnResource arnResource = arn.resource();
        verifyArnResource(arnResource);
        assertThat(arnResource.qualifier()).isPresent();
        assertThat(arnResource.qualifier().get()).isEqualTo("1");
    }

    @Test
    public void arnWithResourceTypeAndResource_SlashSplitter_ParsesCorrectly() {
        String arnString = "arn:aws:s3:us-east-1:12345678910:bucket/foobar";
        Arn arn = Arn.fromString(arnString);
        assertThat(arn.partition()).isEqualTo("aws");
        assertThat(arn.service()).isEqualTo("s3");
        assertThat(arn.region()).hasValue("us-east-1");
        assertThat(arn.resourceAsString()).isEqualTo("bucket/foobar");
        verifyArnResource(arn.resource());
    }

    @Test
    public void arnWithResourceTypeAndResourceAndQualifier_SlashSplitter_ParsesCorrectly() {
        String arnString = "arn:aws:s3:us-east-1:12345678910:bucket/foobar/1";
        Arn arn = Arn.fromString(arnString);
        assertThat(arn.partition()).isEqualTo("aws");
        assertThat(arn.service()).isEqualTo("s3");
        assertThat(arn.region()).hasValue("us-east-1");
        assertThat(arn.resourceAsString()).isEqualTo("bucket/foobar/1");
        verifyArnResource(arn.resource());
        assertThat(arn.resource().qualifier().get()).isEqualTo("1");
    }

    @Test
    public void oneArnEqualsEquivalentArn() {
        String arnString = "arn:aws:s3:us-east-1:12345678910:myresource:foobar";
        Arn arn1 = Arn.fromString(arnString);
        Arn arn2 = Arn.fromString(arnString);
        assertThat(arn1).isEqualTo(arn2);
        assertThat(arn1.resource()).isEqualTo(arn2.resource());
    }

    @Test
    public void arnFromBuilder_ParsesCorrectly() {
        Arn arn = Arn.builder()
                     .partition("aws")
                     .service("s3")
                     .region("us-east-1")
                     .accountId("123456789012")
                     .resource("bucket:foobar:1")
                     .build();

        assertThat(arn.partition()).isEqualTo("aws");
        assertThat(arn.service()).isEqualTo("s3");
        assertThat(arn.region()).hasValue("us-east-1");
        assertThat(arn.accountId()).hasValue("123456789012");
        assertThat(arn.resourceAsString()).isEqualTo("bucket:foobar:1");
        verifyArnResource(arn.resource());
        assertThat(arn.resource().qualifier()).isPresent();
        assertThat(arn.resource().qualifier().get()).isEqualTo("1");
    }

    @Test
    public void arnResourceWithColonAndSlash_ParsesOnFirstSplitter() {
        String resourceWithColonAndSlash = "object:foobar/myobjectname:1";
        Arn arn = Arn.builder()
                     .partition("aws")
                     .service("s3")
                     .region("us-east-1")
                     .accountId("123456789012")
                     .resource(resourceWithColonAndSlash)
                     .build();
        assertThat(arn.partition()).isEqualTo("aws");
        assertThat(arn.service()).isEqualTo("s3");
        assertThat(arn.region()).hasValue("us-east-1");
        assertThat(arn.accountId()).hasValue("123456789012");
        assertThat(arn.resourceAsString()).isEqualTo(resourceWithColonAndSlash);

        assertThat(arn.resource().resource()).isEqualTo("foobar/myobjectname");
        assertThat(arn.resource().qualifier()).hasValue("1");
        assertThat(arn.resource().resourceType()).hasValue("object");
    }

    @Test
    public void arnWithoutRegion_ParsesCorrectly() {
        String arnString = "arn:aws:s3::123456789012:myresource";
        Arn arn = Arn.fromString(arnString);
        assertThat(arn.partition()).isEqualTo("aws");
        assertThat(arn.service()).isEqualTo("s3");
        assertThat(arn.region()).isEmpty();
        assertThat(arn.accountId()).hasValue("123456789012");
        assertThat(arn.resourceAsString()).isEqualTo("myresource");
    }

    @Test
    public void arnWithoutAccountId_ParsesCorrectly() {
        String arnString = "arn:aws:s3:us-east-1::myresource";
        Arn arn = Arn.fromString(arnString);
        assertThat(arn.partition()).isEqualTo("aws");
        assertThat(arn.service()).isEqualTo("s3");
        assertThat(arn.region()).hasValue("us-east-1");
        assertThat(arn.accountId()).isEmpty();
        assertThat(arn.resourceAsString()).isEqualTo("myresource");
    }

    @Test
    public void arnResourceContainingDots_ParsesCorrectly() {
        String arnString = "arn:aws:s3:us-east-1:12345678910:myresource:foobar.1";
        Arn arn = Arn.fromString(arnString);
        assertThat(arn.partition()).isEqualTo("aws");
        assertThat(arn.service()).isEqualTo("s3");
        assertThat(arn.region()).hasValue("us-east-1");
        assertThat(arn.accountId()).hasValue("12345678910");
        assertThat(arn.resourceAsString()).isEqualTo("myresource:foobar.1");
    }

    @Test
    public void toBuilder() {
        Arn oneArn = Arn.builder()
                      .partition("aws")
                      .service("s3")
                      .region("us-east-1")
                      .accountId("123456789012")
                      .resource("bucket:foobar:1")
                      .build();

        Arn anotherArn = oneArn.toBuilder().build();

        assertThat(oneArn).isEqualTo(anotherArn);
        assertThat(oneArn.hashCode()).isEqualTo(anotherArn.hashCode());
    }

    @Test
    public void hashCodeEquals() {
        Arn oneArn = Arn.builder()
                        .partition("aws")
                        .service("s3")
                        .region("us-east-1")
                        .accountId("123456789012")
                        .resource("bucket:foobar:1")
                        .build();

        Arn anotherArn = oneArn.toBuilder().region("somethingelse").build();
        assertThat(oneArn).isNotEqualTo(anotherArn);
        assertThat(oneArn.hashCode()).isNotEqualTo(anotherArn.hashCode());
    }

    @Test
    public void hashCodeEquals_minimalProperties() {
        Arn arn = Arn.builder()
                     .partition("aws")
                     .service("foobar")
                     .resource("resource")
                     .build();
        Arn anotherArn = arn.toBuilder().build();
        assertThat(arn.hashCode()).isEqualTo(anotherArn.hashCode());
        assertThat(arn.region()).isEmpty();
        assertThat(arn.accountId()).isEmpty();
        assertThat(arn.equals(anotherArn)).isTrue();
    }

    @Test
    public void arnWithoutPartition_ThrowsIllegalArgumentException() {
        String arnString = "arn::s3:us-east-1:12345678910:myresource";
        assertThatThrownBy(() -> Arn.fromString(arnString)).hasMessageContaining("artition must not be blank or empty.");
    }

    @Test
    public void arnWithoutService_ThrowsIllegalArgumentException() {
        String arnString = "arn:aws::us-east-1:12345678910:myresource";
        assertThatThrownBy(() -> Arn.fromString(arnString)).hasMessageContaining("service must not be blank or empty");
    }

    @Test
    public void arnWithoutResource_ThrowsIllegalArgumentException() {
        String arnString = "arn:aws:s3:us-east-1:12345678910:";
        assertThatThrownBy(() -> Arn.fromString(arnString)).hasMessageContaining("Malformed ARN");
    }

    @Test
    public void invalidArn_ThrowsIllegalArgumentException() {
        String arnString = "arn:aws:";
        assertThatThrownBy(() -> Arn.fromString(arnString)).hasMessageContaining("Malformed ARN");
    }

    @Test
    public void arnDoesntStartWithArn_ThrowsIllegalArgumentException() {
        String arnString = "fakearn:aws:";
        assertThatThrownBy(() -> Arn.fromString(arnString)).hasMessageContaining("Malformed ARN");
    }

    @Test
    public void invalidArnWithoutPartition_ThrowsIllegalArgumentException() {
        String arnString = "arn:";
        assertThatThrownBy(() -> Arn.fromString(arnString)).hasMessageContaining("Malformed ARN");
    }

    @Test
    public void invalidArnWithoutService_ThrowsIllegalArgumentException() {
        String arnString = "arn:aws:";
        assertThatThrownBy(() -> Arn.fromString(arnString)).hasMessageContaining("Malformed ARN");
    }

    @Test
    public void invalidArnWithoutRegion_ThrowsIllegalArgumentException() {
        String arnString = "arn:aws:s3:";
        assertThatThrownBy(() -> Arn.fromString(arnString)).hasMessageContaining("Malformed ARN");
    }

    @Test
    public void invalidArnWithoutAccountId_ThrowsIllegalArgumentException() {
        String arnString = "arn:aws:s3:us-east-1:";
        assertThatThrownBy(() -> Arn.fromString(arnString)).hasMessageContaining("Malformed ARN");
    }

    private static Stream<Arguments> validArnTestCases() {
        return Stream.of(
            // Test case name, ARN string
            Arguments.of("Basic Resource", "arn:aws:s3:us-east-1:12345678910:myresource"),
            Arguments.of("Minimal Requirements", "arn:aws:foobar:::myresource"),
            Arguments.of("Qualified Resource", "arn:aws:s3:us-east-1:12345678910:myresource:foobar:1"),
            Arguments.of("Minimal Resources", "arn:aws:s3:::bucket"),
            Arguments.of("Without Region", "arn:aws:iam::123456789012:root"),
            Arguments.of("Resource Type And Resource", "arn:aws:s3:us-east-1:12345678910:bucket:foobar"),
            Arguments.of("Resource Type And Resource And Qualifier", "arn:aws:s3:us-east-1:12345678910:bucket:foobar:1"),
            Arguments.of("Resource Type And Resource With Slash", "arn:aws:s3:us-east-1:12345678910:bucket/foobar"),
            Arguments.of("Resource Type And Resource And Qualifier With Slash", "arn:aws:s3:us-east-1:12345678910:bucket/foobar/1"),
            Arguments.of("Without Region", "arn:aws:s3::123456789012:myresource"),
            Arguments.of("Without AccountId", "arn:aws:s3:us-east-1::myresource"),
            Arguments.of("Resource Containing Dots", "arn:aws:s3:us-east-1:12345678910:myresource:foobar.1")
        );
    }

    private static Stream<Arguments> invalidArnTestCases() {
        return Stream.of(
            // Test case name, ARN string
            Arguments.of("Without Partition", "arn::s3:us-east-1:12345678910:myresource"),
            Arguments.of("Without Service", "arn:aws::us-east-1:12345678910:myresource"),
            Arguments.of("Without Resource", "arn:aws:s3:us-east-1:12345678910:"),
            Arguments.of("Invalid ARN", "arn:aws:"),
            Arguments.of("Doesn't Start With ARN", "fakearn:aws:"),
            Arguments.of("Invalid Without Partition", "arn:"),
            Arguments.of("Invalid Without Service", "arn:aws:"),
            Arguments.of("Invalid Without Region", "arn:aws:s3:"),
            Arguments.of("Invalid Without AccountId", "arn:aws:s3:us-east-1:")
        );
    }

    private static Stream<Arguments> exceptionThrowingArnTestCases() {
        return Stream.of(
            Arguments.of("Without Partition", "arn::s3:us-east-1:12345678910:myresource"),
            Arguments.of("Without Service", "arn:aws::us-east-1:12345678910:myresource")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validArnTestCases")
    public void optionalArnFromString_ValidArns_ReturnsPopulatedOptional(String testName, String arnString) {
        Optional<Arn> optionalArn = Arn.tryFromString(arnString);

        assertThat(optionalArn).isPresent();

        // Compare with the original fromString implementation
        Arn expectedArn = Arn.fromString(arnString);
        Arn actualArn = optionalArn.get();

        assertThat(actualArn.partition()).isEqualTo(expectedArn.partition());
        assertThat(actualArn.service()).isEqualTo(expectedArn.service());
        assertThat(actualArn.region()).isEqualTo(expectedArn.region());
        assertThat(actualArn.accountId()).isEqualTo(expectedArn.accountId());
        assertThat(actualArn.resourceAsString()).isEqualTo(expectedArn.resourceAsString());

        // Verify the ARN string representation matches
        assertThat(actualArn.toString()).isEqualTo(arnString);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidArnTestCases")
    public void optionalArnFromString_InvalidArns_ReturnsEmptyOptional(String testName, String arnString) {
        Optional<Arn> optionalArn = Arn.tryFromString(arnString);
        assertThat(optionalArn).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("exceptionThrowingArnTestCases")
    public void tryFromString_InvalidArns_ShouldThrowExceptions(String testName, String arnString) {
        assertThrows(IllegalArgumentException.class, () -> {
            Arn.tryFromString(arnString);
        });
    }

    @Test
    public void optionalArnFromString_NullInput_ReturnsEmptyOptional() {
        Optional<Arn> optionalArn = Arn.tryFromString(null);
        assertThat(optionalArn).isEmpty();
    }

    @ParameterizedTest(name = "Resource parsing: {0}")
    @MethodSource("validArnTestCases")
    public void tryFromString_ResourceParsing_MatchesOriginalImplementation(String testName, String arnString) {
        // Skip test cases that would throw exceptions in the resource parsing
        if (arnString.contains("bucket:") || arnString.contains("bucket/")) {
            Optional<Arn> optionalArn = Arn.tryFromString(arnString);
            assertThat(optionalArn).isPresent();

            Arn expectedArn = Arn.fromString(arnString);
            Arn actualArn = optionalArn.get();

            // Verify resource parsing
            ArnResource expectedResource = expectedArn.resource();
            ArnResource actualResource = actualArn.resource();

            assertThat(actualResource.resourceType()).isEqualTo(expectedResource.resourceType());
            assertThat(actualResource.resource()).isEqualTo(expectedResource.resource());
            assertThat(actualResource.qualifier()).isEqualTo(expectedResource.qualifier());
        }
    }

}
