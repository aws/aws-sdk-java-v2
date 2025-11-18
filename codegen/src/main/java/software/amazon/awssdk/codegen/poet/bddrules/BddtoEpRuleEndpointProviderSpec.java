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

package software.amazon.awssdk.codegen.poet.bddrules;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ConditionModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.RuleModel;
import software.amazon.awssdk.codegen.model.service.EndpointBddModel;
import software.amazon.awssdk.codegen.model.service.EndpointRuleSetModel;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.rules2.EndpointProviderSpec2;

/**
 * This class translate a BDD to a standard endpoint ruleset and then renders it that way.
 */
public class BddtoEpRuleEndpointProviderSpec implements ClassSpec {
    private final IntermediateModel intermediateModel;
    private final EndpointProviderSpec2 endpointProviderSpec;

    public BddtoEpRuleEndpointProviderSpec(IntermediateModel intermediateModel) {
        this.intermediateModel = intermediateModel;
        EndpointRuleSetModel translatedRules = translateBddToRules(intermediateModel.getEndpointBddModel());
        IntermediateModel imWithTranslatedRules = new IntermediateModel(
            intermediateModel.getMetadata(),
            intermediateModel.getOperations(),
            intermediateModel.getShapes(),
            intermediateModel.getCustomizationConfig(),
            intermediateModel.getEndpointOperation().orElseGet(() -> null),
            intermediateModel.getPaginators(),
            intermediateModel.getNamingStrategy(),
            intermediateModel.getWaiters(),
            translatedRules,
            intermediateModel.getEndpointTestSuiteModel(),
            null,
            intermediateModel.getClientContextParams()
        );

        this.endpointProviderSpec = new EndpointProviderSpec2(imWithTranslatedRules);

    }

    private EndpointRuleSetModel translateBddToRules(EndpointBddModel endpointBddModel) {
        List<EndpointBddModel.BddNode> bddNodes = endpointBddModel.getDecodedNodes();
        int rootRef = endpointBddModel.getRoot();
        boolean inverted = rootRef < 0;
        int rootI = Math.abs(rootRef) - 1;
        RuleModel rootTree = nodeToRuleModel(rootI, bddNodes, endpointBddModel, inverted);

        EndpointRuleSetModel endpointRuleSetModel = new EndpointRuleSetModel();
        endpointRuleSetModel.setRules(rootTree.getRules()); // the root is always a tree without conditions
        endpointRuleSetModel.setServiceId(intermediateModel.getEndpointRuleSetModel().getServiceId());
        endpointRuleSetModel.setVersion(intermediateModel.getEndpointRuleSetModel().getVersion());
        endpointRuleSetModel.setParameters(intermediateModel.getEndpointRuleSetModel().getParameters());
        return endpointRuleSetModel;
    }

    private RuleModel nodeToRuleModel(
        int nodeI, List<EndpointBddModel.BddNode> bddNodes, EndpointBddModel bddModel, boolean inverted) {
        RuleModel ruleModel = new RuleModel();
        // a Node is ALWAYS a tree rule.
        ruleModel.setConditions(Collections.emptyList());
        ruleModel.setType("tree");
        List<RuleModel> rules = new ArrayList<>();
        int trueRef;
        int falseRef;
        EndpointBddModel.BddNode bddNode = bddNodes.get(nodeI);
        if (!inverted) {
            trueRef = bddNode.getHighRef();
            falseRef = bddNode.getLowRef();
        } else {
            trueRef = bddNode.getLowRef();
            falseRef = bddNode.getHighRef();
        }

        RuleModel trueRuleModel = new RuleModel();
        ConditionModel condition = bddModel.getConditions().get(bddNode.getConditionIndex());
        trueRuleModel.setConditions(Collections.singletonList(condition));
        edgeToRuleModel(trueRuleModel, trueRef, bddNodes, bddModel);

        RuleModel falseRuleModel = new RuleModel();
        falseRuleModel.setConditions(Collections.emptyList());
        edgeToRuleModel(falseRuleModel, falseRef, bddNodes, bddModel);

        rules.add(trueRuleModel);

        // if the false (non matching) branch is a tree, we can collapse the graph
        if ("tree".equals(falseRuleModel.getType())) {
            rules.addAll(falseRuleModel.getRules());
        } else {
            rules.add(falseRuleModel);
        }
        ruleModel.setRules(rules);
        return ruleModel;
    }

    // modify ruleModel, applying the ref to it
    private void edgeToRuleModel(
        RuleModel ruleModel, int ref, List<EndpointBddModel.BddNode> bddNodes, EndpointBddModel bddModel) {
        if (ref == 1 || ref == -1) {
            // BDD Terminal node - no match
            ruleModel.setType("error");
            ruleModel.setError("No Match Found"); // TODO: use consistent error message
        } else if (ref > 100000000) {
            // Result node
            RuleModel result = bddModel.getResults().get(ref - 100000001);
            ruleModel.setType(result.getType());
            ruleModel.setEndpoint(result.getEndpoint());
            ruleModel.setError(result.getError());
        } else {
            // connected to another node
            boolean inverted = ref < 0; // TODO: Switch me back to < 0
            int nodeI = Math.abs(ref) - 1;
            ruleModel.setType("tree");
            ruleModel.setRules(Collections.singletonList(
                nodeToRuleModel(nodeI, bddNodes, bddModel, inverted)
            ));
        }
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec spec =  endpointProviderSpec.poetSpec();
        return spec.toBuilder().addJavadoc("BDD").build();
    }

    @Override
    public ClassName className() {
        return endpointProviderSpec.className();
    }
}
