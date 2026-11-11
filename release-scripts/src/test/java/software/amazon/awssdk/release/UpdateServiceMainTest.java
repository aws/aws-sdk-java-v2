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

package software.amazon.awssdk.release;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers how {@link UpdateServiceMain} copies model files into a service's {@code codegen-resources} directory, and in
 * particular which of the two endpoint models it copies.
 *
 * <p>A BDD model supersedes the rule set: when {@code --endpoint-bdd-json} names an existing file, that file is the
 * endpoint model for the service and {@code --endpoint-rule-set-json} is ignored. Only when no BDD model is available
 * does the rule set get copied.
 *
 * <p>Driven through {@code main} rather than by calling the copy helper directly, so the option registration and the
 * destination path are covered too. An unregistered option would leave the argument silently ignored, which no test of
 * the helper alone would catch.
 */
class UpdateServiceMainTest {

    @Test
    void endpointBddJson_available_isCopiedEvenWhenNotAlreadyPresent(@TempDir Path tmp) throws Exception {
        Fixture fixture = new Fixture(tmp);

        fixture.run();

        assertThat(fixture.destination("endpoint-bdd-1.json")).hasContent(Fixture.BDD_CONTENT);
    }

    @Test
    void endpointBddJson_available_overwritesAStaleModel(@TempDir Path tmp) throws Exception {
        Fixture fixture = new Fixture(tmp);
        fixture.writeDestination("endpoint-bdd-1.json", "{\"stale\": true}");

        fixture.run();

        assertThat(fixture.destination("endpoint-bdd-1.json")).hasContent(Fixture.BDD_CONTENT);
    }

    /**
     * The rule set is ignored rather than refreshed when a BDD model wins, so a checked-in rule set is left exactly as
     * it was instead of being updated from the release.
     */
    @Test
    void endpointBddJson_available_leavesTheRuleSetAlone(@TempDir Path tmp) throws Exception {
        Fixture fixture = new Fixture(tmp);
        fixture.writeDestination("endpoint-rule-set.json", "{\"untouched\": true}");

        fixture.run();

        assertThat(fixture.destination("endpoint-rule-set.json")).hasContent("{\"untouched\": true}");
    }

    @Test
    void endpointBddJson_available_doesNotCreateARuleSet(@TempDir Path tmp) throws Exception {
        Fixture fixture = new Fixture(tmp);

        fixture.run();

        assertThat(fixture.destination("endpoint-rule-set.json"))
            .as("the rule set is ignored when a BDD model is available")
            .doesNotExist();
    }

    @Test
    void endpointBddJson_argumentOmitted_copiesTheRuleSet(@TempDir Path tmp) throws Exception {
        Fixture fixture = new Fixture(tmp);

        fixture.withoutEndpointBddArgument().run();

        assertThat(fixture.destination("endpoint-rule-set.json")).hasContent(Fixture.RULE_SET_CONTENT);
        assertThat(fixture.destination("endpoint-bdd-1.json")).doesNotExist();
    }

    /**
     * Upstream does not publish a BDD model for every service, so the argument can point at a path that was never
     * written. That falls back to the rule set rather than failing or leaving the service without an endpoint model.
     */
    @Test
    void endpointBddJson_sourceMissing_copiesTheRuleSet(@TempDir Path tmp) throws Exception {
        Fixture fixture = new Fixture(tmp);

        fixture.withMissingEndpointBddSource().run();

        assertThat(fixture.destination("endpoint-rule-set.json")).hasContent(Fixture.RULE_SET_CONTENT);
        assertThat(fixture.destination("endpoint-bdd-1.json")).doesNotExist();
    }

    /**
     * The BDD copy is on the source, not the destination, so omitting the argument leaves an existing model alone
     * rather than deleting or emptying it.
     */
    @Test
    void endpointBddJson_argumentOmitted_leavesAnExistingModelAlone(@TempDir Path tmp) throws Exception {
        Fixture fixture = new Fixture(tmp);
        fixture.writeDestination("endpoint-bdd-1.json", "{\"kept\": true}");

        fixture.withoutEndpointBddArgument().run();

        assertThat(fixture.destination("endpoint-bdd-1.json")).hasContent("{\"kept\": true}");
    }

    @Test
    void bothEndpointModelArgumentsOmitted_isNotAnError(@TempDir Path tmp) throws Exception {
        Fixture fixture = new Fixture(tmp);

        fixture.withoutEndpointBddArgument().withoutEndpointRuleSetArgument().run();

        assertThat(fixture.destination("service-2.json")).hasContent(Fixture.SERVICE_CONTENT);
        assertThat(fixture.destination("endpoint-bdd-1.json")).doesNotExist();
        assertThat(fixture.destination("endpoint-rule-set.json")).doesNotExist();
    }

    /**
     * The other model files keep their unconditional behaviour. A service legitimately gains waiters or endpoint tests
     * for the first time, and those must land on the release that carries them.
     */
    @Test
    void otherModelFiles_areCopiedEvenWhenNotAlreadyPresent(@TempDir Path tmp) throws Exception {
        Fixture fixture = new Fixture(tmp);

        fixture.run();

        assertThat(fixture.destination("service-2.json")).hasContent(Fixture.SERVICE_CONTENT);
        assertThat(fixture.destination("paginators-1.json")).exists();
        assertThat(fixture.destination("waiters-2.json")).exists();
        assertThat(fixture.destination("endpoint-tests.json")).exists();
    }

    /**
     * Endpoint tests are copied regardless of which endpoint model was chosen; they are not part of the BDD/rule set
     * decision.
     */
    @Test
    void endpointTestsJson_isCopiedWithEitherEndpointModel(@TempDir Path tmp) throws Exception {
        Fixture withBdd = new Fixture(tmp.resolve("bdd"));
        Fixture withRuleSet = new Fixture(tmp.resolve("ruleSet"));

        withBdd.run();
        withRuleSet.withoutEndpointBddArgument().run();

        assertThat(withBdd.destination("endpoint-tests.json")).exists();
        assertThat(withRuleSet.destination("endpoint-tests.json")).exists();
    }

    /**
     * DynamoDB's models live in a nested directory, so this checks the endpoint model lands in the same resolved
     * destination the other files use rather than the module root.
     */
    @Test
    void endpointBddJson_isCopiedToTheResolvedNestedDestination(@TempDir Path tmp) throws Exception {
        Fixture fixture = new Fixture(tmp, "dynamodb", "DynamoDB", "dynamodb");

        fixture.run();

        assertThat(fixture.destination("endpoint-bdd-1.json")).hasContent(Fixture.BDD_CONTENT);
        assertThat(tmp.resolve("project/services/dynamodb/src/main/resources/codegen-resources/endpoint-bdd-1.json"))
            .as("the nested service must not also get a copy at the module root")
            .doesNotExist();
    }

    private static final class Fixture {
        private static final String BDD_CONTENT = "{\"version\": \"1.0\", \"nodes\": \"AAAA\"}";
        private static final String RULE_SET_CONTENT = "{\"version\": \"1.0\"}";
        private static final String SERVICE_CONTENT = "{\"metadata\": {}}";

        private final Path projectRoot;
        private final Path sourceDir;
        private final Path destinationDir;
        private final String serviceModuleName;
        private final String serviceId;
        private boolean includeEndpointBddArgument = true;
        private boolean includeEndpointRuleSetArgument = true;
        private String endpointBddSourceName = "endpoint-bdd-1.json";

        private Fixture(Path tmp) throws IOException {
            this(tmp, "myservice", "MyService", null);
        }

        private Fixture(Path tmp, String serviceModuleName, String serviceId, String nestedDir) throws IOException {
            this.projectRoot = tmp.resolve("project");
            this.sourceDir = tmp.resolve("source");
            this.serviceModuleName = serviceModuleName;
            this.serviceId = serviceId;

            Path codegenResources = projectRoot.resolve("services")
                                               .resolve(serviceModuleName)
                                               .resolve("src")
                                               .resolve("main")
                                               .resolve("resources")
                                               .resolve("codegen-resources");
            this.destinationDir = nestedDir == null ? codegenResources : codegenResources.resolve(nestedDir);
            Files.createDirectories(destinationDir);
            Files.createDirectories(sourceDir);

            write(sourceDir.resolve("service-2.json"), SERVICE_CONTENT);
            write(sourceDir.resolve("paginators-1.json"), "{\"pagination\": {}}");
            write(sourceDir.resolve("waiters-2.json"), "{\"waiters\": {}}");
            write(sourceDir.resolve("endpoint-rule-set.json"), RULE_SET_CONTENT);
            write(sourceDir.resolve("endpoint-tests.json"), "{\"testCases\": []}");
            write(sourceDir.resolve("endpoint-bdd-1.json"), BDD_CONTENT);
        }

        private Fixture withoutEndpointBddArgument() {
            this.includeEndpointBddArgument = false;
            return this;
        }

        private Fixture withoutEndpointRuleSetArgument() {
            this.includeEndpointRuleSetArgument = false;
            return this;
        }

        private Fixture withMissingEndpointBddSource() {
            this.endpointBddSourceName = "no-such-endpoint-bdd-1.json";
            return this;
        }

        private void writeDestination(String name, String content) throws IOException {
            write(destinationDir.resolve(name), content);
        }

        private Path destination(String name) {
            return destinationDir.resolve(name);
        }

        private void run() {
            List<String> args = new ArrayList<>();
            args.add("--maven-project-root");
            args.add(projectRoot.toString());
            args.add("--service-module-name");
            args.add(serviceModuleName);
            args.add("--service-id");
            args.add(serviceId);
            args.add("--service-json");
            args.add(sourceDir.resolve("service-2.json").toString());
            args.add("--paginators-json");
            args.add(sourceDir.resolve("paginators-1.json").toString());
            args.add("--waiters-json");
            args.add(sourceDir.resolve("waiters-2.json").toString());
            args.add("--endpoint-tests-json");
            args.add(sourceDir.resolve("endpoint-tests.json").toString());
            if (includeEndpointRuleSetArgument) {
                args.add("--endpoint-rule-set-json");
                args.add(sourceDir.resolve("endpoint-rule-set.json").toString());
            }
            if (includeEndpointBddArgument) {
                args.add("--endpoint-bdd-json");
                args.add(sourceDir.resolve(endpointBddSourceName).toString());
            }

            UpdateServiceMain.main(args.toArray(new String[0]));
        }

        private static void write(Path path, String content) throws IOException {
            Files.createDirectories(path.getParent());
            Files.write(path, content.getBytes(UTF_8));
        }
    }
}
