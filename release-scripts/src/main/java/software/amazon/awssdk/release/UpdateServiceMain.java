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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * A command line application to update an existing service.
 *
 * Example usage:
 * <pre>
  mvn exec:java -pl :release-scripts \
      -Dexec.mainClass="software.amazon.awssdk.release.UpdateServiceMain" \
      -Dexec.args="--maven-project-root /path/to/root
                   --service-module-name service-module-name
                   --service-json /path/to/service-2.json
                   [--paginators-json /path/to/paginators-1.json
                    --waiters-json /path/to/waiters-2.json]"
 * </pre>
 */
public class UpdateServiceMain extends Cli {
    private static final Logger log = Logger.loggerFor(UpdateServiceMain.class);

    private UpdateServiceMain() {
        super(requiredOption("service-module-name", "The name of the service module to be created."),
              requiredOption("service-id", "The service ID of the service to be updated."),
              requiredOption("maven-project-root", "The root directory for the maven project."),
              requiredOption("service-json", "The service-2.json file for the service."),
              optionalOption("paginators-json", "The paginators-1.json file for the service."),
              optionalOption("waiters-json", "The waiters-2.json file for the service."));
    }

    public static void main(String[] args) {
        new UpdateServiceMain().run(args);
    }

    @Override
    protected void run(CommandLine commandLine) throws Exception {
        new ServiceUpdater(commandLine).run();
    }

    private static class ServiceUpdater {
        private final String serviceModuleName;
        private final String serviceId;
        private final Path mavenProjectRoot;
        private final Path serviceJson;
        private final Path paginatorsJson;
        private final Path waitersJson;

        private ServiceUpdater(CommandLine commandLine) {
            this.mavenProjectRoot = Paths.get(commandLine.getOptionValue("maven-project-root").trim());
            this.serviceId = commandLine.getOptionValue("service-id").trim();
            this.serviceModuleName = commandLine.getOptionValue("service-module-name").trim();
            this.serviceJson = Paths.get(commandLine.getOptionValue("service-json").trim());
            this.paginatorsJson = optionalPath(commandLine.getOptionValue("paginators-json"));
            this.waitersJson = optionalPath(commandLine.getOptionValue("waiters-json"));
        }

        private Path optionalPath(String path) {
            path = StringUtils.trimToNull(path);
            if (path != null) {
                return Paths.get(path);
            }
            return null;
        }

        public void run() throws Exception {
            Validate.isTrue(Files.isRegularFile(serviceJson), serviceJson + " is not a file.");

            Path codegenFileLocation = codegenFileLocation(serviceModuleName, serviceId);

            copyFile(serviceJson, codegenFileLocation.resolve("service-2.json"));
            copyFile(paginatorsJson, codegenFileLocation.resolve("paginators-1.json"));
            copyFile(waitersJson, codegenFileLocation.resolve("waiters-2.json"));
        }

        private Path codegenFileLocation(String serviceModuleName, String serviceId) {

            Path codegenPath = mavenProjectRoot.resolve("services")
                                               .resolve(serviceModuleName)
                                               .resolve("src")
                                               .resolve("main")
                                               .resolve("resources")
                                               .resolve("codegen-resources");

            switch (serviceId) {
                case "WAF Regional":
                    return codegenPath.resolve("wafregional");
                case "WAF":
                    return codegenPath.resolve("waf");
                case "DynamoDB Streams":
                    return codegenPath.resolve("dynamodbstreams");
                case "DynamoDB":
                    return codegenPath.resolve("dynamodb");
                default:
                    return codegenPath;
            }
        }

        private void copyFile(Path source, Path destination) throws IOException {
            if (source != null && Files.isRegularFile(source)) {
                log.info(() -> "Copying " + source + " to " + destination);
                FileUtils.copyFile(source.toFile(), destination.toFile());
            }
        }
    }
}
