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

package software.amazon.awssdk.benchmark.endpointsbdd;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.tools.ToolProvider;
import software.amazon.smithy.java.endpoints.EndpointResolver;
import software.amazon.smithy.java.rulesengine.Bytecode;
import software.amazon.smithy.java.rulesengine.JavaEndpointResolverGenerator;

/**
 * Compiles a {@link software.amazon.smithy.java.rulesengine.GeneratedEndpointResolver} at
 * benchmark setup time from serialized BDD bytecode.
 *
 * <p>The generator produces a Java source file, compiles it with the system Java compiler using
 * the current classpath, then loads the resulting class via a transient {@link URLClassLoader}.
 * The JDK (not just the JRE) is required for this to work.
 *
 * <p>Adapted from the smithy-java reference benchmark {@code GeneratedResolverFactory}.
 */
final class GeneratedResolverFactory {

    private static final String PACKAGE = "software.amazon.awssdk.benchmark.endpointsbdd.generated";

    private GeneratedResolverFactory() {
    }

    static EndpointResolver create(Bytecode bytecode, String className) {
        try {
            Path outputDir = Files.createTempDirectory("sdk-generated-endpoint-");
            Path sourceDir = outputDir.resolve(PACKAGE.replace('.', '/'));
            Files.createDirectories(sourceDir);
            Path sourceFile = sourceDir.resolve(className + ".java");

            Files.writeString(sourceFile,
                              new JavaEndpointResolverGenerator(bytecode).generate(PACKAGE, className));

            // Write the BDD binary resource file that the generated resolver loads at construction time
            Path resourceFile = sourceDir.resolve(className + ".bdd");
            Files.write(resourceFile, bytecode.getBytecode());

            var compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) {
                throw new IllegalStateException(
                        "A JDK (not just a JRE) is required to compile generated endpoint resolvers. "
                        + "Run benchmarks with a JDK.");
            }

            var diagnostics = new StringBuilder();
            int exitCode = compiler.run(
                    null,
                    null,
                    new OutputStream() {
                        @Override
                        public void write(int b) {
                            diagnostics.append((char) b);
                        }
                    },
                    "-proc:none",
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", outputDir.toString(),
                    sourceFile.toString());

            if (exitCode != 0) {
                throw new IllegalStateException(
                        "Generated resolver compilation failed for " + className + ":\n" + diagnostics);
            }

            var loader = new URLClassLoader(
                    new java.net.URL[]{outputDir.toUri().toURL()},
                    GeneratedResolverFactory.class.getClassLoader());

            Class<?> resolverClass = Class.forName(PACKAGE + "." + className, true, loader);
            return (EndpointResolver) resolverClass.getConstructor().newInstance();

        } catch (ReflectiveOperationException | IOException e) {
            throw new IllegalStateException("Unable to create generated endpoint resolver: " + className, e);
        }
    }
}
