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

package software.amazon.awssdk.codegen.smithy.build;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Settings for the {@value AwsSdkJavaCodegenPlugin#NAME} plugin.
 *
 * <p>A module with one client sets {@code customizationConfig}. A module with several clients, such as a client and
 * another that shares its model classes, sets {@code clients} instead, one entry per service.
 */
@SdkInternalApi
public final class AwsSdkJavaCodegenSettings {

    static final String CUSTOMIZATION_CONFIG = "customizationConfig";
    static final String BASE_DIR = "baseDir";
    static final String CLIENTS = "clients";
    static final String SERVICE = "service";

    private final List<Client> clients;

    private AwsSdkJavaCodegenSettings(List<Client> clients) {
        this.clients = clients;
    }

    /**
     * Creates settings from plugin configuration.
     */
    public static AwsSdkJavaCodegenSettings fromNode(ObjectNode node) {
        node.expectNoAdditionalProperties(Arrays.asList(CUSTOMIZATION_CONFIG, BASE_DIR, CLIENTS));

        Path baseDir = node.getStringMember(BASE_DIR)
                           .map(s -> Paths.get(s.getValue()))
                           .orElseGet(() -> Paths.get(""));

        if (!node.getMember(CLIENTS).isPresent()) {
            return new AwsSdkJavaCodegenSettings(
                Collections.singletonList(new Client(null, customizationConfig(node, baseDir))));
        }
        if (node.getMember(CUSTOMIZATION_CONFIG).isPresent()) {
            throw new ExpectationNotMetException(
                "Set '" + CUSTOMIZATION_CONFIG + "' on each entry of '" + CLIENTS + "', not on the plugin", node);
        }
        List<Client> clients = new ArrayList<>();
        for (Node entry : node.expectArrayMember(CLIENTS).getElements()) {
            ObjectNode client = entry.expectObjectNode();
            client.expectNoAdditionalProperties(Arrays.asList(SERVICE, CUSTOMIZATION_CONFIG));
            clients.add(new Client(ShapeId.from(client.expectStringMember(SERVICE).getValue()),
                                   customizationConfig(client, baseDir)));
        }
        return new AwsSdkJavaCodegenSettings(Collections.unmodifiableList(clients));
    }

    private static Path customizationConfig(ObjectNode node, Path baseDir) {
        return node.getStringMember(CUSTOMIZATION_CONFIG).map(s -> baseDir.resolve(s.getValue())).orElse(null);
    }

    /**
     * The clients to generate.
     */
    public List<Client> clients() {
        return clients;
    }

    /**
     * One client to generate.
     */
    public static final class Client {
        private final ShapeId service;
        private final Path customizationConfig;

        private Client(ShapeId service, Path customizationConfig) {
            this.service = service;
            this.customizationConfig = customizationConfig;
        }

        /**
         * The client's service shape, or null when the model has only one service.
         */
        public ShapeId service() {
            return service;
        }

        /**
         * The client's customization file, if any.
         */
        public Path customizationConfig() {
            return customizationConfig;
        }
    }
}
