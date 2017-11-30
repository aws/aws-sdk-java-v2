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
import software.amazon.awssdk.core.auth.AwsCredentials;
import software.amazon.awssdk.core.client.builder.ClientAsyncHttpConfiguration;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.http.nio.netty.NettySdkHttpClientFactory;
import software.amazon.awssdk.http.nio.netty.h2.NettyH2AsyncHttpClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.ListStreamsResponse;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.awssdk.utils.FunctionalUtils;

public class H2Demo {

    public static final int COUNT = 500_000;
    public static final int INTERVAL = 10;

    public static void main(String[] args) throws InterruptedException {
        BasicConfigurator.configure();

        System.setProperty(AwsSystemSetting.AWS_CBOR_ENABLED.property(), "false");
        NettyH2AsyncHttpClient sdkHttpClient = new NettyH2AsyncHttpClient();
        KinesisAsyncClient client = KinesisAsyncClient
            .builder()
            .credentialsProvider(() -> new AwsCredentials("AKIAFKNUZVAC6HDWUJRA", "YF/V6JcKVN30trTF5jqgXEVAJNkAOb/N20GXuHsq"))
            .asyncHttpConfiguration(ClientAsyncHttpConfiguration
                                        .builder()
                                        .httpClient(sdkHttpClient)
//                                         .httpClientFactory(NettySdkHttpClientFactory.builder().trustAllCertificates(true).build())
                                        .build())
            .region(Region.US_EAST_1)
            .endpointOverride(URI.create("https://yasemin-6.desktop.amazon.com:8001"))
            .build();

        client.listStreams()
              .thenApply(ListStreamsResponse::streamNames)
              .thenAccept(s -> s.forEach(System.out::println))
              .join();

        //        List<Throwable> exceptions = new ArrayList<>();
        //        CountDownLatch latch = new CountDownLatch(COUNT);
        //        AtomicInteger submitCount = new AtomicInteger(0);
        //
        //        int workerThreadCount = 1;
        //        int councurrentConnections = 1;
        //        ExecutorService executorService = Executors.newFixedThreadPool(workerThreadCount);
        //        for (int i = 0; i < workerThreadCount; i++) {
        //            executorService.submit(() -> {
        //                Semaphore permits = new Semaphore(councurrentConnections / workerThreadCount);
        //                while (submitCount.incrementAndGet() <= COUNT) {
        //                    invokeSafely((FunctionalUtils.UnsafeRunnable) permits::acquire);
        //                    client.putRecord(PutRecordRequest.builder()
        //                                                     .stream("FooStream")
        //                                                     .key("mykey")
        //                                                     .explicitHashKey("mykey")
        //                                                     .data(ByteBuffer.wrap(new byte[] {1, 2, 3}))
        //                                                     .build())
        //                          .whenComplete((r, e) -> {
        //                              System.out.println(r);
        //                              if (submitCount.get() % INTERVAL == 0) {
        //                                  System.out.println("COUNT=" + submitCount.get());
        //                              }
        //                              permits.release();
        //                              if (e != null) {
        //                                  exceptions.add(e);
        //                              }
        //                              latch.countDown();
        //                          });
        //                }
        //            });
        //        }
        //        latch.await();
        //        System.out.println("Exceptions::::::::");
        //        for (Throwable e : exceptions) {
        //            e.printStackTrace();
        //        }
        //        executorService.shutdown();
        //        executorService.awaitTermination(30, TimeUnit.SECONDS);
        System.out.println("SHUTTING DOWN CLIENT");
        client.close();
        sdkHttpClient.close();
    }
}
