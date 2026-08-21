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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.codegen.model.config.customization.KeyTypePair;
import software.amazon.awssdk.codegen.poet.rules.EndpointRulesSpecUtils;
import software.amazon.awssdk.codegen.poet.rules2.BooleanAndExpression;
import software.amazon.awssdk.codegen.poet.rules2.BooleanNotExpression;
import software.amazon.awssdk.codegen.poet.rules2.EndpointExpression;
import software.amazon.awssdk.codegen.poet.rules2.EndpointUrlCodeEmitter;
import software.amazon.awssdk.codegen.poet.rules2.ErrorExpression;
import software.amazon.awssdk.codegen.poet.rules2.FunctionCallExpression;
import software.amazon.awssdk.codegen.poet.rules2.HeadersExpression;
import software.amazon.awssdk.codegen.poet.rules2.IndexedAccessExpression;
import software.amazon.awssdk.codegen.poet.rules2.LetExpression;
import software.amazon.awssdk.codegen.poet.rules2.ListExpression;
import software.amazon.awssdk.codegen.poet.rules2.LiteralBooleanExpression;
import software.amazon.awssdk.codegen.poet.rules2.LiteralIntegerExpression;
import software.amazon.awssdk.codegen.poet.rules2.LiteralStringExpression;
import software.amazon.awssdk.codegen.poet.rules2.MemberAccessExpression;
import software.amazon.awssdk.codegen.poet.rules2.MethodCallExpression;
import software.amazon.awssdk.codegen.poet.rules2.PrepareForCodegenVisitor;
import software.amazon.awssdk.codegen.poet.rules2.PropertiesExpression;
import software.amazon.awssdk.codegen.poet.rules2.RuleExpression;
import software.amazon.awssdk.codegen.poet.rules2.RuleExpressionVisitor;
import software.amazon.awssdk.codegen.poet.rules2.RuleFunctionMirror;
import software.amazon.awssdk.codegen.poet.rules2.RuleRuntimeTypeMirror;
import software.amazon.awssdk.codegen.poet.rules2.RuleSetExpression;
import software.amazon.awssdk.codegen.poet.rules2.RuleType;
import software.amazon.awssdk.codegen.poet.rules2.StringConcatExpression;
import software.amazon.awssdk.codegen.poet.rules2.VariableReferenceExpression;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.endpoints.Endpoint;

/**
 * Code generator visitor for BDD result methods that returns {@link Endpoint} directly
 * and throws {@link SdkClientException} for error results. This eliminates the RuleResult
 * wrapper allocation on the hot path.
 */
public class BddResultCodeGeneratorVisitor implements RuleExpressionVisitor<RuleType> {
    private static final Logger log = LoggerFactory.getLogger(BddResultCodeGeneratorVisitor.class);

    private static final ClassName DYNAMIC_ENDPOINT_AUTH_SCHEME_FACTORY =
        ClassName.get("software.amazon.awssdk.services.s3.endpoints.authscheme", "DynamicEndpointAuthSchemeFactory");

    private final CodeBlock.Builder builder;
    private final RuleRuntimeTypeMirror typeMirror;
    private final Map<String, RegistryInfo> registerInfoMap;
    private final Map<String, KeyTypePair> knownEndpointAttributes;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;
    private final boolean useS3ExpressSessionAuth;

    public BddResultCodeGeneratorVisitor(
        CodeBlock.Builder builder, RuleRuntimeTypeMirror typeMirror,
        Map<String, RegistryInfo> registerInfoMap,
        Map<String, KeyTypePair> knownEndpointAttributes,
        EndpointRulesSpecUtils endpointRulesSpecUtils,
        boolean useS3ExpressSessionAuth) {
        this.builder = builder;
        this.typeMirror = typeMirror;
        this.registerInfoMap = registerInfoMap;
        this.knownEndpointAttributes = knownEndpointAttributes;
        this.endpointRulesSpecUtils = endpointRulesSpecUtils;
        this.useS3ExpressSessionAuth = useS3ExpressSessionAuth;
    }

    @Override
    public RuleType visitLiteralBooleanExpression(LiteralBooleanExpression e) {
        builder.add(Boolean.toString(e.value()));
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    @Override
    public RuleType visitLiteralIntegerExpression(LiteralIntegerExpression e) {
        builder.add(Integer.toString(e.value()));
        return RuleRuntimeTypeMirror.INTEGER;
    }

    @Override
    public RuleType visitLiteralStringExpression(LiteralStringExpression e) {
        builder.add("$S", e.value());
        return RuleRuntimeTypeMirror.STRING;
    }

    @Override
    public RuleType visitBooleanNotExpression(BooleanNotExpression e) {
        builder.add("!");
        e.expression().accept(this);
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    @Override
    public RuleType visitBooleanAndExpression(BooleanAndExpression e) {
        List<RuleExpression> expressions = e.expressions();
        boolean isFirst = true;
        for (RuleExpression expr : expressions) {
            if (!isFirst) {
                builder.add(" && ");
            }
            expr.accept(this);
            isFirst = false;
        }
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    @Override
    public RuleType visitFunctionCallExpression(FunctionCallExpression e) {
        String fn = e.name();
        if ("not".equals(fn)) {
            builder.add("!(");
            e.arguments().get(0).accept(this);
            builder.add(")");
            return RuleRuntimeTypeMirror.BOOLEAN;
        }
        if ("isSet".equals(fn)) {
            e.arguments().get(0).accept(this);
            builder.add(" != null");
            return RuleRuntimeTypeMirror.BOOLEAN;
        }
        if ("isNotSet".equals(fn)) {
            e.arguments().get(0).accept(this);
            builder.add(" == null");
            return RuleRuntimeTypeMirror.BOOLEAN;
        }

        // Peephole-optimized synthetic functions
        if (fn.startsWith("__")) {
            return emitPeepholeOptimized(fn, e.arguments());
        }

        RuleFunctionMirror func = typeMirror.resolveFunction(e.name());
        builder.add("$T.$L(", func.containingType().type(), func.javaName());
        List<RuleExpression> args = e.arguments();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                builder.add(", ");
            }
            args.get(i).accept(this);
        }
        builder.add(")");
        return func.returns();
    }

    /**
     * Emits peephole-optimized native Java code for synthetic function calls.
     */
    private RuleType emitPeepholeOptimized(String fn, List<RuleExpression> args) {
        switch (fn) {
            case PrepareForCodegenVisitor.STARTS_WITH:
                return emitStartsWith(args);
            case PrepareForCodegenVisitor.ENDS_WITH:
                return emitEndsWith(args);
            case PrepareForCodegenVisitor.REGION_MATCHES:
                return emitRegionMatches(args);
            case PrepareForCodegenVisitor.ITE:
                return emitIte(args);
            case PrepareForCodegenVisitor.COALESCE_BOOL:
                return emitCoalesceBoolean(args);
            case PrepareForCodegenVisitor.IS_VALID_HOST_LABEL:
                return emitIsValidHostLabel(args);
            default:
                throw new IllegalStateException("Unknown peephole function: " + fn);
        }
    }

    private RuleType emitStartsWith(List<RuleExpression> args) {
        builder.add("(");
        args.get(0).accept(this);
        builder.add(" != null && ");
        args.get(0).accept(this);
        builder.add(".startsWith(");
        args.get(1).accept(this);
        builder.add("))");
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    private RuleType emitEndsWith(List<RuleExpression> args) {
        builder.add("(");
        args.get(0).accept(this);
        builder.add(" != null && ");
        args.get(0).accept(this);
        builder.add(".endsWith(");
        args.get(1).accept(this);
        builder.add("))");
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    private RuleType emitRegionMatches(List<RuleExpression> args) {
        RuleExpression strExpr = args.get(0);
        int offset = ((LiteralIntegerExpression) args.get(1)).value();
        String literal = ((LiteralStringExpression) args.get(2)).value();
        int litLen = literal.length();

        builder.add("(");
        strExpr.accept(this);
        builder.add(" != null && ");

        if (offset >= 0) {
            strExpr.accept(this);
            builder.add(".length() >= $L && ", offset + litLen);
            strExpr.accept(this);
            builder.add(".regionMatches($L, $S, 0, $L)", offset, literal, litLen);
        } else {
            int stopIdx = -offset;
            strExpr.accept(this);
            builder.add(".length() >= $L && ", stopIdx);
            strExpr.accept(this);
            builder.add(".regionMatches(");
            strExpr.accept(this);
            builder.add(".length() - $L, $S, 0, $L)", stopIdx, literal, litLen);
        }
        builder.add(")");
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    private RuleType emitIte(List<RuleExpression> args) {
        builder.add("(");
        args.get(0).accept(this);
        builder.add(" ? ");
        args.get(1).accept(this);
        builder.add(" : ");
        args.get(2).accept(this);
        builder.add(")");
        return RuleRuntimeTypeMirror.STRING;
    }

    private RuleType emitCoalesceBoolean(List<RuleExpression> args) {
        builder.add("(");
        args.get(0).accept(this);
        builder.add(" != null ? ");
        args.get(0).accept(this);
        builder.add(" : ");
        args.get(1).accept(this);
        builder.add(")");
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    private RuleType emitIsValidHostLabel(List<RuleExpression> args) {
        boolean allowDots = ((LiteralBooleanExpression) args.get(1)).value();
        RuleFunctionMirror func = typeMirror.resolveFunction("isValidHostLabel");
        builder.add("$T.$L(", func.containingType().type(),
                    allowDots ? "isValidHostLabelMulti" : "isValidHostLabelSingle");
        args.get(0).accept(this);
        builder.add(")");
        return RuleRuntimeTypeMirror.BOOLEAN;
    }

    @Override
    public RuleType visitMethodCallExpression(MethodCallExpression e) {
        // Wrap compound expressions (string concat) in parens to ensure correct binding
        boolean needsParens = e.source().kind() == RuleExpression.RuleExpressionKind.STRING_CONCAT;
        if (needsParens) {
            builder.add("(");
        }
        e.source().accept(this);
        if (needsParens) {
            builder.add(")");
        }
        builder.add(".$L(", e.name());
        boolean isFirst = true;
        for (RuleExpression arg : e.arguments()) {
            if (!isFirst) {
                builder.add(", ");
            }
            arg.accept(this);
            isFirst = false;
        }
        builder.add(")");
        return e.type();
    }

    @Override
    public RuleType visitVariableReferenceExpression(VariableReferenceExpression e) {
        RegistryInfo registryInfo = registerInfoMap.get(e.variableName());
        if (registryInfo.isNonRegionParam()) {
            builder.add("params.$L()", endpointRulesSpecUtils.paramMethodName(registryInfo.getNonRegionParamKey()));
        } else {
            builder.add("$L", registryInfo.getName());
        }
        return registerInfoMap.get(e.variableName()).getRuleType();
    }

    @Override
    public RuleType visitMemberAccessExpression(MemberAccessExpression e) {
        RuleType sourceType = e.source().accept(this);
        if (!e.directIndex()) {
            builder.add(".$L()", e.name());
        }
        return sourceType.property(e.name());
    }

    @Override
    public RuleType visitIndexedAccessExpression(IndexedAccessExpression e) {
        RuleFunctionMirror func = typeMirror.resolveFunction("listAccess");
        builder.add("$T.$L(", func.containingType().type(), func.javaName());
        RuleType sourceType = e.source().accept(this);
        builder.add(", $L)", e.index());
        return sourceType.typeParam();
    }

    @Override
    public RuleType visitStringConcatExpression(StringConcatExpression e) {
        boolean isFirst = true;
        for (RuleExpression expr : e.expressions()) {
            if (!isFirst) {
                builder.add(" + ");
            }
            expr.accept(this);
            isFirst = false;
        }
        return RuleRuntimeTypeMirror.STRING;
    }

    @Override
    public RuleType visitLetExpression(LetExpression e) {
        throw new IllegalStateException("Unexpected LetExpression in BDD result");
    }

    @Override
    public RuleType visitRuleSetExpression(RuleSetExpression e) {
        // BDD results MUST NOT contain any conditions
        if (e.conditions().size() != 0) {
            throw new IllegalStateException("Expected exactly zero conditions in BDD result");
        }
        if (e.isError()) {
            return e.error().accept(this);
        }
        if (e.isEndpoint()) {
            return e.endpoint().accept(this);
        }
        throw new IllegalStateException("Expected Result to be either error or endpoint.");
    }

    @Override
    public RuleType visitListExpression(ListExpression e) {
        builder.add("$T.asList(", Arrays.class);
        boolean isFirst = true;
        for (RuleExpression expr : e.expressions()) {
            if (!isFirst) {
                builder.add(", ");
            }
            expr.accept(this);
            isFirst = false;
        }
        builder.add(")");
        return RuleRuntimeTypeMirror.LIST_OF_STRING;
    }

    @Override
    public RuleType visitEndpointExpression(EndpointExpression e) {
        Map<String, RuleExpression> properties = e.properties().properties();
        boolean hasHeaders = !e.headers().headers().isEmpty();
        boolean hasNoAttributes = !hasHeaders && properties.isEmpty();
        boolean hasAuthSchemesOnly = !hasHeaders && properties.size() == 1 && properties.containsKey("authSchemes");
        boolean hasTwoAttrs = !hasHeaders && properties.size() == 2 && properties.containsKey("authSchemes");

        if (hasNoAttributes) {
            // Most optimized: Endpoint.of(url) — no attributes, no headers, no builder allocation
            builder.add("return $T.of(", Endpoint.class);
            EndpointUrlCodeEmitter.emit(e.url(), builder, this);
            builder.addStatement(")");
        } else if (hasAuthSchemesOnly) {
            // Optimized: Endpoint.ofAttribute(url, AUTH_SCHEMES, list)
            builder.add("return $T.ofAttribute(", Endpoint.class);
            EndpointUrlCodeEmitter.emit(e.url(), builder, this);
            builder.add(", $T.AUTH_SCHEMES, ", AwsEndpointAttribute.class);
            addAuthSchemesValue(properties.get("authSchemes"));
            builder.addStatement(")");
        } else if (hasTwoAttrs) {
            // Optimized: Endpoint.ofAttributes(url, key1, val1, key2, val2)
            builder.add("return $T.ofAttributes(", Endpoint.class);
            EndpointUrlCodeEmitter.emit(e.url(), builder, this);
            for (Map.Entry<String, RuleExpression> entry : properties.entrySet()) {
                builder.add(", ");
                if ("authSchemes".equals(entry.getKey())) {
                    builder.add("$T.AUTH_SCHEMES, ", AwsEndpointAttribute.class);
                    addAuthSchemesValue(entry.getValue());
                } else if (knownEndpointAttributes.containsKey(entry.getKey())) {
                    KeyTypePair keyType = knownEndpointAttributes.get(entry.getKey());
                    ClassConstant classConstant = parseClassConstant(keyType.getKey());
                    builder.add("$T.$L, ", classConstant.className(), classConstant.fieldName());
                    entry.getValue().accept(this);
                } else {
                    // Unknown attribute — use AwsEndpointAttribute for safety
                    builder.add("$T.AUTH_SCHEMES, ", AwsEndpointAttribute.class);
                    entry.getValue().accept(this);
                }
            }
            builder.addStatement(")");
        } else {
            // General case: use builder pattern
            builder.add("return $T.builder().endpointUrl(", Endpoint.class);
            EndpointUrlCodeEmitter.emit(e.url(), builder, this);
            builder.add(")");
            e.headers().accept(this);
            e.properties().accept(this);
            builder.addStatement(".build()");
        }
        return null;
    }

    private void addAuthSchemesValue(RuleExpression authSchemesExpr) {
        ListExpression expr = (ListExpression) authSchemesExpr;
        builder.add("$T.asList(", Arrays.class);
        boolean isFirst = true;
        for (RuleExpression authSchemeExpr : expr.expressions()) {
            if (!isFirst) {
                builder.add(", ");
            }
            addAuthSchemesBody(authSchemeExpr);
            isFirst = false;
        }
        builder.add(")");
    }

    @Override
    public RuleType visitPropertiesExpression(PropertiesExpression e) {
        Map<String, RuleExpression> properties = e.properties();
        properties.forEach((k, v) -> {
            if ("authSchemes".equals(k)) {
                addAuthSchemesBlock(v);
            } else if ("metricValues".equals(k)) {
                addMetricValuesBlock(v);
            } else if (knownEndpointAttributes.containsKey(k)) {
                addAttributeBlock(k, v);
            } else {
                log.warn("Ignoring unknown endpoint property: {}", k);
            }
        });
        return null;
    }

    @Override
    public RuleType visitHeadersExpression(HeadersExpression e) {
        e.headers().forEach((k, v) -> {
            for (RuleExpression value : v.expressions()) {
                builder.add(".putHeader($S, ", k);
                value.accept(this);
                builder.add(")");
            }
        });
        return null;
    }

    @Override
    public RuleType visitErrorExpression(ErrorExpression e) {
        // Throw SdkClientException directly — no RuleResult wrapper
        builder.add("throw $T.create(", SdkClientException.class);
        e.error().accept(this);
        builder.addStatement(")");
        return null;
    }

    private void addAuthSchemesBlock(RuleExpression e) {
        ListExpression expr = (ListExpression) e;
        builder.add(".putAttribute($T.AUTH_SCHEMES, ", AwsEndpointAttribute.class);
        builder.add("$T.asList(", Arrays.class);
        boolean isFirst = true;
        for (RuleExpression authSchemeExpr : expr.expressions()) {
            if (!isFirst) {
                builder.add(", ");
            }
            addAuthSchemesBody(authSchemeExpr);
            isFirst = false;
        }
        builder.add("))");
    }

    private void addAuthSchemesBody(RuleExpression e) {
        if (e.kind() != RuleExpression.RuleExpressionKind.PROPERTIES) {
            throw new RuntimeException("Expecting properties, got: " + e);
        }
        PropertiesExpression expr = (PropertiesExpression) e;
        RuleExpression nameExpr = expr.properties().get("name");
        boolean isStaticName = nameExpr.kind() == RuleExpression.RuleExpressionKind.STRING_VALUE;

        if (isStaticName) {
            builder.add("$T.builder()", authSchemeClass(stringValueOf(nameExpr)));
        } else {
            validateDynamicAuthSchemeSupported(nameExpr);
            builder.add("$T.builder()", DYNAMIC_ENDPOINT_AUTH_SCHEME_FACTORY);
        }

        expr.properties().forEach((k, v) -> {
            if (!"name".equals(k)) {
                builder.add(".$L(", k);
                v.accept(this);
                builder.add(")");
            }
        });

        if (isStaticName) {
            builder.add(".build()");
        } else {
            builder.add(".create(");
            nameExpr.accept(this);
            builder.add(")");
        }
    }

    private void validateDynamicAuthSchemeSupported(RuleExpression nameExpr) {
        if (!useS3ExpressSessionAuth) {
            throw new IllegalStateException(
                "Endpoint ruleset contains an auth scheme whose name is resolved at runtime (" + nameExpr + "), but the "
                + "'useS3ExpressSessionAuth' customization is not enabled for this service. Dynamically resolved auth "
                + "scheme names are currently only supported for S3.");
        }
    }

    private String stringValueOf(RuleExpression e) {
        if (e.kind() != RuleExpression.RuleExpressionKind.STRING_VALUE) {
            throw new RuntimeException("Expecting string value, got: " + e);
        }
        return ((LiteralStringExpression) e).value();
    }

    private ClassName authSchemeClass(String name) {
        switch (name) {
            case "sigv4":
                return ClassName.get(SigV4AuthScheme.class);
            case "sigv4a":
                return ClassName.get(SigV4aAuthScheme.class);
            case "sigv4-s3express":
                return ClassName.get("software.amazon.awssdk.services.s3.endpoints.authscheme",
                                     "S3ExpressEndpointAuthScheme");
            default:
                throw new RuntimeException("Unknown auth scheme: " + name);
        }
    }

    private void addAttributeBlock(String k, RuleExpression v) {
        KeyTypePair keyType = knownEndpointAttributes.get(k);
        ClassConstant classConstant = parseClassConstant(keyType.getKey());
        builder.add(".putAttribute($T.$L, ", classConstant.className(), classConstant.fieldName());
        v.accept(this);
        builder.add(")");
    }

    private ClassConstant parseClassConstant(String value) {
        int lastDot = value.lastIndexOf('.');
        if (lastDot == -1) {
            throw new IllegalArgumentException("cannot parse class constant: " + value);
        }
        String fieldName = value.substring(lastDot + 1);
        String className = value.substring(0, lastDot);
        int classLastDot = className.lastIndexOf('.');
        if (classLastDot == -1) {
            throw new IllegalArgumentException("cannot parse class constant: " + value);
        }
        String simpleName = className.substring(classLastDot + 1);
        String packageName = className.substring(0, classLastDot);
        return new ClassConstant(ClassName.get(packageName, simpleName), fieldName);
    }

    private void addMetricValuesBlock(RuleExpression v) {
        builder.add(".putAttribute($T.METRIC_VALUES, ", AwsEndpointAttribute.class);
        v.accept(this);
        builder.add(")");
    }

    static class ClassConstant {
        private final ClassName className;
        private final String fieldName;

        ClassConstant(ClassName className, String fieldName) {
            this.className = className;
            this.fieldName = fieldName;
        }

        public ClassName className() {
            return className;
        }

        public String fieldName() {
            return fieldName;
        }
    }
}
