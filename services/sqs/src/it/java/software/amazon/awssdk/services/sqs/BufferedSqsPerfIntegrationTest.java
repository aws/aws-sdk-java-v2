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

package software.amazon.awssdk.services.sqs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.services.sqs.buffered.SqsBufferedAsyncClient;
import software.amazon.awssdk.services.sqs.buffered.QueueBufferConfig;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * Spawns up a number of threads to send receive and delete messages from a single queue, and prints
 * throughput statistics every five seconds. Sender,receiver and deleter threads communicate with
 * one another, and the slowest operation will become the bottleneck. threads performing faster
 * operations will either sleep or spin idle while waiting for the other threads. <br>
 * This class assumes that functional tests have already been run and that everything works. It is
 * intended to measure throughput, not find bugs.
 */
public class BufferedSqsPerfIntegrationTest extends IntegrationTestBase {

    public static final String BODY = "a";
    private static final int MAX_SENDER = 15;
    private static final int MAX_CONSUMER = 15;
    private static final int MAX_DELETER = 15;
    private static final int MAX_LIST = 15;

    /**
     * how long the test will run. the 1 minute default is woefully inadequate for any serious
     * performance test, but is acceptable for experimenting with various settings and for
     * troubleshooting. those intending to run this test should modify it as need when running
     * proper pef tests
     */
    private static final long RUN_TIME_MS = 60_000;

    private final String queueName = getUniqueQueueName();
    private AtomicLong sendCounter = new AtomicLong(0);
    private AtomicLong receiveCounter = new AtomicLong(0);
    private AtomicLong deleteCounter = new AtomicLong(0);

    @Test
    @Ignore
    public void testPerf() throws Exception {
        QueueBufferConfig config = new QueueBufferConfig();
        config.setMaxInflightOutboundBatches(1000);
        config.setMaxInflightReceiveBatches(100);
        config.setMaxDoneReceiveBatches(100);
        SQSAsyncClient buffSqs = new SqsBufferedAsyncClient(sqsAsync, config);

        BasicConfigurator.resetConfiguration();
        Logger logger = Logger.getRootLogger();
        logger.setLevel(Level.ERROR);
        ExecutorService exec = Executors.newCachedThreadPool();

        CreateQueueResponse createRes = buffSqs.createQueue(CreateQueueRequest.builder().queueName(queueName).build()).join();

        AtomicBoolean keepGoing = new AtomicBoolean(true);
        List<Future<?>> allFutures = new LinkedList<Future<?>>();

        Set<String> sentSet = new HashSet<String>();

        List<List<String>> handleLists = new ArrayList<List<String>>(MAX_LIST);
        for (int i = 0; i < MAX_LIST; i++) {
            handleLists.add(new LinkedList<String>());
        }

        for (int i = 0; i < MAX_SENDER; i++) {
            Sender sender = new Sender(keepGoing, buffSqs, sendCounter, createRes.queueUrl(), sentSet);
            allFutures.add(exec.submit(sender));
        }
        for (int i = 0; i < MAX_CONSUMER; i++) {
            Consumer consumer = new Consumer(keepGoing, buffSqs, receiveCounter, createRes.queueUrl(), sentSet,
                                             handleLists);
            allFutures.add(exec.submit(consumer));
        }

        for (int i = 0; i < MAX_DELETER; i++) {
            Deleter deleter = new Deleter(keepGoing, buffSqs, deleteCounter, createRes.queueUrl(), handleLists);
            allFutures.add(exec.submit(deleter));
        }

        long start = System.currentTimeMillis();
        long split = start;
        long lastSent = 0;
        long lastRec = 0;
        long lastDel = 0;

        while (true) {

            try {
                Thread.sleep(5000);
            } catch (InterruptedException ie) {
                // do nothing
            }
            long sent = sendCounter.get();
            long received = receiveCounter.get();
            long deleted = deleteCounter.get();
            long now = System.currentTimeMillis();

            int sentSetSize = 0;
            int handleListSize = 0;

            synchronized (sentSet) {
                sentSetSize = sentSet.size();
            }

            for (int i = 0; i < handleLists.size(); i++) {
                List<String> theList = handleLists.get(i);
                synchronized (theList) {
                    handleListSize += theList.size();
                }
            }

            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
            System.out.println("SentSetSize=" + sentSetSize + " HandleListSize=" + handleListSize);
            print("Sent", sent, lastSent, now, split, start);
            print("Received", received, lastRec, now, split, start);
            print("Deleted", deleted, lastDel, now, split, start);

            split = now;
            lastSent = sent;
            lastRec = received;
            lastDel = deleted;

            if ((now - start) > RUN_TIME_MS) {
                break;
            } else {
                System.out.println("Seconds left to run: " + ((RUN_TIME_MS - now + start) / 1000));
            }

        }

        keepGoing.set(false);

        for (Future<?> f : allFutures) {
            f.get();
            System.out.println("A task completed....");
        }

        System.out.println("All tasks completed....");

    }

    void print(String statName, long total, long last, long totalMs, long splitMs, long startMs) {
        long split = total - last;
        long totalSec = (totalMs - startMs) / 1000;
        totalSec = totalSec > 0 ? totalSec : 1;
        long splitSec = (totalMs - splitMs) / 1000;
        splitSec = splitSec > 0 ? splitSec : 1;

        long totalRate = total / totalSec;
        long splitRate = split / splitSec;

        System.out.println(statName + " ttlRate=" + totalRate + " splitRate=" + splitRate + " count=" + total
                           + " splitCount=" + split + " ttlSec=" + totalSec + " splitSec=" + splitSec);

    }

    class Sender implements Runnable {

        AtomicBoolean keepGoing;
        SQSAsyncClient sqs;
        AtomicLong counter;
        String url;
        Set<String> sent;

        public Sender(AtomicBoolean paramGoing, SQSAsyncClient paramSqs, AtomicLong paramCounter, String paramUrl,
                      Set<String> paramSent) {
            keepGoing = paramGoing;
            sqs = paramSqs;
            counter = paramCounter;
            url = paramUrl;
            sent = paramSent;
        }

        public void run() {

            System.out.println("Sender starting...");

            while (keepGoing.get()) {
                final String body = BufferedSqsPerfIntegrationTest.BODY + counter.addAndGet(1) + "_" + System.nanoTime();
                SendMessageRequest sendReq = SendMessageRequest.builder()
                        .queueUrl(url)
                        .messageBody(body)
                        .build();
                sqs.sendMessage(sendReq);
                int size = 0;
                synchronized (sent) {
                    sent.add(body);
                    size = sent.size();
                }

                if (size > 10000) {
                    // back off on sending
                    try {
                        Thread.sleep(size - 10000);
                    } catch (InterruptedException ie) {
                        // nothing
                    }
                }
            }
            System.out.println("Sender finishing...");

        }
    }

    class Consumer implements Runnable {

        final Random random = new Random();
        AtomicBoolean keepGoing;
        SQSAsyncClient sqs;
        AtomicLong recCount;
        String url;
        Set<String> sentSet;
        List<List<String>> hanldeListList;

        public Consumer(AtomicBoolean paramGoing, SQSAsyncClient paramSqs, AtomicLong paramRecCount, String paramUrl,
                        Set<String> paramSentSet, List<List<String>> paramHandleSet) {
            keepGoing = paramGoing;
            sqs = paramSqs;
            recCount = paramRecCount;
            url = paramUrl;
            sentSet = paramSentSet;
            hanldeListList = paramHandleSet;
        }

        public void run() {

            System.out.println("Consumer starting...");
            while (keepGoing.get()) {
                ReceiveMessageRequest recReq = ReceiveMessageRequest.builder()
                        .queueUrl(url)
                        .build();
                ReceiveMessageResponse recRes = sqs.receiveMessage(recReq).join();
                recCount.addAndGet(recRes.messages().size());
                int listSize = 0;
                for (Message m : recRes.messages()) {
                    synchronized (sentSet) {
                        sentSet.remove(m.body());
                    }

                    List<String> list = hanldeListList.get(random.nextInt(hanldeListList.size()));
                    synchronized (list) {
                        list.add(m.receiptHandle());
                        listSize = list.size();
                    }
                }

                if (listSize > 1000) {
                    // back off on receiving
                    try {

                        Thread.sleep(listSize - 1000);
                    } catch (InterruptedException ie) {
                        // nothing
                    }
                }
            }
            System.out.println("Consumer finishing...");
        }
    }

    class Deleter implements Runnable {

        AtomicBoolean keepGoing;
        SQSAsyncClient sqs;
        AtomicLong delCount;
        String url;
        List<List<String>> hanldeListList;

        public Deleter(AtomicBoolean paramGoing, SQSAsyncClient paramSqs, AtomicLong paramDelCount, String paramUrl,
                       List<List<String>> paramHandleSet) {
            keepGoing = paramGoing;
            sqs = paramSqs;
            delCount = paramDelCount;
            url = paramUrl;
            hanldeListList = paramHandleSet;
        }

        public void run() {

            Random random = new Random();
            boolean setWasEmpty = false;
            int listIndex = random.nextInt(hanldeListList.size());

            System.out.println("Deleter starting...");
            while (!setWasEmpty || keepGoing.get()) {

                List<String> list = hanldeListList.get(listIndex);
                listIndex = (listIndex + 1) % hanldeListList.size();
                int outstanding = 0;
                String handle = null;
                synchronized (list) {
                    outstanding = list.size();
                    if (!list.isEmpty()) {
                        handle = list.remove(0);
                        setWasEmpty = false;
                    } else {
                        setWasEmpty = true;
                    }
                }

                if (!setWasEmpty && !keepGoing.get() && (outstanding % 100) == 0) {
                    System.out.println("Mop-up; " + outstanding + " left");
                }

                if (null != handle) {
                    DeleteMessageRequest delReq = DeleteMessageRequest.builder()
                            .queueUrl(url)
                            .receiptHandle(handle)
                            .build();
                    sqs.deleteMessage(delReq);
                    delCount.addAndGet(1);

                } else {
                    // this should not happen too often
                    // System.out.println("Deleter thread sleeping....");
                    try {
                        Thread.sleep(100);

                    } catch (InterruptedException ie) {
                        // nothing
                    }
                }

            }
            System.out.println("Deleter finishing...");
        }
    }
}
