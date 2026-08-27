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

package software.amazon.awssdk.codegen.poet.rules2.bdd;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import software.amazon.awssdk.codegen.model.rules.endpoints.ConditionModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.RuleModel;
import software.amazon.awssdk.codegen.model.service.EndpointBddModel;
import software.amazon.awssdk.codegen.poet.rules2.ExpressionParser;
import software.amazon.awssdk.codegen.poet.rules2.FunctionCallExpression;
import software.amazon.awssdk.codegen.poet.rules2.IndexedAccessExpression;
import software.amazon.awssdk.codegen.poet.rules2.MemberAccessExpression;
import software.amazon.awssdk.codegen.poet.rules2.RuleExpression;
import software.amazon.awssdk.codegen.poet.rules2.VariableReferenceExpression;
import software.amazon.awssdk.codegen.poet.rules2.WalkRuleExpressionVisitor;

/**
 * Works out how each endpoint parameter is used by a BDD, so that the generated result cache only compares what can
 * actually change the resolved endpoint.
 *
 * <p>Analysis runs over the BDD's conditions and results using the same parser the generator uses, so the answer
 * reflects what the generated provider reads rather than what the model happens to declare.
 *
 * <p>Two things fall out of it:
 *
 * <ul>
 *   <li>{@link Usage#UNREFERENCED} - a parameter no condition and no result reads. It cannot change the endpoint, so
 *       comparing it can only produce misses that resolve to the endpoint already cached. S3 is the motivating case:
 *       it declares {@code Key}, {@code Prefix} and {@code CopySource}, no rule reads any of them, and {@code Key}
 *       changes on essentially every object request - so including it means the cache almost never hits.</li>
 *   <li>{@link Usage#FIRST_ELEMENT_ONLY} - a {@code stringArray} whose every use is an index-0 access. Only the first
 *       element can reach the endpoint, so the generated comparison is optimized by comparing only that element instead
 *       of walking the list.
 * </ul>
 */
final class BddParameterReferences {

    /**
     * How much of a parameter's value can influence the resolved endpoint.
     */
    enum Usage {
        /** No condition and no result reads it. It cannot be part of the cache key. */
        UNREFERENCED,

        /**
         * A {@code stringArray} the rules can only observe through {@code isSet(param)} and {@code param[0]}. Whether
         * the list is present, plus its first element, is therefore the whole of what can reach the endpoint, and
         * nothing past element 0 can change the answer.
         */
        FIRST_ELEMENT_ONLY,

        /** Read in a way that can depend on the entire value. */
        FULL
    }

    private BddParameterReferences() {
    }

    /**
     * Returns the usage of every parameter the model declares, in declaration order.
     */
    static Map<String, Usage> analyze(EndpointBddModel model) {
        Collector collector = new Collector();

        for (ConditionModel condition : model.getConditions()) {
            RuleModel synthetic = new RuleModel();
            synthetic.setType("error");
            synthetic.setError("synthetic");
            synthetic.setConditions(Collections.singletonList(condition));
            ExpressionParser.parseRuleSetExpression(synthetic).accept(collector);
        }
        for (RuleModel result : model.getResults()) {
            ExpressionParser.parseRuleSetExpression(result).accept(collector);
        }

        Map<String, Usage> usage = new LinkedHashMap<>();
        model.getParameters().forEach((name, parameter) -> usage.put(name, usageOf(name, parameter, collector)));
        return Collections.unmodifiableMap(usage);
    }

    private static Usage usageOf(String name, ParameterModel parameter, Collector collector) {
        if (!collector.referenced.contains(name)) {
            return Usage.UNREFERENCED;
        }
        if (collector.wholeValue.contains(name)) {
            return Usage.FULL;
        }
        // Only lists benefit, and only lists can be read element-wise. Anything else that somehow reached here is
        // compared in full rather than guessed at.
        return isList(parameter) ? Usage.FIRST_ELEMENT_ONLY : Usage.FULL;
    }

    private static boolean isList(ParameterModel parameter) {
        return "stringarray".equals(parameter.getType().toLowerCase(Locale.ENGLISH));
    }

    /**
     * Collects, for every name the expressions reference, whether any reference needs more than the first element.
     *
     * <p>Erring towards {@link Usage#FULL} is the safe direction: it costs comparison work, whereas erring the other
     * way would drop something from the cache key that can change the endpoint.
     */
    private static final class Collector extends WalkRuleExpressionVisitor {
        private final Set<String> referenced = new HashSet<>();
        private final Set<String> wholeValue = new HashSet<>();

        @Override
        public Void visitIndexedAccessExpression(IndexedAccessExpression e) {
            String subject = firstElementSubject(e);
            if (subject != null) {
                referenced.add(subject);
                // Deliberately not descending. Descending would reach the variable reference underneath and record it
                // as a whole-value read, which is the thing this case exists to avoid.
                return null;
            }
            return super.visitIndexedAccessExpression(e);
        }

        /**
         * {@code isSet(param)} observes only whether the parameter is present, so on its own it does not force a
         * whole-value comparison.
         */
        @Override
        public Void visitFunctionCallExpression(FunctionCallExpression e) {
            if ("isSet".equals(e.name()) && e.arguments().size() == 1) {
                RuleExpression argument = e.arguments().get(0);
                if (argument instanceof VariableReferenceExpression) {
                    referenced.add(((VariableReferenceExpression) argument).variableName());
                    return null;
                }
            }
            return super.visitFunctionCallExpression(e);
        }

        @Override
        public Void visitVariableReferenceExpression(VariableReferenceExpression e) {
            referenced.add(e.variableName());
            wholeValue.add(e.variableName());
            return null;
        }

        /**
         * Returns the parameter name when this is an index-0 read of a plain parameter, otherwise null.
         *
         * <p>Both spellings reach here: {@code "list[0]"} inside a template parses to an indexed access straight over
         * the variable, while {@code getAttr(list, "[0]")} wraps it in a direct-index member access first.
         */
        private static String firstElementSubject(IndexedAccessExpression e) {
            if (e.index() != 0) {
                return null;
            }
            RuleExpression source = e.source();
            if (source instanceof MemberAccessExpression) {
                MemberAccessExpression member = (MemberAccessExpression) source;
                if (!member.directIndex()) {
                    return null;
                }
                source = member.source();
            }
            if (source instanceof VariableReferenceExpression) {
                return ((VariableReferenceExpression) source).variableName();
            }
            return null;
        }
    }
}
