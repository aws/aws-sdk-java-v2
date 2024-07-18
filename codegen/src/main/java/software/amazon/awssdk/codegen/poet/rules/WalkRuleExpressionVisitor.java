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

package software.amazon.awssdk.codegen.poet.rules;

import java.util.Collection;
import java.util.List;

public class WalkRuleExpressionVisitor implements RuleExpressionVisitor<Void> {
    @Override
    public Void visitLiteralBooleanExpression(LiteralBooleanExpression e) {
        return null;
    }

    @Override
    public Void visitLiteralIntegerExpression(LiteralIntegerExpression e) {
        return null;
    }

    @Override
    public Void visitLiteralStringExpression(LiteralStringExpression e) {
        return null;
    }

    @Override
    public Void visitFunctionCallExpression(FunctionCallExpression e) {
        visitAll(e.arguments());
        return null;
    }

    @Override
    public Void visitMethodCallExpression(MethodCallExpression e) {
        e.source().accept(this);
        visitAll(e.arguments());
        return null;
    }

    @Override
    public Void visitVariableReferenceExpression(VariableReferenceExpression e) {
        return null;
    }

    @Override
    public Void visitMemberAccessExpression(MemberAccessExpression e) {
        e.source().accept(this);
        return null;
    }

    @Override
    public Void visitIndexedAccessExpression(IndexedAccessExpression e) {
        e.source().accept(this);
        return null;
    }

    @Override
    public Void visitStringConcatExpression(StringConcatExpression e) {
        visitAll(e.expressions());
        return null;
    }

    @Override
    public Void visitLetExpression(LetExpression e) {
        visitAll(e.bindings().values());
        return null;
    }

    @Override
    public Void visitBooleanAndExpression(BooleanAndExpression e) {
        visitAll(e.expressions());
        return null;
    }

    @Override
    public Void visitBooleanNotExpression(BooleanNotExpression e) {
        e.expression().accept(this);
        return null;
    }

    @Override
    public Void visitRuleSetExpression(RuleSetExpression e) {
        visitAll(e.conditions());
        ErrorExpression error = e.error();
        if (error != null) {
            e.accept(this);
        }
        EndpointExpression endpoint = e.endpoint();
        if (endpoint != null) {
            endpoint.accept(this);
        }
        List<RuleSetExpression> children = e.children();
        if (children != null) {
            visitAll(children);
        }
        return null;
    }

    @Override
    public Void visitEndpointExpression(EndpointExpression e) {
        e.url().accept(this);
        e.properties().accept(this);
        e.headers().accept(this);
        return null;
    }

    @Override
    public Void visitErrorExpression(ErrorExpression e) {
        e.error().accept(this);
        return null;
    }

    @Override
    public Void visitPropertiesExpression(PropertiesExpression e) {
        visitAll(e.properties().values());
        return null;
    }

    @Override
    public Void visitHeadersExpression(HeadersExpression e) {
        e.headers().forEach((k, v) -> {
            v.accept(this);
        });
        return null;
    }

    @Override
    public Void visitListExpression(ListExpression e) {
        visitAll(e.expressions());
        return null;
    }

    protected void visitAll(Collection<? extends RuleExpression> expressions) {
        for (RuleExpression expr : expressions) {
            expr.accept(this);
        }
    }
}
