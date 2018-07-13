/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.ec2.transform;

import static java.util.stream.Collectors.toList;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import software.amazon.awssdk.core.SdkRequest;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSpotInstanceRequestsResponse;
import software.amazon.awssdk.services.ec2.model.GroupIdentifier;
import software.amazon.awssdk.services.ec2.model.ImportKeyPairRequest;
import software.amazon.awssdk.services.ec2.model.LaunchSpecification;
import software.amazon.awssdk.services.ec2.model.ModifyReservedInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RequestSpotInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RequestSpotInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.SpotInstanceRequest;
import software.amazon.awssdk.utils.BinaryUtils;

public class EC2Interceptor implements ExecutionInterceptor {
    @Override
    public SdkHttpFullRequest modifyHttpRequest(Context.ModifyHttpRequest context, ExecutionAttributes executionAttributes) {
        SdkHttpFullRequest request = context.httpRequest();
        SdkRequest originalRequest = context.request();
        SdkHttpFullRequest.Builder mutableRequest = request.toBuilder();
        if (originalRequest instanceof ImportKeyPairRequest) {
            ImportKeyPairRequest importKeyPairRequest = (ImportKeyPairRequest) originalRequest;
            String publicKeyMaterial = importKeyPairRequest.publicKeyMaterial();
            String encodedKeyMaterial = BinaryUtils.toBase64(publicKeyMaterial.getBytes(StandardCharsets.UTF_8));
            mutableRequest.putRawQueryParameter("PublicKeyMaterial", encodedKeyMaterial);
        } else if (originalRequest instanceof RequestSpotInstancesRequest) {
            // Request -> Query string marshalling for RequestSpotInstancesRequest is a little tricky since
            // the query string params follow a different form than the XML responses, so we manually set the parameters here.
            RequestSpotInstancesRequest requestSpotInstancesRequest = (RequestSpotInstancesRequest) originalRequest;

            // Marshall the security groups specified only by name
            int groupNameCount = 1;
            for (String groupName : requestSpotInstancesRequest.launchSpecification().securityGroups()) {
                mutableRequest.putRawQueryParameter("LaunchSpecification.SecurityGroup." + groupNameCount++, groupName);
            }

            // Then loop through the GroupIdentifier objects and marshall any specified IDs
            // and any additional group names
            int groupIdCount = 1;
            for (GroupIdentifier group : requestSpotInstancesRequest.launchSpecification().allSecurityGroups()) {
                if (group.groupId() != null) {
                    mutableRequest.putRawQueryParameter("LaunchSpecification.SecurityGroupId." + groupIdCount++, group.groupId());
                }

                if (group.groupName() != null) {
                    mutableRequest.putRawQueryParameter("LaunchSpecification.SecurityGroup." + groupNameCount++, group
                        .groupName());
                }
            }

            // Remove any of the incorrect parameters.
            request.rawQueryParameters().keySet().stream()
                   .filter(parameter -> parameter.startsWith("LaunchSpecification.GroupSet."))
                   .forEach(mutableRequest::removeQueryParameter);
        } else if (originalRequest instanceof RunInstancesRequest) {
            // If a RunInstancesRequest doesn't specify a ClientToken, fill one in, otherwise
            // retries could result in unwanted instances being launched in the customer's account.
            RunInstancesRequest runInstancesRequest = (RunInstancesRequest) originalRequest;
            if (runInstancesRequest.clientToken() == null) {
                mutableRequest.putRawQueryParameter("ClientToken", UUID.randomUUID().toString());
            }
        } else if (originalRequest instanceof ModifyReservedInstancesRequest) {
            // If a ModifyReservedInstancesRequest doesn't specify a ClientToken, fill one in, otherwise
            // retries could result in duplicate requests.
            ModifyReservedInstancesRequest modifyReservedInstancesRequest = (ModifyReservedInstancesRequest) originalRequest;
            if (modifyReservedInstancesRequest.clientToken() == null) {
                mutableRequest.putRawQueryParameter("ClientToken", UUID.randomUUID().toString());
            }
        }
        return mutableRequest.build();
    }

    @Override
    public SdkResponse modifyResponse(Context.ModifyResponse context, ExecutionAttributes executionAttributes) {
        SdkResponse awsResponse = context.response();
        /*
         * For backwards compatibility, we preserve the existing List<String> of
         * security group names by explicitly populating it from the full list
         * of security group info. TODO: Is this actually needed? We might be able to drop it after we drop EC2 customizations
         */
        if (awsResponse instanceof DescribeSpotInstanceRequestsResponse) {
            return convertDescribeSpotInstanceRequestsResponse((DescribeSpotInstanceRequestsResponse) awsResponse);
        } else if (awsResponse instanceof RequestSpotInstancesResponse) {
            return convertRequestSpotInstancesResponse((RequestSpotInstancesResponse) awsResponse);
        } else if (awsResponse instanceof DescribeInstancesResponse) {
            return convertDescribeInstancesResponse((DescribeInstancesResponse) awsResponse);
        } else if (awsResponse instanceof RunInstancesResponse) {
            return convertRunInstanceResponse((RunInstancesResponse) awsResponse);
        }

        return awsResponse;
    }

    private DescribeSpotInstanceRequestsResponse convertDescribeSpotInstanceRequestsResponse(
            DescribeSpotInstanceRequestsResponse response) {
        DescribeSpotInstanceRequestsResponse.Builder responseBuilder = response.toBuilder();
        responseBuilder.spotInstanceRequests(convertSpotInstanceRequests(response.spotInstanceRequests()));
        return responseBuilder.build();
    }

    private RequestSpotInstancesResponse convertRequestSpotInstancesResponse(RequestSpotInstancesResponse response) {
        RequestSpotInstancesResponse.Builder responseBuilder = response.toBuilder();
        responseBuilder.spotInstanceRequests(convertSpotInstanceRequests(response.spotInstanceRequests()));
        return responseBuilder.build();
    }

    private DescribeInstancesResponse convertDescribeInstancesResponse(DescribeInstancesResponse response) {
        DescribeInstancesResponse.Builder responseBuilder = response.toBuilder();
        responseBuilder.reservations(convertReservation(response.reservations()));
        return responseBuilder.build();
    }

    private RunInstancesResponse convertRunInstanceResponse(RunInstancesResponse response) {
        RunInstancesResponse.Builder responseBuilder = response.toBuilder();
        responseBuilder.reservation(convertReservationSecurityGroupNames(response.reservation()));
        return responseBuilder.build();
    }

    private List<Reservation> convertReservation(List<Reservation> reservations) {
        return reservations.stream()
                           .map(this::convertReservationSecurityGroupNames)
                           .collect(Collectors.toList());
    }

    private Reservation convertReservationSecurityGroupNames(Reservation reservation) {
        Reservation.Builder builder = reservation.toBuilder();
        builder.groupNames(reservation.groups().stream()
                                      .map(GroupIdentifier::groupName)
                                      .collect(toList()));
        return builder.build();
    }

    private List<SpotInstanceRequest> convertSpotInstanceRequests(List<SpotInstanceRequest> spotInstanceRequests) {
        return spotInstanceRequests.stream()
                                   .map(this::convertSpotInstanceRequest)
                                   .collect(Collectors.toList());
    }

    private SpotInstanceRequest convertSpotInstanceRequest(SpotInstanceRequest spotInstanceRequest) {
        SpotInstanceRequest.Builder spotInstanceRequestBuilder = spotInstanceRequest.toBuilder();

        LaunchSpecification launchSpecification = spotInstanceRequest.launchSpecification();
        LaunchSpecification.Builder launchSpecificationBuilder = launchSpecification.toBuilder();

        launchSpecificationBuilder.securityGroups(convertSpecificationSecurityGroupNames(launchSpecification));
        spotInstanceRequestBuilder.launchSpecification(launchSpecificationBuilder.build());
        return spotInstanceRequestBuilder.build();
    }

    private List<String> convertSpecificationSecurityGroupNames(LaunchSpecification launchSpecification) {
        return launchSpecification.allSecurityGroups().stream()
                                                      .map(GroupIdentifier::groupName)
                                                      .collect(toList());
    }
}
