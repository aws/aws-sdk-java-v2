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

import java.util.ArrayList;
import java.util.List;

/**
 * Config required to generate the utilities method that returns an instance of
 * hand-written Utilities class
 */
public class UtilitiesMethod {

    public static final String METHOD_NAME = "utilities";

    /** Fqcn of the return type of the operation */
    private String returnType;

    /**
     * The utilities method will call a protected create() method in the hand-written Utilities class.
     * These the ordered list of parameters that needs to be passed to the create method.
     */
    private List<String> createMethodParams = new ArrayList<>();

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public List<String> getCreateMethodParams() {
        return createMethodParams;
    }

    public void setCreateMethodParams(List<String> createMethodParams) {
        this.createMethodParams = createMethodParams;
    }
}
