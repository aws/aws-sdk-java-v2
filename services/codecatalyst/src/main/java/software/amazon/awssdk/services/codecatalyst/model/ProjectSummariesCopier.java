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
final class ProjectSummariesCopier {
    static List<ProjectSummary> copy(Collection<? extends ProjectSummary> projectSummariesParam) {
        List<ProjectSummary> list;
        if (projectSummariesParam == null || projectSummariesParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<ProjectSummary> modifiableList = new ArrayList<>();
            projectSummariesParam.forEach(entry -> {
                modifiableList.add(entry);
            });
            list = Collections.unmodifiableList(modifiableList);
        }
        return list;
    }

    static List<ProjectSummary> copyFromBuilder(Collection<? extends ProjectSummary.Builder> projectSummariesParam) {
        List<ProjectSummary> list;
        if (projectSummariesParam == null || projectSummariesParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<ProjectSummary> modifiableList = new ArrayList<>();
            projectSummariesParam.forEach(entry -> {
                ProjectSummary member = entry == null ? null : entry.build();
                modifiableList.add(member);
            });
            list = Collections.unmodifiableList(modifiableList);
        }
        return list;
    }

    static List<ProjectSummary.Builder> copyToBuilder(Collection<? extends ProjectSummary> projectSummariesParam) {
        List<ProjectSummary.Builder> list;
        if (projectSummariesParam == null || projectSummariesParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<ProjectSummary.Builder> modifiableList = new ArrayList<>();
            projectSummariesParam.forEach(entry -> {
                ProjectSummary.Builder member = entry == null ? null : entry.toBuilder();
                modifiableList.add(member);
            });
            list = Collections.unmodifiableList(modifiableList);
        }
        return list;
    }
}
