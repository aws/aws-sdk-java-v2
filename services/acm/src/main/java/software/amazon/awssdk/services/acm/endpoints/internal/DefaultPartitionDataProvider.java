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

package software.amazon.awssdk.services.acm.endpoints.internal;

import software.amazon.awssdk.annotations.Generated;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.protocols.jsoncore.JsonNode;
import software.amazon.awssdk.utils.Lazy;

@Generated("software.amazon.awssdk:codegen")
@SdkInternalApi
public final class DefaultPartitionDataProvider implements PartitionDataProvider {
    private static final String DEFAULT_PARTITION_DATA = "{\n" + "  \"partitions\" : [ {\n" + "    \"id\" : \"aws\",\n"
            + "    \"outputs\" : {\n" + "      \"dnsSuffix\" : \"amazonaws.com\",\n"
            + "      \"dualStackDnsSuffix\" : \"api.aws\",\n" + "      \"name\" : \"aws\",\n"
            + "      \"supportsDualStack\" : true,\n" + "      \"supportsFIPS\" : true\n" + "    },\n"
            + "    \"regionRegex\" : \"^(us|eu|ap|sa|ca|me|af)\\\\-\\\\w+\\\\-\\\\d+$\",\n" + "    \"regions\" : {\n"
            + "      \"af-south-1\" : {\n" + "        \"description\" : \"Africa (Cape Town)\"\n" + "      },\n"
            + "      \"ap-east-1\" : {\n" + "        \"description\" : \"Asia Pacific (Hong Kong)\"\n" + "      },\n"
            + "      \"ap-northeast-1\" : {\n" + "        \"description\" : \"Asia Pacific (Tokyo)\"\n" + "      },\n"
            + "      \"ap-northeast-2\" : {\n" + "        \"description\" : \"Asia Pacific (Seoul)\"\n" + "      },\n"
            + "      \"ap-northeast-3\" : {\n" + "        \"description\" : \"Asia Pacific (Osaka)\"\n" + "      },\n"
            + "      \"ap-south-1\" : {\n" + "        \"description\" : \"Asia Pacific (Mumbai)\"\n" + "      },\n"
            + "      \"ap-south-2\" : {\n" + "        \"description\" : \"Asia Pacific (Hyderabad)\"\n" + "      },\n"
            + "      \"ap-southeast-1\" : {\n" + "        \"description\" : \"Asia Pacific (Singapore)\"\n" + "      },\n"
            + "      \"ap-southeast-2\" : {\n" + "        \"description\" : \"Asia Pacific (Sydney)\"\n" + "      },\n"
            + "      \"ap-southeast-3\" : {\n" + "        \"description\" : \"Asia Pacific (Jakarta)\"\n" + "      },\n"
            + "      \"ap-southeast-4\" : {\n" + "        \"description\" : \"Asia Pacific (Melbourne)\"\n" + "      },\n"
            + "      \"aws-global\" : {\n" + "        \"description\" : \"AWS Standard global region\"\n" + "      },\n"
            + "      \"ca-central-1\" : {\n" + "        \"description\" : \"Canada (Central)\"\n" + "      },\n"
            + "      \"eu-central-1\" : {\n" + "        \"description\" : \"Europe (Frankfurt)\"\n" + "      },\n"
            + "      \"eu-central-2\" : {\n" + "        \"description\" : \"Europe (Zurich)\"\n" + "      },\n"
            + "      \"eu-north-1\" : {\n" + "        \"description\" : \"Europe (Stockholm)\"\n" + "      },\n"
            + "      \"eu-south-1\" : {\n" + "        \"description\" : \"Europe (Milan)\"\n" + "      },\n"
            + "      \"eu-south-2\" : {\n" + "        \"description\" : \"Europe (Spain)\"\n" + "      },\n"
            + "      \"eu-west-1\" : {\n" + "        \"description\" : \"Europe (Ireland)\"\n" + "      },\n"
            + "      \"eu-west-2\" : {\n" + "        \"description\" : \"Europe (London)\"\n" + "      },\n"
            + "      \"eu-west-3\" : {\n" + "        \"description\" : \"Europe (Paris)\"\n" + "      },\n"
            + "      \"me-central-1\" : {\n" + "        \"description\" : \"Middle East (UAE)\"\n" + "      },\n"
            + "      \"me-south-1\" : {\n" + "        \"description\" : \"Middle East (Bahrain)\"\n" + "      },\n"
            + "      \"sa-east-1\" : {\n" + "        \"description\" : \"South America (Sao Paulo)\"\n" + "      },\n"
            + "      \"us-east-1\" : {\n" + "        \"description\" : \"US East (N. Virginia)\"\n" + "      },\n"
            + "      \"us-east-2\" : {\n" + "        \"description\" : \"US East (Ohio)\"\n" + "      },\n"
            + "      \"us-west-1\" : {\n" + "        \"description\" : \"US West (N. California)\"\n" + "      },\n"
            + "      \"us-west-2\" : {\n" + "        \"description\" : \"US West (Oregon)\"\n" + "      }\n" + "    }\n"
            + "  }, {\n" + "    \"id\" : \"aws-cn\",\n" + "    \"outputs\" : {\n"
            + "      \"dnsSuffix\" : \"amazonaws.com.cn\",\n"
            + "      \"dualStackDnsSuffix\" : \"api.amazonwebservices.com.cn\",\n" + "      \"name\" : \"aws-cn\",\n"
            + "      \"supportsDualStack\" : true,\n" + "      \"supportsFIPS\" : true\n" + "    },\n"
            + "    \"regionRegex\" : \"^cn\\\\-\\\\w+\\\\-\\\\d+$\",\n" + "    \"regions\" : {\n"
            + "      \"aws-cn-global\" : {\n" + "        \"description\" : \"AWS China global region\"\n" + "      },\n"
            + "      \"cn-north-1\" : {\n" + "        \"description\" : \"China (Beijing)\"\n" + "      },\n"
            + "      \"cn-northwest-1\" : {\n" + "        \"description\" : \"China (Ningxia)\"\n" + "      }\n" + "    }\n"
            + "  }, {\n" + "    \"id\" : \"aws-us-gov\",\n" + "    \"outputs\" : {\n"
            + "      \"dnsSuffix\" : \"amazonaws.com\",\n" + "      \"dualStackDnsSuffix\" : \"api.aws\",\n"
            + "      \"name\" : \"aws-us-gov\",\n" + "      \"supportsDualStack\" : true,\n" + "      \"supportsFIPS\" : true\n"
            + "    },\n" + "    \"regionRegex\" : \"^us\\\\-gov\\\\-\\\\w+\\\\-\\\\d+$\",\n" + "    \"regions\" : {\n"
            + "      \"aws-us-gov-global\" : {\n" + "        \"description\" : \"AWS GovCloud (US) global region\"\n"
            + "      },\n" + "      \"us-gov-east-1\" : {\n" + "        \"description\" : \"AWS GovCloud (US-East)\"\n"
            + "      },\n" + "      \"us-gov-west-1\" : {\n" + "        \"description\" : \"AWS GovCloud (US-West)\"\n"
            + "      }\n" + "    }\n" + "  }, {\n" + "    \"id\" : \"aws-iso\",\n" + "    \"outputs\" : {\n"
            + "      \"dnsSuffix\" : \"c2s.ic.gov\",\n" + "      \"dualStackDnsSuffix\" : \"c2s.ic.gov\",\n"
            + "      \"name\" : \"aws-iso\",\n" + "      \"supportsDualStack\" : false,\n" + "      \"supportsFIPS\" : true\n"
            + "    },\n" + "    \"regionRegex\" : \"^us\\\\-iso\\\\-\\\\w+\\\\-\\\\d+$\",\n" + "    \"regions\" : {\n"
            + "      \"aws-iso-global\" : {\n" + "        \"description\" : \"AWS ISO (US) global region\"\n" + "      },\n"
            + "      \"us-iso-east-1\" : {\n" + "        \"description\" : \"US ISO East\"\n" + "      },\n"
            + "      \"us-iso-west-1\" : {\n" + "        \"description\" : \"US ISO WEST\"\n" + "      }\n" + "    }\n"
            + "  }, {\n" + "    \"id\" : \"aws-iso-b\",\n" + "    \"outputs\" : {\n"
            + "      \"dnsSuffix\" : \"sc2s.sgov.gov\",\n" + "      \"dualStackDnsSuffix\" : \"sc2s.sgov.gov\",\n"
            + "      \"name\" : \"aws-iso-b\",\n" + "      \"supportsDualStack\" : false,\n" + "      \"supportsFIPS\" : true\n"
            + "    },\n" + "    \"regionRegex\" : \"^us\\\\-isob\\\\-\\\\w+\\\\-\\\\d+$\",\n" + "    \"regions\" : {\n"
            + "      \"aws-iso-b-global\" : {\n" + "        \"description\" : \"AWS ISOB (US) global region\"\n" + "      },\n"
            + "      \"us-isob-east-1\" : {\n" + "        \"description\" : \"US ISOB East (Ohio)\"\n" + "      }\n" + "    }\n"
            + "  }, {\n" + "    \"id\" : \"aws-iso-e\",\n" + "    \"outputs\" : {\n"
            + "      \"dnsSuffix\" : \"cloud.adc-e.uk\",\n" + "      \"dualStackDnsSuffix\" : \"cloud.adc-e.uk\",\n"
            + "      \"name\" : \"aws-iso-e\",\n" + "      \"supportsDualStack\" : false,\n" + "      \"supportsFIPS\" : true\n"
            + "    },\n" + "    \"regionRegex\" : \"^eu\\\\-isoe\\\\-\\\\w+\\\\-\\\\d+$\",\n" + "    \"regions\" : { }\n"
            + "  } ],\n" + "  \"version\" : \"1.1\"\n" + "}";

    private static final Lazy<Partitions> PARTITIONS = new Lazy<>(DefaultPartitionDataProvider::doLoadPartitions);;

    @Override
    public Partitions loadPartitions() {
        return PARTITIONS.getValue();
    }

    private static Partitions doLoadPartitions() {
        return Partitions.fromNode(JsonNode.parser().parse(DEFAULT_PARTITION_DATA));
    }
}
