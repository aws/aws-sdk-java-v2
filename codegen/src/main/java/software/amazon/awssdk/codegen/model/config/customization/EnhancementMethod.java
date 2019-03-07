/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.codegen.model.config.customization;

import java.util.List;

/**
 * Config required to generate a method that delegates to a hand-written client
 */
public class EnhancementMethod {
    /** Name of the operation */
    private String name;

    /** Fqcn of the return type of the operation */
    private String returnType;

    /** List of parameters of the operation */
    private List<EnhancementMethodParam> parameters;

    /** List of exceptions that are thrown by the operation */
    private List<String> exceptions;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public List<EnhancementMethodParam> getParameters() {
        return parameters;
    }

    public void setParameters(List<EnhancementMethodParam> parameters) {
        this.parameters = parameters;
    }

    public List<String> getExceptions() {
        return exceptions;
    }

    public void setExceptions(List<String> exceptions) {
        this.exceptions = exceptions;
    }
}
