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

import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.CpuOptionsRequest;
import com.amazonaws.services.ec2.model.EbsBlockDevice;
import com.amazonaws.services.ec2.model.ElasticGpuSpecification;
import com.amazonaws.services.ec2.model.InstanceNetworkInterfaceSpecification;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.VolumeType;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class V1ItemFactory {
    private static final String ALPHA = "abcdefghijklmnopqrstuvwxyz";

    private static final Random RNG = new Random();

    RunInstancesRequest tiny() {
        return new RunInstancesRequest()
            .withAdditionalInfo(randomS(50))
            .withDisableApiTermination(true)
            .withMaxCount(5);
    }

    RunInstancesRequest small() {
        return new RunInstancesRequest()
            .withAdditionalInfo(randomS(50))
            .withDisableApiTermination(true)
            .withMaxCount(5)
            .withBlockDeviceMappings(blockDeviceMappings(3))
            .withCpuOptions(new CpuOptionsRequest().withCoreCount(5).withThreadsPerCore(5))
            .withElasticGpuSpecification(new ElasticGpuSpecification().withType(randomS(50)))
            .withNetworkInterfaces(networkInterfaces(3));
    }

    RunInstancesRequest huge() {
        return new RunInstancesRequest()
            .withAdditionalInfo(randomS(50))
            .withDisableApiTermination(true)
            .withMaxCount(5)
            .withBlockDeviceMappings(blockDeviceMappings(100))
            .withCpuOptions(new CpuOptionsRequest().withCoreCount(5).withThreadsPerCore(5))
            .withElasticGpuSpecification(new ElasticGpuSpecification().withType(randomS(50)))
            .withNetworkInterfaces(networkInterfaces(100));
    }

    static InstanceNetworkInterfaceSpecification networkInterface() {
        return new InstanceNetworkInterfaceSpecification()
            .withAssociatePublicIpAddress(true)
            .withDeleteOnTermination(true)
            .withDeviceIndex(50)
            .withGroups(randomS(50), randomS(50), randomS(50))
            .withDescription(randomS(50));
    }

    static List<InstanceNetworkInterfaceSpecification> networkInterfaces(int num) {
        return IntStream.of(num)
                        .mapToObj(i -> networkInterface())
                        .collect(Collectors.toList());
    }

    private static BlockDeviceMapping blockDeviceMapping() {
        return new BlockDeviceMapping()
            .withDeviceName(randomS(100))
            .withVirtualName(randomS(50))
            .withNoDevice(randomS(50))
            .withEbs(new EbsBlockDevice().withDeleteOnTermination(true)
                                         .withEncrypted(false)
                                         .withIops(50)
                                         .withKmsKeyId(randomS(50))
                                         .withSnapshotId(randomS(50))
                                         .withVolumeSize(50)
                                         .withVolumeType(VolumeType.Gp2));
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
