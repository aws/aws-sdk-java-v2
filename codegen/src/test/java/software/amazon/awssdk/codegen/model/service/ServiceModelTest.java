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

package software.amazon.awssdk.codegen.model.service;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;

public class ServiceModelTest {

    private static final String ERROR_CODE = "fooErrorCode";
    private static final String QUERY_ERROR_CODE = "queryErrorCode";
    private final AwsQueryCompatible model = new AwsQueryCompatible(ERROR_CODE);
    private final Map<String, AwsQueryCompatible> awsQueryCompatible = new HashMap() {
        {
            put(QUERY_ERROR_CODE, model);
        }
    };

    @Test
    public void validateAwsQueryCompatible() {


        ServiceMetadata metadata = new ServiceMetadata();
        metadata.setProtocol(Protocol.REST_JSON.getValue());
        metadata.setServiceId("empty-service");
        metadata.setSignatureVersion("V4");

        ServiceModel testModel = new ServiceModel(metadata,
                                               Collections.emptyMap(),
                                               Collections.emptyMap(),
                                               Collections.emptyMap(),
                                               awsQueryCompatible);

        assertEquals(awsQueryCompatible, testModel.getAwsQueryCompatible());
        assertNotNull(testModel.getAwsQueryCompatible().get(QUERY_ERROR_CODE));
        AwsQueryCompatible awsQueryCompatible = testModel.getAwsQueryCompatible().get(QUERY_ERROR_CODE);
        assertEquals(ERROR_CODE, awsQueryCompatible.getErrorCode());
    }
}
