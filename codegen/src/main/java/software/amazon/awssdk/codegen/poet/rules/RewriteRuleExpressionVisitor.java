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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import software.amazon.awssdk.utils.Validate;

/**
 * Base class for all the rewriting expression tasks.
 */
public class RewriteRuleExpressionVisitor implements RuleExpressionVisitor<RuleExpression> {
    @Override
    public RuleExpression visitLiteralBooleanExpression(LiteralBooleanExpression expr) {
        return expr;
    }

    @Override
    public RuleExpression visitLiteralIntegerExpression(LiteralIntegerExpression expr) {
        return expr;
    }

    @Override
    public RuleExpression visitLiteralStringExpression(LiteralStringExpression expr) {
        return expr;
    }

    @Override
    public RuleExpression visitFunctionCallExpression(FunctionCallExpression e) {
        FunctionCallExpression.Builder builder = FunctionCallExpression
            .builder()
            .name(e.name())
            .type(e.type());
        for (RuleExpression arg : e.arguments()) {
            builder.addArgument(arg.accept(this));
        }
        return builder.build();
    }

    @Override
    public RuleExpression visitMethodCallExpression(MethodCallExpression e) {
        MethodCallExpression.Builder builder = MethodCallExpression
            .builder()
            .source(e.source().accept(this))
            .name(e.name());
        for (RuleExpression arg : e.arguments()) {
            builder.addArgument(arg.accept(this));
        }
        return builder.build();

    }

    @Override
    public RuleExpression visitVariableReferenceExpression(VariableReferenceExpression e) {
        return e;
    }

    @Override
    public RuleExpression visitMemberAccessExpression(MemberAccessExpression e) {
        MemberAccessExpression.Builder builder = MemberAccessExpression
            .builder()
            .name(e.name())
            .type(e.type())
            .source(e.source().accept(this));
        return builder.build();
    }

    @Override
    public RuleExpression visitIndexedAccessExpression(IndexedAccessExpression e) {
        IndexedAccessExpression.Builder builder = IndexedAccessExpression
            .builder()
            .index(e.index())
            .type(e.type())
            .source(e.source().accept(this));
        return builder.build();
    }

    @Override
    public RuleExpression visitStringConcatExpression(StringConcatExpression e) {
        StringConcatExpression.Builder builder = StringConcatExpression
            .builder();
        visitAllWith(e.expressions(), builder::addExpression);
        return builder.build();
    }

    @Override
    public RuleExpression visitLetExpression(LetExpression e) {
        LetExpression.Builder builder = LetExpression.builder();
        e.bindings().forEach((k, v) -> {
            builder.putBinding(k, v.accept(this));
        });
        return builder.build();
    }

    @Override
    public RuleExpression visitBooleanAndExpression(BooleanAndExpression e) {
        BooleanAndExpression.Builder builder = BooleanAndExpression.builder();
        visitAllWith(e.expressions(), builder::addExpression);
        return builder.build();
    }

    @Override
    public RuleExpression visitBooleanNotExpression(BooleanNotExpression e) {
        BooleanNotExpression.Builder builder = BooleanNotExpression.builder();
        builder.expression(e.expression().accept(this));
        return builder.build();
    }

    @Override
    public RuleExpression visitRuleSetExpression(RuleSetExpression e) {
        RuleSetExpression.Builder builder = e.toBuilder();
        builder.clearConditions();
        visitAllWith(e.conditions(), builder::addCondition);
        List<RuleSetExpression> children = e.children();
        if (children != null) {
            builder.clearChildren();
            visitAllWith(children, child -> {
                builder.addChildren(Validate.isInstanceOf(RuleSetExpression.class, child,
                                                          "expected RuleSetExpression"));
            });
        }
        EndpointExpression endpoint = e.endpoint();
        if (endpoint != null) {
            builder.endpoint(Validate.isInstanceOf(EndpointExpression.class, endpoint.accept(this),
                                                   "expected EndpointExpression"));
        }
        ErrorExpression error = e.error();
        if (error != null) {
            builder.error(Validate.isInstanceOf(ErrorExpression.class, error.accept(this),
                                                "expected ErrorExpression"));
        }
        return builder.build();
    }

    @Override
    public RuleExpression visitEndpointExpression(EndpointExpression e) {
        EndpointExpression.Builder builder = EndpointExpression
            .builder()
            .url(e.url().accept(this))
            .headers(Validate.isInstanceOf(HeadersExpression.class, e.headers().accept(this), "expected HeadersExpression"))
            .properties(Validate.isInstanceOf(PropertiesExpression.class, e.properties().accept(this),
                                              "expected PropertiesExpression"));
        return builder.build();
    }

    @Override
    public RuleExpression visitErrorExpression(ErrorExpression e) {
        return new ErrorExpression(e.error().accept(this));
    }

    @Override
    public RuleExpression visitPropertiesExpression(PropertiesExpression e) {
        PropertiesExpression.Builder builder = PropertiesExpression.builder();
        e.properties().forEach((k, v) -> builder.putProperty(k, v.accept(this)));
        return builder.build();
    }

    @Override
    public RuleExpression visitHeadersExpression(HeadersExpression e) {
        HeadersExpression.Builder builder = HeadersExpression.builder();
        e.headers().forEach((k, v) -> builder.putHeader(k, (ListExpression) v.accept(this)));
        return builder.build();
    }

    @Override
    public RuleExpression visitListExpression(ListExpression e) {
        ListExpression.Builder builder = ListExpression.builder();
        visitAllWith(e.expressions(), builder::addExpression);
        return builder.build();
    }

    protected List<RuleExpression> visitAll(List<RuleExpression> expressions) {
        if (expressions.isEmpty()) {
            return Collections.emptyList();
        }
        List<RuleExpression> result = new ArrayList<>();
        visitAllWith(expressions, result::add);
        return result;
    }

    protected void visitAllWith(List<? extends RuleExpression> expressions, Consumer<RuleExpression> consumer) {
        for (RuleExpression expr : expressions) {
            consumer.accept(expr.accept(this));
        }
    }
}
