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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import software.amazon.awssdk.utils.Validate;

/**
 * A command line application to add new services to the shared pom.xml files.
 *
 * Example usage:
 * <pre>
 * mvn exec:java -pl :release-scripts \
 *     -Dexec.mainClass="software.amazon.awssdk.release.FinalizeNewServiceModuleMain" \
 *     -Dexec.args="--maven-project-root /path/to/root
 *                  --service-module-names service-module-name-1,service-module-name-2"
 * </pre>
 */
public class FinalizeNewServiceModuleMain extends Cli {
    private FinalizeNewServiceModuleMain() {
        super(requiredOption("service-module-names",
                             "A comma-separated list containing the name of the service modules to be created."),
              requiredOption("maven-project-root", "The root directory for the maven project."));
    }

    public static void main(String[] args) {
        new FinalizeNewServiceModuleMain().run(args);
    }

    @Override
    protected void run(CommandLine commandLine) throws Exception {
        new NewServiceCreator(commandLine).run();
    }

    private static class NewServiceCreator {
        private final Path mavenProjectRoot;
        private final List<String> serviceModuleNames;

        private NewServiceCreator(CommandLine commandLine) {
            this.mavenProjectRoot = Paths.get(commandLine.getOptionValue("maven-project-root").trim());
            this.serviceModuleNames = Stream.of(commandLine.getOptionValue("service-module-names").split(","))
                                            .map(String::trim)
                                            .collect(Collectors.toList());

            Validate.isTrue(Files.exists(mavenProjectRoot), "Project root does not exist: " + mavenProjectRoot);
        }

        public void run() throws Exception {
            for (String serviceModuleName : serviceModuleNames) {
                Path servicesPomPath = mavenProjectRoot.resolve("services").resolve("pom.xml");
                Path aggregatePomPath = mavenProjectRoot.resolve("aws-sdk-java").resolve("pom.xml");
                Path bomPomPath = mavenProjectRoot.resolve("bom").resolve("pom.xml");

                new AddSubmoduleTransformer(serviceModuleName).transform(servicesPomPath);
                new AddDependencyTransformer(serviceModuleName).transform(aggregatePomPath);
                new AddDependencyManagementDependencyTransformer(serviceModuleName).transform(bomPomPath);
            }
        }

        private static class AddSubmoduleTransformer extends PomTransformer {
            private final String serviceModuleName;

            private AddSubmoduleTransformer(String serviceModuleName) {
                this.serviceModuleName = serviceModuleName;
            }

            @Override
            protected void updateDocument(Document doc) {
                Node project = findChild(doc, "project");
                Node modules = findChild(project, "modules");
                addChild(modules, textElement(doc, "module", serviceModuleName));
            }
        }

        private static class AddDependencyTransformer extends PomTransformer {
            private final String serviceModuleName;

            private AddDependencyTransformer(String serviceModuleName) {
                this.serviceModuleName = serviceModuleName;
            }

            @Override
            protected void updateDocument(Document doc) {
                Node project = findChild(doc, "project");
                Node dependencies = findChild(project, "dependencies");
                addChild(dependencies, sdkDependencyElement(doc, serviceModuleName));
            }
        }

        private static class AddDependencyManagementDependencyTransformer extends PomTransformer {
            private final String serviceModuleName;

            private AddDependencyManagementDependencyTransformer(String serviceModuleName) {
                this.serviceModuleName = serviceModuleName;
            }

            @Override
            protected void updateDocument(Document doc) {
                Node project = findChild(doc, "project");
                Node dependencyManagement = findChild(project, "dependencyManagement");
                Node dependencies = findChild(dependencyManagement, "dependencies");
                addChild(dependencies, sdkDependencyElement(doc, serviceModuleName));
            }
        }
    }
}
