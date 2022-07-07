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

import java.io.IOException;
import java.util.List;
import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.imds.internal.OutputParser;

/**
 * The class tests the utility methods provided by Output Parser Class .
 */
@SdkPublicApi
public class OutputParserTest {

    private final static OutputParser OUTPUT_PARSER = new OutputParser();

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

        String result = OUTPUT_PARSER.getStringValueFromJson(jsonResponse,"imageId");
        assertThat(result).isEqualTo("ami-a49665cc");
    }

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

        String[] result = OUTPUT_PARSER.getStringArrayValuesFromJson(jsonResponse,"devpayProductCodes");
        assertThat(result).hasSize(2);
    }

    @Test
    public void get_outputIamCredList_from_list_success() throws IOException {

        String jsonResponse = "test1\ntest2";
        List<String> result = OUTPUT_PARSER.getListfromJson(jsonResponse);
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

        String[] result = OUTPUT_PARSER.getStringArrayValuesFromJson(jsonResponse,"devpayProductCodes1");
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

        String result = OUTPUT_PARSER.getStringValueFromJson(jsonResponse,"imageId1");
        assertThat(result).isNull();
    }

    @Test
    public void get_outputIamCredList_from_list_failure() throws IOException {

        String jsonResponse = "test1-test2";
        List<String> result = OUTPUT_PARSER.getListfromJson(jsonResponse);
        assertThat(result).isNullOrEmpty();
    }
}
