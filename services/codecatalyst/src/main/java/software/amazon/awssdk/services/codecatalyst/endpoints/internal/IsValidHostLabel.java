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

import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public class IsValidHostLabel extends VarargFn {
    public static final String ID = "isValidHostLabel";

    public IsValidHostLabel(FnNode fnNode) {
        super(fnNode);
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitIsValidHostLabel(this);
    }

    public static IsValidHostLabel ofExprs(Expr expr, boolean allowDots) {
        return new IsValidHostLabel(FnNode.ofExprs(ID, expr, Expr.of(allowDots)));
    }

    public Expr hostLabel() {
        return expectTwoArgs().left();
    }

    public Expr allowDots() {
        return expectTwoArgs().right();
    }

    @Override
    public Value eval(Scope<Value> scope) {
        String hostLabel = expectTwoArgs().left().eval(scope).expectString();
        // TODO: use compiled Pattern
        if (allowDots(scope)) {
            return Value.fromBool(hostLabel.matches("[a-zA-Z\\d][a-zA-Z\\d\\-.]{0,62}"));
        } else {
            return Value.fromBool(hostLabel.matches("[a-zA-Z\\d][a-zA-Z\\d\\-]{0,62}"));
        }
    }

    private boolean allowDots(Scope<Value> scope) {
        return allowDots().eval(scope).expectBool();
    }
}
