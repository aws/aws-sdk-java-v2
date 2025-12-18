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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.SimpleGeneratorTask;
import software.amazon.awssdk.codegen.internal.DocumentationUtils;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;

/**
 * Emits the package-info.java for the base service package. Includes the service
 * level documentation.
 */
public final class PackageInfoGeneratorTasks extends BaseGeneratorTasks {

    private final String baseDirectory;

    PackageInfoGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.baseDirectory = dependencies.getPathProvider().getClientDirectory();
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        Metadata metadata = model.getMetadata();

        String baseDocumentation = metadata.getDocumentation();

        String codeExamples = getCodeExamples(metadata);
        
        String packageInfoContents =
            String.format("/**%n"
                          + " * %s%n"
                          + (codeExamples.isEmpty() ? "" : " *%n * %s%n")
                          + "*/%n"
                          + "package %s;",
                          baseDocumentation,
                          codeExamples,
                          metadata.getFullClientPackageName());
        return Collections.singletonList(new SimpleGeneratorTask(baseDirectory,
                                                                 "package-info.java",
                                                                 model.getFileHeader(),
                                                                 () -> packageInfoContents));
    }

    String getCodeExamples(Metadata metadata) {
        String exampleMetaPath = "software/amazon/awssdk/codegen/example-meta.json";
        List<DocumentationUtils.ExampleData> examples = 
            DocumentationUtils.getServiceCodeExamples(metadata, exampleMetaPath);
        
        if (examples.isEmpty()) {
            return "";
        }
        
        String codeExamplesJavadoc = generateCodeExamplesJavadoc(examples);

        StringBuilder result = new StringBuilder();
        String[] lines = codeExamplesJavadoc.split("\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                result.append(line);
                if (!line.equals(lines[lines.length - 1])) {
                    result.append(System.lineSeparator()).append(" * ");
                }
            }
        }
        
        return result.toString();
    }
    

    private String generateCodeExamplesJavadoc(List<DocumentationUtils.ExampleData> examples) {
        Map<String, List<DocumentationUtils.ExampleData>> categorizedExamples = new java.util.LinkedHashMap<>();
        for (DocumentationUtils.ExampleData example : examples) {
            categorizedExamples.computeIfAbsent(example.getCategory(), k -> new java.util.ArrayList<>()).add(example);
        }
        
        StringBuilder javadoc = new StringBuilder();
        javadoc.append("<h3>Code Examples</h3>").append("\n");
        javadoc.append("<p>The following code examples show how to use this service with the AWS SDK for Java v2:</p>")
               .append("\n");
        
        Map<String, String> categoryMapping = new java.util.LinkedHashMap<>();
        categoryMapping.put("Hello", "Getting Started");
        categoryMapping.put("Api", "API Actions");
        categoryMapping.put("Basics", "Basics");
        categoryMapping.put("Scenarios", "Scenarios");
        categoryMapping.put("Serverless examples", "Serverless Examples");
        
        for (Map.Entry<String, String> entry : categoryMapping.entrySet()) {
            String category = entry.getKey();
            String displayName = entry.getValue();
            List<DocumentationUtils.ExampleData> categoryExamples = categorizedExamples.get(category);
            if (categoryExamples != null && !categoryExamples.isEmpty()) {
                appendCategorySection(javadoc, displayName, categoryExamples);
            }
        }

        for (Map.Entry<String, List<DocumentationUtils.ExampleData>> entry : categorizedExamples.entrySet()) {
            String category = entry.getKey();
            if (!categoryMapping.containsKey(category)) {
                List<DocumentationUtils.ExampleData> categoryExamples = entry.getValue();
                if (!categoryExamples.isEmpty()) {
                    appendCategorySection(javadoc, category, categoryExamples);
                }
            }
        }
        
        return javadoc.toString();
    }

    private void appendCategorySection(StringBuilder javadoc, String displayName,
                                       List<DocumentationUtils.ExampleData> categoryExamples) {
        javadoc.append("<h4>").append(displayName).append("</h4>").append("\n");
        javadoc.append("<ul>").append("\n");
        
        for (DocumentationUtils.ExampleData example : categoryExamples) {
            javadoc.append("<li><a href=\"").append(example.getUrl()).append("\" target=\"_top\">")
                   .append(example.getTitle()).append("</a></li>").append("\n");
        }
        javadoc.append("</ul>").append("\n");
    }

}
