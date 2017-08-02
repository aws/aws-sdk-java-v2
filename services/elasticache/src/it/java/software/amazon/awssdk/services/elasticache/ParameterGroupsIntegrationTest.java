/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.elasticache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.services.elasticache.model.CacheNodeTypeSpecificParameter;
import software.amazon.awssdk.services.elasticache.model.CacheParameterGroup;
import software.amazon.awssdk.services.elasticache.model.CreateCacheParameterGroupRequest;
import software.amazon.awssdk.services.elasticache.model.DeleteCacheParameterGroupRequest;
import software.amazon.awssdk.services.elasticache.model.DescribeCacheParameterGroupsRequest;
import software.amazon.awssdk.services.elasticache.model.DescribeCacheParameterGroupsResponse;
import software.amazon.awssdk.services.elasticache.model.DescribeCacheParametersRequest;
import software.amazon.awssdk.services.elasticache.model.DescribeCacheParametersResponse;
import software.amazon.awssdk.services.elasticache.model.DescribeEngineDefaultParametersRequest;
import software.amazon.awssdk.services.elasticache.model.EngineDefaults;
import software.amazon.awssdk.services.elasticache.model.ModifyCacheParameterGroupRequest;
import software.amazon.awssdk.services.elasticache.model.ModifyCacheParameterGroupResponse;
import software.amazon.awssdk.services.elasticache.model.Parameter;
import software.amazon.awssdk.services.elasticache.model.ParameterNameValue;
import software.amazon.awssdk.services.elasticache.model.ResetCacheParameterGroupRequest;
import software.amazon.awssdk.services.elasticache.model.ResetCacheParameterGroupResponse;

public class ParameterGroupsIntegrationTest extends ElastiCacheIntegrationTestBase {

    private static final String DESCRIPTION = "Java SDK integ test param group";
    private static final String CACHE_PARAMETER_GROUP_FAMILY = "memcached1.4";

    private String cacheParameterGroupName;

    /** Releases all resources created by tests. */
    @After
    public void tearDown() throws Exception {
        if (cacheParameterGroupName != null) {
            try {
                elasticache.deleteCacheParameterGroup(
                        DeleteCacheParameterGroupRequest.builder().cacheParameterGroupName(cacheParameterGroupName).build());
            } catch (Exception e) {
                // Ignored or expected.
            }
        }
    }

    /** Tests that we can call the parameter group operations in the ElastiCache API. */
    @Test
    public void testParameterGroupOperations() throws Exception {

        // Describe Engine Default Parameters
        EngineDefaults engineDefaults = elasticache
                .describeEngineDefaultParameters(
                        DescribeEngineDefaultParametersRequest.builder().cacheParameterGroupFamily(CACHE_PARAMETER_GROUP_FAMILY)
                                                              .build()).engineDefaults();
        assertTrue(engineDefaults.cacheNodeTypeSpecificParameters().size() > 0);
        CacheNodeTypeSpecificParameter cacheNodeParameter = engineDefaults.cacheNodeTypeSpecificParameters().get(0);
        assertNotEmpty(cacheNodeParameter.parameterName());
        assertTrue(cacheNodeParameter.cacheNodeTypeSpecificValues().size() > 0);
        assertEquals(CACHE_PARAMETER_GROUP_FAMILY, engineDefaults.cacheParameterGroupFamily());
        assertTrue(engineDefaults.parameters().size() > 0);
        Parameter parameter = engineDefaults.parameters().get(0);
        assertNotEmpty(parameter.parameterName());
        assertNotEmpty(parameter.parameterValue());


        // Create Cache Parameter Group
        cacheParameterGroupName = "java-sdk-integ-test-" + System.currentTimeMillis();
        CacheParameterGroup cacheParameterGroup = elasticache.createCacheParameterGroup(
                CreateCacheParameterGroupRequest.builder().cacheParameterGroupName(cacheParameterGroupName)
                                                .cacheParameterGroupFamily(CACHE_PARAMETER_GROUP_FAMILY).description(DESCRIPTION)
                                                .build()).cacheParameterGroup();
        assertEquals(CACHE_PARAMETER_GROUP_FAMILY, cacheParameterGroup.cacheParameterGroupFamily());
        assertEquals(cacheParameterGroupName, cacheParameterGroup.cacheParameterGroupName());
        assertEquals(DESCRIPTION, cacheParameterGroup.description());


        // Describe Cache Parameters
        DescribeCacheParametersResponse describeCacheParameters =
                elasticache.describeCacheParameters(
                        DescribeCacheParametersRequest.builder().cacheParameterGroupName(cacheParameterGroupName).build());
        assertTrue(describeCacheParameters.cacheNodeTypeSpecificParameters().size() > 0);
        cacheNodeParameter = describeCacheParameters.cacheNodeTypeSpecificParameters().get(0);
        assertNotEmpty(cacheNodeParameter.parameterName());
        assertTrue(cacheNodeParameter.cacheNodeTypeSpecificValues().size() > 0);
        assertTrue(describeCacheParameters.parameters().size() > 0);
        parameter = describeCacheParameters.parameters().get(0);
        assertNotEmpty(parameter.parameterName());
        assertNotEmpty(parameter.parameterValue());


        // Modify Cache Parameter Group
        List<ParameterNameValue> paramsToModify = new ArrayList<ParameterNameValue>();
        paramsToModify.add(ParameterNameValue.builder().parameterName("max_item_size").parameterValue("100000").build());
        ModifyCacheParameterGroupResponse modifyCacheParameterGroup = elasticache
                .modifyCacheParameterGroup(
                        ModifyCacheParameterGroupRequest.builder().cacheParameterGroupName(cacheParameterGroupName)
                                                        .parameterNameValues(paramsToModify).build());
        assertEquals(cacheParameterGroupName, modifyCacheParameterGroup.cacheParameterGroupName());


        // Reset Cache Parameter Group
        List<ParameterNameValue> paramsToReset = new ArrayList<ParameterNameValue>();
        paramsToReset.add(ParameterNameValue.builder().parameterName("binding_protocol").build());
        ResetCacheParameterGroupResponse resetCacheParameterGroup =
                elasticache.resetCacheParameterGroup(
                        ResetCacheParameterGroupRequest.builder().cacheParameterGroupName(cacheParameterGroupName)
                                                       .parameterNameValues(paramsToReset).build());
        assertEquals(cacheParameterGroupName, resetCacheParameterGroup.cacheParameterGroupName());


        // Describe Cache Parameter Groups
        DescribeCacheParameterGroupsResponse describeCacheParameterGroups =
                elasticache.describeCacheParameterGroups(
                        DescribeCacheParameterGroupsRequest.builder().cacheParameterGroupName(cacheParameterGroupName).build());
        assertEquals(1, describeCacheParameterGroups.cacheParameterGroups().size());
        CacheParameterGroup parameterGroup = describeCacheParameterGroups.cacheParameterGroups().get(0);
        assertEquals(CACHE_PARAMETER_GROUP_FAMILY, parameterGroup.cacheParameterGroupFamily());
        assertEquals(cacheParameterGroupName, parameterGroup.cacheParameterGroupName());
        assertEquals(DESCRIPTION, parameterGroup.description());


        // Delete Cache Parameter Group
        elasticache.deleteCacheParameterGroup(
                DeleteCacheParameterGroupRequest.builder().cacheParameterGroupName(cacheParameterGroupName).build());
    }

}
