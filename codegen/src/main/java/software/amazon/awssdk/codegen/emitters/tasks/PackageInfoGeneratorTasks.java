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

package software.amazon.awssdk.codegen.emitters.tasks;

import static software.amazon.awssdk.codegen.internal.Constant.EXAMPLE_META_PATH;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.SimpleGeneratorTask;
import software.amazon.awssdk.codegen.internal.ExampleMetadataProvider;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.utils.CollectionUtils;

/**
 * Emits the package-info.java for the base service package. Includes the service
 * level documentation and code examples for that service organized by category.
 */
public final class PackageInfoGeneratorTasks extends BaseGeneratorTasks {

    /**
     * Mapping from internal category names to user-friendly display names.
     * This defines the preferred order and display format for code example categories.
     */
    private static final Map<String, String> CATEGORY_DISPLAY_MAPPING;
    
    static {
        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("Hello", "Getting Started");
        mapping.put("Basics", "Basics");
        mapping.put("Api", "API Actions");
        mapping.put("Scenarios", "Scenarios");
        mapping.put("Serverless examples", "Serverless Examples");
        CATEGORY_DISPLAY_MAPPING = Collections.unmodifiableMap(mapping);
    }

    private final String baseDirectory;

    PackageInfoGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.baseDirectory = dependencies.getPathProvider().getClientDirectory();
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        Metadata metadata = model.getMetadata();
        String packageInfoContents = buildPackageInfoContent(metadata, EXAMPLE_META_PATH);
        
        return Collections.singletonList(new SimpleGeneratorTask(baseDirectory,
                                                                 "package-info.java",
                                                                 model.getFileHeader(),
                                                                 () -> packageInfoContents));
    }

    /**
     * Builds the complete package-info.java content including Javadoc and package declaration.
     *
     * @param metadata the service metadata containing documentation and package information
     * @param exampleMetaPath the path to the example metadata JSON file
     * @return the complete package-info.java file content
     */
    String buildPackageInfoContent(Metadata metadata, String exampleMetaPath) {
        String baseDocumentation = metadata.getDocumentation();
        String codeExamples = getCodeExamplesWithPath(metadata, exampleMetaPath);
        String javadocContent = buildJavadocContent(baseDocumentation, codeExamples);
        
        return javadocContent + System.lineSeparator() + "package " + metadata.getFullClientPackageName() + ";";
    }

    /**
     * Builds the Javadoc comment content for the package-info.java file.
     *
     * @param baseDocumentation the base service documentation
     * @param codeExamples the formatted code examples content, or empty string if none
     * @return the complete Javadoc comment including opening and closing markers
     */
    private String buildJavadocContent(String baseDocumentation, String codeExamples) {
        StringBuilder javadoc = new StringBuilder();
        javadoc.append("/**").append(System.lineSeparator());
        javadoc.append(" * ").append(baseDocumentation).append(System.lineSeparator());

        if (!codeExamples.isEmpty()) {
            javadoc.append(" *").append(System.lineSeparator());
            javadoc.append(" * ").append(codeExamples).append(System.lineSeparator());
        }

        javadoc.append(" */");
        return javadoc.toString();
    }
    
    /**
     * Gets code examples using a custom example metadata path.
     */
    private String getCodeExamplesWithPath(Metadata metadata, String exampleMetaPath) {
        ExampleMetadataProvider exampleProvider = new ExampleMetadataProvider(exampleMetaPath);
        List<ExampleMetadataProvider.ExampleData> examples = exampleProvider.getServiceCodeExamples(metadata);
        
        if (examples.isEmpty()) {
            return "";
        }
        
        return generateCodeExamplesJavadoc(examples);
    }

    private String generateCodeExamplesJavadoc(List<ExampleMetadataProvider.ExampleData> examples) {
        Map<String, List<ExampleMetadataProvider.ExampleData>> categorizedExamples = 
            examples.stream().collect(Collectors.groupingBy(ExampleMetadataProvider.ExampleData::getCategory, 
                                                            LinkedHashMap::new, 
                                                            Collectors.toList()));
        
        StringBuilder javadoc = new StringBuilder();
        javadoc.append("<h2>Code Examples</h2>").append(System.lineSeparator());
        javadoc.append("<p>For code examples demonstrating how to use this service with the AWS SDK for Java v2, see:</p>")
               .append(System.lineSeparator());

        appendPredefinedCategories(javadoc, categorizedExamples, CATEGORY_DISPLAY_MAPPING);
        appendRemainingCategories(javadoc, categorizedExamples, CATEGORY_DISPLAY_MAPPING);

        return formatAsJavadocLines(javadoc.toString());
    }

    /**
     * Formats HTML content as properly indented Javadoc comment lines.
     * Each non-empty line gets prefixed with appropriate Javadoc comment formatting.
     *
     * @param htmlContent the HTML content to format for Javadoc
     * @return formatted string ready for inclusion in Javadoc comments
     */
    private String formatAsJavadocLines(String htmlContent) {
        StringBuilder result = new StringBuilder();
        String[] lines = htmlContent.split(System.lineSeparator());
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (!line.trim().isEmpty()) {
                result.append(line);
                if (i < lines.length - 1) {
                    result.append(System.lineSeparator()).append(" * ");
                }
            }
        }
        return result.toString();
    }

    /**
     * Appends predefined categories to the Javadoc in the preferred order.
     * Only includes categories that exist in the categorized examples and have content.
     */
    private void appendPredefinedCategories(StringBuilder javadoc,
                                            Map<String, List<ExampleMetadataProvider.ExampleData>> categorizedExamples,
                                            Map<String, String> categoryMapping) {
        categoryMapping.forEach((category, displayName) ->
            appendCategoryIfExists(javadoc, categorizedExamples, category, displayName));
    }

    /**
     * Appends any remaining categories that weren't in the predefined mapping.
     */
    private void appendRemainingCategories(StringBuilder javadoc,
                                           Map<String, List<ExampleMetadataProvider.ExampleData>> categorizedExamples,
                                           Map<String, String> categoryMapping) {
        categorizedExamples.entrySet().stream()
            .filter(entry -> !categoryMapping.containsKey(entry.getKey()))
            .forEach(entry -> appendCategoryIfExists(javadoc, categorizedExamples, entry.getKey(),
                                                     entry.getKey()));
    }

    /**
     * Appends a category section if examples exist for the given category.
     */
    private void appendCategoryIfExists(StringBuilder javadoc,
                                        Map<String, List<ExampleMetadataProvider.ExampleData>> categorizedExamples,
                                        String category,
                                        String displayName) {
        List<ExampleMetadataProvider.ExampleData> categoryExamples = categorizedExamples.get(category);
        if (!CollectionUtils.isNullOrEmpty(categoryExamples)) {
            appendCategorySection(javadoc, displayName, categoryExamples);
        }
    }

    private void appendCategorySection(StringBuilder javadoc, String displayName,
                                       List<ExampleMetadataProvider.ExampleData> categoryExamples) {
        javadoc.append("<h3>").append(displayName).append("</h3>").append(System.lineSeparator());
        javadoc.append("<ul>").append(System.lineSeparator());

        for (ExampleMetadataProvider.ExampleData example : categoryExamples) {
            javadoc.append("<li><a href=\"").append(example.getUrl()).append("\" target=\"_top\">")
                   .append(example.getTitle()).append("</a></li>").append(System.lineSeparator());
        }
        javadoc.append("</ul>").append(System.lineSeparator());
    }

}
