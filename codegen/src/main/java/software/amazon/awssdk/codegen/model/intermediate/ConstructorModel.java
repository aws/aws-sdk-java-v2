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

package software.amazon.awssdk.codegen.model.intermediate;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;

@ReviewBeforeRelease("If we get rid of 'simple constructors' for simple models, this can be deleted.")
public class ConstructorModel {

    private final List<ArgumentModel> arguments = new LinkedList<ArgumentModel>();

    public ConstructorModel(String modelClassName) {
    }

    public List<ArgumentModel> getArguments() {
        return arguments;
    }

    public void addArgument(ArgumentModel argument) {
        this.arguments.add(argument);
    }

    public String getArgumentsDeclaration() {
        StringBuilder builder = new StringBuilder();

        Iterator<ArgumentModel> iter = arguments.iterator();
        while (iter.hasNext()) {
            ArgumentModel arg = iter.next();

            builder.append(arg.getType())
                   .append(" ")
                   .append(arg.getName());

            if (iter.hasNext()) {
                builder.append(", ");
            }
        }

        return builder.toString();
    }
}
