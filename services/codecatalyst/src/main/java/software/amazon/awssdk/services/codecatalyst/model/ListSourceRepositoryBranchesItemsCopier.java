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
final class ListSourceRepositoryBranchesItemsCopier {
    static List<ListSourceRepositoryBranchesItem> copy(
            Collection<? extends ListSourceRepositoryBranchesItem> listSourceRepositoryBranchesItemsParam) {
        List<ListSourceRepositoryBranchesItem> list;
        if (listSourceRepositoryBranchesItemsParam == null
                || listSourceRepositoryBranchesItemsParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<ListSourceRepositoryBranchesItem> modifiableList = new ArrayList<>();
            listSourceRepositoryBranchesItemsParam.forEach(entry -> {
                modifiableList.add(entry);
            });
            list = Collections.unmodifiableList(modifiableList);
        }
        return list;
    }

    static List<ListSourceRepositoryBranchesItem> copyFromBuilder(
            Collection<? extends ListSourceRepositoryBranchesItem.Builder> listSourceRepositoryBranchesItemsParam) {
        List<ListSourceRepositoryBranchesItem> list;
        if (listSourceRepositoryBranchesItemsParam == null
                || listSourceRepositoryBranchesItemsParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<ListSourceRepositoryBranchesItem> modifiableList = new ArrayList<>();
            listSourceRepositoryBranchesItemsParam.forEach(entry -> {
                ListSourceRepositoryBranchesItem member = entry == null ? null : entry.build();
                modifiableList.add(member);
            });
            list = Collections.unmodifiableList(modifiableList);
        }
        return list;
    }

    static List<ListSourceRepositoryBranchesItem.Builder> copyToBuilder(
            Collection<? extends ListSourceRepositoryBranchesItem> listSourceRepositoryBranchesItemsParam) {
        List<ListSourceRepositoryBranchesItem.Builder> list;
        if (listSourceRepositoryBranchesItemsParam == null
                || listSourceRepositoryBranchesItemsParam instanceof SdkAutoConstructList) {
            list = DefaultSdkAutoConstructList.getInstance();
        } else {
            List<ListSourceRepositoryBranchesItem.Builder> modifiableList = new ArrayList<>();
            listSourceRepositoryBranchesItemsParam.forEach(entry -> {
                ListSourceRepositoryBranchesItem.Builder member = entry == null ? null : entry.toBuilder();
                modifiableList.add(member);
            });
            list = Collections.unmodifiableList(modifiableList);
        }
        return list;
    }
}
