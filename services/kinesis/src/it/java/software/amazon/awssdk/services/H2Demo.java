package software.amazon.awssdk.services;

import static software.amazon.awssdk.utils.FunctionalUtils.invokeSafely;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.BasicConfigurator;
import software.amazon.awssdk.core.AwsSystemSetting;
import software.amazon.awssdk.core.client.builder.ClientAsyncHttpConfiguration;
import software.amazon.awssdk.http.nio.netty.NettySdkHttpClientFactory;
import software.amazon.awssdk.http.nio.netty.h2.NettyH2AsyncHttpClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.utils.FunctionalUtils;

public class H2Demo {

    public static final int COUNT = 500_000;

    public static void main(String[] args) throws InterruptedException {
        BasicConfigurator.configure();
        System.setProperty(AwsSystemSetting.AWS_CBOR_ENABLED.property(), "false");
        NettyH2AsyncHttpClient sdkHttpClient = new NettyH2AsyncHttpClient();
        KinesisAsyncClient client = KinesisAsyncClient
            .builder()
            .asyncHttpConfiguration(ClientAsyncHttpConfiguration
                                        .builder()
                                        .httpClient(sdkHttpClient)
                                        // httpClientFactory(NettySdkHttpClientFactory.builder().trustAllCertificates(true).build())
                                        .build())
            .endpointOverride(URI.create("https://bmercier-2.aka.corp.amazon.com:8001/"))
            .build();

        List<Throwable> exceptions = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(COUNT);
        AtomicInteger submitCount = new AtomicInteger(0);

        int workerThreadCount = 1;
        int councurrentConnections = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(workerThreadCount);
        for (int i = 0; i < workerThreadCount; i++) {
            executorService.submit(() -> {
                Semaphore permits = new Semaphore(councurrentConnections / workerThreadCount);
                while (submitCount.incrementAndGet() <= COUNT) {
                    invokeSafely((FunctionalUtils.UnsafeRunnable) permits::acquire);
                    client.putRecord(PutRecordRequest.builder()
                                                     .streamName("FooStream")
                                                     .partitionKey("mykey")
                                                     .explicitHashKey("mykey")
                                                     .data(ByteBuffer.wrap(new byte[] {1, 2, 3}))
                                                     .build())
                          .whenComplete((r, e) -> {
                              if (submitCount.get() % 10_000 == 0) {
                                  System.out.println("COUNT=" + submitCount.get());
                              }
                              permits.release();
                              if (e != null) {
                                  exceptions.add(e);
                              }
                              latch.countDown();
                          });
                }
            });
        }
        latch.await();
        System.out.println("Exceptions::::::::");
        for (Throwable e : exceptions) {
            e.printStackTrace();
        }
        executorService.shutdown();
        executorService.awaitTermination(30, TimeUnit.SECONDS);
        System.out.println("SHUTTING DOWN CLIENT");
        client.close();
        sdkHttpClient.close();
    }
}
