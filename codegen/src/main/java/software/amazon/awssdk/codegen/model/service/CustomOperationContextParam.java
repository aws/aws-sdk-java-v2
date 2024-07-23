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


import java.util.Map;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class CustomOperationContextParam {
    Map<String, OperationContextParam> operationContextParamsMap;

    String operationName;

    public Map<String, OperationContextParam> getOperationContextParamsMap() {
        return operationContextParamsMap;
    }

    public void setOperationContextParamsMap(Map<String, OperationContextParam> operationContextParamsMap) {
        this.operationContextParamsMap = operationContextParamsMap;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }
}
