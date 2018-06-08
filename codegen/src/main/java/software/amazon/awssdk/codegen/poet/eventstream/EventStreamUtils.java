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

package software.amazon.awssdk.codegen.poet.eventstream;

import java.util.Objects;
import java.util.stream.Stream;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;

public class EventStreamUtils {

    private final OperationModel operation;

    public EventStreamUtils(OperationModel eventStreamOperation) {
        this.operation = eventStreamOperation;
    }

    public String getApiName() {
        return Utils.capitialize(operation.getOperationName());
    }

    public ShapeModel getEventStreamMember() {
        ShapeModel outputShape = operation.getOutputShape();
        return outputShape.getMembers()
                          .stream()
                          .map(MemberModel::getShape)
                          .filter(Objects::nonNull)
                          .filter(ShapeModel::isEventStream)
                          .findFirst()
                          .orElseThrow(() -> new IllegalStateException("Did not find event stream member on " +
                                                                       outputShape.getC2jName()));
    }

    public Stream<ShapeModel> getEventSubTypes() {
        return getEventStreamMember()
            .getMembers()
            .stream()
            .map(MemberModel::getShape);
    }

}
