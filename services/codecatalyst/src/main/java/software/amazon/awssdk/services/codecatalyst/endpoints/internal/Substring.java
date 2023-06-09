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
public class Substring extends VarargFn {
    public static final String ID = "substring";
    public static final Identifier SUBSTRING = Identifier.of("substring");
    private static final int EXPECTED_NUMBER_ARGS = 4;

    public Substring(FnNode fnNode) {
        super(fnNode);
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitSubstring(this);
    }

    public static Substring ofExprs(Expr expr, int startIndex, int stopIndex, Boolean reverse) {
        return new Substring(FnNode.ofExprs(ID, expr, Expr.of(startIndex), Expr.of(stopIndex), Expr.of(reverse)));
    }

    public Expr stringToParse() {
        return expectVariableArgs(EXPECTED_NUMBER_ARGS).get(0);
    }

    public Expr startIndex() {
        return expectVariableArgs(EXPECTED_NUMBER_ARGS).get(1);
    }

    public Expr stopIndex() {
        return expectVariableArgs(EXPECTED_NUMBER_ARGS).get(2);
    }

    public Expr reverse() {
        return expectVariableArgs(EXPECTED_NUMBER_ARGS).get(3);
    }

    @Override
    public Value eval(Scope<Value> scope) {
        List<Expr> args = expectVariableArgs(EXPECTED_NUMBER_ARGS);
        String str = args.get(0).eval(scope).expectString();
        int startIndex = args.get(1).eval(scope).expectInt();
        int stopIndex = args.get(2).eval(scope).expectInt();
        boolean reverse = args.get(3).eval(scope).expectBool();

        if (startIndex >= stopIndex || str.length() - 1 < stopIndex) {
            return new Value.None();
        }

        String substr;
        if (reverse) {
            String reversedStr = new StringBuilder(str).reverse().toString();
            substr = new StringBuilder(reversedStr.substring(startIndex, stopIndex)).reverse().toString();
        } else {
            substr = str.substring(startIndex, stopIndex);
        }

        return Value.fromStr(substr);

    }
}
