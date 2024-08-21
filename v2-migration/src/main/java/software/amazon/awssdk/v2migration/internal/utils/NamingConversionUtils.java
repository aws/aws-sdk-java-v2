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

package software.amazon.awssdk.v2migration.internal.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.internal.CodegenNamingUtils;

@SdkInternalApi
public final class NamingConversionUtils {

    private static final String V1_PACKAGE_PREFIX = "com.amazonaws.services";
    private static final String V2_PACKAGE_PREFIX = "software.amazon.awssdk.services";
    private static final Map<String, String> SPECIAL_MAPPING = new HashMap<>();

    static {
        SPECIAL_MAPPING.put("appregistry", "servicecatalogappregistry");
        SPECIAL_MAPPING.put("certificatemanager", "acm");
        SPECIAL_MAPPING.put("cloudcontrolapi", "cloudcontrol");
        SPECIAL_MAPPING.put("cloudsearchv2", "cloudsearch");
        SPECIAL_MAPPING.put("cloudwatchevidently", "evidently");
        SPECIAL_MAPPING.put("logs", "cloudwatchlogs");
        SPECIAL_MAPPING.put("cloudwatchrum", "rum");
        SPECIAL_MAPPING.put("cognitoidp", "cognitoidentityprovider");
        SPECIAL_MAPPING.put("connectcampaign", "connectcampaigns");
        SPECIAL_MAPPING.put("connectwisdom", "wisdom");
        SPECIAL_MAPPING.put("databasemigrationservice", "databasemigration");
        SPECIAL_MAPPING.put("dynamodbv2", "dynamodb");
        SPECIAL_MAPPING.put("elasticfilesystem", "efs");
        SPECIAL_MAPPING.put("elasticmapreduce", "emr");
        SPECIAL_MAPPING.put("gluedatabrew", "databrew");
        SPECIAL_MAPPING.put("iamrolesanywhere", "rolesanywhere");
        SPECIAL_MAPPING.put("identitymanagement", "iam");
        SPECIAL_MAPPING.put("iotdata", "iotdataplane");
        SPECIAL_MAPPING.put("mainframemodernization", "m2");
        SPECIAL_MAPPING.put("managedgrafana", "grafana");
        SPECIAL_MAPPING.put("migrationhubstrategyrecommendations", "migrationhubstrategy");
        SPECIAL_MAPPING.put("nimblestudio", "nimble");
        SPECIAL_MAPPING.put("private5g", "privatenetworks");
        SPECIAL_MAPPING.put("prometheus", "amp");
        SPECIAL_MAPPING.put("recyclebin", "rbin");
        SPECIAL_MAPPING.put("redshiftdataapi", "redshiftdata");
        SPECIAL_MAPPING.put("sagemakeredgemanager", "sagemakeredge");
        SPECIAL_MAPPING.put("securitytoken", "sts");
        SPECIAL_MAPPING.put("servermigration", "sms");
        SPECIAL_MAPPING.put("simpleemail", "ses");
        SPECIAL_MAPPING.put("simpleemailv2", "sesv2");
        SPECIAL_MAPPING.put("simplesystemsmanagement", "ssm");
        SPECIAL_MAPPING.put("stepfunctions", "sfn");
    }

    private NamingConversionUtils() {
    }

    public static String getV2Equivalent(String currentFqcn) {
        int lastIndexOfDot = currentFqcn.lastIndexOf(".");
        String v1ClassName = currentFqcn.substring(lastIndexOfDot + 1, currentFqcn.length());
        String packagePrefix = currentFqcn.substring(0, lastIndexOfDot);

        String v2ClassName = CodegenNamingUtils.pascalCase(v1ClassName);
        String v2PackagePrefix = packagePrefix.replace(V1_PACKAGE_PREFIX, V2_PACKAGE_PREFIX);
        v2PackagePrefix = checkPackageServiceNameForV2Suffix(v2PackagePrefix);

        if (Stream.of("Abstract", "Amazon", "AWS").anyMatch(v1ClassName::startsWith)) {
            v2ClassName = getV2ClientOrExceptionEquivalent(v1ClassName);
        } else if (v1ClassName.endsWith("Result")) {
            int lastIndex = v1ClassName.lastIndexOf("Result");
            v2ClassName = v1ClassName.substring(0, lastIndex) + "Response";
        }

        return v2PackagePrefix + "." + v2ClassName;
    }

    /**
     * Edge cases in v1 package names
     */
    private static String checkPackageServiceNameForV2Suffix(String v2PackagePrefix) {
        for (Map.Entry<String, String> entry : SPECIAL_MAPPING.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (v2PackagePrefix.contains(key)) {
                return v2PackagePrefix.replace(key, value);
            }
        }
        return v2PackagePrefix;
    }

    public static String getV2ModelPackageWildCardEquivalent(String currentFqcn) {
        int lastIndexOfDot = currentFqcn.lastIndexOf(".");
        String packagePrefix = currentFqcn.substring(0, lastIndexOfDot);
        String v2PackagePrefix = packagePrefix.replace(V1_PACKAGE_PREFIX, V2_PACKAGE_PREFIX);
        v2PackagePrefix = checkPackageServiceNameForV2Suffix(v2PackagePrefix);
        return v2PackagePrefix + ".*";
    }

    private static String getV2ClientOrExceptionEquivalent(String className) {
        if (className.startsWith("Abstract")) {
            className = className.substring(8);
        }
        if (className.startsWith("Amazon")) {
            className = className.substring(6);
        } else if (className.startsWith("AWS")) {
            className = className.substring(3);
        }

        String v2Style = CodegenNamingUtils.pascalCase(className);

        if (className.endsWith("Exception")) {
            return v2Style;
        }

        if (!className.endsWith("Client") && !className.endsWith("Builder")) {
            v2Style = v2Style + "Client";
        }

        return v2Style;
    }
}
