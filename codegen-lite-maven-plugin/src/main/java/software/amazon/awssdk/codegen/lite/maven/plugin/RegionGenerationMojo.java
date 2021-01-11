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

package software.amazon.awssdk.codegen.lite.maven.plugin;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import software.amazon.awssdk.codegen.lite.CodeGenerator;
import software.amazon.awssdk.codegen.lite.regions.PartitionMetadataGenerator;
import software.amazon.awssdk.codegen.lite.regions.PartitionMetadataProviderGenerator;
import software.amazon.awssdk.codegen.lite.regions.RegionGenerator;
import software.amazon.awssdk.codegen.lite.regions.RegionMetadataGenerator;
import software.amazon.awssdk.codegen.lite.regions.RegionMetadataLoader;
import software.amazon.awssdk.codegen.lite.regions.RegionMetadataProviderGenerator;
import software.amazon.awssdk.codegen.lite.regions.ServiceMetadataGenerator;
import software.amazon.awssdk.codegen.lite.regions.ServiceMetadataProviderGenerator;
import software.amazon.awssdk.codegen.lite.regions.model.Partitions;

/**
 * The Maven mojo to generate Java client code using software.amazon.awssdk:codegen module.
 */
@Mojo(name = "generate-regions")
public class RegionGenerationMojo extends AbstractMojo {

    private static final String PARTITION_METADATA_BASE = "software.amazon.awssdk.regions.partitionmetadata";
    private static final String SERVICE_METADATA_BASE = "software.amazon.awssdk.regions.servicemetadata";
    private static final String REGION_METADATA_BASE = "software.amazon.awssdk.regions.regionmetadata";
    private static final String REGION_BASE = "software.amazon.awssdk.regions";

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}")
    private String outputDirectory;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(property = "endpoints", defaultValue =
        "${basedir}/src/main/resources/software/amazon/awssdk/regions/internal/region/endpoints.json")
    private File endpoints;

    public void execute() throws MojoExecutionException {
        Path baseSourcesDirectory = Paths.get(outputDirectory).resolve("generated-sources").resolve("sdk");
        Path testsDirectory = Paths.get(outputDirectory).resolve("generated-test-sources").resolve("sdk-tests");

        Partitions partitions = RegionMetadataLoader.build(endpoints);

        generatePartitionMetadataClass(baseSourcesDirectory, partitions);
        generateRegionClass(baseSourcesDirectory, partitions);
        generateServiceMetadata(baseSourcesDirectory, partitions);
        generateRegions(baseSourcesDirectory, partitions);
        generatePartitionProvider(baseSourcesDirectory, partitions);
        generateRegionProvider(baseSourcesDirectory, partitions);
        generateServiceProvider(baseSourcesDirectory, partitions);

        project.addCompileSourceRoot(baseSourcesDirectory.toFile().getAbsolutePath());
        project.addTestCompileSourceRoot(testsDirectory.toFile().getAbsolutePath());
    }

    public void generatePartitionMetadataClass(Path baseSourcesDirectory, Partitions partitions) {
        Path sourcesDirectory = baseSourcesDirectory.resolve(PARTITION_METADATA_BASE.replace(".", "/"));
        partitions.getPartitions()
                  .forEach(p -> new CodeGenerator(sourcesDirectory.toString(),
                                                  new PartitionMetadataGenerator(p,
                                                                                 PARTITION_METADATA_BASE,
                                                                                 REGION_BASE)).generate());
    }

    public void generateRegionClass(Path baseSourcesDirectory, Partitions partitions) {
        Path sourcesDirectory = baseSourcesDirectory.resolve(REGION_BASE.replace(".", "/"));
        new CodeGenerator(sourcesDirectory.toString(), new RegionGenerator(partitions, REGION_BASE)).generate();
    }

    public void generateServiceMetadata(Path baseSourcesDirectory, Partitions partitions) {
        Path sourcesDirectory = baseSourcesDirectory.resolve(SERVICE_METADATA_BASE.replace(".", "/"));
        Set<String> services = new HashSet<>();
        partitions.getPartitions().forEach(p -> services.addAll(p.getServices().keySet()));

        services.forEach(s -> new CodeGenerator(sourcesDirectory.toString(), new ServiceMetadataGenerator(partitions,
                                                                                                          s,
                                                                                                          SERVICE_METADATA_BASE,
                                                                                                          REGION_BASE))
            .generate());
    }

    public void generateRegions(Path baseSourcesDirectory, Partitions partitions) {
        Path sourcesDirectory = baseSourcesDirectory.resolve(REGION_METADATA_BASE.replace(".", "/"));
        partitions.getPartitions()
                  .forEach(p -> p.getRegions().forEach((k, v) ->
                                                           new CodeGenerator(sourcesDirectory.toString(),
                                                                             new RegionMetadataGenerator(p,
                                                                                                         k,
                                                                                                         v.getDescription(),
                                                                                                         REGION_METADATA_BASE,
                                                                                                         REGION_BASE))
                                                               .generate()));
    }

    public void generatePartitionProvider(Path baseSourcesDirectory, Partitions partitions) {
        Path sourcesDirectory = baseSourcesDirectory.resolve(REGION_BASE.replace(".", "/"));
        new CodeGenerator(sourcesDirectory.toString(), new PartitionMetadataProviderGenerator(partitions,
                                                                                              PARTITION_METADATA_BASE,
                                                                                              REGION_BASE))
            .generate();
    }

    public void generateRegionProvider(Path baseSourcesDirectory, Partitions partitions) {
        Path sourcesDirectory = baseSourcesDirectory.resolve(REGION_BASE.replace(".", "/"));
        new CodeGenerator(sourcesDirectory.toString(), new RegionMetadataProviderGenerator(partitions,
                                                                                           REGION_METADATA_BASE,
                                                                                           REGION_BASE))
            .generate();
    }

    public void generateServiceProvider(Path baseSourcesDirectory, Partitions partitions) {
        Path sourcesDirectory = baseSourcesDirectory.resolve(REGION_BASE.replace(".", "/"));
        new CodeGenerator(sourcesDirectory.toString(), new ServiceMetadataProviderGenerator(partitions,
                                                                                            SERVICE_METADATA_BASE,
                                                                                            REGION_BASE))
            .generate();
    }
}
