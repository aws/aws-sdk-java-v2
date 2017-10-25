/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.model.config.templates;

import static software.amazon.awssdk.codegen.internal.Constants.PROTOCOL_CONFIG_LOCATION;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import software.amazon.awssdk.codegen.internal.ClassLoaderHelper;
import software.amazon.awssdk.codegen.internal.Jackson;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;

public class CodeGenTemplatesConfig {


    private TopLevelTemplate syncClientBuilder = new TopLevelTemplate("/templates/common/SyncClientBuilder.ftl", null);
    private TopLevelTemplate asyncClientBuilder = new TopLevelTemplate("/templates/common/AsyncClientBuilder.ftl", null);
    private TopLevelTemplate modelUnmarshaller;
    private TopLevelTemplate modelMarshaller;
    private TopLevelTemplate requestMarshaller;
    private TopLevelTemplate baseExceptionClass;
    private TopLevelTemplate exceptionUnmarshaller;
    private TopLevelTemplate policyActionClass;
    private TopLevelTemplate packageInfo;
    private TopLevelTemplate customRequestSignerClass;
    private TopLevelTemplate cucumberModuleInjector = new TopLevelTemplate("/templates/cucumber/ModuleInjector.ftl", null);
    private TopLevelTemplate cucumberTest = new TopLevelTemplate("/templates/cucumber/RunCucumberTest.ftl", null);
    private TopLevelTemplate cucumberPropertiesFile = new TopLevelTemplate("/templates/cucumber/cucumberProperties.ftl", null);
    private TopLevelTemplate apiGatewayPomTemplate = new TopLevelTemplate("/templates/api-gateway/maven/pom.xml.ftl", null);
    private TopLevelTemplate apiGatewayGradleBuildTemplate =
            new TopLevelTemplate("/templates/api-gateway/gradle/build.gradle.ftl", null);
    private TopLevelTemplate apiGatewayGradleSettingsTemplate =
            new TopLevelTemplate("/templates/api-gateway/gradle/settings.gradle.ftl", null);
    private TopLevelTemplate apiGatewayReadmeTemplate =
            new TopLevelTemplate("/templates/api-gateway/README.md.ftl", Collections.singletonList(
                    new ChildTemplate("/templates/api-gateway/README_Dependencies.ftl", "README_Dependencies")));

    private List<ChildTemplate> commonChildTemplates;

    public static CodeGenTemplatesConfig load(Protocol protocol) {

        final String protocolConfigFilePath = String.format(
                PROTOCOL_CONFIG_LOCATION, protocol.getValue());

        InputStream input = ClassLoaderHelper.getResourceAsStream(
                protocolConfigFilePath, CodeGenTemplatesConfig.class);
        if (input == null) {
            input = ClassLoaderHelper.getResourceAsStream("/"
                                                          + protocolConfigFilePath, CodeGenTemplatesConfig.class);
        }

        try {
            return Jackson.load(CodeGenTemplatesConfig.class,
                                input);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to load the protocol specific config file from the location "
                    + protocolConfigFilePath, e);
        }
    }

    public static CodeGenTemplatesConfig merge(CodeGenTemplatesConfig config,
                                               CodeGenTemplatesConfig override) {

        CodeGenTemplatesConfig merged = new CodeGenTemplatesConfig();

        merged.setModelUnmarshaller(TopLevelTemplate.merge(
                config.getModelUnmarshaller(), override.getModelUnmarshaller()));
        merged.setModelMarshaller(TopLevelTemplate.merge(
                config.getModelMarshaller(), override.getModelMarshaller()));
        merged.setRequestMarshaller(TopLevelTemplate.merge(
                config.getRequestMarshaller(), override.getRequestMarshaller()));

        merged.setBaseExceptionClass(TopLevelTemplate.merge(
                config.getBaseExceptionClass(), override.getBaseExceptionClass()));
        merged.setExceptionUnmarshaller(TopLevelTemplate.merge(
                config.getExceptionUnmarshaller(),
                override.getExceptionUnmarshaller()));

        merged.setPolicyActionClass(TopLevelTemplate.merge(
                config.getPolicyActionClass(),
                override.getPolicyActionClass()));

        merged.setPackageInfo(TopLevelTemplate.merge(
                config.getPackageInfo(), override.getPackageInfo()));

        merged.setCustomRequestSignerClass(TopLevelTemplate.merge(
                config.getCustomRequestSignerClass(), override.getCustomRequestSignerClass()));

        List<ChildTemplate> commonChildTemplates = new LinkedList<ChildTemplate>();
        if (config.getCommonChildTemplates() != null) {
            commonChildTemplates.addAll(config.getCommonChildTemplates());
        }
        if (override.getCommonChildTemplates() != null) {
            commonChildTemplates.addAll(override.getCommonChildTemplates());
        }
        merged.setCommonChildTemplates(commonChildTemplates);


        return merged;
    }

    public TopLevelTemplate getSyncClientBuilder() {
        return syncClientBuilder;
    }

    public void setSyncClientBuilder(TopLevelTemplate syncClientBuilder) {
        this.syncClientBuilder = syncClientBuilder;
    }

    public TopLevelTemplate getAsyncClientBuilder() {
        return asyncClientBuilder;
    }

    public void setAsyncClientBuilder(TopLevelTemplate syncClientBuilder) {
        this.asyncClientBuilder = syncClientBuilder;
    }

    public TopLevelTemplate getModelUnmarshaller() {
        return modelUnmarshaller;
    }

    public void setModelUnmarshaller(TopLevelTemplate modelUnmarshaller) {
        this.modelUnmarshaller = modelUnmarshaller;
    }

    public TopLevelTemplate getModelMarshaller() {
        return modelMarshaller;
    }

    public TopLevelTemplate getRequestMarshaller() {
        return requestMarshaller;
    }

    public void setModelMarshaller(TopLevelTemplate modelMarshaller) {
        this.modelMarshaller = modelMarshaller;
    }

    public void setRequestMarshaller(TopLevelTemplate requestMarshaller) {
        this.requestMarshaller = requestMarshaller;
    }

    public TopLevelTemplate getExceptionUnmarshaller() {
        return exceptionUnmarshaller;
    }

    public void setExceptionUnmarshaller(
            TopLevelTemplate exceptionUnmarshaller) {
        this.exceptionUnmarshaller = exceptionUnmarshaller;
    }

    public List<ChildTemplate> getCommonChildTemplates() {
        return commonChildTemplates;
    }

    public void setCommonChildTemplates(
            List<ChildTemplate> commonChildTemplates) {
        this.commonChildTemplates = commonChildTemplates;
    }

    public TopLevelTemplate getPolicyActionClass() {
        return policyActionClass;
    }

    public void setPolicyActionClass(TopLevelTemplate policyActionClass) {
        this.policyActionClass = policyActionClass;
    }

    public TopLevelTemplate getPackageInfo() {
        return packageInfo;
    }

    public void setPackageInfo(TopLevelTemplate packageInfo) {
        this.packageInfo = packageInfo;
    }

    public TopLevelTemplate getBaseExceptionClass() {
        return baseExceptionClass;
    }

    public void setBaseExceptionClass(TopLevelTemplate baseExceptionClass) {
        this.baseExceptionClass = baseExceptionClass;
    }

    public TopLevelTemplate getCucumberModuleInjector() {
        return cucumberModuleInjector;
    }

    public TopLevelTemplate getCucumberTest() {
        return cucumberTest;
    }

    public TopLevelTemplate getCucumberPropertiesFile() {
        return cucumberPropertiesFile;
    }

    public TopLevelTemplate getCustomRequestSignerClass() {
        return customRequestSignerClass;
    }

    public void setCustomRequestSignerClass(TopLevelTemplate customRequestSignerClass) {
        this.customRequestSignerClass = customRequestSignerClass;
    }

    public TopLevelTemplate getApiGatewayPomTemplate() {
        return apiGatewayPomTemplate;
    }

    public TopLevelTemplate getApiGatewayGradleBuildTemplate() {
        return apiGatewayGradleBuildTemplate;
    }

    public TopLevelTemplate getApiGatewayGradleSettingsTemplate() {
        return apiGatewayGradleSettingsTemplate;
    }

    public TopLevelTemplate getApiGatewayReadmeTemplate() {
        return apiGatewayReadmeTemplate;
    }
}
