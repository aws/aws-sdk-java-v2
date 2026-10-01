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

package software.amazon.awssdk.codegen.maven.plugin;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import software.amazon.awssdk.codegen.smithy.build.AwsSdkJavaCodegenPlugin;
import software.amazon.smithy.build.ProjectionResult;
import software.amazon.smithy.build.SmithyBuild;
import software.amazon.smithy.build.SmithyBuildResult;
import software.amazon.smithy.build.model.ProjectionConfig;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Runs smithy-build for a Smithy-based service module and registers the generated directories with the Maven project.
 *
 * <p>This class is used only when {@link GenerationMojo} builds a module that has a {@code smithy-build.json} at its
 * root. The {@code maven} block of {@code smithy-build.json} is ignored here, because Maven has already resolved the
 * classpath: this plugin's dependencies plus the module's {@code provided} dependencies. The {@code maven} block is
 * only used when building with the Smithy CLI.
 */
final class SmithyBuildGenerator {

    // Output directories written by AwsSdkJavaCodegenPlugin.
    private static final String SOURCES_DIR = "generated-sources";
    private static final String RESOURCES_DIR = "generated-resources";
    private static final String TESTS_DIR = "generated-test-sources";

    private static final String SDK_VERSION_PLACEHOLDER = "${AWS_SDK_JAVA_VERSION}";

    private static final String BASE_DIR_SETTING = "baseDir";

    private final MavenProject project;
    private final Log log;

    SmithyBuildGenerator(MavenProject project, Log log) {
        this.project = project;
        this.log = log;
    }

    void generate(Path smithyBuildFile, Path outputDirectory) throws MojoExecutionException {
        log.info("Generating from " + smithyBuildFile);
        SmithyBuildConfig smithyBuildConfig = loadSmithyBuildConfig(smithyBuildFile);

        smithyBuildConfig = injectBaseDir(smithyBuildFile, smithyBuildConfig, project.getBasedir().toPath());

        List<Path> sources = resolvePaths(smithyBuildConfig.getSources());
        List<Path> imports = resolvePaths(smithyBuildConfig.getImports());
        if (sources.isEmpty() && imports.isEmpty()) {
            throw new MojoExecutionException(
                "No 'sources' or 'imports' are declared in " + smithyBuildFile
                + ", so there is no model to generate from.");
        }

        try (URLClassLoader classLoader = buildClassLoader()) {
            Model model = assembleModel(smithyBuildFile, sources, imports, classLoader);
            // The model already contains the top-level imports; SmithyBuild would otherwise load them again.
            SmithyBuildConfig smithyBuildConfigWithoutImports =
                smithyBuildConfig.toBuilder().imports(Collections.emptyList()).build();

            SmithyBuildResult result;
            try {
                result = SmithyBuild.create(classLoader)
                                    .config(smithyBuildConfigWithoutImports)
                                    .model(model)
                                    .registerSources(sources.toArray(new Path[0]))
                                    .outputDirectory(outputDirectory)
                                    .build();
            } catch (RuntimeException e) {
                throw new MojoExecutionException("smithy-build failed for " + smithyBuildFile, e);
            }

            if (result.anyBroken()) {
                throw new MojoExecutionException("smithy-build reported validation errors for " + smithyBuildFile
                                                 + ":\n" + describeFailures(result));
            }

            registerOutputDirectories(result);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to close the Smithy build classpath for " + smithyBuildFile, e);
        }
    }

    /**
     * Returns the plugin's class loader extended with the module's {@code provided}-scope dependencies, which is where
     * a service supplies its own transforms. Plugin dependencies can't be used for this because Maven resolves them
     * from repositories only, not from the reactor.
     */
    private URLClassLoader buildClassLoader() throws MojoExecutionException {
        ClassLoader parent = AwsSdkJavaCodegenPlugin.class.getClassLoader();
        List<URL> urls = new ArrayList<>();
        for (Artifact artifact : resolvedArtifacts()) {
            if (!Artifact.SCOPE_PROVIDED.equals(artifact.getScope()) || artifact.getFile() == null) {
                continue;
            }
            try {
                urls.add(artifact.getFile().toURI().toURL());
                log.debug("Adding to Smithy build classpath: " + artifact.getFile());
            } catch (MalformedURLException e) {
                throw new MojoExecutionException("Could not add " + artifact.getFile()
                                                 + " to the Smithy build classpath", e);
            }
        }
        return new URLClassLoader(urls.toArray(new URL[0]), parent);
    }

    // MavenProject#getArtifacts returns a raw Set in the Maven 2 API.
    @SuppressWarnings("unchecked")
    private Set<Artifact> resolvedArtifacts() {
        Set<Artifact> artifacts = project.getArtifacts();
        return artifacts == null ? Collections.emptySet() : artifacts;
    }

    /**
     * Assembles the model and logs its validation errors. A trait with no definition on the classpath is left out of
     * the model, so generation continues; any other validation error fails the build when smithy-build re-validates.
     */
    private Model assembleModel(Path smithyBuildFile, List<Path> sources, List<Path> imports, ClassLoader classLoader)
            throws MojoExecutionException {
        ModelAssembler assembler = Model.assembler(classLoader).discoverModels(classLoader);
        for (Path source : sources) {
            requireExists(smithyBuildFile, source, "sources");
            assembler.addImport(source);
        }
        for (Path modelImport : imports) {
            requireExists(smithyBuildFile, modelImport, "imports");
            assembler.addImport(modelImport);
        }

        ValidatedResult<Model> assembled = assembler.assemble();
        if (assembled.isBroken()) {
            log.warn("Smithy model validation errors for " + smithyBuildFile + ":\n"
                     + describeEvents(assembled.getValidationEvents()));
        }
        return assembled.getResult().orElseThrow(() -> new MojoExecutionException(
            "The Smithy model referenced by " + smithyBuildFile + " could not be assembled:\n"
            + describeEvents(assembled.getValidationEvents())));
    }

    private static void requireExists(Path smithyBuildFile, Path path, String setting) throws MojoExecutionException {
        if (!Files.exists(path)) {
            throw new MojoExecutionException(String.format(
                "The '%s' entry '%s' declared in %s does not exist.", setting, path, smithyBuildFile));
        }
    }

    private static List<Path> resolvePaths(List<String> paths) {
        return paths.stream().map(Paths::get).collect(Collectors.toList());
    }

    private static String describeEvents(List<ValidationEvent> events) {
        return events.stream()
                     .filter(e -> e.getSeverity() == Severity.ERROR || e.getSeverity() == Severity.DANGER)
                     .map(e -> String.format("  %s: %s", e.getSeverity(), e.getMessage()))
                     .collect(Collectors.joining("\n"));
    }

    /**
     * Loads {@code smithy-build.json} with {@code ${AWS_SDK_JAVA_VERSION}} replaced by the project version.
     */
    private SmithyBuildConfig loadSmithyBuildConfig(Path smithyBuildFile) throws MojoExecutionException {
        String content;
        try {
            content = new String(Files.readAllBytes(smithyBuildFile), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to read " + smithyBuildFile, e);
        }
        String substituted = content.replace(SDK_VERSION_PLACEHOLDER, project.getVersion());
        return SmithyBuildConfig.fromNode(Node.parseJsonWithComments(substituted, smithyBuildFile.toString()));
    }

    /**
     * Sets {@code baseDir} on the codegen plugin's settings so relative paths resolve against the module directory.
     * {@code baseDir} is supplied by this class, so a value written in {@code smithy-build.json} is rejected.
     */
    private static SmithyBuildConfig injectBaseDir(Path smithyBuildFile, SmithyBuildConfig smithyBuildConfig,
                                                   Path baseDir) throws MojoExecutionException {
        SmithyBuildConfig.Builder builder = smithyBuildConfig.toBuilder();
        builder.plugins(withBaseDir(smithyBuildFile, smithyBuildConfig.getPlugins(), baseDir));
        if (!smithyBuildConfig.getProjections().isEmpty()) {
            Map<String, ProjectionConfig> projections = new HashMap<>();
            for (Map.Entry<String, ProjectionConfig> entry : smithyBuildConfig.getProjections().entrySet()) {
                ProjectionConfig projection = entry.getValue();
                Map<String, ObjectNode> plugins = withBaseDir(smithyBuildFile, projection.getPlugins(), baseDir);
                projections.put(entry.getKey(), projection.toBuilder().plugins(plugins).build());
            }
            builder.projections(projections);
        }
        return builder.build();
    }

    private static Map<String, ObjectNode> withBaseDir(Path smithyBuildFile, Map<String, ObjectNode> plugins,
                                                       Path baseDir) throws MojoExecutionException {
        ObjectNode settings = plugins.get(AwsSdkJavaCodegenPlugin.NAME);
        if (settings == null) {
            return plugins;
        }
        if (settings.getMember(BASE_DIR_SETTING).isPresent()) {
            throw new MojoExecutionException(String.format(
                "The '%s' plugin settings in %s set '%s', which is supplied by codegen-maven-plugin. Remove it; "
                + "relative paths in the plugin settings resolve against the module directory.",
                AwsSdkJavaCodegenPlugin.NAME, smithyBuildFile, BASE_DIR_SETTING));
        }

        Map<String, ObjectNode> updated = new HashMap<>(plugins);
        updated.put(AwsSdkJavaCodegenPlugin.NAME,
                    settings.withMember(BASE_DIR_SETTING, baseDir.toAbsolutePath().toString()));
        return updated;
    }

    private static String describeFailures(SmithyBuildResult result) {
        return result.getProjectionResults().stream()
                     .filter(ProjectionResult::isBroken)
                     .flatMap(projection -> projection.getEvents().stream()
                                                      .filter(e -> e.getSeverity() == Severity.ERROR
                                                                   || e.getSeverity() == Severity.DANGER)
                                                      .map(e -> describeEvent(projection.getProjectionName(), e)))
                     .collect(Collectors.joining("\n"));
    }

    private static String describeEvent(String projectionName, ValidationEvent event) {
        return String.format("  [%s] %s: %s", projectionName, event.getSeverity(), event.getMessage());
    }

    /**
     * Registers the codegen plugin's output directories. Unlike the C2J output, the root POM does not add them.
     */
    private void registerOutputDirectories(SmithyBuildResult result) throws MojoExecutionException {
        List<Path> pluginOutputs = new ArrayList<>();
        for (ProjectionResult projection : result.getProjectionResults()) {
            projection.getPluginManifest(AwsSdkJavaCodegenPlugin.NAME)
                      .ifPresent(manifest -> pluginOutputs.add(manifest.getBaseDir()));
        }
        if (pluginOutputs.isEmpty()) {
            throw new MojoExecutionException(
                "smithy-build completed without running the '" + AwsSdkJavaCodegenPlugin.NAME + "' plugin. Add it to "
                + "the 'plugins' block of smithy-build.json.");
        }
        for (Path pluginOutput : pluginOutputs) {
            addCompileSourceRoot(pluginOutput.resolve(SOURCES_DIR));
            addTestCompileSourceRoot(pluginOutput.resolve(TESTS_DIR));
            addResource(pluginOutput.resolve(RESOURCES_DIR));
        }
    }

    private void addCompileSourceRoot(Path directory) throws MojoExecutionException {
        if (containsFiles(directory)) {
            project.addCompileSourceRoot(directory.toAbsolutePath().toString());
        }
    }

    private void addTestCompileSourceRoot(Path directory) throws MojoExecutionException {
        if (containsFiles(directory)) {
            project.addTestCompileSourceRoot(directory.toAbsolutePath().toString());
        }
    }

    private void addResource(Path directory) throws MojoExecutionException {
        if (containsFiles(directory)) {
            Resource resource = new Resource();
            resource.setDirectory(directory.toAbsolutePath().toString());
            project.addResource(resource);
        }
    }

    // The plugin always creates all three output directories; empty ones are not registered.
    private static boolean containsFiles(Path directory) throws MojoExecutionException {
        if (!Files.isDirectory(directory)) {
            return false;
        }
        try (Stream<Path> files = Files.walk(directory)) {
            return files.anyMatch(Files::isRegularFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to inspect generated directory " + directory, e);
        }
    }
}
