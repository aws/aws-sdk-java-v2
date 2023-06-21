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
import software.amazon.awssdk.utils.Pair;

@SdkInternalApi
public class BooleanEqualsFn extends Fn {
    public static final String ID = "booleanEquals";

    public BooleanEqualsFn(FnNode fnNode) {
        super(fnNode);
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitBoolEquals(this);
    }

    public static BooleanEqualsFn ofExprs(Expr left, Expr right) {
        return new BooleanEqualsFn(FnNode.ofExprs(ID, left, right));
    }

    public Expr getLeft() {
        return expectTwoArgs().left();
    }

    public Expr getRight() {
        return expectTwoArgs().right();
    }

    @Override
    public Value eval(Scope<Value> scope) {
        Pair<Expr, Expr> args = expectTwoArgs();
        return RuleError.ctx("while evaluating booleanEquals",
                () -> Value.fromBool(args.left().eval(scope).expectBool() == args.right().eval(scope).expectBool()));
    }

    public static BooleanEqualsFn fromParam(Parameter param, Expr value) {
        return ofExprs(param.expr(), value);
    }
}
