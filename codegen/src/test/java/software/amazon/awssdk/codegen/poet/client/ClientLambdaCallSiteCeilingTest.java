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

package software.amazon.awssdk.codegen.poet.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Bounds the number of lambda call sites in each generated client reference fixture.
 *
 * <p>Per-operation lambdas are the dominant removable constant pool cost in generated clients.
 * These fixtures span all five protocols and the special operation kinds such as event streams, streaming payloads.
 *
 */
public class ClientLambdaCallSiteCeilingTest {

    /**
     * Ceilings are the count when this test was written plus 2. Any construct added to every operation body raises a
     * fixture's count by its operation count, which is at least 4 for every fixture listed here, so the headroom absorbs
     * a couple of new shared per-client lambdas without absorbing a per-operation one. Single-operation fixtures are
     * excluded for that reason: no headroom smaller than one operation exists.
     */
    private static Stream<Arguments> ceilings() {
        return Stream.of(
            Arguments.of("test-json-async-client-class.java", 29),
            Arguments.of("test-json-client-class.java", 6),
            Arguments.of("test-aws-json-async-client-class.java", 25),
            Arguments.of("test-cbor-async-client-class.java", 25),
            Arguments.of("test-cbor-client-class.java", 6),
            Arguments.of("test-rpcv2-async-client-class.java", 7),
            Arguments.of("test-rpcv2-sync.java", 6),
            Arguments.of("test-query-async-client-class.java", 14),
            Arguments.of("test-query-client-class.java", 5),
            Arguments.of("test-xml-async-client-class.java", 18),
            Arguments.of("test-xml-client-class.java", 5),
            Arguments.of("test-unsigned-payload-trait-async-client-class.java", 7),
            Arguments.of("test-unsigned-payload-trait-sync-client-class.java", 6)
        );
    }

    @ParameterizedTest
    @MethodSource("ceilings")
    void lambdaCallSites_inGeneratedClient_areWithinCeiling(String fixture, int ceiling) throws IOException {
        String client = fixtureContent(fixture);
        int callSites = lambdaCallSites(client);

        assertThat(callSites)
            .withFailMessage(regressionMessage(fixture, callSites, ceiling, operationCount(client)))
            .isLessThanOrEqualTo(ceiling);
    }

    private static String regressionMessage(String fixture, int callSites, int ceiling, int operations) {
        return String.format(
            Locale.ROOT,
            "%s contains %d lambda call sites, ceiling is %d.%n%n"
            + "Per-operation lambdas are the dominant removable constant pool cost in generated clients: each one costs "
            + "roughly 5 pool entries per operation. This fixture has %d operations, so a lambda added to every "
            + "operation body raises this count by %d.%n%n"
            + "If you added a construct to every operation body, hoist it into a shared helper.%n%n"
            + "If the growth is intended and justified, raise the ceiling deliberately and record the reason "
            + "alongside it.",
            fixture, callSites, ceiling, operations, operations);
    }

    /**
     * Comment lines are excluded so that a lambda in generated javadoc is not counted as a call site.
     */
    private static int lambdaCallSites(String client) {
        return (int) Stream.of(client.split("\n"))
                           .map(String::trim)
                           .filter(line -> !line.startsWith("*") && !line.startsWith("//") && !line.startsWith("/*"))
                           .mapToLong(line -> countOccurrences(line, "->"))
                           .sum();
    }

    private static int operationCount(String client) {
        return (int) countOccurrences(client, ".withOperationName(");
    }

    private static long countOccurrences(String text, String token) {
        long count = 0;
        int from = text.indexOf(token);
        while (from >= 0) {
            count++;
            from = text.indexOf(token, from + token.length());
        }
        return count;
    }

    private static String fixtureContent(String fixture) throws IOException {
        try (InputStream resource = ClientLambdaCallSiteCeilingTest.class.getResourceAsStream(fixture)) {
            Validate.notNull(resource, "Failed to load fixture " + fixture);
            return IoUtils.toUtf8String(resource);
        }
    }
}
