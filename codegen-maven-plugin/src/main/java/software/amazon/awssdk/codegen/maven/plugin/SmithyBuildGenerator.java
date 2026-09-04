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
 * Runs {@code smithy-build} for a service module, using the module's {@code smithy-build.json} as the source of
 * truth, and registers the resulting output directories with the Maven project.
 *
 * <p>The {@code smithy-build} library is embedded rather than invoked through the Smithy CLI so that generation stays
 * inside the Maven lifecycle. One consequence is that the {@code maven} block of {@code smithy-build.json} is not
 * honored, because dependency resolution for that block is a CLI feature; this is rejected explicitly rather than
 * ignored silently.
 */
final class SmithyBuildGenerator {
    /**
     * Directory names written by {@link AwsSdkJavaCodegenPlugin}, relative to its plugin manifest.
     */
    private static final String SOURCES_DIR = "java";
    private static final String RESOURCES_DIR = "resources";
    private static final String TESTS_DIR = "tests";

    private final MavenProject project;
    private final Log log;

    SmithyBuildGenerator(MavenProject project, Log log) {
        this.project = project;
        this.log = log;
    }

    void generate(Path configFile, Path outputDirectory) throws MojoExecutionException {
        log.info("Generating from " + configFile);

        SmithyBuildConfig config = SmithyBuildConfig.load(configFile);

        if (config.getMaven().isPresent()) {
            // Resolving this block is a Smithy CLI feature. It is retained in the config so that the same
            // smithy-build.json can be built with the CLI, but Maven supplies the classpath itself.
            log.info("Ignoring the 'maven' block in " + configFile + "; it applies to the Smithy CLI only. Under Maven, "
                     + "the Smithy build classpath comes from the codegen module plus any provided-scope dependencies "
                     + "of this module.");
        }

        config = injectBaseDir(config, project.getBasedir().toPath());

        List<Path> sources = resolvePaths(config.getSources());
        List<Path> imports = resolvePaths(config.getImports());
        if (sources.isEmpty() && imports.isEmpty()) {
            throw new MojoExecutionException(
                "No 'sources' or 'imports' are declared in " + configFile + ", so there is no model to generate from.");
        }

        ClassLoader classLoader = buildClassLoader();
        Model model = assembleModel(configFile, sources, imports, classLoader);

        SmithyBuildResult result;
        try {
            result = SmithyBuild.create(classLoader)
                                .config(config)
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

        registerOutputDirectories(result);
    }

    /**
     * Assembles the model that {@code smithy-build} projects and hands to plugins.
     *
     * <p>The embedded {@code smithy-build} library does not read {@code sources} and {@code imports} itself; that is
     * the responsibility of whatever drives it, which is normally the Smithy CLI. Validation is left enabled so that
     * model problems surface here rather than as confusing code generation failures later.
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
            throw new MojoExecutionException("The Smithy model referenced by " + configFile + " has validation "
                                            + "errors:\n" + describeEvents(assembled.getValidationEvents()));
        }

        return assembled.unwrap();
    }

    private static void requireExists(Path configFile, Path path, String setting) throws MojoExecutionException {
        if (!Files.exists(path)) {
            throw new MojoExecutionException(String.format(
                "The '%s' entry '%s' declared in %s does not exist.", setting, path, configFile));
        }
    }

    /**
     * Resolves configured paths. {@link SmithyBuildConfig#load(Path)} already resolves them against the directory
     * containing the config file, so they only need normalizing here.
     */
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
     * Builds the class loader that {@code smithy-build} uses to discover plugins, transforms, and trait definitions.
     *
     * <p>The parent is this Maven plugin's own class loader, which supplies the codegen plugin and the Smithy
     * libraries. Layered on top are the module's {@code provided}-scope dependencies, which is how a service declares
     * build-time-only code such as a custom {@link software.amazon.smithy.build.ProjectionTransformer}. This is the
     * Maven counterpart to the Smithy Gradle plugin's {@code smithyBuild} configuration.
     *
     * <p>{@code provided} is used rather than the Maven plugin's own {@code <dependencies>} because plugin
     * dependencies are resolved from repositories only, never from the reactor, so a customization module living
     * beside the service could not be built and consumed in the same invocation.
     */
    private ClassLoader buildClassLoader() throws MojoExecutionException {
        ClassLoader parent = AwsSdkJavaCodegenPlugin.class.getClassLoader();
        List<URL> urls = new ArrayList<>();

        for (Artifact artifact : resolvedArtifacts()) {
            if (!Artifact.SCOPE_PROVIDED.equals(artifact.getScope()) || artifact.getFile() == null) {
                continue;
            }
            try {
                urls.add(artifact.getFile().toURI().toURL());
                log.info("Adding to Smithy build classpath: " + artifact.getFile());
            } catch (MalformedURLException e) {
                throw new MojoExecutionException("Could not add " + artifact.getFile()
                                                 + " to the Smithy build classpath", e);
            }
        }

        if (urls.isEmpty()) {
            return parent;
        }
        return new URLClassLoader(urls.toArray(new URL[0]), parent);
    }

    /**
     * {@code MavenProject#getArtifacts} is untyped in the Maven 2 project API this plugin compiles against.
     */
    @SuppressWarnings("unchecked")
    private Set<Artifact> resolvedArtifacts() {
        return project.getArtifacts();
    }

    /**
     * Adds an absolute {@code baseDir} to every {@code aws-sdk-java-v2-codegen} plugin configuration so that relative
     * paths in plugin settings resolve against the module directory.
     *
     * <p>This is necessary because the process working directory during a Maven build is the directory Maven was
     * invoked from, which is the repository root for an invocation like {@code mvn install -pl :account}, not the
     * module being built.
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

    /**
     * Extracts the plugin name from a plugin ID, which may carry a trailing {@code ::artifact-name}.
     */
    private static String pluginName(String pluginId) {
        int separator = pluginId.indexOf("::");
        return separator < 0 ? pluginId : pluginId.substring(0, separator);
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
     * Registers the source, resource, and test roots produced by the codegen plugin. Unlike the C2J path, these
     * directories are not known to the root POM's {@code build-helper} configuration, so they are added here.
     */
    private void registerOutputDirectories(SmithyBuildResult result) throws MojoExecutionException {
        List<Path> pluginOutputs = new ArrayList<>();

        for (ProjectionResult projection : result.getProjectionResults()) {
            projection.getPluginManifests().forEach((artifactName, manifest) -> {
                if (pluginName(artifactName).equals(AwsSdkJavaCodegenPlugin.NAME)) {
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
            log.info("Adding compile source root " + directory);
            project.addCompileSourceRoot(directory.toAbsolutePath().toString());
        }
    }

    private void addTestCompileSourceRoot(Path directory) {
        if (containsFiles(directory)) {
            log.info("Adding test compile source root " + directory);
            project.addTestCompileSourceRoot(directory.toAbsolutePath().toString());
        }
    }

    private void addResource(Path directory) {
        if (containsFiles(directory)) {
            log.info("Adding resource directory " + directory);
            Resource resource = new Resource();
            resource.setDirectory(directory.toAbsolutePath().toString());
            project.addResource(resource);
        }
    }

    /**
     * Empty directories are skipped so that the build does not gain source roots or resource directories that would
     * produce nothing. The codegen plugin always creates all three directories, but only some are populated.
     */
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
