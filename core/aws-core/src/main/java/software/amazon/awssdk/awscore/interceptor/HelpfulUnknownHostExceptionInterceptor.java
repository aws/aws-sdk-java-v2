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

package software.amazon.awssdk.awscore.interceptor;

import java.net.UnknownHostException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.regions.PartitionMetadata;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionMetadata;
import software.amazon.awssdk.regions.ServiceMetadata;
import software.amazon.awssdk.regions.ServicePartitionMetadata;

/**
 * This interceptor will monitor for {@link UnknownHostException}s and provide the customer with additional information they can
 * use to debug or fix the problem.
 */
@SdkInternalApi
public final class HelpfulUnknownHostExceptionInterceptor implements ExecutionInterceptor {
    @Override
    public Throwable modifyException(Context.FailedExecution context, ExecutionAttributes executionAttributes) {
        if (!hasCause(context.exception(), UnknownHostException.class)) {
            return context.exception();
        }

        StringBuilder error = new StringBuilder();
        error.append("Received an UnknownHostException when attempting to interact with a service. See cause for the "
                     + "exact endpoint that is failing to resolve. ");

        Optional<String> globalRegionErrorDetails = getGlobalRegionErrorDetails(executionAttributes);

        if (globalRegionErrorDetails.isPresent()) {
            error.append(globalRegionErrorDetails.get());
        } else {
            error.append("If this is happening on an endpoint that previously worked, there may be a network connectivity "
                         + "issue or your DNS cache could be storing endpoints for too long.");
        }

        return SdkClientException.builder().message(error.toString()).cause(context.exception()).build();
    }

    /**
     * If the customer is interacting with a global service (one with a single endpoint/region for an entire partition), this
     * will return error details that can instruct the customer on how to configure their client for success.
     */
    private Optional<String> getGlobalRegionErrorDetails(ExecutionAttributes executionAttributes) {
        Region clientRegion = clientRegion(executionAttributes);
        if (clientRegion.isGlobalRegion()) {
            return Optional.empty();
        }

        List<ServicePartitionMetadata> globalPartitionsForService = globalPartitionsForService(executionAttributes);
        if (globalPartitionsForService.isEmpty()) {
            return Optional.empty();
        }

        String clientPartition = Optional.ofNullable(clientRegion.metadata())
                                         .map(RegionMetadata::partition)
                                         .map(PartitionMetadata::id)
                                         .orElse(null);

        Optional<Region> globalRegionForClientRegion =
            globalPartitionsForService.stream()
                                      .filter(p -> p.partition().id().equals(clientPartition))
                                      .findAny()
                                      .flatMap(ServicePartitionMetadata::globalRegion);

        if (!globalRegionForClientRegion.isPresent()) {
            String globalRegionsForThisService = globalPartitionsForService.stream()
                                                                           .map(ServicePartitionMetadata::globalRegion)
                                                                           .filter(Optional::isPresent)
                                                                           .map(Optional::get)
                                                                           .filter(Region::isGlobalRegion)
                                                                           .map(Region::id)
                                                                           .collect(Collectors.joining("/"));

            return Optional.of("This specific service may be a global service, in which case you should configure a global "
                               + "region like " + globalRegionsForThisService + " on the client.");
        }

        Region globalRegion = globalRegionForClientRegion.get();

        return Optional.of("This specific service is global in the same partition as the region configured on this client ("
                           + clientRegion + "). If this is the first time you're trying to talk to this service in this region, "
                           + "you should try configuring the global region on your client, instead: " + globalRegion);
    }

    /**
     * Retrieve the region configured on the client.
     */
    private Region clientRegion(ExecutionAttributes executionAttributes) {
        return executionAttributes.getAttribute(AwsExecutionAttribute.AWS_REGION);
    }

    /**
     * Retrieve all global partitions for the AWS service that we're interacting with.
     */
    private List<ServicePartitionMetadata> globalPartitionsForService(ExecutionAttributes executionAttributes) {
        return ServiceMetadata.of(executionAttributes.getAttribute(AwsExecutionAttribute.ENDPOINT_PREFIX))
                              .servicePartitions()
                              .stream()
                              .filter(sp -> sp.globalRegion().isPresent())
                              .collect(Collectors.toList());
    }

    private boolean hasCause(Throwable thrown, Class<? extends Throwable> cause) {
        if (thrown == null) {
            return false;
        }

        if (cause.isAssignableFrom(thrown.getClass())) {
            return true;
        }

        return hasCause(thrown.getCause(), cause);
    }
}
