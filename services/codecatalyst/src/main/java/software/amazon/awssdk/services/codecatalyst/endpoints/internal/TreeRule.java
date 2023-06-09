/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with
 * the License. A copy of the License is located at
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package software.amazon.awssdk.services.codecatalyst.endpoints.internal;

import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class TreeRule extends Rule {
    private final List<Rule> rules;

    protected TreeRule(Builder builder, List<Rule> rules) {
        super(builder);
        this.rules = rules;
    }

    @Override
    public <T> T accept(RuleValueVisitor<T> v) {
        return v.visitTreeRule(rules);
    }

    @Override
    public String toString() {
        return "TreeRule{" + "conditions=" + conditions + ", documentation='" + documentation + '\'' + ", rules=" + rules + '}';
    }
}
