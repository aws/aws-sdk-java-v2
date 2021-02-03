package software.amazonaws.test;

import software.amazon.awssdk.services.s3.S3AsyncClient;


public class Handler {
    private final S3AsyncClient s3Client;

    public Handler() {
        s3Client = DependencyFactory.s3Client();
    }

    public void sendRequest() {
        // TODO: invoking the api calls using s3Client.
    }
}
