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
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.codegen.model.config.customization.KeyTypePair;
import software.amazon.awssdk.codegen.poet.rules2.BooleanAndExpression;
import software.amazon.awssdk.codegen.poet.rules2.BooleanNotExpression;
import software.amazon.awssdk.codegen.poet.rules2.CodeGeneratorVisitor;
import software.amazon.awssdk.codegen.poet.rules2.EndpointExpression;
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
import software.amazon.awssdk.codegen.poet.rules2.PropertiesExpression;
import software.amazon.awssdk.codegen.poet.rules2.RuleExpression;
import software.amazon.awssdk.codegen.poet.rules2.RuleExpressionVisitor;
import software.amazon.awssdk.codegen.poet.rules2.RuleFunctionMirror;
import software.amazon.awssdk.codegen.poet.rules2.RuleRuntimeTypeMirror;
import software.amazon.awssdk.codegen.poet.rules2.RuleSetExpression;
import software.amazon.awssdk.codegen.poet.rules2.RuleType;
import software.amazon.awssdk.codegen.poet.rules2.StringConcatExpression;
import software.amazon.awssdk.codegen.poet.rules2.VariableReferenceExpression;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.utils.uri.SdkUri;

public class ResultFnCodeGeneratorVisitor implements RuleExpressionVisitor<RuleType> {
    private static final Logger log = LoggerFactory.getLogger(RuleExpressionVisitor.class);

    private final CodeBlock.Builder builder;
    private final RuleRuntimeTypeMirror typeMirror;
    private final Map<String, RegistryInfo> registerInfoMap;
    private final Map<String, KeyTypePair> knownEndpointAttributes;
    private final boolean endpointCaching;

    public ResultFnCodeGeneratorVisitor(
        CodeBlock.Builder builder, RuleRuntimeTypeMirror typeMirror,
        Map<String, RegistryInfo> registerInfoMap,
        Map<String, KeyTypePair> knownEndpointAttributes, boolean endpointCaching) {
        this.builder = builder;
        this.typeMirror = typeMirror;
        this.registerInfoMap = registerInfoMap;
        this.knownEndpointAttributes = knownEndpointAttributes;
        this.endpointCaching = endpointCaching;
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

    @Override
    public RuleType visitMethodCallExpression(MethodCallExpression e) {
        e.source().accept(this);
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
        String registerName = registerInfoMap.get(e.variableName()).getName();
        builder.add("registers.$L", registerName);
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
        throw new IllegalStateException("Unexpected LetExpression");
    }

    @Override
    public RuleType visitRuleSetExpression(RuleSetExpression e) {
        // BDD results MUST NOT contain any conditions
        if (e.conditions().size() != 0) {
            throw new IllegalStateException("Expected exactly zero conditions");
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
        // TODO: this could potentially be another type
        return RuleRuntimeTypeMirror.LIST_OF_STRING;
    }

    @Override
    public RuleType visitEndpointExpression(EndpointExpression e) {
        builder.add("return $T.endpoint(", typeMirror.rulesResult().type());
        if (endpointCaching) {
            builder.add("$T.builder().url($T.getInstance().create(", Endpoint.class, SdkUri.class);
        } else {
            // optimize common cases
            // TODO: Validate we do not have userinfo, port, query or fragment!
            if (e.url() instanceof StringConcatExpression) {
                StringConcatExpression url = (StringConcatExpression) e.url();

                // schema from variable, eg: {url#scheme}://<rest>
                if (url.expressions().size() >= 3 &&
                    url.expressions().get(0).kind() == RuleExpression.RuleExpressionKind.MEMBER_ACCESS &&
                    url.expressions().get(1).kind() == RuleExpression.RuleExpressionKind.STRING_VALUE &&
                    "://".equals(((LiteralStringExpression)url.expressions().get(1)).value()))
                {
                    // use schema and drop the ://
                    builder.add("$T.builder().url(RulesFunctions.createURI(", Endpoint.class);
                    url.expressions().get(0).accept(this);
                    builder.add(",");
                    StringConcatExpression ssp =
                        StringConcatExpression
                            .builder()
                            .addExpressions(url.expressions().subList(2, url.expressions().size()))
                            .build();
                    ssp.accept(this);
                } else if (url.expressions().size() >= 2 &&
                           url.expressions().get(0).kind() == RuleExpression.RuleExpressionKind.STRING_VALUE &&
                           ((LiteralStringExpression)url.expressions().get(0)).value().startsWith("https://")) {
                    // starts with a hard coded https://.  May be https://{part1} or https://prefix.{part1}
                    // hard code schema and drop the ://
                    builder.add("$T.builder().url(RulesFunctions.createURI(\"https\",", Endpoint.class);
                    String prefixValue = ((LiteralStringExpression)url.expressions().get(0)).value();
                    StringConcatExpression.Builder sspBuilder = StringConcatExpression.builder();
                    if (prefixValue.length() > 8) { // more than just https://
                        sspBuilder.addExpression(new LiteralStringExpression(prefixValue.substring(8))); //drop the https://
                    }
                    sspBuilder.addExpressions(url.expressions().subList(1, url.expressions().size()));
                    sspBuilder.build().accept(this);
                }
                else {
                    builder.add("$T.builder().url($T.create(", Endpoint.class, URI.class);
                    e.url().accept(this);
                }
            } else {
                builder.add("$T.builder().url($T.create(", Endpoint.class, URI.class);
                e.url().accept(this);
            }
        }
        builder.add("))");
        e.headers().accept(this);
        e.properties().accept(this);
        builder.add(".build()");
        builder.addStatement(")");
        return null;
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
        builder.add("return $T.error(", typeMirror.rulesResult().type());
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
        String name = stringValueOf(expr.properties().get("name"));
        builder.add("$T.builder()", authSchemeClass(name));
        expr.properties().forEach((k, v) -> {
            if (!"name".equals(k)) {
                builder.add(".$L(", k);
                v.accept(this);
                builder.add(")");
            }
        });
        builder.add(".build()");
    }

    private String stringValueOf(RuleExpression e) {
        if (e.kind() != RuleExpression.RuleExpressionKind.STRING_VALUE) {
            throw new RuntimeException("Expecting string value, got: " + e);
        }
        LiteralStringExpression expr = (LiteralStringExpression) e;
        return expr.value();
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

    // We assume that the value of the class constants follows the form
    // "<package-part0> ⋯ <package-partN>.<Class>.<CONSTANT>"
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

    /**
     * Helper class to represent a constant field within a fully qualified class.
     */
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
