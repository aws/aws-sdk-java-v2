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

package software.amazon.awssdk.regions.internal.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.util.json.JacksonUtils;

/**
 * Utility class for retrieving Amazon EC2 instance metadata.<br>
 * You can use the data to build more generic AMIs that can be modified by
 * configuration files supplied at launch time. For example, if you run web
 * servers for various small businesses, they can all use the same AMI and
 * retrieve their content from the Amazon S3 bucket you specify at launch. To
 * add a new customer at any time, simply create a bucket for the customer, add
 * their content, and launch your AMI.<br>
 *
 * <P>
 * If {@link SdkSystemSetting#AWS_EC2_METADATA_DISABLED} is set to true, EC2 metadata usage
 * will be disabled and {@link SdkClientException} will be thrown for any metadata retrieval attempt.
 *
 * <p>
 * More information about Amazon EC2 Metadata
 *
 * @see <a
 * href="http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AESDG-chapter-instancedata.html">Amazon
 * EC2 User Guide: Instance Metadata</a>
 */
@ReviewBeforeRelease("Cleanup")
@SdkInternalApi
public final class EC2MetadataUtils {

    /** Default resource path for credentials in the Amazon EC2 Instance Metadata Service. */
    private static final String REGION = "region";
    private static final String INSTANCE_IDENTITY_DOCUMENT = "instance-identity/document";
    private static final String EC2_METADATA_ROOT = "/latest/meta-data";
    private static final String EC2_USERDATA_ROOT = "/latest/user-data/";
    private static final String EC2_DYNAMICDATA_ROOT = "/latest/dynamic/";
    private static final int DEFAULT_QUERY_RETRIES = 3;
    private static final int MINIMUM_RETRY_WAIT_TIME_MILLISECONDS = 250;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(EC2MetadataUtils.class);
    private static Map<String, String> cache = new ConcurrentHashMap<>();

    private EC2MetadataUtils() {}

    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.setPropertyNamingStrategy(PropertyNamingStrategy.PASCAL_CASE_TO_CAMEL_CASE);
    }

    /**
     * Get the AMI ID used to launch the instance.
     */
    public static String getAmiId() {
        return fetchData(EC2_METADATA_ROOT + "/ami-id");
    }

    /**
     * Get the index of this instance in the reservation.
     */
    public static String getAmiLaunchIndex() {
        return fetchData(EC2_METADATA_ROOT + "/ami-launch-index");
    }

    /**
     * Get the manifest path of the AMI with which the instance was launched.
     */
    public static String getAmiManifestPath() {
        return fetchData(EC2_METADATA_ROOT + "/ami-manifest-path");
    }

    /**
     * Get the list of AMI IDs of any instances that were rebundled to created
     * this AMI. Will only exist if the AMI manifest file contained an
     * ancestor-amis key.
     */
    public static List<String> getAncestorAmiIds() {
        return getItems(EC2_METADATA_ROOT + "/ancestor-ami-ids");
    }

    /**
     * Notifies the instance that it should reboot in preparation for bundling.
     * Valid values: none | shutdown | bundle-pending.
     */
    public static String getInstanceAction() {
        return fetchData(EC2_METADATA_ROOT + "/instance-action");
    }

    /**
     * Get the ID of this instance.
     */
    public static String getInstanceId() {
        return fetchData(EC2_METADATA_ROOT + "/instance-id");
    }

    /**
     * Get the type of the instance.
     */
    public static String getInstanceType() {
        return fetchData(EC2_METADATA_ROOT + "/instance-type");
    }

    /**
     * Get the local hostname of the instance. In cases where multiple network
     * interfaces are present, this refers to the eth0 device (the device for
     * which device-number is 0).
     */
    public static String getLocalHostName() {
        return fetchData(EC2_METADATA_ROOT + "/local-hostname");
    }

    /**
     * Get the MAC address of the instance. In cases where multiple network
     * interfaces are present, this refers to the eth0 device (the device for
     * which device-number is 0).
     */
    public static String getMacAddress() {
        return fetchData(EC2_METADATA_ROOT + "/mac");
    }

    /**
     * Get the private IP address of the instance. In cases where multiple
     * network interfaces are present, this refers to the eth0 device (the
     * device for which device-number is 0).
     */
    public static String getPrivateIpAddress() {
        return fetchData(EC2_METADATA_ROOT + "/local-ipv4");
    }

    /**
     * Get the Availability Zone in which the instance launched.
     */
    public static String getAvailabilityZone() {
        return fetchData(EC2_METADATA_ROOT + "/placement/availability-zone");
    }

    /**
     * Get the list of product codes associated with the instance, if any.
     */
    public static List<String> getProductCodes() {
        return getItems(EC2_METADATA_ROOT + "/product-codes");
    }

    /**
     * Get the public key. Only available if supplied at instance launch time.
     */
    public static String getPublicKey() {
        return fetchData(EC2_METADATA_ROOT + "/public-keys/0/openssh-key");
    }

    /**
     * Get the ID of the RAM disk specified at launch time, if applicable.
     */
    public static String getRamdiskId() {
        return fetchData(EC2_METADATA_ROOT + "/ramdisk-id");
    }

    /**
     * Get the ID of the reservation.
     */
    public static String getReservationId() {
        return fetchData(EC2_METADATA_ROOT + "/reservation-id");
    }

    /**
     * Get the list of names of the security groups applied to the instance.
     */
    public static List<String> getSecurityGroups() {
        return getItems(EC2_METADATA_ROOT + "/security-groups");
    }

    /**
     * Get information about the last time the instance profile was updated,
     * including the instance's LastUpdated date, InstanceProfileArn, and
     * InstanceProfileId.
     */
    public static IamInfo getIamInstanceProfileInfo() {
        String json = getData(EC2_METADATA_ROOT + "/iam/info");
        if (null == json) {
            return null;
        }

        try {

            return MAPPER.readValue(json, IamInfo.class);

        } catch (Exception e) {
            log.warn("Unable to parse IAM Instance profile info (" + json
                     + "): " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * The instance info is only guaranteed to be a JSON document per
     * http://docs
     * .aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html
     * <p>
     * This method is only a best attempt to capture the instance info as a
     * typed object.
     * <p>
     * Get an InstanceInfo object with dynamic information about this instance.
     */
    public static InstanceInfo getInstanceInfo() {
        return doGetInstanceInfo(getData(
                EC2_DYNAMICDATA_ROOT + INSTANCE_IDENTITY_DOCUMENT));
    }

    static InstanceInfo doGetInstanceInfo(String json) {
        if (null != json) {
            try {
                InstanceInfo instanceInfo = JacksonUtils.fromJsonString(json,
                                                                        InstanceInfo.class);
                return instanceInfo;
            } catch (Exception e) {
                log.warn("Unable to parse dynamic EC2 instance info (" + json
                         + ") : " + e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Returns the current region of this running EC2 instance; or null if
     * it is unable to do so. The method avoids interpreting other parts of the
     * instance info JSON document to minimize potential failure.
     * <p>
     * The instance info is only guaranteed to be a JSON document per
     * http://docs
     * .aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html
     */
    public static String getEC2InstanceRegion() {
        return doGetEC2InstanceRegion(getData(
                EC2_DYNAMICDATA_ROOT + INSTANCE_IDENTITY_DOCUMENT));
    }

    static String doGetEC2InstanceRegion(final String json) {
        if (null != json) {
            try {
                JsonNode node = MAPPER.readTree(json.getBytes(StandardCharsets.UTF_8));
                JsonNode region = node.findValue(REGION);
                return region.asText();
            } catch (Exception e) {
                log.warn("Unable to parse EC2 instance info (" + json
                         + ") : " + e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Returns the temporary security credentials (AccessKeyId, SecretAccessKey,
     * SessionToken, and Expiration) associated with the IAM roles on the
     * instance.
     */
    public static Map<String, IamSecurityCredential> getIamSecurityCredentials() {
        Map<String, IamSecurityCredential> credentialsInfoMap = new HashMap<>();

        List<String> credentials = getItems(EC2_METADATA_ROOT
                                            + "/iam/security-credentials");

        if (null != credentials) {
            for (String credential : credentials) {
                String json = getData(EC2_METADATA_ROOT
                                      + "/iam/security-credentials/" + credential);
                try {
                    IamSecurityCredential credentialInfo = MAPPER
                            .readValue(json, IamSecurityCredential.class);
                    credentialsInfoMap.put(credential, credentialInfo);
                } catch (Exception e) {
                    log.warn("Unable to process the credential (" + credential
                             + "). " + e.getMessage(), e);
                }
            }
        }
        return credentialsInfoMap;
    }

    /**
     * Get the virtual devices associated with the ami, root, ebs, and swap.
     */
    public static Map<String, String> getBlockDeviceMapping() {
        Map<String, String> blockDeviceMapping = new HashMap<>();

        List<String> devices = getItems(EC2_METADATA_ROOT
                                        + "/block-device-mapping");
        for (String device : devices) {
            blockDeviceMapping.put(device, getData(EC2_METADATA_ROOT
                                                   + "/block-device-mapping/" + device));
        }
        return blockDeviceMapping;
    }

    /**
     * Get the list of network interfaces on the instance.
     */
    public static List<NetworkInterface> getNetworkInterfaces() {
        List<NetworkInterface> networkInterfaces = new LinkedList<>();

        List<String> macs = getItems(EC2_METADATA_ROOT
                                     + "/network/interfaces/macs/");
        for (String mac : macs) {
            String key = mac.trim();
            if (key.endsWith("/")) {
                key = key.substring(0, key.length() - 1);
            }
            networkInterfaces.add(new NetworkInterface(key));
        }
        return networkInterfaces;
    }

    /**
     * Get the metadata sent to the instance
     */
    public static String getUserData() {
        return getData(EC2_USERDATA_ROOT);
    }

    public static String getData(String path) {
        return getData(path, DEFAULT_QUERY_RETRIES);
    }

    public static String getData(String path, int tries) {
        List<String> items = getItems(path, tries, true);
        if (null != items && items.size() > 0) {
            return items.get(0);
        }
        return null;
    }

    public static List<String> getItems(String path) {
        return getItems(path, DEFAULT_QUERY_RETRIES, false);
    }

    public static List<String> getItems(String path, int tries) {
        return getItems(path, tries, false);
    }

    private static List<String> getItems(String path, int tries, boolean slurp) {
        if (tries == 0) {
            throw new SdkClientException(
                    "Unable to contact EC2 metadata service.");
        }

        if (SdkSystemSetting.AWS_EC2_METADATA_DISABLED.getBooleanValueOrThrow()) {
            throw new SdkClientException("EC2 metadata usage is disabled.");
        }

        List<String> items;
        try {
            String hostAddress = SdkSystemSetting.AWS_EC2_METADATA_SERVICE_ENDPOINT.getStringValueOrThrow();
            String response = HttpResourcesUtils.instance().readResource(new URI(hostAddress + path));
            if (slurp) {
                items = Collections.singletonList(response);
            } else {
                items = Arrays.asList(response.split("\n"));
            }
            return items;
        } catch (SdkClientException ace) {
            log.warn("Unable to retrieve the requested metadata.");
            return null;
        } catch (IOException | URISyntaxException | RuntimeException e) {
            // Retry on any other exceptions
            int pause = (int) (Math.pow(2, DEFAULT_QUERY_RETRIES - tries) * MINIMUM_RETRY_WAIT_TIME_MILLISECONDS);
            try {
                Thread.sleep(pause < MINIMUM_RETRY_WAIT_TIME_MILLISECONDS ? MINIMUM_RETRY_WAIT_TIME_MILLISECONDS
                                                                          : pause);
            } catch (InterruptedException e1) {
                Thread.currentThread().interrupt();
            }
            return getItems(path, tries - 1, slurp);
        }
    }

    private static String fetchData(String path) {
        return fetchData(path, false);
    }

    private static String fetchData(String path, boolean force) {
        if (SdkSystemSetting.AWS_EC2_METADATA_DISABLED.getBooleanValueOrThrow()) {
            throw new SdkClientException("EC2 metadata usage is disabled.");
        }

        try {
            if (force || !cache.containsKey(path)) {
                cache.put(path, getData(path));
            }
            return cache.get(path);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Information about the last time the instance profile was updated,
     * including the instance's LastUpdated date, InstanceProfileArn, and
     * InstanceProfileId.
     */
    public static class IamInfo {
        public String code;
        public String message;
        public String lastUpdated;
        public String instanceProfileArn;
        public String instanceProfileId;
    }

    /**
     * The temporary security credentials (AccessKeyId, SecretAccessKey,
     * SessionToken, and Expiration) associated with the IAM role.
     */
    public static class IamSecurityCredential {
        public String code;
        public String message;
        public String lastUpdated;
        public String type;
        public String accessKeyId;
        public String secretAccessKey;
        public String token;
        public String expiration;

        /**
         * @deprecated because it is spelled incorrectly
         * @see #accessKeyId
         */
        @Deprecated
        public String secretAcessKey;
    }

    /**
     * This POJO is a best attempt to capture the instance info which is only
     * guaranteed to be a JSON document per
     * http://docs.aws.amazon.com/AWSEC2/latest
     * /UserGuide/ec2-instance-metadata.html
     *
     * Instance info includes dynamic information about the current instance
     * such as region, instanceId, private IP address, etc.
     */
    public static class InstanceInfo {
        private final String pendingTime;
        private final String instanceType;
        private final String imageId;
        private final String instanceId;
        private final String[] billingProducts;
        private final String architecture;
        private final String accountId;
        private final String kernelId;
        private final String ramdiskId;
        private final String region;
        private final String version;
        private final String availabilityZone;
        private final String privateIp;
        private final String[] devpayProductCodes;

        @JsonCreator
        public InstanceInfo(
                @JsonProperty(value = "pendingTime", required = true) String pendingTime,
                @JsonProperty(value = "instanceType", required = true) String instanceType,
                @JsonProperty(value = "imageId", required = true) String imageId,
                @JsonProperty(value = "instanceId", required = true) String instanceId,
                @JsonProperty(value = "billingProducts", required = false) String[] billingProducts,
                @JsonProperty(value = "architecture", required = true) String architecture,
                @JsonProperty(value = "accountId", required = true) String accountId,
                @JsonProperty(value = "kernelId", required = true) String kernelId,
                @JsonProperty(value = "ramdiskId", required = false) String ramdiskId,
                @JsonProperty(value = REGION, required = true) String region,
                @JsonProperty(value = "version", required = true) String version,
                @JsonProperty(value = "availabilityZone", required = true) String availabilityZone,
                @JsonProperty(value = "privateIp", required = true) String privateIp,
                @JsonProperty(value = "devpayProductCodes", required = false) String[] devpayProductCodes) {
            this.pendingTime = pendingTime;
            this.instanceType = instanceType;
            this.imageId = imageId;
            this.instanceId = instanceId;
            this.billingProducts = billingProducts == null
                                   ? null : billingProducts.clone();
            this.architecture = architecture;
            this.accountId = accountId;
            this.kernelId = kernelId;
            this.ramdiskId = ramdiskId;
            this.region = region;
            this.version = version;
            this.availabilityZone = availabilityZone;
            this.privateIp = privateIp;
            this.devpayProductCodes = devpayProductCodes == null
                                      ? null : devpayProductCodes.clone();
        }

        public String getPendingTime() {
            return pendingTime;
        }

        public String getInstanceType() {
            return instanceType;
        }

        public String getImageId() {
            return imageId;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public String[] getBillingProducts() {
            return billingProducts == null ? null : billingProducts.clone();
        }

        public String getArchitecture() {
            return architecture;
        }

        public String getAccountId() {
            return accountId;
        }

        public String getKernelId() {
            return kernelId;
        }

        public String getRamdiskId() {
            return ramdiskId;
        }

        public String getRegion() {
            return region;
        }

        public String getVersion() {
            return version;
        }

        public String getAvailabilityZone() {
            return availabilityZone;
        }

        public String getPrivateIp() {
            return privateIp;
        }

        public String[] getDevpayProductCodes() {
            return devpayProductCodes == null ? null : devpayProductCodes.clone();
        }
    }

    /**
     * All of the metada associated with a network interface on the instance.
     */
    public static class NetworkInterface {
        private String path;
        private String mac;

        private List<String> availableKeys;
        private Map<String, String> data = new HashMap<>();

        public NetworkInterface(String macAddress) {
            mac = macAddress;
            path = "/network/interfaces/macs/" + mac + "/";
        }

        /**
         * The interface's Media Acess Control (mac) address
         */
        public String getMacAddress() {
            return mac;
        }

        /**
         * The ID of the owner of the network interface.<br>
         * In multiple-interface environments, an interface can be attached by a
         * third party, such as Elastic Load Balancing. Traffic on an interface
         * is always billed to the interface owner.
         */
        public String getOwnerId() {
            return getData("owner-id");
        }

        /**
         * The interface's profile.
         */
        public String getProfile() {
            return getData("profile");
        }

        /**
         * The interface's local hostname.
         */
        public String getHostname() {
            return getData("local-hostname");
        }

        /**
         * The private IP addresses associated with the interface.
         */
        public List<String> getLocalIPv4s() {
            return getItems("local-ipv4s");
        }

        /**
         * The interface's public hostname.
         */
        public String getPublicHostname() {
            return getData("public-hostname");
        }

        /**
         * The elastic IP addresses associated with the interface.<br>
         * There may be multiple IP addresses on an instance.
         */
        public List<String> getPublicIPv4s() {
            return getItems("public-ipv4s");
        }

        /**
         * Security groups to which the network interface belongs.
         */
        public List<String> getSecurityGroups() {
            return getItems("security-groups");
        }

        /**
         * IDs of the security groups to which the network interface belongs.
         * Returned only for Amazon EC2 instances launched into a VPC.
         */
        public List<String> getSecurityGroupIds() {
            return getItems("security-group-ids");
        }

        /**
         * The CIDR block of the Amazon EC2-VPC subnet in which the interface
         * resides.<br>
         * Returned only for Amazon EC2 instances launched into a VPC.
         */
        public String getSubnetIPv4CidrBlock() {
            return getData("subnet-ipv4-cidr-block");
        }

        /**
         * ID of the subnet in which the interface resides.<br>
         * Returned only for Amazon EC2 instances launched into a VPC.
         */
        public String getSubnetId() {
            return getData("subnet-id");
        }

        /**
         * The CIDR block of the Amazon EC2-VPC in which the interface
         * resides.<br>
         * Returned only for Amazon EC2 instances launched into a VPC.
         */
        public String getVpcIPv4CidrBlock() {
            return getData("vpc-ipv4-cidr-block");
        }

        /**
         * ID of the Amazon EC2-VPC in which the interface resides.<br>
         * Returned only for Amazon EC2 instances launched into a VPC.
         */
        public String getVpcId() {
            return getData("vpc-id");
        }

        /**
         * Get the private IPv4 address(es) that are associated with the
         * public-ip address and assigned to that interface.
         *
         * @param publicIp
         *            The public IP address
         * @return Private IPv4 address(es) associated with the public IP
         *         address.
         */
        public List<String> getIPv4Association(String publicIp) {
            return getItems(EC2_METADATA_ROOT + path + "ipv4-associations/" + publicIp);
        }

        private String getData(String key) {
            if (data.containsKey(key)) {
                return data.get(key);
            }

            // Since the keys are variable, cache a list of which ones are
            // available
            // to prevent unnecessary trips to the service.
            if (null == availableKeys) {
                availableKeys = EC2MetadataUtils.getItems(EC2_METADATA_ROOT
                                                          + path);
            }

            if (availableKeys.contains(key)) {
                data.put(key, EC2MetadataUtils.getData(EC2_METADATA_ROOT + path
                                                       + key));
                return data.get(key);
            } else {
                return null;
            }
        }

        private List<String> getItems(String key) {
            if (null == availableKeys) {
                availableKeys = EC2MetadataUtils.getItems(EC2_METADATA_ROOT
                                                          + path);
            }

            if (availableKeys.contains(key)) {
                return EC2MetadataUtils
                        .getItems(EC2_METADATA_ROOT + path + key);
            } else {
                return Collections.emptyList();
            }
        }
    }
}
