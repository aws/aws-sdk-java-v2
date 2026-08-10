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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codegen.C2jModels;
import software.amazon.awssdk.codegen.CodeGenerator;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.service.ErrorMap;
import software.amazon.awssdk.codegen.model.service.ErrorTrait;
import software.amazon.awssdk.codegen.model.service.Http;
import software.amazon.awssdk.codegen.model.service.Input;
import software.amazon.awssdk.codegen.model.service.Member;
import software.amazon.awssdk.codegen.model.service.Operation;
import software.amazon.awssdk.codegen.model.service.Output;
import software.amazon.awssdk.codegen.model.service.ServiceMetadata;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.model.service.Shape;

/**
 * Bounds the marginal constant pool cost of one additional operation in the generated clients and client interfaces.
 *
 * <p>A class file holds at most 65,535 constant pool entries, and a generated client grows with the operation count of
 * its service model. That growth is affine, not proportional: {@code entries = fixed + marginal * ops}, measured here as
 * {@code 742 + 25 * ops} for the async client. The fixed part is the constructor, builder plumbing, endpoint and
 * auth-scheme resolution, and shared helpers, all of which a client pays for even with no operations at all.
 *
 * <p>This test generates one synthetic model at two operation counts and subtracts the totals. The fixed cost is
 * present in both and cancels, leaving the marginal cost: the entries that one <em>additional</em> operation adds, which
 * is the quantity a per-operation regression changes. Something added once per client moves both totals equally and the
 * budget holds, correctly; something added once per operation moves only the larger model and the budget breaks.
 *
 */
public class ClientConstantPoolBudgetTest {
    private static final int CONSTANT_POOL_LIMIT = 65_535;
    private static final int FEWER_OPERATIONS = 50;
    private static final int MORE_OPERATIONS = 150;

    private static final String SERVICE_NAME = "PoolGuard";
    private static final String CLIENT_PACKAGE = "software.amazon.awssdk.services.poolguard";

    private static Map<String, PoolGrowth> growthByClass;
    private static Path workDir;

    @BeforeAll
    static void measurePoolGrowth() throws IOException {
        workDir = Files.createTempDirectory("constant-pool-budget");
        Map<String, Integer> fewer = compileAndCountPoolEntries(FEWER_OPERATIONS);
        Map<String, Integer> more = compileAndCountPoolEntries(MORE_OPERATIONS);

        Map<String, PoolGrowth> growth = new LinkedHashMap<>();
        fewer.forEach((className, entries) -> growth.put(className, new PoolGrowth(className, entries, more.get(className))));
        growthByClass = growth;
    }

    @AfterAll
    static void deleteWorkDir() throws IOException {
        deleteRecursively(workDir);
    }

    /**
     * Budgets are the cost measured when this test was written plus a margin. Every margin is under 6 entries per
     * operation, the cost of one reintroduced per-operation lambda, so that such a regression still breaches the budget.
     *
     * <p>Measured on this synthetic model: 25.0 entries/operation for the async client (budget 28.0, +12%), 24.0 for the
     * sync client (27.0, +12.5%), and 15.0 for each interface (17.0, +13%).
     */
    private static Stream<Arguments> budgets() {
        return Stream.of(
            Arguments.of("DefaultPoolGuardAsyncClient", 28.0),
            Arguments.of("DefaultPoolGuardClient", 27.0),
            Arguments.of("PoolGuardAsyncClient", 17.0),
            Arguments.of("PoolGuardClient", 17.0)
        );
    }

    @ParameterizedTest
    @MethodSource("budgets")
    void constantPoolEntries_perAdditionalOperation_areWithinBudget(String className, double budget) {
        PoolGrowth growth = growthByClass.get(className);
        assertThat(growth).as("no measurement for %s; measured: %s", className, growthByClass.keySet()).isNotNull();
        assertThat(growth.marginalCostPerOperation())
            .withFailMessage(growth.regressionMessage(budget))
            .isLessThanOrEqualTo(budget);
    }

    private static Map<String, Integer> compileAndCountPoolEntries(int operationCount) throws IOException {
        Path root = workDir.resolve("ops-" + operationCount);
        Path sources = root.resolve("sources");
        Path classes = root.resolve("classes");
        Files.createDirectories(classes);

        CodeGenerator.builder()
                     .models(syntheticModels(operationCount))
                     .sourcesDirectory(sources.toString())
                     .resourcesDirectory(root.resolve("resources").toString())
                     .testsDirectory(root.resolve("tests").toString())
                     .build()
                     .execute();

        compile(sources, classes);

        Path clientPackage = classes.resolve(CLIENT_PACKAGE.replace('.', '/'));
        Map<String, Integer> entriesByClass = new LinkedHashMap<>();
        for (String className : Arrays.asList("Default" + SERVICE_NAME + "AsyncClient",
                                              "Default" + SERVICE_NAME + "Client",
                                              SERVICE_NAME + "AsyncClient",
                                              SERVICE_NAME + "Client")) {
            entriesByClass.put(className, constantPoolEntries(clientPackage.resolve(className + ".class")));
        }
        return entriesByClass;
    }

    /**
     * Reads {@code constant_pool_count}, the big-endian u2 at offset 8 of a class file. Index 0 is unused, so the entry
     * count is one less than the stored count.
     */
    private static int constantPoolEntries(Path classFile) throws IOException {
        byte[] bytes = Files.readAllBytes(classFile);
        return (((bytes[8] & 0xFF) << 8) | (bytes[9] & 0xFF)) - 1;
    }

    private static void compile(Path sources, Path classes) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).as("no system Java compiler; this test requires a JDK, not a JRE").isNotNull();

        List<Path> sourceFiles = new ArrayList<>();
        Files.walkFileTree(sources, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(".java")) {
                    sourceFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(classes.toFile()));
            boolean succeeded = compiler.getTask(null, fileManager, diagnostics,
                                                 Arrays.asList("-nowarn", "-proc:none", "-classpath", testClasspath()),
                                                 null,
                                                 fileManager.getJavaFileObjectsFromFiles(toFiles(sourceFiles)))
                                        .call();
            assertThat(succeeded).as("failed to compile the generated client: %s", errors(diagnostics)).isTrue();
        }
    }

    /**
     * The generated client compiles against the same dependencies this test runs with, so the test JVM's own classpath is
     * the compilation classpath.
     */
    private static String testClasspath() {
        return System.getProperty("java.class.path");
    }

    private static List<File> toFiles(List<Path> paths) {
        List<File> files = new ArrayList<>();
        paths.forEach(p -> files.add(p.toFile()));
        return files;
    }

    private static String errors(DiagnosticCollector<JavaFileObject> diagnostics) {
        StringBuilder message = new StringBuilder();
        diagnostics.getDiagnostics().stream()
                   .filter(d -> d.getKind() == Diagnostic.Kind.ERROR)
                   .limit(10)
                   .forEach(d -> message.append('\n').append(d));
        return message.toString();
    }

    /**
     * A service model that differs from the other operation count only in how many operations it declares, so that
     * differencing the two pool totals leaves the per-operation cost.
     */
    private static C2jModels syntheticModels(int operationCount) {
        ServiceMetadata metadata = new ServiceMetadata();
        metadata.setApiVersion("2010-05-08");
        metadata.setEndpointPrefix("poolguard");
        metadata.setJsonVersion("1.1");
        metadata.setProtocol("json");
        metadata.setServiceAbbreviation(SERVICE_NAME);
        metadata.setServiceFullName("Pool Guard Service");
        metadata.setServiceId(SERVICE_NAME);
        metadata.setSignatureVersion("v4");
        metadata.setSigningName("poolguard");
        metadata.setTargetPrefix("PoolGuard");
        metadata.setUid("poolguard-2010-05-08");

        Map<String, Operation> operations = new LinkedHashMap<>();
        Map<String, Shape> shapes = new LinkedHashMap<>();
        shapes.put("StringType", stringShape());
        shapes.put("InvalidInputException", exceptionShape());

        for (int i = 0; i < operationCount; i++) {
            String operationName = String.format(Locale.ROOT, "Operation%03d", i);
            operations.put(operationName, operation(operationName));
            shapes.put(operationName + "Request", structureShape());
            shapes.put(operationName + "Response", structureShape());
        }

        ServiceModel serviceModel = new ServiceModel();
        serviceModel.setMetadata(metadata);
        serviceModel.setDocumentation("A synthetic service used to measure constant pool growth.");
        serviceModel.setOperations(operations);
        serviceModel.setShapes(shapes);

        return C2jModels.builder()
                        .serviceModel(serviceModel)
                        .customizationConfig(CustomizationConfig.create())
                        .build();
    }

    private static Operation operation(String operationName) {
        Http http = new Http();
        http.setMethod("POST");
        http.setRequestUri("/");

        Input input = new Input();
        input.setShape(operationName + "Request");

        Output output = new Output();
        output.setShape(operationName + "Response");

        ErrorMap error = new ErrorMap();
        error.setShape("InvalidInputException");

        Operation operation = new Operation();
        operation.setName(operationName);
        operation.setHttp(http);
        operation.setInput(input);
        operation.setOutput(output);
        operation.setErrors(Collections.singletonList(error));
        operation.setDocumentation("Performs " + operationName + ".");
        return operation;
    }

    private static Shape structureShape() {
        Member member = new Member();
        member.setShape("StringType");
        member.setDocumentation("A string member.");

        Shape shape = new Shape();
        shape.setType("structure");
        shape.setMembers(Collections.singletonMap("StringMember", member));
        return shape;
    }

    private static Shape stringShape() {
        Shape shape = new Shape();
        shape.setType("string");
        return shape;
    }

    private static Shape exceptionShape() {
        ErrorTrait errorTrait = new ErrorTrait();
        errorTrait.setHttpStatusCode(400);

        Shape shape = new Shape();
        shape.setType("structure");
        shape.setException(true);
        shape.setError(errorTrait);
        shape.setDocumentation("The request was rejected.");
        return shape;
    }

    private static void deleteRecursively(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path visited, IOException exc) throws IOException {
                Files.delete(visited);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static final class PoolGrowth {
        private final String className;
        private final int fewerEntries;
        private final int moreEntries;

        private PoolGrowth(String className, int fewerEntries, int moreEntries) {
            this.className = className;
            this.fewerEntries = fewerEntries;
            this.moreEntries = moreEntries;
        }

        private double marginalCostPerOperation() {
            return (moreEntries - fewerEntries) / (double) (MORE_OPERATIONS - FEWER_OPERATIONS);
        }

        private int fixedCost() {
            return (int) Math.round(fewerEntries - marginalCostPerOperation() * FEWER_OPERATIONS);
        }

        private long operationCeiling(double budget) {
            return Math.round((CONSTANT_POOL_LIMIT - fixedCost()) / budget);
        }

        private String regressionMessage(double budget) {
            return String.format(
                Locale.ROOT,
                "Per-operation constant pool cost of %s regressed.%n%n"
                + "  measured   %.1f entries/operation   (%d entries at %d ops vs %d at %d ops)%n"
                + "  budget     %.1f entries/operation%n"
                + "  fixed cost %d entries (operation-count independent)%n%n"
                + "At %.1f entries/operation a client exceeds the JVM's %d-entry class constant pool limit at ~%d "
                + "operations. The largest service model in the SDK (EC2) currently has over 750 operations.%n%n"
                + "Something in the generated operation body is now emitted once per operation instead of once per "
                + "client. The usual cause is an inline lambda or method reference; each one costs ~6 entries per "
                + "operation. Hoist it into a shared private helper on the client, as "
                + "ClientClassUtils.publishMetrics and publishMetricsWhenComplete do for metric publishing.%n%n"
                + "If this growth is intended and justified, raise the budget deliberately and record the reason "
                + "alongside it.",
                className, marginalCostPerOperation(), fewerEntries, FEWER_OPERATIONS, moreEntries, MORE_OPERATIONS,
                budget, fixedCost(), marginalCostPerOperation(), CONSTANT_POOL_LIMIT,
                operationCeiling(marginalCostPerOperation()));
        }
    }
}
