/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.internal;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import java.io.IOException;
import java.util.List;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.config.templates.ChildTemplate;
import software.amazon.awssdk.codegen.model.config.templates.CodeGenTemplatesConfig;
import software.amazon.awssdk.codegen.model.config.templates.TopLevelTemplate;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;

/**
 * Util class that sets up the freemarker configuration and loads the templates.
 */
public class Freemarker {

    private final CodeGenTemplatesConfig templateConfig;

    private Freemarker(CodeGenTemplatesConfig templateConfig) {
        this.templateConfig = templateConfig;
    }

    public static Freemarker create(IntermediateModel model) {
        return new Freemarker(loadProtocolTemplatesConfig(model));
    }

    private static CodeGenTemplatesConfig loadProtocolTemplatesConfig(IntermediateModel model) {
        // CBOR is a type of JSON Protocol.  Use JSON Protocol for templates
        Protocol templateProtocol = model.getMetadata().getProtocol();
        if (Protocol.CBOR.equals(model.getMetadata().getProtocol()) ||
            Protocol.ION.equals(model.getMetadata().getProtocol())) {
            templateProtocol = Protocol.AWS_JSON;
        }
        CodeGenTemplatesConfig protocolDefaultConfig = CodeGenTemplatesConfig.load(templateProtocol);

        CustomizationConfig customConfig = model.getCustomizationConfig();

        if (customConfig == null || customConfig.getCustomCodeTemplates() == null) {
            return protocolDefaultConfig;
        }

        // merge any custom config and return the result.
        return CodeGenTemplatesConfig.merge(protocolDefaultConfig, customConfig.getCustomCodeTemplates());
    }

    private static void importChildTemplates(
            Configuration freeMarkerConfig,
            List<ChildTemplate> childTemplates) {

        if (childTemplates == null) {
            return;
        }

        for (ChildTemplate template : childTemplates) {
            freeMarkerConfig.addAutoImport(template.getImportAsNamespace(),
                                           template.getLocation());
        }
    }

    private Configuration newFreeMarkerConfig() {
        Configuration freeMarkerConfig = new Configuration(Configuration.VERSION_2_3_24);
        freeMarkerConfig.setDefaultEncoding("UTF-8");
        freeMarkerConfig.setClassForTemplateLoading(this.getClass(), "/");
        freeMarkerConfig
                .setTemplateExceptionHandler(TemplateExceptionHandler.DEBUG_HANDLER);

        return freeMarkerConfig;
    }

    private Template getTemplate(TopLevelTemplate template) throws IOException {

        // Create a new FreeMarker config for each top-level template, so that
        // they don't share the same macro namespace
        Configuration fmConfig = newFreeMarkerConfig();

        // Common child templates
        importChildTemplates(fmConfig, templateConfig.getCommonChildTemplates());
        // Child templates declared for the top-level template
        importChildTemplates(fmConfig, template.getChildTemplates());

        return fmConfig.getTemplate(template.getMainTemplate());
    }

    public Template getModelMarshallerTemplate() throws IOException {
        return getTemplate(templateConfig.getModelMarshaller());
    }

    public Template getModelUnmarshallerTemplate() throws IOException {
        return getTemplate(templateConfig.getModelUnmarshaller());
    }

    public Template getExceptionUnmarshallerTemplate() throws IOException {
        return getTemplate(templateConfig.getExceptionUnmarshaller());
    }

    public Template getPolicyActionClassTemplate() throws IOException {
        return getTemplate(templateConfig.getPolicyActionClass());
    }

    public Template getCucumberModuleInjectorTemplate() throws IOException {
        return getTemplate(templateConfig.getCucumberModuleInjector());
    }

    public Template getCucumberTestTemplate() throws IOException {
        return getTemplate(templateConfig.getCucumberTest());
    }

    public Template getCucumberPropertiesTemplate() throws IOException {
        return getTemplate(templateConfig.getCucumberPropertiesFile());
    }

    public Template getApiGatewayPomTemplate() throws IOException {
        return getTemplate(templateConfig.getApiGatewayPomTemplate());
    }

    public Template getApiGatewayGradleBuildTemplate() throws IOException {
        return getTemplate(templateConfig.getApiGatewayGradleBuildTemplate());
    }

    public Template getApiGatewayGradleSettingsTemplate() throws IOException {
        return getTemplate(templateConfig.getApiGatewayGradleSettingsTemplate());
    }

    public Template getApiGatewayReadmeTemplate() throws IOException {
        return getTemplate(templateConfig.getApiGatewayReadmeTemplate());
    }

    public Template getPackageInfoTemplate() throws IOException {
        return getTemplate(templateConfig.getPackageInfo());
    }

    public Template getCustomAuthorizerTemplate() throws IOException {
        return getTemplate(templateConfig.getCustomRequestSignerClass());
    }
}
