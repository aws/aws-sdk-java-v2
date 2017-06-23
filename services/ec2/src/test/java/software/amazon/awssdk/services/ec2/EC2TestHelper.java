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

package software.amazon.awssdk.services.ec2;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.services.ec2.model.AssociateDhcpOptionsRequest;
import software.amazon.awssdk.services.ec2.model.CreateCustomerGatewayRequest;
import software.amazon.awssdk.services.ec2.model.CreateCustomerGatewayResponse;
import software.amazon.awssdk.services.ec2.model.CreateDhcpOptionsRequest;
import software.amazon.awssdk.services.ec2.model.CreateDhcpOptionsResponse;
import software.amazon.awssdk.services.ec2.model.CreateSubnetRequest;
import software.amazon.awssdk.services.ec2.model.CreateSubnetResponse;
import software.amazon.awssdk.services.ec2.model.CreateVpcRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpcResponse;
import software.amazon.awssdk.services.ec2.model.CreateVpnConnectionRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpnConnectionResponse;
import software.amazon.awssdk.services.ec2.model.CreateVpnGatewayRequest;
import software.amazon.awssdk.services.ec2.model.CreateVpnGatewayResponse;
import software.amazon.awssdk.services.ec2.model.CustomerGateway;
import software.amazon.awssdk.services.ec2.model.DeleteCustomerGatewayRequest;
import software.amazon.awssdk.services.ec2.model.DeleteDhcpOptionsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteSubnetRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVpcRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVpnConnectionRequest;
import software.amazon.awssdk.services.ec2.model.DeleteVpnGatewayRequest;
import software.amazon.awssdk.services.ec2.model.DescribeCustomerGatewaysRequest;
import software.amazon.awssdk.services.ec2.model.DescribeCustomerGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.DescribeDhcpOptionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeDhcpOptionsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcAttributeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcAttributeResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpnConnectionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpnConnectionsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpnGatewaysRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpnGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.DhcpConfiguration;
import software.amazon.awssdk.services.ec2.model.ModifyVpcAttributeRequest;
import software.amazon.awssdk.services.ec2.model.PurchaseReservedInstancesOfferingRequest;
import software.amazon.awssdk.services.ec2.model.PurchaseReservedInstancesOfferingResponse;
import software.amazon.awssdk.services.ec2.model.Vpc;
import software.amazon.awssdk.services.ec2.model.VpnConnection;
import software.amazon.awssdk.services.ec2.model.VpnGateway;
import software.amazon.awssdk.test.AwsTestBase;

@Deprecated
public class EC2TestHelper {

    /** Shared EC2 client for all tests to use. */
    public static EC2Client EC2;

    public static AwsCredentials CREDENTIALS;

    static {
        try {
            if (CREDENTIALS == null) {
                try {
                    CREDENTIALS = AwsTestBase.CREDENTIALS_PROVIDER_CHAIN.getCredentials();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            EC2 = EC2Client.builder().credentialsProvider(new StaticCredentialsProvider(CREDENTIALS)).build();
        } catch (Exception exception) {
            // Ignored or expected.
        }
    }

    /**
     * Deletes all customer gateways
     */
    public static void deleteAllCustomerGateways() {
        DescribeCustomerGatewaysRequest request = DescribeCustomerGatewaysRequest.builder().build();
        DescribeCustomerGatewaysResponse result = EC2.describeCustomerGateways(request);

        for (CustomerGateway gateway : result.customerGateways()) {
            deleteCustomerGateway(gateway.customerGatewayId());

        }
    }

    /**
     * Deletes all Vpn Connections
     */
    public static void deleteAllVpnConnections() {
        DescribeVpnConnectionsRequest request = DescribeVpnConnectionsRequest.builder().build();
        DescribeVpnConnectionsResponse result = EC2.describeVpnConnections(request);

        for (VpnConnection connection : result.vpnConnections()) {
            DeleteVpnConnectionRequest r = DeleteVpnConnectionRequest.builder()
                    .vpnConnectionId(connection.vpnConnectionId())
                    .build();
            EC2.deleteVpnConnection(r);
        }
    }

    /**
     * Delete customer gateway by gateway id
     *
     * @param customerGatewayIds
     *            variable list of customer gateway ids
     */
    public static void deleteCustomerGateway(String... customerGatewayIds) {
        for (String customerGatewayId : customerGatewayIds) {
            DeleteCustomerGatewayRequest request = DeleteCustomerGatewayRequest.builder()
                    .customerGatewayId(customerGatewayId)
                    .build();
            EC2.deleteCustomerGateway(request);
        }
    }

    /**
     * Create customer gateway
     *
     * @param ipAddress
     *            IP address of the gateway
     * @param bgpAsn
     *            The customer gateway's Border Gateway Protocol (BGP)
     *            Autonomous System Number (ASN).
     * @param type
     *            The type of VPN connection this customer gateway supports.
     * @return CreateCustomerGatewayResponse
     */
    public static CreateCustomerGatewayResponse createCustomerGateway(
            String ipAddress, Integer bgpAsn, String type) {

        CreateCustomerGatewayRequest request = CreateCustomerGatewayRequest.builder()
                .publicIp(ipAddress)
                .bgpAsn(bgpAsn)
                .type(type)
                .build();

        return EC2.createCustomerGateway(request);
    }

    /**
     * Describe customer gateway by customer gateway Id
     *
     * @param customerGatewayIds
     *            variable list of customer gateway ids
     * @return DescribeCustomerGatewaysResponse
     */
    public static DescribeCustomerGatewaysResponse describeCustomerGateway(
            List<String> customerGatewayIds) {

        DescribeCustomerGatewaysRequest request = DescribeCustomerGatewaysRequest.builder()
                .customerGatewayIds(customerGatewayIds)
                .build();

        return EC2.describeCustomerGateways(request);

    }

    /**
     * Describe customer gateway by customer gateway Id
     *
     * @param customerGatewayId
     *            customer gateway id
     * @return DescribeCustomerGatewaysResponse
     */
    public static DescribeCustomerGatewaysResponse describeCustomerGateway(
            String customerGatewayId) {

        List<String> ids = new ArrayList<String>();

        ids.add(customerGatewayId);

        return describeCustomerGateway(ids);

    }

    public static CreateDhcpOptionsResponse createDhcpOptions(String optionKey,
                                                            String... optionValue) {

        DhcpConfiguration configurationOne = DhcpConfiguration.builder()
                .key(optionKey).values(optionValue).build();

        CreateDhcpOptionsRequest request = CreateDhcpOptionsRequest.builder()
                .dhcpConfigurations(configurationOne)
                .build();

        return EC2.createDhcpOptions(request);
    }

    /**
     * Describe dhcp options by list of option ids
     *
     * @param dhcpOptionsIds
     *            list of option ids
     * @return DescribeDhcpOptionsResponse
     */
    public static DescribeDhcpOptionsResponse describeDhcpOptions(
            List<String> dhcpOptionsIds) {

        DescribeDhcpOptionsRequest request = DescribeDhcpOptionsRequest.builder()
                .dhcpOptionsIds(dhcpOptionsIds).build();

        return EC2.describeDhcpOptions(request);
    }

    /**
     * Describe dhcp options by option id
     *
     * @param dhcpOptionsId
     *            A DHCP options set ID.
     */
    public static DescribeDhcpOptionsResponse describeDhcpOptions(String dhcpOptionsId) {
        List<String> ids = new ArrayList<String>();
        ids.add(dhcpOptionsId);

        return describeDhcpOptions(ids);
    }

    /**
     * Associate dhcp options
     *
     * @param dhcpOptionsId
     *            The ID of the DHCP options you want to associate with the VPC,
     *            or "default" if you want to associate the default DHCP options
     *            with the VPC.
     *
     * @param vpcId
     *            The ID of the VPC you want to associate the DHCP options with.
     */
    public static void associateDhcpOptions(String dhcpOptionsId, String vpcId) {
        AssociateDhcpOptionsRequest request = AssociateDhcpOptionsRequest.builder()
                .dhcpOptionsId(dhcpOptionsId).vpcId(vpcId).build();
        EC2.associateDhcpOptions(request);
    }

    /**
     * Delete dhcp options
     *
     * @param dhcpOptionsIds
     *            variable list of options ids
     */
    public static void deleteDhcpOptions(String... dhcpOptionsIds) {
        for (String dhcpOptionsId : dhcpOptionsIds) {
            DeleteDhcpOptionsRequest request = DeleteDhcpOptionsRequest.builder()
                    .dhcpOptionsId(dhcpOptionsId).build();
            EC2.deleteDhcpOptions(request);
        }
    }

    /**
     * Create vpn gateway
     *
     * @param type
     *            The type of VPN connection this VPN gateway supports.
     * @param availabilityZone
     *            The Availability Zone where you want the VPN gateway.
     * @return CreateVpnGatewayResponse
     */
    public static CreateVpnGatewayResponse createVpnGateway(String type,
                                                          String availabilityZone) {
        CreateVpnGatewayRequest request = CreateVpnGatewayRequest.builder()
                .type(type)
                .availabilityZone(availabilityZone)
                .build();

        return EC2.createVpnGateway(request);
    }

    /**
     * Create vpn gateway
     *
     * @param type
     *            The type of VPN connection this VPN gateway supports.
     * @return CreateVpnGatewayResponse
     */
    public static CreateVpnGatewayResponse createVpnGateway(String type) {
        return createVpnGateway(type, null);
    }

    /**
     * Deletes VPN gateway
     *
     * @param vpnGatewayId
     *            id of the gateway to delete
     */
    public static void deleteVpnGateway(String vpnGatewayId) {
        DeleteVpnGatewayRequest request = DeleteVpnGatewayRequest.builder()
                .vpnGatewayId(vpnGatewayId)
                .build();
        EC2.deleteVpnGateway(request);
    }

    /**
     * Describe vpn gatways
     *
     * @param vpnGatewayIds
     *            list of vpn gateway ids
     * @return DescribeVpnGatewaysResponse
     */
    public static DescribeVpnGatewaysResponse describeVpnGateways(List<String> vpnGatewayIds) {
        DescribeVpnGatewaysRequest request = DescribeVpnGatewaysRequest.builder()
            .vpnGatewayIds(vpnGatewayIds)
            .build();

        return EC2.describeVpnGateways(request);
    }

    /**
     * Describe vpn gatways
     *
     * @param vpnGatewayId
     *            gateway id
     * @return DescribeVpnGatewaysResponse
     */
    public static DescribeVpnGatewaysResponse describeVpnGateway(String vpnGatewayId) {
        List<String> ids = new ArrayList<String>();
        ids.add(vpnGatewayId);

        return describeVpnGateways(ids);
    }

    /**
     * Deletes VPN gateway
     *
     * @param vpnGatewayId
     *            id of the gateway to delete
     */
    public static void deletVpnGateway(String vpnGatewayId) {
        DeleteVpnGatewayRequest request = DeleteVpnGatewayRequest.builder()
                .vpnGatewayId(vpnGatewayId)
                .build();
        EC2.deleteVpnGateway(request);
    }

    /**
     * Deletes VPN connection
     *
     * @param vpnConnectionId
     *            vpn connection id
     */
    public static void deleteVpnConnection(String vpnConnectionId) {
        DeleteVpnConnectionRequest request = DeleteVpnConnectionRequest.builder()
                .vpnConnectionId(vpnConnectionId)
                .build();
        EC2.deleteVpnConnection(request);
    }

    /**
     * Deletes VPN gateway
     *
     */
    public static void deleteAllVpnGateways() {
        DescribeVpnGatewaysResponse describeResult = describeVpnGateways(null);

        for (VpnGateway vpnGateway : describeResult.vpnGateways()) {
            deletVpnGateway(vpnGateway.vpnGatewayId());
        }
    }

    /**
     * Creates VPN connection
     *
     * @param type
     *            The type of VPN connection.
     * @param customerGatewayId
     *            The ID of the customer gateway
     * @param vpnGatewayId
     *            The ID of the customer gateway.
     */
    public static CreateVpnConnectionResponse createVpnConnection(String type,
                                                                String customerGatewayId, String vpnGatewayId) {

        CreateVpnConnectionRequest request = CreateVpnConnectionRequest.builder()
                .type(type)
                .vpnGatewayId(vpnGatewayId)
                .customerGatewayId(customerGatewayId)
                .build();

        return EC2.createVpnConnection(request);
    }

    /**
     * Deletes VPC by VPC Id
     *
     * @param vpcId
     *            VPC id
     */
    public static void deleteVpc(String vpcId) {
        DeleteVpcRequest request = DeleteVpcRequest.builder()
                .vpcId(vpcId)
                .build();
        EC2.deleteVpc(request);
    }

    /**
     * Creates Vpc
     *
     * @param cidrBlock
     *            A valid CIDR block.
     * @return CreateVpcResponse
     */
    public static CreateVpcResponse createVpc(String cidrBlock) {
        CreateVpcRequest request = CreateVpcRequest.builder()
                .cidrBlock(cidrBlock)
                .build();
        return EC2.createVpc(request);
    }

    /**
     * Describe VPC by VPC Id
     *
     * @param vpcId
     *            VPC Id
     * @return DescribeVpcsResponse
     */
    public static DescribeVpcsResponse describeVpc(String vpcId) {
        List<String> ids = new ArrayList<String>();
        ids.add(vpcId);

        return describeVpcs(ids);
    }

    /**
     * Describe VPC by list of VPC Ids
     *
     * @param ids
     *            list of VPC ids
     * @return DescribeVpcsResponse
     */
    public static DescribeVpcsResponse describeVpcs(List<String> ids) {
        DescribeVpcsRequest request = DescribeVpcsRequest.builder()
            .vpcIds(ids)
            .build();

        return EC2.describeVpcs(request);
    }

    public static DescribeVpcAttributeResponse describeVpcAttribute(String vpcId, boolean enableDnsHostnames,
                                                                  boolean enableDnsSupport) {
        DescribeVpcAttributeRequest.Builder describeVpcAttributeRequestBuilder = DescribeVpcAttributeRequest.builder().vpcId(vpcId);
        if (enableDnsHostnames == true) {
            describeVpcAttributeRequestBuilder.attribute("enableDnsHostnames");
        }
        if (enableDnsSupport == true) {
            describeVpcAttributeRequestBuilder.attribute("enableDnsSupport");
        }
        return EC2.describeVpcAttribute(describeVpcAttributeRequestBuilder.build());
    }

    public static void modifyVpcAttribute(String vpcId) {
        EC2.modifyVpcAttribute(ModifyVpcAttributeRequest.builder().vpcId(vpcId).enableDnsSupport(true).build());
    }

    /**
     * Deletes ALL Vpc
     */
    public static void deleteAllVpcs() {
        DescribeVpcsResponse describeResult = describeVpcs(null);
        for (Vpc vpc : describeResult.vpcs()) {
            deleteVpc(vpc.vpcId());
        }
    }

    /**
     * Deletes subnet
     *
     * @param subnetId
     *            subnet id
     */
    public static void deleteSubnet(String subnetId) {
        DeleteSubnetRequest request = DeleteSubnetRequest.builder()
                .subnetId(subnetId)
                .build();
        EC2.deleteSubnet(request);
    }

    /**
     * Creates Subnet
     *
     * @param vpcId
     *            The ID of the VPC where you want to create the subnet.
     * @param cidrBlock
     *            The CIDR block you want the subnet to cover.
     * @return CreateSubnetResponse
     */
    public static CreateSubnetResponse createSubnet(String vpcId, String cidrBlock) {
        CreateSubnetRequest request = CreateSubnetRequest.builder()
                .vpcId(vpcId)
                .cidrBlock(cidrBlock)
                .build();
        return EC2.createSubnet(request);
    }

    /**
     * Describes subnets
     *
     * @param subnetId
     *            subnet id
     * @return DescribeSubnetsResponse
     */
    public static DescribeSubnetsResponse describeSubnet(String subnetId) {
        List<String> ids = new ArrayList<String>();
        ids.add(subnetId);

        return describeSubnets(ids);
    }

    /**
     * Describes subnets given the list of subnet ids
     *
     * @param subnetIds
     *            subnet ids
     * @return DescribeSubnetsResponse
     */
    public static DescribeSubnetsResponse describeSubnets(List<String> subnetIds) {
        DescribeSubnetsRequest request = DescribeSubnetsRequest.builder()
            .subnetIds(subnetIds)
            .build();

        return EC2.describeSubnets(request);
    }

    /**
     * Purchase reserved instance. Careful with this! Use test
     * offering ids.
     *
     * @param offeringId offering id
     * @param instanceCount how many instances to reserve
     */
    public static PurchaseReservedInstancesOfferingResponse purchaseReservedInstancesOffering(
            String offeringId, int instanceCount) {
        PurchaseReservedInstancesOfferingRequest request = PurchaseReservedInstancesOfferingRequest.builder()
                .instanceCount(instanceCount)
                .reservedInstancesOfferingId(offeringId)
                .build();

        return EC2.purchaseReservedInstancesOffering(request);
    }

}
