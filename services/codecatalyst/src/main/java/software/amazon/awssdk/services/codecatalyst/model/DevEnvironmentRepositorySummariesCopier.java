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

package software.amazon.awssdk.services.codecatalyst.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.core.util.DefaultSdkAutoConstructList;
import software.amazon.awssdk.core.util.SdkAutoConstructList;

@Generated("software.amazon.awssdk:codegen")
final class DevEnvironmentRepositorySummariesCopier {
    static List<DevEnvironmentRepositorySummary> copy(
            Collection<? extends DevEnvironmentRepositorySummary> devEnvironmentRepositorySummariesParam) {
        List<DevEnvironmentRepositorySummary> list;
        if (devEnvironmentRepositorySummariesParam == null
                || devEnvironmentRepositorySummariesParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<DevEnvironmentRepositorySummary> modifiableList = new ArrayList<>();
            devEnvironmentRepositorySummariesParam.forEach(entry -> {
                modifiableList.add(entry);
            });
            list = Collections.unmodifiableList(modifiableList);
        }
        return list;
    }

    static List<DevEnvironmentRepositorySummary> copyFromBuilder(
            Collection<? extends DevEnvironmentRepositorySummary.Builder> devEnvironmentRepositorySummariesParam) {
        List<DevEnvironmentRepositorySummary> list;
        if (devEnvironmentRepositorySummariesParam == null
                || devEnvironmentRepositorySummariesParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<DevEnvironmentRepositorySummary> modifiableList = new ArrayList<>();
            devEnvironmentRepositorySummariesParam.forEach(entry -> {
                DevEnvironmentRepositorySummary member = entry == null ? null : entry.build();
                modifiableList.add(member);
            });
            list = Collections.unmodifiableList(modifiableList);
        }
        return list;
    }

    static List<DevEnvironmentRepositorySummary.Builder> copyToBuilder(
            Collection<? extends DevEnvironmentRepositorySummary> devEnvironmentRepositorySummariesParam) {
        List<DevEnvironmentRepositorySummary.Builder> list;
        if (devEnvironmentRepositorySummariesParam == null
                || devEnvironmentRepositorySummariesParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<DevEnvironmentRepositorySummary.Builder> modifiableList = new ArrayList<>();
            devEnvironmentRepositorySummariesParam.forEach(entry -> {
                DevEnvironmentRepositorySummary.Builder member = entry == null ? null : entry.toBuilder();
                modifiableList.add(member);
            });
            list = Collections.unmodifiableList(modifiableList);
        }
        return list;
    }
}
