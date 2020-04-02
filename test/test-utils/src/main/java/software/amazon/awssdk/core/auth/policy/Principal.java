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

package software.amazon.awssdk.core.auth.policy;

/**
 * A principal is an AWS account or AWS web service, which is being allowed or denied access to a
 * resource through an access control policy. The principal is a property of the
 * {@link Statement} object, not directly the {@link Policy} object.
 * <p>
 * The principal is A in the statement
 * "A has permission to do B to C where D applies."
 * <p>
 * In an access control policy statement, you can set the principal to all
 * authenticated AWS users through the {@link Principal#ALL_USERS} member. This
 * is useful when you don't want to restrict access based on the identity of the
 * requester, but instead on other identifying characteristics such as the
 * requester's IP address.
 */
public class Principal {

    /**
     * Principal instance that includes all users, including anonymous users.
     * <p>
     * This is useful when you don't want to restrict access based on the
     * identity of the requester, but instead on other identifying
     * characteristics such as the requester's IP address.
     */
    public static final Principal ALL_USERS = new Principal("AWS", "*");

    /**
     * Principal instance that includes all AWS web services.
     */
    public static final Principal ALL_SERVICES = new Principal("Service", "*");

    /**
     * Principal instance that includes all the web identity providers.
     */
    public static final Principal ALL_WEB_PROVIDERS = new Principal("Federated", "*");

    /**
     * Principal instance that includes all the AWS accounts, AWS web services and web identity providers.
     */
    public static final Principal ALL = new Principal("*", "*");

    private final String id;
    private final String provider;

    /**
     * Constructs a new principal with the specified AWS web service which
     * is being allowed or denied access to a resource through an access control
     * policy.
     *
     * @param service
     *            An AWS service.
     */
    public Principal(Service service) {
        if (service == null) {
            throw new IllegalArgumentException("Null AWS service name specified");
        }
        id = service.getServiceId();
        provider = "Service";
    }


    /**
     * Constructs a new principal with the specified AWS account ID. This method
     * automatically strips hyphen characters found in the account Id.
     *
     * @param accountId
     *            An AWS account ID.
     */
    public Principal(String accountId) {
        this("AWS", accountId);

        if (accountId == null) {
            throw new IllegalArgumentException("Null AWS account ID specified");
        }
    }

    /**
     * Constructs a new principal with the specified id and provider. This
     * method automatically strips hyphen characters found in the account ID if
     * the provider is "AWS".
     */
    public Principal(String provider, String id) {
        this(provider, id, provider.equals("AWS"));
    }

    /**
     * Constructs a new principal with the specified id and provider. This
     * method optionally strips hyphen characters found in the account Id.
     */
    public Principal(String provider, String id, boolean stripHyphen) {
        this.provider = provider;
        this.id = stripHyphen ?
                  id.replace("-", "") : id;
    }

    /**
     * Constructs a new principal with the specified web identity provider.
     *
     * @param webIdentityProvider
     *            An web identity provider.
     */
    public Principal(WebIdentityProvider webIdentityProvider) {
        if (webIdentityProvider == null) {
            throw new IllegalArgumentException("Null web identity provider specified");
        }
        this.id = webIdentityProvider.getWebIdentityProvider();
        provider = "Federated";
    }

    /**
     * Returns the provider for this principal, which indicates in what group of
     * users this principal resides.
     *
     * @return The provider for this principal.
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Returns the unique ID for this principal.
     *
     * @return The unique ID for this principal.
     */
    public String getId() {
        return id;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int hashCode = 1;

        hashCode = prime * hashCode + provider.hashCode();
        hashCode = prime * hashCode + id.hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object principal) {
        if (this == principal) {
            return true;
        }

        if (principal == null) {
            return false;
        }

        if (principal instanceof Principal == false) {
            return false;
        }

        Principal other = (Principal) principal;

        if (this.getProvider().equals(other.getProvider())
            && this.getId().equals(other.getId())) {
            return true;
        }

        return false;
    }

    /**
     * The services who have the right to do the assume the role
     * action. The AssumeRole action returns a set of temporary security
     * credentials that you can use to access resources that are defined in the
     * role's policy. The returned credentials consist of an Access Key ID, a
     * Secret Access Key, and a security token.
     */
    public enum Service {

        AWSDataPipeline("datapipeline.amazonaws.com"),
        AmazonElasticTranscoder("elastictranscoder.amazonaws.com"),
        AmazonEC2("ec2.amazonaws.com"),
        AWSOpsWorks("opsworks.amazonaws.com"),
        AWSCloudHSM("cloudhsm.amazonaws.com"),
        AllServices("*");
        private String serviceId;

        /**
         * The service which has the right to assume the role.
         */
        Service(String serviceId) {
            this.serviceId = serviceId;
        }

        /**
         * Construct the Services object from a string representing the service id.
         */
        public static Service fromString(String serviceId) {
            if (serviceId != null) {
                for (Service s : Service.values()) {
                    if (s.getServiceId().equalsIgnoreCase(serviceId)) {
                        return s;
                    }
                }
            }

            return null;
        }

        public String getServiceId() {
            return serviceId;
        }


    }

    /**
     * Web identity providers, such as Login with Amazon, Facebook, or Google.
     */
    public enum WebIdentityProvider {

        Facebook("graph.facebook.com"),
        Google("accounts.google.com"),
        Amazon("www.amazon.com"),
        AllProviders("*");

        private String webIdentityProvider;

        /**
         * The web identity provider which has the right to assume the role.
         */
        WebIdentityProvider(String webIdentityProvider) {
            this.webIdentityProvider = webIdentityProvider;
        }

        /**
         * Construct the Services object from a string representing web identity provider.
         */
        public static WebIdentityProvider fromString(String webIdentityProvider) {
            if (webIdentityProvider != null) {
                for (WebIdentityProvider provider : WebIdentityProvider.values()) {
                    if (provider.getWebIdentityProvider().equalsIgnoreCase(webIdentityProvider)) {
                        return provider;
                    }
                }
            }

            return null;
        }

        public String getWebIdentityProvider() {
            return webIdentityProvider;
        }


    }


}
