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

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.PoetGeneratorTask;
import software.amazon.awssdk.codegen.poet.auth.scheme.AuthSchemeInterceptorSpec;
import software.amazon.awssdk.codegen.poet.auth.scheme.AuthSchemeParamsSpec;
import software.amazon.awssdk.codegen.poet.auth.scheme.AuthSchemeProviderSpec;
import software.amazon.awssdk.codegen.poet.auth.scheme.AuthSchemeSpecUtils;
import software.amazon.awssdk.codegen.poet.auth.scheme.DefaultAuthSchemeParamsSpec;
import software.amazon.awssdk.codegen.poet.auth.scheme.EndpointAwareAuthSchemeParamsSpec;
import software.amazon.awssdk.codegen.poet.auth.scheme.EndpointBasedAuthSchemeProviderSpec;
import software.amazon.awssdk.codegen.poet.auth.scheme.ModelBasedAuthSchemeProviderSpec;

public final class AuthSchemeGeneratorTasks extends BaseGeneratorTasks {
    private final GeneratorTaskParams generatorTaskParams;

    public AuthSchemeGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.generatorTaskParams = dependencies;
    }

    @Override
    protected List<GeneratorTask> createTasks() {
        AuthSchemeSpecUtils authSchemeSpecUtils = new AuthSchemeSpecUtils(model);
        List<GeneratorTask> tasks = new ArrayList<>();
        tasks.add(generateParamsInterface());
        tasks.add(generateProviderInterface());
        tasks.add(generateDefaultParamsImpl());
        tasks.add(generateModelBasedProvider());
        tasks.add(generateAuthSchemeInterceptor());
        if (authSchemeSpecUtils.useEndpointBasedAuthProvider()) {
            tasks.add(generateEndpointBasedProvider());
        }
        if (authSchemeSpecUtils.useEndpointParamsInAuthScheme()) {
            tasks.add(generateEndpointAwareAuthSchemeParams());
        }
        return tasks;
    }

    private GeneratorTask generateParamsInterface() {
        return new PoetGeneratorTask(authSchemeDir(), model.getFileHeader(), new AuthSchemeParamsSpec(model));
    }

    private GeneratorTask generateDefaultParamsImpl() {
        return new PoetGeneratorTask(authSchemeInternalDir(), model.getFileHeader(), new DefaultAuthSchemeParamsSpec(model));
    }

    private GeneratorTask generateProviderInterface() {
        return new PoetGeneratorTask(authSchemeDir(), model.getFileHeader(), new AuthSchemeProviderSpec(model));
    }

    private GeneratorTask generateModelBasedProvider() {
        return new PoetGeneratorTask(authSchemeInternalDir(), model.getFileHeader(), new ModelBasedAuthSchemeProviderSpec(model));
    }

    private GeneratorTask generateEndpointBasedProvider() {
        return new PoetGeneratorTask(authSchemeInternalDir(), model.getFileHeader(),
                                     new EndpointBasedAuthSchemeProviderSpec(model));
    }

    private GeneratorTask generateEndpointAwareAuthSchemeParams() {
        return new PoetGeneratorTask(authSchemeDir(), model.getFileHeader(), new EndpointAwareAuthSchemeParamsSpec(model));

    }

    private GeneratorTask generateAuthSchemeInterceptor() {
        return new PoetGeneratorTask(authSchemeInternalDir(), model.getFileHeader(), new AuthSchemeInterceptorSpec(model));
    }

    private String authSchemeDir() {
        return generatorTaskParams.getPathProvider().getAuthSchemeDirectory();
    }

    private String authSchemeInternalDir() {
        return generatorTaskParams.getPathProvider().getAuthSchemeInternalDirectory();
    }
}
