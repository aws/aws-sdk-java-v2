**Design:** New Feature, **Status:** [Released](../../../README.md)

# Bearer Token Authorization and Token Providers

**What is Bearer Token authorization?**

Bearer Token authorization is a method used to authenticate service request using 
[Bearer Token](https://oauth.net/2/bearer-tokens/) instead of traditional AWS credentials .This is performed by 
populating authorization header with bearer token.

**What is the user experience?**

First, users will be able to configure a profile in the shared SDK configuration files that allows them to 
perform an initial login via the AWS CLI or AWS IDE plugins. For example, this profile might look like this:

 ```
 [profile sono]
 sso_start_url = https://sono.aws
 sso_region = us-east-1
 ```

With this configuration, a user would be able to login via a supported tool such as the AWS CLI:

 ```
 $ aws sso login --profile sono
 ```

Upon completing this login, a cached token will be available under
`~/.aws/sso/cache` which will be used by the SDK to perform bearer token
authorization. 

The service will be modeled at the service level to use the `bearer`
signature version. This client would know how to load the cached token based on the
profile configuration and populate the bearer authorization header using it.
This is similar to how an SDK would resolve AWS credentials and perform SigV4
signing.

*Example*

For example, given a token value of `"mF_9.B5f-4.1JqM"` the value of the
`Authorization` header would be: `"Bearer mF_9.B5f-4.1JqM"`. A full HTTP
request may look like the following:

 ```
 GET /resource HTTP/1.1
 Host: server.example.com
 Authorization: Bearer mF_9.B5f-4.1JqM
 ```

## Proposed APIs

When the SDK 2.x client encounters a service or operation that is modeled or configured to use the 
`bearer` signature version, the client will consult the token provider chain to 
derive a token to be used in generating and attaching the authorization to a request.

### API Design for Token/Token Providers and Token Provider Chains

Even though Tokens/TokenProvider and Token Providers is similar to AWS Credentials, 
AWS Credential Provider and AWSCredentialProvider Chain, we will need to provide new interfaces for the following reasons.
 1. Credentials get finally resolved to accessKeyId and secretAccessKey. These are related to AWS account login.
 2. SDK should support clients configured for multiple types of credentials (e.g. token and AWS credentials)

#### Token 

SDK will support representations of a token that contains additional metadata including the token string and
the expiration field.
```java
@SdkPublicApi
public interface SdkToken {
    String token();
    Optional<Instant> expirationTime();
}

```

#### Token Provider

To produce a token object SDKs will implement support for token providers and token provider chains.
The token provider and provider chain interfaces will mirror the existing AWS credential provider
interfaces in the SDK, but instead return a Token object.

```java
@FunctionalInterface
@SdkPublicApi
public interface SdkTokenProvider {

    SdkToken resolveToken();
}
```
SsoTokenProvider is an implementation of SdkTokenProvider that is capable of loading and  storing SSO tokens to 
`~/.aws/sso/cache`. This is also capable of refreshing the cached token via the SSO-OIDC service.


```java
        SsoTokenProvider ssoTokenProvider =  SsoTokenProvider.builder()
                               .startUrl("https://d-abc123.awsapps.com/start")
                               .build();
```

The StaticTokenProvider represents the simplest possible token provider. 
It simply returns a static token string and has an optional expiration.

```java
 StaticTokenProvider provider = StaticTokenProvider.create(bearerToken);
```

#### Token Provider Chains

SdkTokenProviderChain is an implementation of SdkTokenProvider that chains together multiple token providers.
The public method of this class is exactly same as [AwsCredentialsProviderChain](https://github.com/aws/aws-sdk-java-v2/blob/master/core/auth/src/main/java/software/amazon/awssdk/auth/credentials/AwsCredentialsProviderChain.java)
except it resolves the Token providers.

#### ClientBuilder API to set Token Privder

A new API will be added in [AwsClientBuilder](https://github.com/aws/aws-sdk-java-v2/blob/master/core/aws-core/src/main/java/software/amazon/awssdk/awscore/client/builder/AwsClientBuilder.java)
to set the tokenProvider to a client.

```java

   SdkTokenProviderChain TOKEN_PROVIDER_CHAIN =  DefaultSdkTokenProvderChain.create(); 
   ServiceClient.builder()
                 .region(REGION)
                 .tokenProvider(TOKEN_PROVIDER_CHAIN)
                 .build();
```


### Bearer Token Authorizer

Bearer Token Authorization will be done by BearerTokenSigner that will update the Authorization header with bearer 
token that is resolved from Token Provider.

```java
@SdkPublicApi
public final class BearerTokenSigner implements Signer {

    @Override
    public CredentialType credentialType() {
        return CredentialType.TOKEN;
    }
    
    @Override
    public SdkHttpFullRequest sign(SdkHttpFullRequest request, ExecutionAttributes executionAttributes) {
       /**
        * doSign will do following
        * 1. Resolve token from Token Provider
        * 2. Set the Authorization header by filling in the token to the following format string: "Bearer {token}".
        */
        return doSign(request, executionAttributes);
    }
    public static BearerTokenSigner create() {
        return new BearerTokenSigner();
    }
}

```
#### ClientBuilder API to set Bearer Token Signer

A new AdvancedOption setting `BEARER_SIGNER` will be created to add bearer signer

```java

   ServiceClient.builder()
                   .region(REGION)
                   .overrideConfiguration(
                        ClientOverrideConfiguration.builder()
                                                   .putAdvancedOption(BEARER_SIGNER, DefaultBearerTokenSigner.create())
                                                   .build())
                    .build();
```

Also, ClientBuilder can be updated with Bearer Token Signer by using existing `SIGNER` advancedOption
```java
   serviceClient.builder()
                   .region(REGION)
                   .overrideConfiguration(
                        ClientOverrideConfiguration.builder()
                                                   .putAdvancedOption(SIGNER, DefaultBearerTokenSigner.create())
                                                   .build())
```
#### OperationRequest Builder API to set Bearer Token Signer
Bearer Token Signer can be set at request level by overriding configuration for request operation.
A new API `bearerTokenSigner(BearerTokenSigner bearerTokenSigner)` will be added in 
[RequestOverrideConfiguration](https://github.com/aws/aws-sdk-java-v2/blob/master/core/aws-core/src/main/java/software/amazon/awssdk/awscore/AwsRequestOverrideConfiguration.java).

```java
   serviceClient.operation(OperationRequest.builder()
                                           .overrideConfiguration(o -> o.bearerTokenSigner(DefaultBearerTokenSigner.create()))
                                           .build());
```

Also, BearerTokenSigner can be updated by setting signer on RequestOverrideConfiguration.

```java
   serviceClient.operation(OperationRequest.builder()
                                        .overrideConfiguration(o -> o.signer(DefaultBearerTokenSigner.create()))
                                        .build());
```
