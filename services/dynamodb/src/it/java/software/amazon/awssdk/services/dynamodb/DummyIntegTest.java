package software.amazon.awssdk.services.dynamodb;


import java.util.Collections;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

public class DummyIntegTest {

    @Test
    public void test() {

        Region region = Region.US_WEST_1;


//        AwsSignerParams signerParams = new AwsSignerParams();
//        signerParams.setAwsCredentials(DefaultCredentialsProvider.create().getCredentials());
//        signerParams.setRegion(region);
//        signerParams.setSigningName("dynamodb");

        DynamoDBClient client = DynamoDBClient.builder()
                                              .region(region)
//                                              .overrideConfiguration(ClientOverrideConfiguration
//                                                                         .builder()
//                                                                         .advancedOption(SdkAdvancedClientOption.SIGNER, new Aws4Signer())
//                                                                         .advancedOption(SdkAdvancedClientOption.SIGNER_CONTEXT,
//                                                                                         new SignerContext().putAttribute(AWS_SIGNER_PARAMS, signerParams))
//                                                                         .build())
                                              .build();

        Map<String, AttributeValue> item =
            client.getItem(GetItemRequest.builder()
                                         .tableName("VoxTests1")
                                         .key(Collections.singletonMap("UID", AttributeValue.builder()
                                                                                            .s("varunkn")
                                                                                            .build()))
                                         .build())
                  .item();

        System.out.println(item);
    }

//    @Test
//    public void signWithoutUsingSdkClient() throws Exception {
//        final String content = "{\"TableName\":\"VoxTests1\",\"Key\":{\"UID\":{\"S\":\"varunkn\"}}}";
//        final InputStream contentStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
//
//
//        SdkHttpFullRequest httpFullRequest = SdkHttpFullRequest.builder()
//                                                               .method(SdkHttpMethod.POST)
//                                                               .host("dynamodb.us-west-1.amazonaws.com")
//                                                               .protocol("https")
//                                                               .encodedPath("/")
//                                                               .header("Content-Length", Integer.toString(content.length()))
//                                                               .header("Content-Type", " application/x-amz-json-1.0")
//                                                               .header("X-Amz-Target", "DynamoDB_20120810.GetItem")
//                                                               .content(contentStream)
//                                                               .build();
//        Aws4Signer signer = new Aws4Signer();
//        SdkHttpFullRequest signedRequest = signer.sign(httpFullRequest, constructSignerParams());
//
//        ApacheSdkHttpClientFactory httpClientFactory = ApacheSdkHttpClientFactory.builder().build();
//        SdkHttpClient httpClient = httpClientFactory.createHttpClient();
//
//        SdkRequestContext context = SdkRequestContext.builder().build();
//
//        SdkHttpFullResponse response = httpClient.prepareRequest(signedRequest, context)
//                                                 .call();
//
//        //
//        String str = IoUtils.toString(response.content().get());
//        System.out.println(str);
//    }
//
//    private SignerContext constructSignerParams() {
//        AwsSignerParams signerParams = new AwsSignerParams();
//        signerParams.setAwsCredentials(DefaultCredentialsProvider.create().getCredentials());
//        signerParams.setSigningName("dynamodb");
//        signerParams.setRegion(Region.US_WEST_1);
//
//        SignerContext signerContext = new SignerContext();
//        signerContext.putAttribute(AwsExecutionAttributes.SIGNER_PARAMS, signerParams);
//        return signerContext;
//    }
}