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

package software.amazon.awssdk.codegen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codegen.model.intermediate.AcceptorModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.WaiterDefinitionModel;
import software.amazon.awssdk.codegen.model.service.Acceptor;
import software.amazon.awssdk.codegen.model.service.WaiterDefinition;
import software.amazon.awssdk.codegen.model.service.Waiters;

class AddWaiters {

    private final Waiters waiters;
    private final Map<String, OperationModel> operations;

    AddWaiters(Waiters waiters, Map<String, OperationModel> operations) {
        this.waiters = waiters;
        this.operations = operations;
    }

    Map<String, WaiterDefinitionModel> constructWaiters() throws IOException {

        Map<String, WaiterDefinitionModel> javaWaiterModels = new HashMap<>();

        for (Map.Entry<String, WaiterDefinition> entry : waiters.getWaiters().entrySet()) {

            final String waiterName = entry.getKey();
            final WaiterDefinition waiterDefinition = entry.getValue();
            List<AcceptorModel> acceptors = new ArrayList<>();

            WaiterDefinitionModel waiterDefinitionModel = new WaiterDefinitionModel();

            waiterDefinitionModel.setDelay(waiterDefinition.getDelay());
            waiterDefinitionModel.setMaxAttempts(waiterDefinition.getMaxAttempts());
            waiterDefinitionModel.setWaiterName(waiterName);
            waiterDefinitionModel.setOperationModel(operations.get(waiterDefinition.getOperation()));
            for (Acceptor acceptor : waiterDefinition.getAcceptors()) {
                AcceptorModel acceptorModel = new AcceptorModel();
                acceptorModel.setMatcher(acceptor.getMatcher());
                acceptorModel.setState(acceptor.getState());
                acceptorModel.setExpected(acceptor.getExpected());
                acceptorModel.setArgument(acceptor.getArgument());

                acceptors.add(acceptorModel);
            }
            waiterDefinitionModel.setAcceptors(acceptors);
            javaWaiterModels.put(waiterName, waiterDefinitionModel);
        }

        return javaWaiterModels;
    }
}
