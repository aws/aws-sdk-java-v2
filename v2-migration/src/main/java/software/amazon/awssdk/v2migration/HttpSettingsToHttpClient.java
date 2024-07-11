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

package software.amazon.awssdk.v2migration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Pair;
import software.amazon.awssdk.v2migration.internal.utils.IdentifierUtils;
import software.amazon.awssdk.v2migration.internal.utils.SdkTypeUtils;

/**
 * This recipe moves the HTTP settings such as maxConnections configured on v1 ClientConfiguration to v2 ApacheHttpClient builder
 * for sync SDK client or to v2 NettyNioAsyncHttpClient builder for async SDK client.
 * <p>
 * {@snippet :
 *         ClientConfiguration clientConfiguration = new ClientConfiguration()
 *             .withMaxConnections(100)
 *             .withTcpKeepAlive(true);
 *
 *         AmazonSQS sqs = AmazonSQSClient.builder()
 *                                        .withClientConfiguration(clientConfiguration)
 *                                        .build();
 *}
 * <p>
 * to v2:
 * <p>
 * {@snippet :
 *         ClientOverrideConfiguration clientConfiguration = ClientOverrideConfiguration.builder().build();
 *
 *         SqsClient sqs = SqsClient.builder()
 *                                  .overrideConfiguration(clientConfiguration)
 *                                  .httpClientBuilder(ApacheHttpClient.builder()
 *                                                      .maxConnections(100)
 *                                                      .tcpKeepAlive(true))
 *                                  .build();
 *}
 */
@SdkInternalApi
public class HttpSettingsToHttpClient extends Recipe {
    private static final Logger log = Logger.loggerFor(HttpSettingsToHttpClient.class);
    private static final String CONFIG_VAR_TO_HTTP_SETTINGS_KEY = "software.amazon.awssdk.migration.variableReference";
    private static final String CONFIG_METHOD_TO_HTTP_SETTINGS_KEY = "software.amazon.awssdk.migration.configReference";
    private static final String OVERRIDE_CONFIG_BUILDER_HTTP_SETTINGS_CURSOR = "clientOverrideConfigBuilderHttpSettingsCursor";

    private static final Set<String> HTTP_SETTING_METHOD_NAMES = new HashSet<>(Arrays.asList("connectionTimeout",
                                                                                             "connectionTimeToLive",
                                                                                             "connectionMaxIdleTime",
                                                                                             "tcpKeepAlive",
                                                                                             "connectionTtl",
                                                                                             "socketTimeout",
                                                                                             "maxConnections"));


    @Override
    public String getDisplayName() {
        return "Move HTTP settings from the ClientOverrideConfiguration to ApacheHttpClient for sync and "
               + "NettyNioAsyncHttpClient for async";
    }

    @Override
    public String getDescription() {
        return "Move HTTP settings from the ClientOverrideConfiguration to ApacheHttpClient for sync SDK client and "
               + "NettyNioAsyncHttpClient for async SDK client.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MoveHttpSettingsVisitor();
    }

    private static final class MoveHttpSettingsVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable,
                                                                  ExecutionContext executionContext) {
            variable = super.visitVariable(variable, executionContext);

            JavaType type = variable.getType();

            if (!isClientOverrideConfigurationType(type)) {
                return variable;
            }

            Expression initializer = variable.getInitializer();

            if (initializer instanceof J.MethodInvocation) {
                saveHttpSettingsInExecutionContextIfNeeded(executionContext, CONFIG_VAR_TO_HTTP_SETTINGS_KEY,
                                                           variable.getSimpleName());
            }
            return variable;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
            method = super.visitMethodDeclaration(method, executionContext);
            JavaType.Method methodType = method.getMethodType();
            if (methodType == null) {
                return method;
            }

            if (!returnClientOverrideConfiguration(methodType)) {
                return method;
            }

            saveHttpSettingsInExecutionContextIfNeeded(executionContext, CONFIG_METHOD_TO_HTTP_SETTINGS_KEY,
                                                       method.getSimpleName());

            return method;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation originalMethod, ExecutionContext executionContext) {
            J.MethodInvocation method = super.visitMethodInvocation(originalMethod, executionContext);
            if (isClientOverrideConfigurationBuilder(method)) {
                return handleClientOverrideConfiguration(originalMethod, method, executionContext);
            }

            if (isSdkClientBuilder(method)) {
                return configureHttpClientBuilder(originalMethod, method, executionContext);
            }

            return method;
        }


        private void saveHttpSettingsInExecutionContextIfNeeded(ExecutionContext executionContext,
                                                                String configVarToHttpSettingsKey,
                                                                String variable) {
            Map<String, Expression> httpSettings = getCursor()
                .getMessage(OVERRIDE_CONFIG_BUILDER_HTTP_SETTINGS_CURSOR, new HashMap<>());
            Map<String, Map<String, Expression>> variableToHttpSettings =
                executionContext.getMessage(configVarToHttpSettingsKey, new HashMap<>());

            variableToHttpSettings.put(variable, httpSettings);

            executionContext.putMessage(configVarToHttpSettingsKey,
                                        variableToHttpSettings);
        }

        private static boolean isClientOverrideConfigurationType(JavaType type) {
            return Optional.ofNullable(TypeUtils.asFullyQualified(type))
                           .filter(t -> t.isAssignableTo(ClientOverrideConfiguration.class.getCanonicalName()))
                           .isPresent();
        }

        private static boolean isSdkClientBuilder(J.MethodInvocation method) {
            return Optional.ofNullable(method.getMethodType()).map(mt -> mt.getDeclaringType())
                           .filter(t -> SdkTypeUtils.isV2ClientClass(t))
                           .isPresent();
        }

        private static boolean returnClientOverrideConfiguration(JavaType.Method methodType) {
            if (methodType == null) {
                return false;
            }

            return isClientOverrideConfigurationType(methodType.getReturnType());
        }

        private static boolean isClientOverrideConfigurationBuilder(J.MethodInvocation method) {
            return Optional.ofNullable(method.getMethodType()).map(mt -> mt.getDeclaringType())
                           .filter(t -> t.isAssignableTo(ClientOverrideConfiguration.class.getCanonicalName()))
                           .isPresent();
        }

        private J.MethodInvocation configureHttpClientBuilder(
            J.MethodInvocation originalMethod, J.MethodInvocation method, ExecutionContext executionContext) {
            JavaType.Method methodType = method.getMethodType();
            if (methodType == null) {
                return method;
            }

            if (method.getSimpleName().equals("overrideConfiguration")) {
                return addHttpClientBuilderIfNeeded(Pair.of(originalMethod, method), executionContext);
            }

            return method;
        }

        /**
         * This method adds ".httpClientBuilder(ApacheHttpClient.builder().xxx)" right
         * before ".overrideConfiguration(xxx)" if there are HTTP settings stored in execution context
         */
        private J.@NotNull MethodInvocation addHttpClientBuilderIfNeeded(
            Pair<J.MethodInvocation, J.MethodInvocation> methods, ExecutionContext executionContext) {
            J.MethodInvocation method = methods.right();
            Expression expression = method.getArguments().get(0);

            // If a variable is passed to overrideConfiguration, i.e., clientBuilder.overrideConfiguration(config)
            if (expression instanceof J.Identifier) {
                J.Identifier id = (J.Identifier) expression;

                Map<String, Map<String, Expression>> configToHttpSettings =
                    executionContext.getMessage(CONFIG_VAR_TO_HTTP_SETTINGS_KEY);

                return addHttpClientBuilderIfNeeded(methods, configToHttpSettings, id.getSimpleName(), executionContext);
            }

            // If a method invocation is passed to overrideConfiguration, i.e., clientBuilder.overrideConfiguration(getConfig())
            if (expression instanceof J.MethodInvocation) {
                J.MethodInvocation methodInvocation = (J.MethodInvocation) expression;
                String methodInvocationName = methodInvocation.getName().getSimpleName();
                Map<String, Map<String, Expression>> configToHttpSettings =
                    executionContext.getMessage(CONFIG_METHOD_TO_HTTP_SETTINGS_KEY);

                return addHttpClientBuilderIfNeeded(methods, configToHttpSettings, methodInvocationName, executionContext);

            }

            return method;
        }

        private J.MethodInvocation addHttpClientBuilderIfNeeded(
            Pair<J.MethodInvocation, J.MethodInvocation> methods,
            Map<String, Map<String, Expression>> configToHttpSettings,
            String configKey,
            ExecutionContext executionContext) {

            J.MethodInvocation method = methods.right();
            if (CollectionUtils.isNullOrEmpty(configToHttpSettings) ||
                !configToHttpSettings.containsKey(configKey)) {
                return method;
            }

            Map<String, Expression> httpSettings = configToHttpSettings.get(configKey);
            JavaType.FullyQualified classType =
                Optional.ofNullable(method.getMethodType())
                        .map(JavaType.Method::getDeclaringType)
                        .orElse(null);

            if (classType == null) {
                return method;
            }

            JavaType.FullyQualified builderType = SdkTypeUtils.v2Builder(classType);
            Expression expressionBeforeOverrideConfiguration = method.getSelect();

            if (expressionBeforeOverrideConfiguration == null ||
                !(expressionBeforeOverrideConfiguration instanceof J.MethodInvocation ||
                  expressionBeforeOverrideConfiguration instanceof J.Identifier)) {
                return method;
            }

            if (expressionBeforeOverrideConfiguration instanceof J.MethodInvocation) {
                J.MethodInvocation selectInvoke = (J.MethodInvocation) expressionBeforeOverrideConfiguration;

                // If we've already added httpClientBuilder, skip
                if (selectInvoke.getSimpleName().equals("httpClientBuilder")) {
                    return method;
                }
            }

            Space space = (expressionBeforeOverrideConfiguration instanceof J.MethodInvocation) ? Space.format("\n") :
                          expressionBeforeOverrideConfiguration.getPrefix();

            Pair<Class, Class> httpClientClassNamePair = httpClientClassNamePair(classType);

            List<JavaType> parametersTypes = new ArrayList<>();
            parametersTypes.add(JavaType.buildType(httpClientClassNamePair.right().getCanonicalName()));

            J.Identifier httpClientBuilderName =
                IdentifierUtils.makeId("httpClientBuilder", builderType);

            JavaType.Method httpClientBuilderMethod = new JavaType.Method(
                null,
                0L,
                builderType,
                "httpClientBuilder",
                builderType,
                Collections.emptyList(),
                parametersTypes,
                Collections.emptyList(),
                Collections.emptyList()
            );

            J.MethodInvocation httpClientBuilderMethodInvoke = new J.MethodInvocation(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                new JRightPadded(expressionBeforeOverrideConfiguration, space, Markers.EMPTY),
                null,
                httpClientBuilderName,
                httpClientBuilderInvoke(httpSettings, httpClientClassNamePair),
                httpClientBuilderMethod
            );

            method = method.withSelect(httpClientBuilderMethodInvoke);
            maybeAddImport(httpClientClassNamePair.left().getCanonicalName());
            return maybeAutoFormat(methods.left(), method, executionContext);
        }

        private Pair<Class, Class> httpClientClassNamePair(JavaType.FullyQualified classType) {
            Pair<Class, Class> httpClientClassNamePair;
            if (!classType.getClassName().contains("Async")) {
                httpClientClassNamePair = Pair.of(ApacheHttpClient.class,
                                                  ApacheHttpClient.Builder.class);
            } else {
                httpClientClassNamePair = Pair.of(NettyNioAsyncHttpClient.class,
                                                  NettyNioAsyncHttpClient.Builder.class);
            }
            return httpClientClassNamePair;
        }

        private JContainer<Expression> httpClientBuilderInvoke(Map<String, Expression> httpSettings,
                                                               Pair<Class, Class> httpClientClassNamePair) {
            Class httpClientClassName = httpClientClassNamePair.left();
            Class httpClientBuilderClassName = httpClientClassNamePair.right();

            JavaType.FullyQualified httpClientType =
                TypeUtils.asFullyQualified(JavaType.buildType(httpClientClassName.getCanonicalName()));
            JavaType.FullyQualified httpClientBuilderType =
                TypeUtils.asFullyQualified(JavaType.buildType(httpClientBuilderClassName.getCanonicalName()));


            JavaType.Method httpClientBuilder = new JavaType.Method(
                null,
                0L,
                httpClientType,
                "builder",
                httpClientBuilderType,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
            );

            J.Identifier httpClient =
                IdentifierUtils.makeId(httpClientClassName.getSimpleName(), httpClientType);

            J.Identifier httpClientBuilderName =
                IdentifierUtils.makeId("builder", httpClientBuilderType);

            J.MethodInvocation httpClientBuilderMethodInvoke = new J.MethodInvocation(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                JRightPadded.build(httpClient),
                null,
                httpClientBuilderName,
                JContainer.empty(),
                httpClientBuilder
            );

            for (Map.Entry<String, Expression> entry : httpSettings.entrySet()) {
                httpClientBuilderMethodInvoke = invokeHttpSetting(entry, httpClientBuilderType, httpClientBuilderMethodInvoke);
            }

            return JContainer.build(Arrays.asList(JRightPadded.build(httpClientBuilderMethodInvoke)));
        }

        private static J.@NotNull MethodInvocation invokeHttpSetting(Map.Entry<String, Expression> entry,
                                                                     JavaType.FullyQualified httpClientBuilderType,
                                                                     J.MethodInvocation httpClientBuilderMethodInvoke) {

            String settingName = entry.getKey();
            Expression value = entry.getValue();

            J.Identifier settingBuilderName =
                IdentifierUtils.makeId(settingName, httpClientBuilderType);

            List<JavaType> parametersTypes = Collections.singletonList(value.getType());

            JavaType.Method settingMethod = new JavaType.Method(
                null,
                0L,
                httpClientBuilderType,
                settingName,
                httpClientBuilderType,
                Collections.emptyList(),
                parametersTypes,
                Collections.emptyList(),
                Collections.emptyList()
            );

            JContainer argument = JContainer.build(Arrays.asList(JRightPadded.build(value)));

            J.MethodInvocation settingsInvoke = new J.MethodInvocation(
                Tree.randomId(),
                Space.EMPTY,
                Markers.EMPTY,
                new JRightPadded(httpClientBuilderMethodInvoke, Space.format("\n"), Markers.EMPTY),
                null,
                settingBuilderName,
                argument,
                settingMethod
            );

            return settingsInvoke;
        }

        private J.@NotNull MethodInvocation handleClientOverrideConfiguration(
            J.MethodInvocation originalMethod,
            J.MethodInvocation method,
            ExecutionContext executionContext) {

            Expression methodSelectExpression = method.getSelect();
            if (methodSelectExpression == null || !(methodSelectExpression instanceof J.MethodInvocation)) {
                return method;
            }

            // method: ClientOverrideConfiguration.builder().maxConnection(xx).aNonHttpSetting(xx)
            // method.getSelect: ClientOverrideConfiguration.builder().maxConnection(xx)
            J.MethodInvocation selectInvoke = (J.MethodInvocation) methodSelectExpression;

            String selectMethodName = selectInvoke.getSimpleName();

            if (!HTTP_SETTING_METHOD_NAMES.contains(selectMethodName)) {
                return method;
            }

            if (!(selectInvoke.getSelect() instanceof J.MethodInvocation)) {
                return method;
            }

            addHttpSettingsToClientOverrideConfigCursor(selectMethodName, selectInvoke);

            // select.getSelect: ClientOverrideConfiguration.builder()
            J.MethodInvocation selectInvokeSelect = (J.MethodInvocation) selectInvoke.getSelect();

            // new method: ClientOverrideConfiguration.builder().aNonHttpSetting(xx)
            method = method.withSelect(selectInvokeSelect);

            if (method.getSimpleName().equals("build")) {
                method = method.withPrefix(Space.SINGLE_SPACE);
            }


            return maybeAutoFormat(originalMethod, method, executionContext);
        }

        /**
         * Add HTTP settings to clientOverrideConfiguration parent cursor so that it can be popped out later to pass to SDK client
         * builder.
         * <p>
         * The location of parent cursor depends on where the clientOverrideConfiguration is defined. If it is a variable, the
         * parent cursor is the NamedVariable; if it is a method, then the parent cursor is the MethodDeclaration.
         */
        private void addHttpSettingsToClientOverrideConfigCursor(String selectMethodName,
                                                                 J.MethodInvocation selectInvoke) {
            Cursor parentCursor = getCursor();

            try {
                parentCursor = getCursor().dropParentUntil(parent -> isClientOverrideConfigurationNamedVariableType(parent));
            } catch (Exception e) {
                // Ignore if it's not a variable
                log.debug(() -> "Cannot find named variable type", e);

            }

            try {
                parentCursor = getCursor()
                    .dropParentUntil(parent -> (parent instanceof J.MethodDeclaration) &&
                                               returnClientOverrideConfiguration(((J.MethodDeclaration) parent).getMethodType()));
            } catch (Exception e) {
                // Ignore if it's not a method declaration
                log.debug(() -> "Cannot find method declaration type", e);
            }

            Map<String, Expression> httpSettings =
                parentCursor.getMessage(OVERRIDE_CONFIG_BUILDER_HTTP_SETTINGS_CURSOR, new HashMap<>());

            httpSettings.put(selectMethodName, selectInvoke.getArguments().get(0));
            parentCursor.putMessage(OVERRIDE_CONFIG_BUILDER_HTTP_SETTINGS_CURSOR, httpSettings);
        }

        private static boolean isClientOverrideConfigurationNamedVariableType(Object parent) {
            return (parent instanceof J.VariableDeclarations.NamedVariable) &&
                   isClientOverrideConfigurationType(((J.VariableDeclarations.NamedVariable) parent).getType());
        }
    }
}
