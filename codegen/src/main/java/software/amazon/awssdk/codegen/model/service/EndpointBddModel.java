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

package software.amazon.awssdk.codegen.model.service;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codegen.model.rules.endpoints.ConditionModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.RuleModel;

public class EndpointBddModel {
    private String serviceId;
    private String version;
    private Map<String, ParameterModel> parameters;
    private List<ConditionModel> conditions;
    private List<RuleModel> results;
    private int root;
    private int nodeCount;
    private String nodes; // Base64-encoded binary representation of BDD nodes.

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, ParameterModel> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, ParameterModel> parameters) {
        this.parameters = parameters;
    }

    public List<ConditionModel> getConditions() {
        return conditions;
    }

    public void setConditions(List<ConditionModel> conditions) {
        this.conditions = conditions;
    }

    public List<RuleModel> getResults() {
        return results;
    }

    public void setResults(List<RuleModel> results) {
        this.results = results;
    }

    public String getNodes() {
        return nodes;
    }

    public int getRoot() {
        return root;
    }

    public void setRoot(int root) {
        this.root = root;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    public void setNodes(String nodes) {
        this.nodes = nodes;
    }

    public List<Integer> getCompactDecodedNodes() {
        List<Integer> out = new ArrayList<>(nodeCount*3);
        byte[] data = Base64.getDecoder().decode(nodes);


        ByteBuffer buf = ByteBuffer.wrap(data); // big-endian by default

        while (buf.remaining() >= 12) {
            out.add(buf.getInt()); //conditionIndex
            out.add(buf.getInt()); //highRef
            out.add(buf.getInt()); //lowRef
        }

        return out;
    }

    /**
     *
     * @return true if any of nodes are complemented (negative)
     */
    public boolean hasComplementedNodes() {
        return getCompactDecodedNodes().stream().anyMatch(n -> n < 0);
    }

    public List<BddNode> getDecodedNodes() {
        List<BddNode> out = new ArrayList<>(nodeCount);
        byte[] data = Base64.getDecoder().decode(nodes);


        ByteBuffer buf = ByteBuffer.wrap(data); // big-endian by default

        while (buf.remaining() >= 12) {
            int conditionIndex = buf.getInt();
            int highRef = buf.getInt();
            int lowRef = buf.getInt();
            out.add(new BddNode(conditionIndex, highRef, lowRef));
        }

        return out;
    }

    public static class BddNode {
        int conditionIndex;
        int highRef;
        int lowRef;

        public BddNode(int conditionIndex, int highRef, int lowRef) {
            this.conditionIndex = conditionIndex;
            this.highRef = highRef;
            this.lowRef = lowRef;
        }

        public int getConditionIndex() {
            return conditionIndex;
        }

        public int getHighRef() {
            return highRef;
        }

        public int getLowRef() {
            return lowRef;
        }

        public String toString() {
            return "[C" + conditionIndex + ", " + highRef + ", " + lowRef + "]";
        }
    }
}
