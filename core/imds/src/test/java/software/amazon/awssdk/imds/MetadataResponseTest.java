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

package software.amazon.awssdk.imds;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.Test;
import software.amazon.awssdk.imds.internal.MetadataResponse;

/**
 * The class tests the utility methods provided by MetadataResponse Class .
 */
public class MetadataResponseTest {

    @Test
    public void get_devpayProductCodes_from_JsonResponse_success() throws IOException {

        String jsonResponse = "{"
                              + "\"pendingTime\":\"2014-08-07T22:07:46Z\","
                              + "\"instanceType\":\"m1.small\","
                              + "\"imageId\":\"ami-a49665cc\","
                              + "\"instanceId\":\"i-6b2de041\","
                              + "\"billingProducts\":[\"foo\"],"
                              + "\"architecture\":\"x86_64\","
                              + "\"accountId\":\"599169622985\","
                              + "\"kernelId\":\"aki-919dcaf8\","
                              + "\"ramdiskId\":\"baz\","
                              + "\"region\":\"us-east-1\","
                              + "\"version\":\"2010-08-31\","
                              + "\"availabilityZone\":\"us-east-1b\","
                              + "\"privateIp\":\"10.201.215.38\","
                              + "\"devpayProductCodes\":[\"bar\",\"foo\"],"
                              + "\"marketplaceProductCodes\":[\"qaz\"]"
                              + "}";

        MetadataResponse metadataResponse = new MetadataResponse(jsonResponse);
        String[] result = metadataResponse.getStringArrayValuesFromJson("devpayProductCodes");
        assertThat(result).hasSize(2);
    }

    @Test
    public void get_imageId_from_JsonResponse_success() throws IOException {

        String jsonResponse = "{"
                              + "\"pendingTime\":\"2014-08-07T22:07:46Z\","
                              + "\"instanceType\":\"m1.small\","
                              + "\"imageId\":\"ami-a49665cc\","
                              + "\"instanceId\":\"i-6b2de041\","
                              + "\"billingProducts\":[\"foo\"],"
                              + "\"architecture\":\"x86_64\","
                              + "\"accountId\":\"599169622985\","
                              + "\"kernelId\":\"aki-919dcaf8\","
                              + "\"ramdiskId\":\"baz\","
                              + "\"region\":\"us-east-1\","
                              + "\"version\":\"2010-08-31\","
                              + "\"availabilityZone\":\"us-east-1b\","
                              + "\"privateIp\":\"10.201.215.38\","
                              + "\"devpayProductCodes\":[\"bar\"],"
                              + "\"marketplaceProductCodes\":[\"qaz\"]"
                              + "}";

        MetadataResponse metadataResponse = new MetadataResponse(jsonResponse);
        String result = metadataResponse.getStringValueFromJson("imageId");
        assertThat(result).isEqualTo("ami-a49665cc");
    }

    @Test
    public void check_asString_success() throws IOException {

        String response = "foobar";

        MetadataResponse metadataResponse = new MetadataResponse(response);
        String result = metadataResponse.asString();
        assertThat(result).isEqualTo(response);

    }

    @Test
    public void check_asList_success() throws IOException {

        String response = "sai\ntest";

        MetadataResponse metadataResponse = new MetadataResponse(response);
        List<String> result = metadataResponse.asList();
        assertThat(result).hasSize(2);

    }

    @Test
    public void get_devpayProductCodes_from_JsonResponse_failure() throws IOException {

        String jsonResponse = "{"
                              + "\"pendingTime\":\"2014-08-07T22:07:46Z\","
                              + "\"instanceType\":\"m1.small\","
                              + "\"imageId\":\"ami-a49665cc\","
                              + "\"instanceId\":\"i-6b2de041\","
                              + "\"billingProducts\":[\"foo\"],"
                              + "\"architecture\":\"x86_64\","
                              + "\"accountId\":\"599169622985\","
                              + "\"kernelId\":\"aki-919dcaf8\","
                              + "\"ramdiskId\":\"baz\","
                              + "\"region\":\"us-east-1\","
                              + "\"version\":\"2010-08-31\","
                              + "\"availabilityZone\":\"us-east-1b\","
                              + "\"privateIp\":\"10.201.215.38\","
                              + "\"devpayProductCodes\":[\"bar\",\"foo\"],"
                              + "\"marketplaceProductCodes\":[\"qaz\"]"
                              + "}";

        MetadataResponse metadataResponse = new MetadataResponse(jsonResponse);
        String[] result = metadataResponse.getStringArrayValuesFromJson("devpayProductCodes1");
        assertThat(result).isNullOrEmpty();
    }

    @Test
    public void get_imageId_from_JsonResponse_failure() throws IOException {

        String jsonResponse = "{"
                              + "\"pendingTime\":\"2014-08-07T22:07:46Z\","
                              + "\"instanceType\":\"m1.small\","
                              + "\"imageId\":\"ami-a49665cc\","
                              + "\"instanceId\":\"i-6b2de041\","
                              + "\"billingProducts\":[\"foo\"],"
                              + "\"architecture\":\"x86_64\","
                              + "\"accountId\":\"599169622985\","
                              + "\"kernelId\":\"aki-919dcaf8\","
                              + "\"ramdiskId\":\"baz\","
                              + "\"region\":\"us-east-1\","
                              + "\"version\":\"2010-08-31\","
                              + "\"availabilityZone\":\"us-east-1b\","
                              + "\"privateIp\":\"10.201.215.38\","
                              + "\"devpayProductCodes\":[\"bar\",\"foo\"],"
                              + "\"marketplaceProductCodes\":[\"qaz\"]"
                              + "}";

        MetadataResponse metadataResponse = new MetadataResponse(jsonResponse);
        String result = metadataResponse.getStringValueFromJson("imageId1");
        assertThat(result).isNull();
    }

    @Test
    public void get_outputIamCredList_from_list_failure() throws IOException {

        String response = "test1-test2";

        MetadataResponse metadataResponse = new MetadataResponse(response);
        List<String> result = metadataResponse.asList();
        assertThat(result).isNullOrEmpty();

    }
}
