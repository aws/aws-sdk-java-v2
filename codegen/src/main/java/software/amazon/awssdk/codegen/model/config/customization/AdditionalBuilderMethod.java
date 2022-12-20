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

package software.amazon.awssdk.codegen.model.config.customization;

import java.util.Locale;
import software.amazon.awssdk.core.ClientType;

/**
 * Config required to generate the additional static methods in Client interface.
 */
public class AdditionalBuilderMethod {

    /**
     * Name of the additional static method.
     */
    private String methodName;

    /**
     * Fqcn of the return type
     */
    private String returnType;

    /**
     * Fqcn of the class that will delegate the static method call
     */
    private String instanceType;

    /**
     * JavaDoc for the method
     */
    private String javaDoc;

    /**
     * Method body
     */
    private String statement;

    /**
     * The clientType for which the builder needs to be added.
     */
    private ClientType clientType;

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getJavaDoc() {
        return javaDoc;
    }

    public void setJavaDoc(String javaDoc) {
        this.javaDoc = javaDoc;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String instanceType) {
        this.instanceType = instanceType;
    }

    public String getClientType() {
        return clientType != null ? clientType.toString() : null;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType != null ? ClientType.valueOf(clientType.toUpperCase(Locale.US)) : null;
    }

    public ClientType getClientTypeEnum() {
        return clientType;
    }

    public void setClientTypeEnum(ClientType clientType) {
        this.clientType = clientType;
    }

    public String getStatement() {
        return statement;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }
}
