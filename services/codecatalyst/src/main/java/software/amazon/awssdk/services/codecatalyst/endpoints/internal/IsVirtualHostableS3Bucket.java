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
public class IsVirtualHostableS3Bucket extends VarargFn {
    public static final String ID = "aws.isVirtualHostableS3Bucket";

    public IsVirtualHostableS3Bucket(FnNode fnNode) {
        super(fnNode);
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitIsVirtualHostLabelsS3Bucket(this);
    }

    public static IsVirtualHostableS3Bucket ofExprs(Expr expr, boolean allowDots) {
        return new IsVirtualHostableS3Bucket(FnNode.ofExprs(ID, expr, Expr.of(allowDots)));
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
        if (allowDots(scope)) {
            // TODO: use compiled Pattern
            return Value.fromBool(hostLabel.matches("[a-z\\d][a-z\\d\\-.]{1,61}[a-z\\d]")
                    && !hostLabel.matches("(\\d+\\.){3}\\d+") // don't allow ip address
                    && !hostLabel.matches(".*[.-]{2}.*") // don't allow names like bucket-.name or bucket.-name
            );
        } else {
            return Value.fromBool(hostLabel.matches("[a-z\\d][a-z\\d\\-]{1,61}[a-z\\d]"));
        }
    }

    private boolean allowDots(Scope<Value> scope) {
        return allowDots().eval(scope).expectBool();
    }
}