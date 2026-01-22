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

package software.amazon.awssdk.codegen.smithy;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.internal.TypeUtils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.Paginators;
import software.amazon.awssdk.codegen.model.service.Waiters;
import software.amazon.awssdk.codegen.naming.DefaultSmithyNamingStrategy;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Builds an intermediate model to be used by the templates from the service model and
 * customizations.
 */
public class SmithyIntermediateModelBuilder {
    private static final Logger log = LoggerFactory.getLogger(SmithyIntermediateModelBuilder.class);

    private final Model smithyModel;
    private final ServiceShape service;
    private final CustomizationConfig customizationConfig;
    private final NamingStrategy namingStrategy;
    private final TypeUtils typeUtils;
    private final ServiceIndex serviceIndex;

    public SmithyIntermediateModelBuilder(SmithyModelWithCustomizations modelWithCustomizations) {
        this.smithyModel = modelWithCustomizations.getSmithyModel();
        this.service = getServiceShape(this.smithyModel);
        this.customizationConfig = modelWithCustomizations.getCustomizationConfig();
        this.namingStrategy = new DefaultSmithyNamingStrategy(smithyModel, service, customizationConfig);
        this.typeUtils = new TypeUtils(namingStrategy);
        this.serviceIndex = ServiceIndex.of(smithyModel);
    }

    private ServiceShape getServiceShape(Model model) {
        if (model.getServiceShapes().size() != 1) {
            throw new IllegalArgumentException("Smithy model must have exactly one service shape");
        }
        return model.getServiceShapes().iterator().next();
    }


    public IntermediateModel build() {
        // TODO: crete  a DefaultSmithyCustomizationProcessor and preorpocess the model here

        Paginators paginators = translatePaginators();
        Waiters waiters = translateWaiters();

        Map<String, ShapeModel> shapes = new HashMap<>();

        Map<String, OperationModel> operations = new TreeMap<>(new AddOperations(this, paginators).constructOperations());
        return null;
    }

    public Model getSmithyModel() {
        return smithyModel;
    }

    public ServiceShape getService() {
        return this.service;
    }

    public ServiceIndex getServiceIndex() {
        return this.serviceIndex;
    }

    public CustomizationConfig getCustomizationConfig() {
        return customizationConfig;
    }

    public NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    public TypeUtils getTypeUtils() {
        return typeUtils;
    }

    private static Waiters translateWaiters() {
        // TODO - transform these!
        return Waiters.none();
    }

    private static Paginators translatePaginators() {
        // TODO - transform these!'
        return Paginators.none();
    }
}
