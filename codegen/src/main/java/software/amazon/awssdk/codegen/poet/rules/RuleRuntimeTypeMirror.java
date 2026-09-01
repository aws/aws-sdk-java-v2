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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.utils.Validate;

public final class RuleRuntimeTypeMirror {
    // Static types
    // Used for undefined/don't care types.
    public static final RuleType VOID = RuleType
        .builder("Void")
        .baseType(TypeName.VOID)
        .build();
    public static final RuleType BOOLEAN = RuleType
        .builder("Boolean")
        .baseType(TypeName.BOOLEAN)
        .build();
    public static final RuleType INTEGER = RuleType
        .builder("Integer")
        .baseType(TypeName.INT)
        .build();
    public static final RuleType STRING = RuleType
        .builder("String")
        .baseType(ClassName.get(String.class))
        .build();
    public static final RuleType LIST_OF_STRING = RuleType
        .builder("List<String>")
        .baseType(ClassName.get(List.class))
        .addTypeParam(ClassName.get(String.class))
        .ruleTypeParam(STRING)
        .build();
    private static final String URL_TYPE_NAME = "Url";
    private static final String PARTITION_TYPE_NAME = "Partition";
    private static final String ARN_TYPE_NAME = "Arn";
    private static final String RESULT_TYPE_NAME = "RuleResult";
    private static final String RULES_FUNCTIONS_TYPE_NAME = "RulesFunctions";
    private final Map<String, RuleType> internalTypes;
    private final Map<String, RuleFunctionMirror> builtinFunctions;

    public RuleRuntimeTypeMirror(String packageName) {
        Map<String, RuleType> types = new HashMap<>();
        for (RuleType.Builder builder : ruleTypes()) {
            RuleType type = builder.packageName(packageName)
                                   .build();
            types.put(type.name(), type);
        }
        this.internalTypes = Collections.unmodifiableMap(types);
        this.builtinFunctions = Collections.unmodifiableMap(buildNameToFunctionMap(types));
    }

    static List<RuleType.Builder> ruleTypes() {
        return Arrays.asList(
            RuleType
                .builder(URL_TYPE_NAME)
                .putProperty("scheme", STRING)
                .putProperty("authority", STRING)
                .putProperty("path", STRING)
                .putProperty("normalizedPath", STRING)
                .putProperty("isIp", BOOLEAN)
                .className("RuleUrl"),
            RuleType
                .builder(PARTITION_TYPE_NAME)
                .putProperty("name", STRING)
                .putProperty("dnsSuffix", STRING)
                .putProperty("dualStackDnsSuffix", STRING)
                .putProperty("supportsFIPS", BOOLEAN)
                .putProperty("supportsDualStack", BOOLEAN)
                .putProperty("supportsDualStack", BOOLEAN)
                .putProperty("implicitGlobalRegion", STRING)
                .className("RulePartition"),
            RuleType
                .builder(ARN_TYPE_NAME)
                .putProperty("partition", STRING)
                .putProperty("service", STRING)
                .putProperty("region", STRING)
                .putProperty("accountId", STRING)
                .putProperty("resourceId", LIST_OF_STRING)
                .className("RuleArn"),
            // Rules result and adjacent types
            RuleType
                .builder(RESULT_TYPE_NAME)
                .className(RESULT_TYPE_NAME),
            // Pseudo-type used to contain the builtin functions.
            RuleType
                .builder(RULES_FUNCTIONS_TYPE_NAME)
                .className(RULES_FUNCTIONS_TYPE_NAME)
        );
    }

    static Map<String, RuleFunctionMirror> buildNameToFunctionMap(Map<String, RuleType> types) {
        Map<String, RuleFunctionMirror> result = new HashMap<>();
        for (RuleFunctionMirror func : builtInFunctions(types)) {
            result.put(func.name(), func);
        }
        return result;
    }

    static List<RuleFunctionMirror> builtInFunctions(Map<String, RuleType> types) {
        RuleType containingType = types.get(RULES_FUNCTIONS_TYPE_NAME);
        return Arrays.asList(
            RuleFunctionMirror
                .builder("not")
                .returns(BOOLEAN)
                .addArgument("value", BOOLEAN)
                .containingType(containingType)
                .build(),
            RuleFunctionMirror
                .builder("substring")
                .returns(STRING)
                .addArgument("index", STRING)
                .addArgument("startIdx", INTEGER)
                .addArgument("endIdx", INTEGER)
                .addArgument("reverse", BOOLEAN)
                .containingType(containingType)
                .build(),
            RuleFunctionMirror
                .builder("stringEquals")
                .returns(BOOLEAN)
                .addArgument("left", STRING)
                .addArgument("right", STRING)
                .containingType(containingType)
                .build(),
            RuleFunctionMirror
                .builder("booleanEquals")
                .returns(BOOLEAN)
                .addArgument("left", BOOLEAN)
                .addArgument("right", BOOLEAN)
                .containingType(containingType)
                .build(),
            RuleFunctionMirror
                .builder("uriEncode")
                .returns(STRING)
                .addArgument("value", STRING)
                .containingType(containingType)
                .build(),
            RuleFunctionMirror
                .builder("parseURL")
                .returns(types.get(URL_TYPE_NAME))
                .addArgument("value", STRING)
                .containingType(containingType)
                .build(),
            RuleFunctionMirror
                .builder("isValidHostLabel")
                .returns(BOOLEAN)
                .addArgument("value", STRING)
                .addArgument("allowSubDomains", BOOLEAN)
                .containingType(containingType)
                .build(),
            RuleFunctionMirror
                .builder("aws.partition")
                .javaName("awsPartition")
                .returns(types.get(PARTITION_TYPE_NAME))
                .addArgument("value", STRING)
                .containingType(containingType)
                .build(),
            RuleFunctionMirror
                .builder("aws.parseArn")
                .javaName("awsParseArn")
                .returns(types.get(ARN_TYPE_NAME))
                .addArgument("value", STRING)
                .containingType(containingType)
                .build(),
            RuleFunctionMirror
                .builder("aws.isVirtualHostableS3Bucket")
                .javaName("awsIsVirtualHostableS3Bucket")
                .returns(BOOLEAN)
                .addArgument("value", STRING)
                .addArgument("allowSubDomains", BOOLEAN)
                .containingType(containingType)
                .build(),
            // Not well-defined, should take List<T> and return T
            // still does the trick for codegen.
            RuleFunctionMirror
                .builder("listAccess")
                .returns(BOOLEAN)
                .addArgument("value", LIST_OF_STRING)
                .addArgument("index", INTEGER)
                .containingType(containingType)
                .build()
        );
    }

    public RuleType rulesFunctions() {
        return requireType(RULES_FUNCTIONS_TYPE_NAME);
    }

    public RuleType rulesResult() {
        return requireType(RESULT_TYPE_NAME);
    }

    public RuleType endpointUrl() {
        return requireType(URL_TYPE_NAME);
    }

    public RuleFunctionMirror resolveFunction(String name) {
        return builtinFunctions.get(name);
    }

    public RuleType resolveType(String name) {
        return internalTypes.get(name);
    }

    public RuleType requireType(String name) {
        return Validate.notNull(internalTypes.get(name), name);
    }
}
