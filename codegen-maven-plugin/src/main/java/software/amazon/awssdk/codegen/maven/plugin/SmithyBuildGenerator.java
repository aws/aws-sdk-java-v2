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
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Runs smithy-build for a service module's {@code smithy-build.json} and registers the generated directories with the
 * Maven project. The config's {@code maven} block is ignored; under Maven the classpath comes from this plugin.
 */
final class SmithyBuildGenerator {

    // Output directories written by AwsSdkJavaCodegenPlugin.
    private static final String SOURCES_DIR = "generated-sources";
    private static final String RESOURCES_DIR = "generated-resources";
    private static final String TESTS_DIR = "generated-test-sources";

    private static final String SDK_VERSION_PROPERTY = "AWS_SDK_JAVA_VERSION";

    private final MavenProject project;
    private final Log log;

    SmithyBuildGenerator(MavenProject project, Log log) {
        this.project = project;
        this.log = log;
    }

    void generate(Path configFile, Path outputDirectory) throws MojoExecutionException {
        log.info("Generating from " + configFile);
        SmithyBuildConfig config = loadConfig(configFile);

        config = injectBaseDir(config, project.getBasedir().toPath());

        List<Path> sources = resolvePaths(config.getSources());
        List<Path> imports = resolvePaths(config.getImports());
        if (sources.isEmpty() && imports.isEmpty()) {
            throw new MojoExecutionException(
                "No 'sources' or 'imports' are declared in " + configFile + ", so there is no model to generate from.");
        }

        try (URLClassLoader classLoader = buildClassLoader()) {
            Model model = assembleModel(configFile, sources, imports, classLoader);
            // The model already contains the top-level imports; SmithyBuild would otherwise load them again.
            SmithyBuildConfig buildConfig = config.toBuilder().imports(Collections.emptyList()).build();

            SmithyBuildResult result;
            try {
                result = SmithyBuild.create(classLoader)
                                    .config(buildConfig)
                                    .model(model)
                                    .registerSources(sources.toArray(new Path[0]))
                                    .outputDirectory(outputDirectory)
                                    .build();
            } catch (RuntimeException e) {
                throw new MojoExecutionException("smithy-build failed for " + configFile, e);
            }

            if (result.anyBroken()) {
                throw new MojoExecutionException("smithy-build reported validation errors for " + configFile + ":\n"
                                                 + describeFailures(result));
            }

            registerOutputDirectories(result, codegenArtifactNames(config));
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to close the Smithy build classpath for " + configFile, e);
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
    private Model assembleModel(Path configFile, List<Path> sources, List<Path> imports, ClassLoader classLoader)
            throws MojoExecutionException {
        ModelAssembler assembler = Model.assembler(classLoader).discoverModels(classLoader);
        for (Path source : sources) {
            requireExists(configFile, source, "sources");
            assembler.addImport(source);
        }
        for (Path modelImport : imports) {
            requireExists(configFile, modelImport, "imports");
            assembler.addImport(modelImport);
        }

        ValidatedResult<Model> assembled = assembler.assemble();
        if (assembled.isBroken()) {
            log.warn("Smithy model validation errors for " + configFile + ":\n"
                     + describeEvents(assembled.getValidationEvents()));
        }
        return assembled.getResult().orElseThrow(() -> new MojoExecutionException(
            "The Smithy model referenced by " + configFile + " could not be assembled:\n"
            + describeEvents(assembled.getValidationEvents())));
    }

    private static void requireExists(Path configFile, Path path, String setting) throws MojoExecutionException {
        if (!Files.exists(path)) {
            throw new MojoExecutionException(String.format(
                "The '%s' entry '%s' declared in %s does not exist.", setting, path, configFile));
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
     * Loads the config with {@code ${AWS_SDK_JAVA_VERSION}} set to the project version.
     */
    private SmithyBuildConfig loadConfig(Path configFile) {
        String previous = System.setProperty(SDK_VERSION_PROPERTY, project.getVersion());
        try {
            return SmithyBuildConfig.load(configFile);
        } finally {
            if (previous == null) {
                System.clearProperty(SDK_VERSION_PROPERTY);
            } else {
                System.setProperty(SDK_VERSION_PROPERTY, previous);
            }
        }
    }

    /**
     * Sets {@code baseDir} on each codegen plugin config so relative paths resolve against the module directory.
     */
    private static SmithyBuildConfig injectBaseDir(SmithyBuildConfig config, Path baseDir) {
        SmithyBuildConfig.Builder builder = config.toBuilder();
        builder.plugins(withBaseDir(config.getPlugins(), baseDir));
        if (!config.getProjections().isEmpty()) {
            Map<String, ProjectionConfig> projections = new HashMap<>();
            config.getProjections().forEach((name, projection) -> projections.put(
                name, projection.toBuilder()
                                .plugins(withBaseDir(projection.getPlugins(), baseDir))
                                .build()));
            builder.projections(projections);
        }
        return builder.build();
    }

    private static Map<String, ObjectNode> withBaseDir(Map<String, ObjectNode> plugins, Path baseDir) {
        Map<String, ObjectNode> updated = new HashMap<>(plugins);
        plugins.forEach((pluginId, settings) -> {
            if (pluginName(pluginId).equals(AwsSdkJavaCodegenPlugin.NAME)) {
                updated.put(pluginId, settings.withMember("baseDir", baseDir.toAbsolutePath().toString()));
            }
        });
        return updated;
    }

    // Plugin IDs may carry an "::artifact-name" suffix.
    private static String pluginName(String pluginId) {
        int separator = pluginId.indexOf("::");
        return separator < 0 ? pluginId : pluginId.substring(0, separator);
    }

    // smithy-build names a plugin's output after its artifact name, which is the plugin name when there is no suffix.
    private static String artifactName(String pluginId) {
        int separator = pluginId.indexOf("::");
        return separator < 0 ? pluginId : pluginId.substring(separator + 2);
    }

    private static Set<String> codegenArtifactNames(SmithyBuildConfig config) {
        List<String> pluginIds = new ArrayList<>(config.getPlugins().keySet());
        config.getProjections().values().forEach(projection -> pluginIds.addAll(projection.getPlugins().keySet()));
        return pluginIds.stream()
                        .filter(pluginId -> pluginName(pluginId).equals(AwsSdkJavaCodegenPlugin.NAME))
                        .map(SmithyBuildGenerator::artifactName)
                        .collect(Collectors.toSet());
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
    private void registerOutputDirectories(SmithyBuildResult result, Set<String> codegenArtifactNames)
            throws MojoExecutionException {
        List<Path> pluginOutputs = new ArrayList<>();
        for (ProjectionResult projection : result.getProjectionResults()) {
            projection.getPluginManifests().forEach((artifactName, manifest) -> {
                if (codegenArtifactNames.contains(artifactName)) {
                    pluginOutputs.add(manifest.getBaseDir());
                }
            });
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

    private void addCompileSourceRoot(Path directory) {
        if (containsFiles(directory)) {
            project.addCompileSourceRoot(directory.toAbsolutePath().toString());
        }
    }

    private void addTestCompileSourceRoot(Path directory) {
        if (containsFiles(directory)) {
            project.addTestCompileSourceRoot(directory.toAbsolutePath().toString());
        }
    }

    private void addResource(Path directory) {
        if (containsFiles(directory)) {
            Resource resource = new Resource();
            resource.setDirectory(directory.toAbsolutePath().toString());
            project.addResource(resource);
        }
    }

    // The plugin always creates all three output directories; empty ones are not registered.
    private static boolean containsFiles(Path directory) {
        if (!Files.isDirectory(directory)) {
            return false;
        }
        try (Stream<Path> files = Files.walk(directory)) {
            return files.anyMatch(Files::isRegularFile);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to inspect generated directory " + directory, e);
        }
    }
}
