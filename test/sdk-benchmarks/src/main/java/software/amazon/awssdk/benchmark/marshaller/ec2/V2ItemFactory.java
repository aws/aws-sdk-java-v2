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

package software.amazon.awssdk.benchmark.marshaller.ec2;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.ElasticGpuSpecification;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterfaceSpecification;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.VolumeType;

final class V2ItemFactory {
    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyz";

    private static final Random RNG = new Random();

    RunInstancesRequest tiny() {
        return RunInstancesRequest.builder()
                                  .additionalInfo(randomS(50))
                                  .disableApiTermination(true)
                                  .maxCount(5)
                                  .build();
    }

    RunInstancesRequest small() {
        return RunInstancesRequest.builder()
                                  .additionalInfo(randomS(50))
                                  .disableApiTermination(true)
                                  .maxCount(5)
                                  .blockDeviceMappings(blockDeviceMappings(3))
                                  .cpuOptions(c -> c.coreCount(5).threadsPerCore(5))
                                  .elasticGpuSpecification(elasticGpuSpecification())
                                  .networkInterfaces(networkInterfaces(3))
                                  .build();
    }

    RunInstancesRequest huge() {
        return RunInstancesRequest.builder()
                                  .additionalInfo(randomS(50))
                                  .disableApiTermination(true)
                                  .maxCount(5)
                                  .blockDeviceMappings(blockDeviceMappings(100))
                                  .cpuOptions(c -> c.coreCount(5).threadsPerCore(5))
                                  .elasticGpuSpecification(elasticGpuSpecification())
                                  .networkInterfaces(networkInterfaces(100))
                                  .build();
    }

    private static ElasticGpuSpecification elasticGpuSpecification() {
        return ElasticGpuSpecification.builder()
                                      .type(randomS(50))
                                      .build();
    }

    private static InstanceNetworkInterfaceSpecification networkInterface() {
        return InstanceNetworkInterfaceSpecification.builder()
                                                    .associatePublicIpAddress(true)
                                                    .deleteOnTermination(true)
                                                    .deviceIndex(50)
                                                    .groups(randomS(50), randomS(50), randomS(50))
                                                    .description(randomS(50))
                                                    .build();
    }

    private static List<InstanceNetworkInterfaceSpecification> networkInterfaces(int num) {
        return IntStream.of(num)
                        .mapToObj(i -> networkInterface())
                        .collect(Collectors.toList());
    }

    private static BlockDeviceMapping blockDeviceMapping() {
        return BlockDeviceMapping.builder()
                                 .deviceName(randomS(100))
                                 .virtualName(randomS(50))
                                 .noDevice(randomS(50))
                                 .ebs(e -> e.deleteOnTermination(true)
                                            .encrypted(false)
                                            .iops(50)
                                            .kmsKeyId(randomS(50))
                                            .snapshotId(randomS(50))
                                            .volumeSize(50)
                                            .volumeType(VolumeType.GP2))
                                 .build();
    }

    private static List<BlockDeviceMapping> blockDeviceMappings(int num) {
        return IntStream.of(num)
                        .mapToObj(i -> blockDeviceMapping())
                        .collect(Collectors.toList());
    }

    private static String randomS(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; ++i) {
            sb.append(ALPHA.charAt(RNG.nextInt(ALPHA.length())));
        }
        return sb.toString();
    }
}
