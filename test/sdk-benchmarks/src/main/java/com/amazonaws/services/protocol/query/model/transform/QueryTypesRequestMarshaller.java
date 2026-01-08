/*
 * Copyright 2021-2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.services.protocol.query.model.transform;

import java.util.List;
import java.util.Map;


import com.amazonaws.SdkClientException;
import com.amazonaws.Request;
import com.amazonaws.DefaultRequest;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.services.protocol.query.model.*;
import com.amazonaws.transform.Marshaller;
import com.amazonaws.util.StringUtils;

/**
 * QueryTypesRequest Marshaller
 */


public class QueryTypesRequestMarshaller implements Marshaller<Request<QueryTypesRequest>, QueryTypesRequest> {

    public Request<QueryTypesRequest> marshall(QueryTypesRequest queryTypesRequest) {

        if (queryTypesRequest == null) {
            throw new SdkClientException("Invalid argument passed to marshall(...)");
        }

        Request<QueryTypesRequest> request = new DefaultRequest<QueryTypesRequest>(queryTypesRequest, "AmazonProtocolQuery");
        request.addParameter("Action", "QueryTypes");
        request.addParameter("Version", "2016-03-11");
        request.setHttpMethod(HttpMethodName.POST);

        if (queryTypesRequest.getFlattenedListOfStrings() != null) {
            List<String> flattenedListOfStringsList = queryTypesRequest.getFlattenedListOfStrings();
            if (flattenedListOfStringsList.isEmpty()) {
                request.addParameter("FlattenedListOfStrings", "");
            } else {
                int flattenedListOfStringsListIndex = 1;

                for (String flattenedListOfStringsListValue : flattenedListOfStringsList) {
                    if (flattenedListOfStringsListValue != null) {
                        request.addParameter("FlattenedListOfStrings." + flattenedListOfStringsListIndex,
                                StringUtils.fromString(flattenedListOfStringsListValue));
                    }
                    flattenedListOfStringsListIndex++;
                }
            }
        }

        if (queryTypesRequest.getNonFlattenedListWithLocation() != null) {
            List<String> nonFlattenedListWithLocationList = queryTypesRequest.getNonFlattenedListWithLocation();
            if (nonFlattenedListWithLocationList.isEmpty()) {
                request.addParameter("NonFlattenedListWithLocation", "");
            } else {
                int nonFlattenedListWithLocationListIndex = 1;

                for (String nonFlattenedListWithLocationListValue : nonFlattenedListWithLocationList) {
                    if (nonFlattenedListWithLocationListValue != null) {
                        request.addParameter("NonFlattenedListWithLocation.item." + nonFlattenedListWithLocationListIndex,
                                StringUtils.fromString(nonFlattenedListWithLocationListValue));
                    }
                    nonFlattenedListWithLocationListIndex++;
                }
            }
        }

        if (queryTypesRequest.getFlattenedListOfStructs() != null) {
            List<SimpleStruct> flattenedListOfStructsList = queryTypesRequest.getFlattenedListOfStructs();
            if (flattenedListOfStructsList.isEmpty()) {
                request.addParameter("FlattenedListOfStructs", "");
            } else {
                int flattenedListOfStructsListIndex = 1;

                for (SimpleStruct flattenedListOfStructsListValue : flattenedListOfStructsList) {
                    if (flattenedListOfStructsListValue != null) {

                        if (flattenedListOfStructsListValue.getStringMember() != null) {
                            request.addParameter("FlattenedListOfStructs." + flattenedListOfStructsListIndex + ".StringMember",
                                    StringUtils.fromString(flattenedListOfStructsListValue.getStringMember()));
                        }
                    }
                    flattenedListOfStructsListIndex++;
                }
            }
        }

        if (queryTypesRequest.getFlattenedListWithLocation() != null) {
            List<String> flattenedListWithLocationList = queryTypesRequest.getFlattenedListWithLocation();
            if (flattenedListWithLocationList.isEmpty()) {
                request.addParameter("item", "");
            } else {
                int flattenedListWithLocationListIndex = 1;

                for (String flattenedListWithLocationListValue : flattenedListWithLocationList) {
                    if (flattenedListWithLocationListValue != null) {
                        request.addParameter("item." + flattenedListWithLocationListIndex, StringUtils.fromString(flattenedListWithLocationListValue));
                    }
                    flattenedListWithLocationListIndex++;
                }
            }
        }

        Map<String, String> flattenedMap = queryTypesRequest.getFlattenedMap();
        if (flattenedMap != null) {
            int flattenedMapListIndex = 1;
            for (Map.Entry<String, String> entry : flattenedMap.entrySet()) {
                if (entry != null && entry.getKey() != null) {
                    request.addParameter("FlattenedMap." + flattenedMapListIndex + ".key", StringUtils.fromString(entry.getKey()));
                }
                if (entry != null && entry.getValue() != null) {
                    request.addParameter("FlattenedMap." + flattenedMapListIndex + ".value", StringUtils.fromString(entry.getValue()));
                }
                flattenedMapListIndex++;
            }
        }

        Map<String, String> flattenedMapWithLocation = queryTypesRequest.getFlattenedMapWithLocation();
        if (flattenedMapWithLocation != null) {
            int flattenedMapWithLocationListIndex = 1;
            for (Map.Entry<String, String> entry : flattenedMapWithLocation.entrySet()) {
                if (entry != null && entry.getKey() != null) {
                    request.addParameter("flatmap." + flattenedMapWithLocationListIndex + ".thekey", StringUtils.fromString(entry.getKey()));
                }
                if (entry != null && entry.getValue() != null) {
                    request.addParameter("flatmap." + flattenedMapWithLocationListIndex + ".thevalue", StringUtils.fromString(entry.getValue()));
                }
                flattenedMapWithLocationListIndex++;
            }
        }

        Map<String, String> nonFlattenedMapWithLocation = queryTypesRequest.getNonFlattenedMapWithLocation();
        if (nonFlattenedMapWithLocation != null) {
            int nonFlattenedMapWithLocationListIndex = 1;
            for (Map.Entry<String, String> entry : nonFlattenedMapWithLocation.entrySet()) {
                if (entry != null && entry.getKey() != null) {
                    request.addParameter("themap.entry." + nonFlattenedMapWithLocationListIndex + ".thekey", StringUtils.fromString(entry.getKey()));
                }
                if (entry != null && entry.getValue() != null) {
                    request.addParameter("themap.entry." + nonFlattenedMapWithLocationListIndex + ".thevalue", StringUtils.fromString(entry.getValue()));
                }
                nonFlattenedMapWithLocationListIndex++;
            }
        }

        return request;
    }

}
