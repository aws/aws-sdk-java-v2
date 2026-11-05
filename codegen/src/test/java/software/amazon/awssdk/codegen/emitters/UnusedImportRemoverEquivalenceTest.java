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

package software.amazon.awssdk.codegen.emitters;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Checks that {@link UnusedImportRemover} produces exactly the same output as its previous, regex-per-import
 * implementation, so that generated code does not change.
 */
class UnusedImportRemoverEquivalenceTest {
    private static final String INJECTED_IMPORTS =
        "import a.unused.NeverReferenced;\n"
        + "import java.util.List;\n"
        + "import static a.unused.Helpers.neverCalled;\n"
        + "import a.wildcard.*;\n"
        + "import a.javadoc.OnlyInJavadoc;\n";

    private static final String JAVADOC_REFERENCE = "\n/** See {@link OnlyInJavadoc}. */\n";

    private final UnusedImportRemover remover = new UnusedImportRemover();
    private final ReferenceUnusedImportRemover reference = new ReferenceUnusedImportRemover();

    static Stream<Path> corpus() throws IOException, URISyntaxException {
        Path emittersDir = Paths.get(UnusedImportRemoverEquivalenceTest.class
                                         .getResource("content-before-unused-import-removal.txt").toURI()).getParent();
        Path codegenResourcesDir = emittersDir.getParent();
        List<Path> files;
        try (Stream<Path> walk = Files.walk(codegenResourcesDir)) {
            files = walk.filter(p -> p.toString().endsWith(".java")).sorted().collect(Collectors.toList());
        }
        assertThat(files).isNotEmpty();
        return files.stream();
    }

    @ParameterizedTest
    @MethodSource("corpus")
    void apply_whenGivenCorpusFile_matchesReferenceImplementation(Path file) throws IOException {
        String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertMatchesReference(content);
    }

    @ParameterizedTest
    @MethodSource("corpus")
    void apply_whenGivenCorpusFileWithInjectedImports_matchesReferenceImplementation(Path file) throws IOException {
        String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertMatchesReference(injectImports(content));
    }

    @Test
    void apply_whenStaticImportUnused_removesIt() {
        String content = "package p;\n\nimport static a.B.used;\nimport static a.B.unused;\n\nclass C { int x = used(); }\n";
        assertThat(applyAndCheck(content))
            .isEqualTo("package p;\n\nimport static a.B.used;\n\n\nclass C { int x = used(); }\n");
    }

    @Test
    void apply_whenWildcardImport_keepsIt() {
        String content = "package p;\n\nimport a.b.*;\n\nclass C { }\n";
        assertThat(applyAndCheck(content)).isEqualTo(content);
    }

    @Test
    void apply_whenSymbolOnlyInJavadocLink_keepsImport() {
        String content = "package p;\n\nimport a.Foo;\n\n/** See {@link Foo}. */\nclass C { }\n";
        assertThat(applyAndCheck(content)).isEqualTo(content);
    }

    @Test
    void apply_whenSymbolOnlyInStringLiteral_keepsImport() {
        String content = "package p;\n\nimport a.Foo;\n\nclass C { String s = \"Foo\"; }\n";
        assertThat(applyAndCheck(content)).isEqualTo(content);
    }

    @Test
    void apply_whenNestedTypeUsedThroughOuter_keepsOuterImport() {
        String content = "package p;\n\nimport a.Outer;\n\nclass C { Outer.Inner i; }\n";
        assertThat(applyAndCheck(content)).isEqualTo(content);
    }

    @Test
    void apply_whenImportDuplicated_keepsOneCopy() {
        String content = "package p;\n\nimport a.Foo;\nimport a.Foo;\n\nclass C { Foo f; }\n";
        assertThat(applyAndCheck(content)).isEqualTo("package p;\n\n\nimport a.Foo;\n\nclass C { Foo f; }\n");
    }

    @Test
    void apply_whenSameSimpleNameFromDifferentPackages_keepsBoth() {
        String content = "package p;\n\nimport a.Foo;\nimport b.Foo;\n\nclass C { Foo f; }\n";
        assertThat(applyAndCheck(content)).isEqualTo(content);
    }

    @Test
    void apply_whenImportTextAppearsInJavadoc_countsItAsDuplicate() {
        String content = "package p;\n\nimport a.Foo;\n\n/** Use import a.Foo; to get it. */\nclass C { Foo f; }\n";
        assertThat(applyAndCheck(content))
            .isEqualTo("package p;\n\n\n\n/** Use import a.Foo; to get it. */\nclass C { Foo f; }\n");
    }

    @Test
    void apply_whenSymbolContainsDollar_usesRegexFallback() {
        String content = "package p;\n\nimport a.Foo$Bar;\nimport a.Baz$Qux;\n\nclass C { Object o = Foo$Bar.X; }\n";
        String result = applyAndCheck(content);
        assertThat(result).contains("import a.Baz$Qux;");
    }

    @Test
    void apply_whenContentIsJson_leavesItUnchanged() {
        String json = "{ \"SomeKey\": 4, \"ADict\": { \"SubKey\": \"Subvalue\" } }";
        assertThat(applyAndCheck(json)).isEqualTo(json);
    }

    @Test
    void apply_whenSymbolOnlyAppearsAfterNonAsciiLetter_matchesReferenceImplementation() {
        // The reference result depends on the JDK's definition of \b (JDK-8264160), so only equality is asserted.
        String content = "package p;\n\nimport a.Foo;\n\nclass C { int \u00e9Foo; }\n";
        assertMatchesReference(content);
    }

    private String applyAndCheck(String content) {
        String result = remover.apply(content);
        assertThat(result).isEqualTo(reference.apply(content));
        return result;
    }

    private void assertMatchesReference(String content) {
        assertThat(remover.apply(content)).isEqualTo(reference.apply(content));
    }

    private static String injectImports(String content) {
        Matcher existingImport = Pattern.compile("^import [^;\\n]+;\\n", Pattern.MULTILINE).matcher(content);
        String duplicate = existingImport.find() ? existingImport.group() : "";
        String injected = INJECTED_IMPORTS + duplicate;
        Matcher packageLine = Pattern.compile("^package [^;]+;\\n", Pattern.MULTILINE).matcher(content);
        String withImports = packageLine.find()
                             ? content.substring(0, packageLine.end()) + injected + content.substring(packageLine.end())
                             : injected + content;
        return withImports + JAVADOC_REFERENCE;
    }

    /**
     * Verbatim copy of the previous {@link UnusedImportRemover} implementation, used as the oracle for the equivalence
     * tests. Code generation output must not change when the optimized implementation is used.
     */
    private static final class ReferenceUnusedImportRemover implements CodeTransformer {
        private static final Pattern IMPORT_PATTERN = Pattern.compile("^import(?:\\s+)(?:static\\s+)?(.*)(?:\\s*);",
                                                                      Pattern.MULTILINE);

        @Override
        public String apply(String content) {
            return findUnusedImports(content).stream().map(this::removeImportFunction).reduce(Function.identity(),
                    Function::andThen).apply(content);
        }

        private Function<String, String> removeImportFunction(String importToRemove) {
            return c -> c.replaceFirst(findSpecificImportRegex(importToRemove), "");
        }

        private Set<String> findUnusedImports(String content) {
            return findImports(content).stream().filter(isUnused(content)).collect(toSet());
        }

        private List<String> findImports(String content) {
            Matcher m = IMPORT_PATTERN.matcher(content);
            List<String> imports = new ArrayList<>();
            while (m.find()) {
                imports.add(m.group(1));
            }
            return imports;
        }

        private String removeAllImports(String content) {
            Matcher matcher = IMPORT_PATTERN.matcher(content);
            return matcher.replaceAll("");
        }

        private Predicate<String> isUnused(String content) {
            String contentWithoutImports = removeAllImports(content);
            return importToCheck -> !importToCheck.contains("*")
                    && (isNotReferenced(contentWithoutImports, importToCheck) || isDuplicate(content, importToCheck));
        }

        private boolean isNotReferenced(String contentWithoutImports, String importToCheck) {
            String symbol = importToCheck.substring(importToCheck.lastIndexOf('.') + 1);
            return !Pattern.compile(String.format("\\b%s\\b", symbol)).matcher(contentWithoutImports).find();
        }

        private boolean isDuplicate(String content, String importToCheck) {
            Matcher m = Pattern.compile(findSpecificImportRegex(importToCheck)).matcher(content);
            return m.find() && m.find();
        }

        private String findSpecificImportRegex(String specificImport) {
            return String.format("import(?:\\s+)(?:static )?(?:%s)(?:\\s*);", specificImport);
        }
    }
}
