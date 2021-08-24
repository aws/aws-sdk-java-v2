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

package software.amazon.awssdk.codegen.poet.batchmanager;

import com.squareup.javapoet.ClassName;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import software.amazon.awssdk.codegen.model.config.customization.BatchFunctionsTypes;
import software.amazon.awssdk.codegen.model.config.customization.BatchManager;

/**
 * Conenvience methods to access the batch types and methods listed in the customization.config
 */
public final class BatchTypesUtils {

    private BatchTypesUtils() {
    }

    public static ClassName getRequestType(Map.Entry<String, BatchFunctionsTypes> batchFunctions, String modelPackage) {
        String requestMethodName = capitalizeRequestMethodName(batchFunctions.getKey()) + "Request";
        return getType(requestMethodName, modelPackage);
    }

    public static ClassName getResponseType(Map.Entry<String, BatchFunctionsTypes> batchFunctions, String modelPackage) {
        String requestMethodName = capitalizeRequestMethodName(batchFunctions.getKey()) + "Response";
        return getType(requestMethodName, modelPackage);
    }

    public static String getBatchRequestMethod(Map.Entry<String, BatchFunctionsTypes> batchFunctions) {
        return batchFunctions.getValue().getBatchMethod();
    }

    public static ClassName getBatchRequestType(Map.Entry<String, BatchFunctionsTypes> batchFunctions, String modelPackage) {
        String requestMethodName = capitalizeRequestMethodName(getBatchRequestMethod(batchFunctions)) + "Request";
        return getType(requestMethodName, modelPackage);
    }

    public static ClassName getBatchRequestEntryType(Map.Entry<String, BatchFunctionsTypes> batchFunctions, String modelPackage) {
        String requestMethodName = batchFunctions.getValue().getBatchRequestEntry();
        return getType(requestMethodName, modelPackage);
    }

    public static ClassName getBatchResponseType(Map.Entry<String, BatchFunctionsTypes> batchFunctions, String modelPackage) {
        String requestMethodName = capitalizeRequestMethodName(batchFunctions.getValue().getBatchMethod()) + "Response";
        return getType(requestMethodName, modelPackage);
    }

    public static ClassName getSuccessBatchEntry(Map.Entry<String, BatchFunctionsTypes> batchFunctions, String modelPackage) {
        return getType(batchFunctions.getValue().getSuccessBatchEntry(), modelPackage);
    }

    public static ClassName getErrorBatchEntry(BatchManager batchManager, String modelPackage) {
        return getType(batchManager.getErrorBatchEntry(), modelPackage);
    }

    public static String getDestinationMethod(Map.Entry<String, BatchFunctionsTypes> batchFunctions) {
        return batchFunctions.getValue().getDestinationMethod();
    }

    public static String getSuccessEntriesMethod(BatchManager batchManager) {
        return batchManager.getSuccessEntriesMethod();
    }

    public static String getErrorEntriesMethod(BatchManager batchManager) {
        return batchManager.getErrorEntriesMethod();
    }

    public static String getErrorCodeMethod(BatchManager batchManager) {
        return batchManager.getErrorCodeMethod();
    }

    public static List<String> getBatchKeyMethods(Map.Entry<String, BatchFunctionsTypes> batchFunctions) {
        return batchFunctions.getValue().getBatchKeyMethods();
    }

    public static ClassName getType(String type, String modelPackage) {
        return ClassName.get(modelPackage, type);
    }

    private static String capitalizeRequestMethodName(String methodName) {
        return methodName.substring(0, 1).toUpperCase(Locale.ROOT) + methodName.substring(1);
    }

}
