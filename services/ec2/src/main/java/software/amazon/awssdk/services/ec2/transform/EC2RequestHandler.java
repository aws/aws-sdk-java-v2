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

package software.amazon.awssdk.services.ec2.transform;

import static java.util.stream.Collectors.toList;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import software.amazon.awssdk.Response;
import software.amazon.awssdk.handlers.AwsHandlerKeys;
import software.amazon.awssdk.handlers.RequestHandler;
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
import software.amazon.awssdk.util.ImmutableObjectUtils;
import software.amazon.awssdk.utils.Base64Utils;

public class EC2RequestHandler extends RequestHandler {
    @Override
    public SdkHttpFullRequest beforeRequest(SdkHttpFullRequest request) {
        Object originalRequest = request.handlerContext(AwsHandlerKeys.REQUEST_CONFIG).getOriginalRequest();
        SdkHttpFullRequest.Builder mutableRequest = request.toBuilder();
        if (originalRequest instanceof ImportKeyPairRequest) {
            ImportKeyPairRequest importKeyPairRequest = (ImportKeyPairRequest) originalRequest;
            String publicKeyMaterial = importKeyPairRequest.publicKeyMaterial();
            String encodedKeyMaterial = Base64Utils.encodeAsString(publicKeyMaterial.getBytes(StandardCharsets.UTF_8));
            mutableRequest.queryParameter("PublicKeyMaterial", encodedKeyMaterial);
        } else if (originalRequest instanceof RequestSpotInstancesRequest) {
            // Request -> Query string marshalling for RequestSpotInstancesRequest is a little tricky since
            // the query string params follow a different form than the XML responses, so we manually set the parameters here.
            RequestSpotInstancesRequest requestSpotInstancesRequest = (RequestSpotInstancesRequest) originalRequest;

            // Marshall the security groups specified only by name
            int groupNameCount = 1;
            for (String groupName : requestSpotInstancesRequest.launchSpecification().securityGroups()) {
                mutableRequest.queryParameter("LaunchSpecification.SecurityGroup." + groupNameCount++, groupName);
            }

            // Then loop through the GroupIdentifier objects and marshall any specified IDs
            // and any additional group names
            int groupIdCount = 1;
            for (GroupIdentifier group : requestSpotInstancesRequest.launchSpecification().allSecurityGroups()) {
                if (group.groupId() != null) {
                    mutableRequest.queryParameter("LaunchSpecification.SecurityGroupId." + groupIdCount++, group.groupId());
                }

                if (group.groupName() != null) {
                    mutableRequest.queryParameter("LaunchSpecification.SecurityGroup." + groupNameCount++, group.groupName());
                }
            }

            // Remove any of the incorrect parameters.
            request.getParameters().keySet().stream()
                   .filter(parameter -> parameter.startsWith("LaunchSpecification.GroupSet."))
                   .forEach(mutableRequest::removeQueryParameter);
        } else if (originalRequest instanceof RunInstancesRequest) {
            // If a RunInstancesRequest doesn't specify a ClientToken, fill one in, otherwise
            // retries could result in unwanted instances being launched in the customer's account.
            RunInstancesRequest runInstancesRequest = (RunInstancesRequest) originalRequest;
            if (runInstancesRequest.clientToken() == null) {
                mutableRequest.queryParameter("ClientToken", UUID.randomUUID().toString());
            }
        } else if (originalRequest instanceof ModifyReservedInstancesRequest) {
            // If a ModifyReservedInstancesRequest doesn't specify a ClientToken, fill one in, otherwise
            // retries could result in duplicate requests.
            ModifyReservedInstancesRequest modifyReservedInstancesRequest = (ModifyReservedInstancesRequest) originalRequest;
            if (modifyReservedInstancesRequest.clientToken() == null) {
                mutableRequest.queryParameter("ClientToken", UUID.randomUUID().toString());
            }
        }
        return mutableRequest.build();
    }

    @Override
    public void afterResponse(SdkHttpFullRequest request, Response<?> response) {
        Object awsResponse = response.getAwsResponse();
        /*
         * For backwards compatibility, we preserve the existing List<String> of
         * security group names by explicitly populating it from the full list
         * of security group info.
         */
        if (awsResponse instanceof DescribeSpotInstanceRequestsResponse) {
            DescribeSpotInstanceRequestsResponse result = (DescribeSpotInstanceRequestsResponse) awsResponse;
            for (SpotInstanceRequest spotInstanceRequest : result.spotInstanceRequests()) {
                LaunchSpecification launchSpecification = spotInstanceRequest.launchSpecification();
                populateLaunchSpecificationSecurityGroupNames(launchSpecification);
            }
        } else if (awsResponse instanceof RequestSpotInstancesResponse) {
            RequestSpotInstancesResponse result = (RequestSpotInstancesResponse) awsResponse;
            for (SpotInstanceRequest spotInstanceRequest : result.spotInstanceRequests()) {
                LaunchSpecification launchSpecification = spotInstanceRequest.launchSpecification();
                populateLaunchSpecificationSecurityGroupNames(launchSpecification);
            }
        } else if (awsResponse instanceof DescribeInstancesResponse) {
            ((DescribeInstancesResponse) awsResponse).reservations().forEach(this::populateReservationSecurityGroupNames);
        } else if (awsResponse instanceof RunInstancesResponse) {
            populateReservationSecurityGroupNames(((RunInstancesResponse) awsResponse).reservation());
        }
    }

    private void populateReservationSecurityGroupNames(Reservation reservation) {
        List<String> groupNames = reservation.groups().stream()
                                             .map(GroupIdentifier::groupName)
                                             .collect(toList());
        ImmutableObjectUtils.setObjectMember(reservation, "groupNames", groupNames);
    }

    private void populateLaunchSpecificationSecurityGroupNames(LaunchSpecification launchSpecification) {
        List<String> groupNames = launchSpecification.allSecurityGroups().stream()
                                                     .map(GroupIdentifier::groupName)
                                                     .collect(toList());
        ImmutableObjectUtils.setObjectMember(launchSpecification, "securityGroups", groupNames);
    }
}
