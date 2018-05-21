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

package software.amazon.awssdk.protocol.runners;

import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolFactory;
import software.amazon.awssdk.awscore.protocol.json.AwsJsonProtocolMetadata;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.core.protocol.json.JsonClientMetadata;


public final class JsonProtocolUtils {

    private JsonProtocolUtils() {
    }

    /**
     * Create the software.amazon.awssdk.protocol factory needed to create the marshallers and response handlers for the
     * tests.
     *
     * @param model Intermediate model of service
     * @return Configured {@link AwsJsonProtocolFactory}
     */
    public static AwsJsonProtocolFactory createProtocolFactory(IntermediateModel model) {
        final Metadata metadata = model.getMetadata();
        return new AwsJsonProtocolFactory(
            new JsonClientMetadata().withSupportsCbor(metadata.isCborProtocol()),
            AwsJsonProtocolMetadata.builder().protocolVersion(metadata.getJsonVersion()).build());
    }
}
