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

import com.squareup.javapoet.CodeBlock;
import java.util.ArrayDeque;
import java.util.Deque;
import software.amazon.awssdk.codegen.jmespath.component.AndExpression;
import software.amazon.awssdk.codegen.jmespath.component.BracketSpecifier;
import software.amazon.awssdk.codegen.jmespath.component.BracketSpecifierWithContents;
import software.amazon.awssdk.codegen.jmespath.component.BracketSpecifierWithQuestionMark;
import software.amazon.awssdk.codegen.jmespath.component.BracketSpecifierWithoutContents;
import software.amazon.awssdk.codegen.jmespath.component.ComparatorExpression;
import software.amazon.awssdk.codegen.jmespath.component.CurrentNode;
import software.amazon.awssdk.codegen.jmespath.component.Expression;
import software.amazon.awssdk.codegen.jmespath.component.ExpressionType;
import software.amazon.awssdk.codegen.jmespath.component.FunctionExpression;
import software.amazon.awssdk.codegen.jmespath.component.IndexExpression;
import software.amazon.awssdk.codegen.jmespath.component.Literal;
import software.amazon.awssdk.codegen.jmespath.component.MultiSelectHash;
import software.amazon.awssdk.codegen.jmespath.component.MultiSelectList;
import software.amazon.awssdk.codegen.jmespath.component.NotExpression;
import software.amazon.awssdk.codegen.jmespath.component.OrExpression;
import software.amazon.awssdk.codegen.jmespath.component.ParenExpression;
import software.amazon.awssdk.codegen.jmespath.component.PipeExpression;
import software.amazon.awssdk.codegen.jmespath.component.SliceExpression;
import software.amazon.awssdk.codegen.jmespath.component.SubExpression;
import software.amazon.awssdk.codegen.jmespath.component.SubExpressionRight;
import software.amazon.awssdk.codegen.jmespath.component.WildcardExpression;
import software.amazon.awssdk.codegen.jmespath.parser.JmesPathParser;
import software.amazon.awssdk.codegen.jmespath.parser.JmesPathVisitor;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;

public class OperationContextParamsGenerator {
    private final String pathExpression;
    private final OperationModel opModel;
    private final Expression parsedPathExpression;

    public OperationContextParamsGenerator(String pathExpression, OperationModel opModel) {
        this.pathExpression = pathExpression;
        this.parsedPathExpression = JmesPathParser.parse(pathExpression);
        this.opModel = opModel;
    }

    public CodeBlock generate() {
        CodeBlock.Builder block = CodeBlock.builder();
        parsedPathExpression.visit(new Visitor(block, "request"));
        return block.build();
    }

    /**
     * An implementation of {@link JmesPathVisitor} used by {@link #interpret(String, String)}.
     */
    private class Visitor implements JmesPathVisitor {
        private final CodeBlock.Builder codeBlock;
        private final Deque<String> variables = new ArrayDeque<>();
        private int variableIndex = 0;

        private Visitor(CodeBlock.Builder codeBlock, String inputValue) {
            this.codeBlock = codeBlock;
            this.codeBlock.add(inputValue);
            this.variables.push(inputValue);
        }

        @Override
        public void visitExpression(Expression input) {
            input.visit(this);
        }

        @Override
        public void visitSubExpression(SubExpression input) {
            visitExpression(input.leftExpression());
            visitSubExpressionRight(input.rightSubExpression());
        }

        @Override
        public void visitSubExpressionRight(SubExpressionRight input) {
            input.visit(this);
        }

        @Override
        public void visitIdentifier(String input) {
            System.out.println("Identifier: " + input);
            // TODO
        }

        @Override
        public void visitWildcardExpression(WildcardExpression input) {
            // TODO
            System.out.println("Wildcard expression: " + input);
        }

        @Override
        public void visitMultiSelectList(MultiSelectList input) {
            // TODO:
            System.out.println("Multi-select list: " + input);
        }

        @Override
        public void visitFunctionExpression(FunctionExpression input) {
            if ("keys".equals(input.function())) {
                input.functionArgs().forEach(arg -> {visitExpression(arg.asExpression());});
                // keys always has exactly one argument
                // TODO: Okay, what do we do now??
            } else {
                throw new IllegalStateException("Unsupported function: " + input.function());
            }
        }

        @Override
        public void visitIndexExpression(IndexExpression input) {
            // This is really a projection expression
            System.out.println("Index expression: " + input);
        }

        @Override
        public void visitBracketSpecifier(BracketSpecifier input) {
            throw new IllegalStateException("Unsupported bracketSpecifier expression");

        }

        @Override
        public void visitBracketSpecifierWithContents(BracketSpecifierWithContents input) {
            throw new IllegalStateException("Unsupported bracketSpecifier expression");
        }

        @Override
        public void visitSliceExpression(SliceExpression input) {
            throw new IllegalStateException("Unsupported slice expression");

        }

        @Override
        public void visitBracketSpecifierWithoutContents(BracketSpecifierWithoutContents input) {
            throw new IllegalStateException("Unsupported bracketSpecifier expression");
        }

        @Override
        public void visitBracketSpecifierWithQuestionMark(BracketSpecifierWithQuestionMark input) {
            throw new IllegalStateException("Unsupported bracketSpecifier expression");
        }

        @Override
        public void visitComparatorExpression(ComparatorExpression input) {
            throw new IllegalStateException("Unsupported comparator expression");

        }

        @Override
        public void visitOrExpression(OrExpression input) {
            throw new IllegalStateException("Unsupported or expression");

        }

        @Override
        public void visitAndExpression(AndExpression input) {
            throw new IllegalStateException("Unsupported and expression");
        }

        @Override
        public void visitNotExpression(NotExpression input) {
            throw new IllegalStateException("Unsupported not expression");

        }

        @Override
        public void visitParenExpression(ParenExpression input) {
            throw new IllegalStateException("Unsupported paren expression");
        }


        @Override
        public void visitMultiSelectHash(MultiSelectHash input) {
            throw new IllegalStateException("Unsupported multiselect map expression");
        }

        @Override
        public void visitExpressionType(ExpressionType asExpressionType) {
            throw new IllegalStateException("Unsupported expression type expression");
        }

        @Override
        public void visitLiteral(Literal input) {
            throw new IllegalStateException("Unsupported literal expression");

        }

        @Override
        public void visitPipeExpression(PipeExpression input) {
            throw new IllegalStateException("Unsupported pipe expression");
        }

        @Override
        public void visitRawString(String input) {
            throw new IllegalStateException("Unsupported raw string expression");
        }

        @Override
        public void visitCurrentNode(CurrentNode input) {
            throw new IllegalStateException("Unsupported current Node expression");
        }

        @Override
        public void visitNumber(int input) {
            throw new IllegalStateException("Unsupported number literal expression");
        }
    }
}
