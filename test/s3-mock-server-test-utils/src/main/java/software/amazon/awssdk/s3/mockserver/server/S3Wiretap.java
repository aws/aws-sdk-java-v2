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

package software.amazon.awssdk.s3.mockserver.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.s3.mockserver.helpers.ByteArrays;
import software.amazon.awssdk.s3.mockserver.helpers.CaseInsensitiveMap;
import software.amazon.awssdk.s3.mockserver.helpers.CaseInsensitiveTreeMap;
import software.amazon.awssdk.s3.mockserver.helpers.Immutable;
import software.amazon.awssdk.utils.SdkAutoCloseable;

public class S3Wiretap implements SdkAutoCloseable {
    public static final String HOST = InetAddress.getLoopbackAddress().getHostAddress();

    public enum Mode { MOCK, FORWARD }

    private static final Logger LOG = LogManager.getLogger("software.amazon.awssdk.s3.mockserver.server.wiretap");
    private static final int MAX_BODY_CHARS = 4096;
    private static final long CLIENT_WRITE_STALL_MILLIS =
            Long.getLong("wiretap.clientWriteStallMillis", 10_000);

    private static final long STALL_POLL_MILLIS = 50;

    private static final long IN_FLIGHT_POLL_MILLIS = 50;

    private static final int MAX_CAPTURE_BYTES = 1024 * 1024;
    private static final int HISTORY_BODY_PREFIX = 64 * 1024;
    private static final byte[] CRLF = {'\r', '\n'};
    private static final OutputStream DISCARD = new OutputStream() {
        @Override
        public void write(int b) {
        }

        @Override
        public void write(byte[] b, int off, int len) {
        }
    };

    private static final Map<Integer, S3Wiretap> BY_PORT = new java.util.concurrent.ConcurrentHashMap<>();

    private final ServerSocket serverSocket;
    private final ExecutorService executor;
    private final SSLSocketFactory tlsServerFactory;
    private final SSLSocketFactory tlsClientFactory;
    private final AtomicInteger connectionCounter = new AtomicInteger();
    private final List<LiveExchange> exchanges = new ArrayList<>();
    private final Object exchangeSignal = new Object();
    private long ledgerVersion;

    private final List<CapturedExchange> history = new ArrayList<>();

    private long historyEpoch;

    private final Object historyLock = new Object();

    private final List<MockResponder> mockResponders = new CopyOnWriteArrayList<>();
    private final List<ServedVersion> servedVersions = new CopyOnWriteArrayList<>();
    private final List<String> unmatchedMockRequests = new CopyOnWriteArrayList<>();
    private final List<ExchangeManipulator> interceptors = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final java.util.Map<ExchangeManipulator, Integer> interceptorMatchCounts =
            new java.util.concurrent.ConcurrentHashMap<>();
    private volatile Mode mode = Mode.MOCK;

    public S3Wiretap() throws IOException {
        this.serverSocket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "connect-proxy");
            t.setDaemon(true);
            return t;
        });
        this.tlsServerFactory = buildServerFactory();
        this.tlsClientFactory = buildTrustAllClientFactory();
        executor.execute(this::acceptLoop);
        BY_PORT.put(port(), this);
    }

    public Mode mode() {
        return mode;
    }

    public S3Wiretap mode(Mode mode) {
        this.mode = mode;
        return this;
    }

    public static S3Wiretap listeningOn(int port) {
        S3Wiretap wiretap = BY_PORT.get(port);
        if (wiretap == null) {
            throw new IllegalStateException("no open wiretap listens on port " + port);
        }
        return wiretap;
    }

    public static long clientWriteStallMillis() {
        return CLIENT_WRITE_STALL_MILLIS;
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    public void enqueueResponse(Response response) {
        mockResponders.add(new OneShotResponder(response));
    }

    public List<ServedVersion> servedVersions() {
        return Immutable.copyOfList(servedVersions);
    }

    public void enqueueResponder(MockResponder responder) {
        servedVersions.addAll(responder.servedVersions());
        mockResponders.add(responder);
    }

    public void enqueueResponder(Predicate<Request> filter,
                                 BiFunction<Request, byte[], Response> producer) {
        mockResponders.add(MockResponder.of(filter, producer));
    }

    public void registerInterceptor(ExchangeManipulator manipulator) {
        interceptors.add(manipulator);
        interceptorMatchCounts.putIfAbsent(manipulator, 0);
    }

    public void clearInterceptors() {
        interceptors.clear();
        interceptorMatchCounts.clear();
    }

    public int unmatchedInterceptorCount() {
        return (int) interceptorMatchCounts.values().stream().filter(c -> c == 0).count();
    }

    public List<String> unmatchedMockRequests() {
        return Immutable.copyOfList(unmatchedMockRequests);
    }

    public void clearPendingResponses() {
        mockResponders.clear();
        clearInterceptors();
    }

    public enum Inclusion {
        REGISTERED,
        SETTLED
    }

    public void reset() {
        clearExchanges();
        servedVersions.clear();
        synchronized (historyLock) {
            history.clear();
            historyEpoch++;
        }
        mockResponders.clear();
        unmatchedMockRequests.clear();
        clearInterceptors();
    }

    public List<CapturedExchange> takeExchanges() {
        List<LiveExchange> drained = new ArrayList<>();
        synchronized (exchangeSignal) {
            exchanges.removeIf(live -> {
                if (!live.settled()) {
                    return false;
                }
                drained.add(live);
                return true;
            });
            ledgerVersion++;
            exchangeSignal.notifyAll();
        }
        return snapshotAll(drained);
    }

    public List<CapturedRequest> takeRequests() {
        return takeExchanges().stream().map(CapturedExchange::request).collect(Immutable.toList());
    }

    public List<CapturedExchange> exchanges() {
        return snapshotAll(live());
    }

    public List<CapturedRequest> requests() {
        return exchanges().stream().map(CapturedExchange::request).collect(Immutable.toList());
    }

    public int requestCount() {
        return exchangeCount(Inclusion.REGISTERED);
    }

    public int exchangeCount(Inclusion inclusion) {
        synchronized (exchangeSignal) {
            return countLocked(inclusion);
        }
    }

    public CapturedExchange exchangeMatching(java.util.function.Predicate<CapturedRequest> predicate) {
        List<CapturedExchange> matches = exchangesMatching(predicate);
        if (matches.size() != 1) {
            List<CapturedExchange> all = exchanges();
            throw new IllegalStateException("Expected exactly one matching request, found "
                    + matches.size() + " of " + all.size() + " recorded "
                    + all.stream().map(e -> e.state().name()).collect(Immutable.toList())
                    + (all.stream().anyMatch(CapturedExchange::inFlight)
                    ? " — at least one exchange is still in flight, so this may be traffic this test"
                    + " caused but has not waited for; see S3Wiretap.Inclusion"
                    : ""));
        }
        return matches.get(0);
    }

    public List<CapturedExchange> exchangesMatching(java.util.function.Predicate<CapturedRequest> predicate) {
        return live().stream()
                .filter(LiveExchange::hasResponseHead)
                .map(LiveExchange::snapshot)
                .filter(e -> predicate.test(e.request()))
                .collect(Immutable.toList());
    }

    public List<CapturedRequest> requestsMatching(java.util.function.Predicate<CapturedRequest> predicate) {
        return requests().stream().filter(predicate).collect(Immutable.toList());
    }

    public List<CapturedExchange> history() {
        List<CapturedExchange> copy;
        synchronized (historyLock) {
            copy = new ArrayList<>(history);
        }
        copy.sort(java.util.Comparator.comparingLong(CapturedExchange::startNanos));
        return copy;
    }

    private void admitToHistory(LiveExchange live) {
        CapturedExchange settled = trimmedForHistory(live.snapshot());
        synchronized (historyLock) {
            if (live.epoch() == historyEpoch) {
                history.add(settled);
            }
        }
    }

    private static CapturedExchange trimmedForHistory(CapturedExchange exchange) {
        CapturedRequest q = exchange.request();
        CapturedRequest request = !"PUT".equals(q.method()) ? q : new CapturedRequest(q.method(), q.path(),
                q.queryParameters(), q.headers(), head(q.rawBody()), head(q.decodedBody()), q.fullBodyLength(),
                q.fullBodyDigest(), q.trailingHeaders(), q.connectHost());
        CapturedResponse a = exchange.response();
        CapturedResponse response = a == null || !S3RequestFilters.getObject().test(q) ? a : new CapturedResponse(a.statusCode(),
                a.statusText(), a.headers(), head(a.body()), head(a.decodedBody()), a.bodyTotalLength());
        return new CapturedExchange(request, response, null, null, exchange.startNanos(), exchange.endNanos(),
                exchange.state());
    }

    private static byte[] head(byte[] body) {
        return body == null || body.length <= HISTORY_BODY_PREFIX ? body : java.util.Arrays.copyOf(body, HISTORY_BODY_PREFIX);
    }

    private long currentEpoch() {
        synchronized (historyLock) {
            return historyEpoch;
        }
    }

    public void clearExchanges() {
        synchronized (exchangeSignal) {
            exchanges.clear();
            ledgerVersion++;
            exchangeSignal.notifyAll();
        }
    }

    private List<LiveExchange> live() {
        synchronized (exchangeSignal) {
            return new ArrayList<>(exchanges);
        }
    }

    private static List<CapturedExchange> snapshotAll(List<LiveExchange> entries) {
        return entries.stream().map(LiveExchange::snapshot).collect(Immutable.toList());
    }

    private void register(LiveExchange live) {
        synchronized (exchangeSignal) {
            exchanges.add(live);
            ledgerVersion++;
            exchangeSignal.notifyAll();
        }
    }

    private void signalLedgerChange() {
        synchronized (exchangeSignal) {
            ledgerVersion++;
            exchangeSignal.notifyAll();
        }
    }

    public int awaitExchangeCount(int target, Inclusion inclusion, long timeoutMillis) {
        long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
        synchronized (exchangeSignal) {
            while (countLocked(inclusion) < target) {
                long remainingMillis = (deadline - System.nanoTime()) / 1_000_000L;
                if (remainingMillis <= 0) {
                    break;
                }
                try {
                    exchangeSignal.wait(remainingMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            return countLocked(inclusion);
        }
    }

    private int countLocked(Inclusion inclusion) {
        if (inclusion == Inclusion.REGISTERED) {
            return exchanges.size();
        }
        return (int) exchanges.stream().filter(LiveExchange::settled).count();
    }

    public int awaitQuiescence(long idleMillis, long timeoutMillis) {
        long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
        synchronized (exchangeSignal) {
            long lastChange = System.nanoTime();
            long lastVersion = ledgerVersion;
            while (true) {
                long now = System.nanoTime();
                if (ledgerVersion != lastVersion) {
                    lastVersion = ledgerVersion;
                    lastChange = now;
                }
                long idleSoFarMillis = (now - lastChange) / 1_000_000L;
                boolean anyInFlight = exchanges.stream().anyMatch(live -> !live.settled());
                boolean idleLongEnough = idleSoFarMillis >= idleMillis;
                if (idleLongEnough && !anyInFlight) {
                    return countLocked(Inclusion.SETTLED);
                }
                long remainingDeadlineMillis = (deadline - now) / 1_000_000L;
                if (remainingDeadlineMillis <= 0) {
                    return countLocked(Inclusion.SETTLED);
                }
                long nextCheckMillis = idleLongEnough
                        ? IN_FLIGHT_POLL_MILLIS
                        : idleMillis - idleSoFarMillis;
                long waitMillis = Math.min(nextCheckMillis, remainingDeadlineMillis);
                try {
                    exchangeSignal.wait(waitMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return countLocked(Inclusion.SETTLED);
                }
            }
        }

    }

    private static CapturedRequest toCapturedRequest(HttpMessage request, BodyFacts body) {
        String fullPath = request.path != null ? request.path : "";
        int q = fullPath.indexOf('?');
        String path = q < 0 ? fullPath : fullPath.substring(0, q);
        String query = q < 0 ? "" : fullPath.substring(q + 1);

        Map<String, List<String>> queryParameters = new LinkedHashMap<>();
        if (!query.isEmpty()) {
            for (String pair : query.split("&")) {
                if (pair.isEmpty()) {
                    continue;
                }
                int eq = pair.indexOf('=');
                String name = eq < 0 ? pair : pair.substring(0, eq);
                String value = eq < 0 ? "" : pair.substring(eq + 1);
                queryParameters.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
            }
        }

        CaseInsensitiveTreeMap<List<String>> headers = new CaseInsensitiveTreeMap<>();
        for (String[] h : request.headers) {
            headers.computeIfAbsent(h[0], k -> new ArrayList<>()).add(h[1]);
        }

        CaseInsensitiveMap<List<String>> trailingHeaders = new CaseInsensitiveTreeMap<>(body.trailers());

        return new CapturedRequest(request.method, path, queryParameters, headers,
                body.prefix(), body.decodedPrefix(), body.payloadLength(), body.digest(),
                trailingHeaders, request.connectHost);
    }

    private static CapturedResponse toCapturedResponse(HttpMessage response, BodyFacts body) {
        CaseInsensitiveTreeMap<List<String>> headers = new CaseInsensitiveTreeMap<>();
        for (String[] h : response.headers) {
            headers.computeIfAbsent(h[0], k -> new ArrayList<>()).add(h[1]);
        }
        int status = response.statusCode.isEmpty() ? 0 : Integer.parseInt(response.statusCode);
        String statusText = reasonFromStatusLine(response.startLine);

        return new CapturedResponse(status, statusText, headers,
                body.prefix(), body.decodedPrefix(), body.wireLength());
    }

    private static SSLSocketFactory buildServerFactory() {
        return SelfSignedCertificate.serverSocketFactory();
    }

    private static SSLSocketFactory buildTrustAllClientFactory() {
        try {
            TrustManager trustAll = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{trustAll}, new java.security.SecureRandom());
            return ctx.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] serveInMemory(String connectHost, byte[] request) throws IOException {
        if (mode != Mode.MOCK) {
            throw new IllegalStateException("the in-memory transport serves MOCK exchanges only; mode is " + mode);
        }
        int connId = connectionCounter.incrementAndGet();
        ByteArrayOutputStream toClient = new ByteArrayOutputStream();
        ClientSide clientSide = new ClientSide(
                new PushbackInputStream(new ByteArrayInputStream(request), 1), toClient, null);
        HttpMessage head = readNextRequest(clientSide.in());
        if (head == null) {
            throw new IOException("an in-memory exchange carried no request");
        }
        head.connectHost = connectHost;
        handleExchange(connId, head, clientSide, new MockQueueResponder(this::selectMockResponse));
        return toClient.toByteArray();
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                executor.execute(() -> handleConnection(clientSocket));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleConnection(Socket rawClient) {
        int connId = connectionCounter.incrementAndGet();
        try {
            String connectLine = readLine(rawClient.getInputStream());
            if (connectLine == null || !connectLine.startsWith("CONNECT ")) {
                rawClient.close();
                return;
            }
            String hostPort = connectLine.substring("CONNECT ".length()).split(" ")[0];
            String connectHost = hostPort.split(":")[0];
            String line;
            do {
                line = readLine(rawClient.getInputStream());
            } while (line != null && !line.isEmpty());

            rawClient.getOutputStream().write(
                    "HTTP/1.1 200 Connection Established\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
            rawClient.getOutputStream().flush();

            SSLSocket tlsClient = (SSLSocket) tlsServerFactory.createSocket(
                    rawClient, null, rawClient.getPort(), true);
            tlsClient.setUseClientMode(false);
            tlsClient.startHandshake();

            Socket upstream = mode == Mode.FORWARD ? openForwardUpstream(hostPort) : null;

            try {
                ResponseProvider responder = upstream == null
                        ? new MockQueueResponder(this::selectMockResponse)
                        : new SocketResponder(upstream);
                pumpRequests(connId, connectHost, tlsClient, rawClient, responder);
            } finally {
                if (upstream != null) {
                    quietClose(upstream);
                }
                quietClose(tlsClient);
                quietClose(rawClient);
            }
        } catch (IOException e) {
            quietClose(rawClient);
        }
    }

    private Socket openForwardUpstream(String hostPort) throws IOException {
        String[] hp = hostPort.split(":");
        String host = hp[0];
        int port = hp.length > 1 ? Integer.parseInt(hp[1]) : 443;
        SSLSocket sock = (SSLSocket) tlsClientFactory.createSocket(host, port);
        sock.startHandshake();
        return sock;
    }

    private void pumpRequests(int connId, String connectHost, Socket downstream, Socket rawDownstream,
                              ResponseProvider serverSide)
            throws IOException {
        ClientSide clientSide = new ClientSide(
                new PushbackInputStream(downstream.getInputStream(), 1),
                downstream.getOutputStream(),
                rawDownstream);

        HttpMessage request;
        while ((request = readNextRequest(clientSide.in())) != null) {
            request.connectHost = connectHost;
            if (!handleExchange(connId, request, clientSide, serverSide)) {
                return;
            }
        }
    }

    private static final class ClientSide {
        private final PushbackInputStream in;
        private final OutputStream out;
        private final Socket abortSocket;

        private ClientSide(PushbackInputStream in, OutputStream out, Socket abortSocket) {
            this.in = in;
            this.out = out;
            this.abortSocket = abortSocket;
        }

        public PushbackInputStream in() {
            return in;
        }

        public OutputStream out() {
            return out;
        }

        public Socket abortSocket() {
            return abortSocket;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ClientSide that = (ClientSide) o;
            return Objects.equals(in, that.in)
                && Objects.equals(out, that.out)
                && Objects.equals(abortSocket, that.abortSocket);
        }

        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + Objects.hashCode(in);
            result = 31 * result + Objects.hashCode(out);
            result = 31 * result + Objects.hashCode(abortSocket);
            return result;
        }

        @Override
        public String toString() {
            return "ClientSide[in=" + in + ", out=" + out + ", abortSocket=" + abortSocket + "]";
        }
    }

    private static HttpMessage readNextRequest(PushbackInputStream clientIn) throws IOException {
        int firstByte = clientIn.read();
        if (firstByte == -1) {
            return null;
        }
        clientIn.unread(firstByte);
        return readRequestHead(clientIn);
    }

    private boolean handleExchange(int connId, HttpMessage request,
                                   ClientSide clientSide, ResponseProvider serverSide) throws IOException {
        long startNanos = System.nanoTime();
        ExchangeManipulator manipulator = selectInterceptor(request);

        HttpMessage originalRequestHead = request.copy();

        LiveExchange live = new LiveExchange(originalRequestHead, startNanos, this::signalLedgerChange,
                currentEpoch(), this::admitToHistory);
        register(live);
        try {
            return handleExchange(connId, request, clientSide, serverSide, manipulator,
                    originalRequestHead, live);
        } catch (Throwable t) {
            live.failIfUnsettled(t);
            throw t;
        }
    }

    private boolean handleExchange(int connId, HttpMessage request, ClientSide clientSide,
                                   ResponseProvider serverSide, ExchangeManipulator manipulator,
                                   HttpMessage originalRequestHead, LiveExchange live) throws IOException {
        logHead(connId, ">>", request);
        if (manipulator != null) {
            applyRequestHeadHook(request, manipulator);
            logModifiedHead(connId, ">>", originalRequestHead, request);
            live.manipulatedRequestHead(request.copy());
        }
        serverSide.sendRequestHead(request);

        RequestExchange r = forwardRequest(connId, clientSide, serverSide, request, manipulator, live);
        if (r.finalResponse() == null) {
            live.noResponse();
            return false;
        }

        HttpMessage finalResponse = r.finalResponse();
        HttpMessage originalResponseHead = finalResponse.copy();
        if (manipulator != null) {
            applyResponseHeadHook(finalResponse, manipulator);
            logModifiedHead(connId, "<<", originalResponseHead, finalResponse);
        }
        live.responseHead(originalResponseHead, manipulator == null ? null : finalResponse.copy());

        LastByteHoldbackOutputStream toClient = new LastByteHoldbackOutputStream(clientSide.out());
        HttpMessage clientHead = finalResponse.copy();
        forceConnectionClose(clientHead);
        clientHead.writeStartLineAndHeaders(toClient);
        BodyCapture responseBody = streamResponseBody(connId, request, finalResponse, toClient, clientSide.abortSocket(),
                serverSide, manipulator, live);

        live.responseStreamed(responseBody.truncated());

        if (responseBody.truncated()) {
            LOG.debug("client abandoned the response body after {} byte(s); marked as truncated",
                    responseBody.originalLength());
            return false;
        }

        toClient.releaseLast();

        return false;
    }

    private static void forceConnectionClose(HttpMessage msg) {
        msg.headers.removeIf(h -> h[0].equalsIgnoreCase("Connection")
                || h[0].equalsIgnoreCase("Keep-Alive"));
        msg.headers.add(new String[]{"Connection", "close"});
    }

    private static final class RequestExchange {
        private final BodyCapture requestBody;
        private final HttpMessage finalResponse;

        private RequestExchange(BodyCapture requestBody, HttpMessage finalResponse) {
            this.requestBody = requestBody;
            this.finalResponse = finalResponse;
        }

        public BodyCapture requestBody() {
            return requestBody;
        }

        public HttpMessage finalResponse() {
            return finalResponse;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RequestExchange that = (RequestExchange) o;
            return Objects.equals(requestBody, that.requestBody)
                && Objects.equals(finalResponse, that.finalResponse);
        }

        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + Objects.hashCode(requestBody);
            result = 31 * result + Objects.hashCode(finalResponse);
            return result;
        }

        @Override
        public String toString() {
            return "RequestExchange[requestBody=" + requestBody + ", finalResponse=" + finalResponse + "]";
        }
    }

    private RequestExchange forwardRequest(int connId, ClientSide clientSide,
                                           ResponseProvider serverSide, HttpMessage request,
                                           ExchangeManipulator manipulator, LiveExchange live) throws IOException {
        if (request.expectsContinue()) {
            HttpMessage head = readResponseHead(connId, serverSide);
            if (head == null) {
                live.requestRead();
                return new RequestExchange(BodyCapture.empty(), null);
            }
            if (!head.statusCode.equals("100")) {
                live.requestRead();
                return new RequestExchange(BodyCapture.empty(), head);
            }
            head.writeStartLineAndHeaders(clientSide.out());
        }

        BodyHook hook = manipulator == null ? null
                : raw -> manipulator.manipulateRequestBody(readonlyHeaders(request), raw);
        BodyCapture requestBody = forwardRequestBody(connId, clientSide, serverSide, request, hook, live);
        live.requestRead();
        return new RequestExchange(requestBody, readFinalResponse(connId, clientSide, serverSide));
    }

    private BodyCapture forwardRequestBody(int connId, ClientSide clientSide,
                                           ResponseProvider serverSide, HttpMessage request, BodyHook hook,
                                           LiveExchange live)
            throws IOException {
        try {
            BodyCapture body = serverSide.forwardRequestBody(clientSide.in(), request, hook, live.requestSink());
            logBody(connId, ">>", body);
            return body;
        } catch (ClientAbortedRequestException e) {
            LOG.debug("[{}] >> client abandoned the request body: {}", connId, e.getMessage());
            live.requestAborted();
            throw e;
        }
    }

    private static HttpMessage readResponseHead(int connId, ResponseProvider serverSide) throws IOException {
        HttpMessage head = serverSide.readResponseHead();
        if (head == null) {
            LOG.debug("[{}] << <upstream closed>", connId);
            return null;
        }
        logHead(connId, "<<", head);
        return head;
    }

    private HttpMessage readFinalResponse(int connId, ClientSide clientSide, ResponseProvider serverSide)
            throws IOException {
        while (true) {
            HttpMessage head = readResponseHead(connId, serverSide);
            if (head == null) {
                return null;
            }
            if (!head.statusCode.startsWith("1")) {
                return head;
            }
            head.writeStartLineAndHeaders(clientSide.out());
        }
    }

    private BodyCapture streamResponseBody(int connId, HttpMessage request, HttpMessage response,
                                           OutputStream dst, Socket clientSocket,
                                           ResponseProvider serverSide,
                                           ExchangeManipulator manipulator, LiveExchange live) throws IOException {
        if (!responseHasBody(request, response)) {
            return BodyCapture.empty();
        }
        BodyHook hook = manipulator == null ? null
                : raw -> manipulator.manipulateResponseBody(readonlyHeaders(response), raw);
        boolean tolerateClientGoingAway = mode == Mode.MOCK;
        InputStream source = serverSide.responseBodySource(response);
        BodyCapture body;
        if (tolerateClientGoingAway && clientSocket == null) {
            body = pumpBody(source, response, new ClientWriteOutputStream(dst), hook, true, false,
                    live.responseSink());
        } else if (tolerateClientGoingAway) {
            ClientWriteOutputStream toClient = new ClientWriteOutputStream(dst);
            try (StallGuard guard = new StallGuard(clientSocket, toClient,
                    CLIENT_WRITE_STALL_MILLIS, connId)) {
                body = pumpBody(source, response, toClient, hook, true, false, live.responseSink());
            }
        } else {
            body = pumpBody(source, response, dst, hook, false, false, live.responseSink());
        }
        logBody(connId, "<<", body);
        return body;
    }

    private static boolean responseHasBody(HttpMessage request, HttpMessage response) {
        if ("HEAD".equalsIgnoreCase(request.method)) {
            return false;
        }
        String code = response.statusCode;
        return !code.startsWith("1") && !code.equals("204") && !code.equals("304");
    }

    private static void applyRequestHeadHook(HttpMessage msg, ExchangeManipulator manipulator) {
        Request out = manipulator.manipulateRequestHead(toRequest(msg));
        msg.method = out.method();
        msg.path = out.path();
        setHeaders(msg, out.headers());
        msg.startLine = out.method() + " " + out.path() + " " + protocolVersion(msg.startLine, 2);
    }

    private static void applyResponseHeadHook(HttpMessage msg, ExchangeManipulator manipulator) {
        int code = msg.statusCode.isEmpty() ? 0 : Integer.parseInt(msg.statusCode);
        Response in = Response.builder()
                .statusCode(code)
                .statusText(reasonFromStatusLine(msg.startLine))
                .headers(toHeaderMap(msg.headers))
                .build();
        Response out = manipulator.manipulateResponseHead(in);
        msg.statusCode = Integer.toString(out.statusCode());
        setHeaders(msg, out.headers());
        msg.startLine = protocolVersion(msg.startLine, 0) + " " + out.statusCode() + " " + out.statusText();
    }

    private static CaseInsensitiveMap<List<String>> toHeaderMap(List<String[]> headers) {
        CaseInsensitiveTreeMap<List<String>> map = new CaseInsensitiveTreeMap<>();
        for (String[] h : headers) {
            map.computeIfAbsent(h[0], k -> new ArrayList<>()).add(h[1]);
        }
        return map;
    }

    private static void setHeaders(HttpMessage msg, Map<String, List<String>> headers) {
        msg.headers.clear();
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            for (String v : e.getValue()) {
                msg.headers.add(new String[]{e.getKey(), v});
            }
        }
    }

    private static String protocolVersion(String startLine, int index) {
        String[] parts = startLine.split(" ", 3);
        return parts.length > index ? parts[index] : "HTTP/1.1";
    }

    private static String reasonFromStatusLine(String statusLine) {
        String[] parts = statusLine.split(" ", 3);
        return parts.length > 2 ? parts[2] : "";
    }

    private ExchangeManipulator selectInterceptor(HttpMessage request) {
        Request view = toRequest(request);
        for (ExchangeManipulator candidate : interceptors) {
            if (candidate.matches(view)) {
                interceptorMatchCounts.merge(candidate, 1, Integer::sum);
                return candidate;
            }
        }
        return null;
    }

    private Response selectMockResponse(HttpMessage request, byte[] body) {
        Request view = toRequest(request);
        CapturedBody capturedBody =
                CapturedBody.of(body, request.fullBodyLength, request.trailingHeaders);
        for (MockResponder responder : mockResponders) {
            if (responder.matches(view)) {
                return responder.respond(view, capturedBody);
            }
        }
        String description = view.method() + " " + view.path();
        LOG.warn("No mock responder matched {} — synthesizing 500 and flagging for teardown", description);
        unmatchedMockRequests.add(description);
        return Response.builder()
                .status(500)
                .header("Connection", "close")
                .build();
    }

    private static final class OneShotResponder implements MockResponder {
        private final Response response;
        private final AtomicBoolean used = new AtomicBoolean(false);

        OneShotResponder(Response response) {
            this.response = response;
        }

        @Override
        public boolean matches(Request request) {
            return used.compareAndSet(false, true);
        }

        @Override
        public Response respond(Request request, byte[] body) {
            return response;
        }
    }

    private static Request toRequest(HttpMessage request) {
        return Request.builder()
                .method(request.method)
                .path(request.path)
                .headers(toHeaderMap(request.headers))
                .build();
    }

    private static Map<String, List<String>> readonlyHeaders(HttpMessage msg) {
        CaseInsensitiveTreeMap<List<String>> map = new CaseInsensitiveTreeMap<>();
        for (String[] h : msg.headers) {
            map.computeIfAbsent(h[0], k -> new ArrayList<>()).add(h[1]);
        }
        return java.util.Collections.unmodifiableMap(map);
    }

    private static void logHead(int connId, String dir, HttpMessage msg) {
        LOG.debug("[{}] {} {}\n{}", connId, dir, msg.startLine, formatHeaders(msg.headers));
    }

    private static void logModifiedHead(int connId, String dir, HttpMessage original, HttpMessage manipulated) {
        boolean changed = !original.startLine.equals(manipulated.startLine)
                || !canonicalHeaders(original.headers).equals(canonicalHeaders(manipulated.headers));
        if (changed) {
            LOG.debug("[{}] {} (head, manipulated) {}\n{}", connId, dir,
                    manipulated.startLine, formatHeaders(manipulated.headers));
        }
    }

    private static List<String> canonicalHeaders(List<String[]> headers) {
        List<String> pairs = new ArrayList<>(headers.size());
        for (String[] h : headers) {
            pairs.add(h[0].toLowerCase(java.util.Locale.ROOT) + ": " + h[1]);
        }
        pairs.sort(null);
        return pairs;
    }

    private static void logBody(int connId, String dir, BodyCapture body) {
        boolean changed = body.manipulated() != null
                && !java.util.Arrays.equals(body.original(), body.manipulated());
        if (changed) {
            LOG.debug("[{}] {} (body, original)\n{}", connId, dir,
                    formatBody(body.original(), body.originalLength()));
            LOG.debug("[{}] {} (body, manipulated)\n{}", connId, dir,
                    formatBody(body.manipulated(), body.manipulatedLength()));
        } else if (body.original().length > 0 || body.originalLength() > 0) {
            LOG.debug("[{}] {} (body)\n{}", connId, dir,
                    formatBody(body.original(), body.originalLength()));
        }
    }

    private static HttpMessage readRequestHead(InputStream in) throws IOException {
        String requestLine = readLine(in);
        if (requestLine == null || requestLine.isEmpty()) {
            return null;
        }
        String[] parts = requestLine.split(" ", 3);
        HttpMessage msg = new HttpMessage();
        msg.method = parts[0];
        msg.path = parts.length > 1 ? parts[1] : "";
        msg.startLine = requestLine;
        readHeaders(in, msg);
        return msg;
    }

    private static HttpMessage readResponseHead(InputStream in) throws IOException {
        String statusLine = readLine(in);
        if (statusLine == null || statusLine.isEmpty()) {
            return null;
        }
        String[] parts = statusLine.split(" ", 3);
        HttpMessage msg = new HttpMessage();
        msg.statusCode = parts.length > 1 ? parts[1] : "";
        msg.startLine = statusLine;
        msg.isResponse = true;
        readHeaders(in, msg);
        return msg;
    }

    private static void readHeaders(InputStream in, HttpMessage msg) throws IOException {
        while (true) {
            String line = readLine(in);
            if (line == null || line.isEmpty()) {
                break;
            }
            int colon = line.indexOf(':');
            if (colon > 0) {
                String name = line.substring(0, colon).trim();
                String value = line.substring(colon + 1).trim();
                msg.headers.add(new String[]{name, value});
            }
        }
    }

    private static void captureBounded(ByteArrayOutputStream out, byte[] data, int len) {
        captureBounded(out, data, 0, len);
    }

    private static void captureBounded(ByteArrayOutputStream out, byte[] data, int off, int len) {
        int room = MAX_CAPTURE_BYTES - out.size();
        if (room <= 0) {
            return;
        }
        out.write(data, off, Math.min(len, room));
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static final class ChunkedCapturingInputStream extends InputStream {
        private enum State { SIZE, DATA, AFTER_DATA, TRAILER, DONE }

        private final InputStream in;
        private State state = State.SIZE;
        private final StringBuilder line = new StringBuilder();
        private long dataRemaining;
        private int crlfRemaining;
        private final ByteArrayOutputStream decoded = new ByteArrayOutputStream();
        private final MessageDigest fullBodyDigest = newSha256();
        private final AtomicLong fullBodyLength = new AtomicLong();
        private final Map<String, List<String>> trailers = new LinkedHashMap<>();

        ChunkedCapturingInputStream(InputStream in) {
            this.in = in;
        }

        byte[] decodedBody() {
            return decoded.toByteArray();
        }

        byte[] fullBodyDigest() {
            return fullBodyDigest.digest();
        }

        long fullBodyLength() {
            return fullBodyLength.get();
        }

        Map<String, List<String>> trailers() {
            return Immutable.copyOfMap(trailers);
        }

        @Override
        public int read() throws IOException {
            int c = in.read();
            if (c >= 0) {
                feed(c);
            }
            return c;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = in.read(b, off, len);
            int i = 0;
            while (i < n && state != State.DONE) {
                if (state == State.DATA) {
                    int run = (int) Math.min(dataRemaining, n - i);
                    data(b, off + i, run);
                    i += run;
                } else {
                    feed(b[off + i] & 0xFF);
                    i++;
                }
            }
            return n;
        }

        private void data(byte[] b, int off, int len) {
            captureBounded(decoded, b, off, len);
            fullBodyDigest.update(b, off, len);
            fullBodyLength.addAndGet(len);
            dataRemaining -= len;
            if (dataRemaining == 0) {
                state = State.AFTER_DATA;
                crlfRemaining = 2;
            }
        }

        private void feed(int c) {
            switch (state) {
                case SIZE:
                    if (c == '\n') {
                        long size = parseSize(line.toString());
                        line.setLength(0);
                        if (size == 0) {
                            state = State.TRAILER;
                        } else {
                            dataRemaining = size;
                            state = State.DATA;
                        }
                    } else if (c != '\r') {
                        line.append((char) c);
                    }
                    break;
                case DATA:
                    data(new byte[]{(byte) c}, 0, 1);
                    break;
                case AFTER_DATA:
                    if (--crlfRemaining == 0) {
                        state = State.SIZE;
                    }
                    break;
                case TRAILER:
                    if (c == '\n') {
                        String header = line.toString();
                        line.setLength(0);
                        if (header.isEmpty()) {
                            state = State.DONE;
                        } else {
                            addTrailer(header);
                        }
                    } else if (c != '\r') {
                        line.append((char) c);
                    }
                    break;
                default:
                    break;
            }
        }

        private static long parseSize(String sizeLine) {
            int semi = sizeLine.indexOf(';');
            String hex = (semi >= 0 ? sizeLine.substring(0, semi) : sizeLine).trim();
            return hex.isEmpty() ? 0 : Long.parseLong(hex, 16);
        }

        private void addTrailer(String header) {
            int colon = header.indexOf(':');
            if (colon <= 0) {
                return;
            }
            String name = header.substring(0, colon).trim().toLowerCase();
            String value = header.substring(colon + 1).trim();
            trailers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        }
    }

    @FunctionalInterface
    private interface BodyHook {
        InputStream apply(InputStream raw);
    }

    private static BodyCapture pumpBody(InputStream in, HttpMessage msg, OutputStream dst, BodyHook hook,
                                        java.util.function.Consumer<LiveBody> sink)
            throws IOException {
        return pumpBody(new ClientReadInputStream(in), msg, dst, hook, false, true, sink);
    }

    private static BodyCapture pumpBody(InputStream in, HttpMessage msg, OutputStream dst,
                                        BodyHook hook, boolean tolerateClientGoingAway,
                                        boolean sourceIsClient, java.util.function.Consumer<LiveBody> sink)
            throws IOException {
        long length = msg.contentLength();
        boolean chunked = msg.isChunked();
        boolean closeDelimited = length < 0 && !chunked && msg.isResponse && msg.connectionClose();
        boolean continueResponse = msg.isResponse && msg.statusCode.startsWith("1");
        if (continueResponse || (length < 0 && !chunked && !closeDelimited)) {
            return BodyCapture.passthrough(new byte[0], 0);
        }

        InputStream framed = chunked ? new ChunkedFramedInputStream(in, sourceIsClient) : in;
        ChunkedCapturingInputStream capturer =
                (chunked || msg.isAwsChunked()) ? new ChunkedCapturingInputStream(framed) : null;
        InputStream parsed = capturer != null ? capturer : framed;
        long limit = chunked ? -1 : length;
        ByteArrayOutputStream originalTap = new ByteArrayOutputStream();
        BoundedTapInputStream source =
                new BoundedTapInputStream(parsed, limit, originalTap, sourceIsClient);

        if (hook == null) {
            LiveBody live = new LiveBody(source, capturer, originalTap, null, null);
            publish(sink, live);
            boolean truncated = false;
            OutputStream out = clientFacing(dst, tolerateClientGoingAway);
            try {
                transfer(source, out);
                out.flush();
            } catch (ClientGoneException e) {
                truncated = true;
            }
            msg.bodyTotalLength = source.consumed();
            live.finish();
            recordChunkCapture(msg, live);
            BodyCapture capture =
                    BodyCapture.passthrough(originalTap.toByteArray(), source.consumed());
            return truncated ? capture.asTruncated() : capture;
        }

        ByteArrayOutputStream manipulatedTap = new ByteArrayOutputStream();
        BoundedTapInputStream manipulated = new BoundedTapInputStream(hook.apply(source), -1, manipulatedTap);
        LiveBody live = new LiveBody(source, capturer, originalTap, manipulated, manipulatedTap);
        publish(sink, live);
        boolean truncated = false;
        OutputStream out = tolerateClientGoingAway ? new ClientWriteOutputStream(dst) : dst;
        try {
            transfer(manipulated, out);
            out.flush();
            source.drain();
        } catch (ClientGoneException e) {
            truncated = true;
        }
        msg.bodyTotalLength = source.consumed();
        live.finish();
        recordChunkCapture(msg, live);
        return new BodyCapture(originalTap.toByteArray(), source.consumed(),
                manipulatedTap.toByteArray(), manipulated.consumed(), truncated);
    }

    private static void publish(java.util.function.Consumer<LiveBody> sink, LiveBody body) {
        if (sink != null) {
            sink.accept(body);
        }
    }

    private static void recordChunkCapture(HttpMessage msg, LiveBody live) {
        BodyFacts facts = live.facts(false);
        msg.decodedBody = facts.decodedPrefix();
        msg.trailingHeaders = facts.trailers();
        msg.fullBodyDigest = facts.digest();
        msg.fullBodyLength = facts.payloadLength();
    }

    private static final class BoundedTapInputStream extends InputStream {
        private final InputStream in;
        private final ByteArrayOutputStream tap;
        private final boolean bounded;
        private long remaining;
        private final AtomicLong consumed = new AtomicLong();
        private final MessageDigest fullBodyDigest = newSha256();
        private final boolean sourceIsClient;

        BoundedTapInputStream(InputStream in, long limit, ByteArrayOutputStream tap) {
            this(in, limit, tap, false);
        }

        BoundedTapInputStream(InputStream in, long limit, ByteArrayOutputStream tap,
                              boolean sourceIsClient) {
            this.in = in;
            this.tap = tap;
            this.bounded = limit >= 0;
            this.remaining = limit;
            this.sourceIsClient = sourceIsClient;
        }

        long consumed() {
            return consumed.get();
        }

        byte[] fullBodyDigest() {
            return fullBodyDigest.digest();
        }

        @Override
        public int read() throws IOException {
            if (bounded && remaining <= 0) {
                return -1;
            }
            int b = in.read();
            if (b < 0) {
                if (bounded) {
                    throw shortRead();
                }
                return -1;
            }
            remaining--;
            consumed.incrementAndGet();
            captureBounded(tap, new byte[]{(byte) b}, 1);
            fullBodyDigest.update((byte) b);
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (bounded && remaining <= 0) {
                return -1;
            }
            int want = bounded ? (int) Math.min(len, remaining) : len;
            int n = in.read(b, off, want);
            if (n < 0) {
                if (bounded) {
                    throw shortRead();
                }
                return -1;
            }
            fullBodyDigest.update(b, off, n);
            remaining -= n;
            consumed.addAndGet(n);
            captureBounded(tap, java.util.Arrays.copyOfRange(b, off, off + n), n);
            return n;
        }

        private IOException shortRead() {
            String message = "unexpected EOF, " + remaining + " bytes short";
            return sourceIsClient ? new ClientAbortedRequestException(message) : new IOException(message);
        }

        void drain() throws IOException {
            byte[] buf = new byte[8192];
            while (read(buf) >= 0) {
            }
        }
    }

    private static final class ChunkedFramedInputStream extends InputStream {
        private final InputStream in;
        private final boolean sourceIsClient;
        private byte[] buf = new byte[0];
        private int pos;
        private boolean done;

        ChunkedFramedInputStream(InputStream in, boolean sourceIsClient) {
            this.in = in;
            this.sourceIsClient = sourceIsClient;
        }

        private IOException framingEndedEarly(String message) {
            return sourceIsClient ? new ClientAbortedRequestException(message) : new IOException(message);
        }

        @Override
        public int read() throws IOException {
            if (pos >= buf.length && !fill()) {
                return -1;
            }
            return buf[pos++] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (pos >= buf.length && !fill()) {
                return -1;
            }
            int n = Math.min(len, buf.length - pos);
            System.arraycopy(buf, pos, b, off, n);
            pos += n;
            return n;
        }

        private boolean fill() throws IOException {
            if (done) {
                return false;
            }
            String sizeLine = readLine(in);
            if (sizeLine == null) {
                throw framingEndedEarly("unexpected EOF in chunked body");
            }
            String hex = sizeLine;
            int semi = hex.indexOf(';');
            if (semi >= 0) {
                hex = hex.substring(0, semi);
            }
            int chunkSize = Integer.parseInt(hex.trim(), 16);
            ByteArrayOutputStream chunk = new ByteArrayOutputStream();
            chunk.write((sizeLine + "\r\n").getBytes(StandardCharsets.US_ASCII));

            if (chunkSize == 0) {
                while (true) {
                    String trailer = readLine(in);
                    if (trailer == null) {
                        break;
                    }
                    chunk.write((trailer + "\r\n").getBytes(StandardCharsets.US_ASCII));
                    if (trailer.isEmpty()) {
                        break;
                    }
                }
                done = true;
            } else {
                byte[] data = readUpTo(in, chunkSize);
                if (data.length < chunkSize) {
                    throw framingEndedEarly("truncated chunk: " + data.length + "/" + chunkSize);
                }
                chunk.write(data);
                int cr = in.read();
                int lf = in.read();
                if (cr != '\r' || lf != '\n') {
                    throw framingEndedEarly("missing CRLF after chunk (cr=" + cr + ", lf=" + lf + ")");
                }
                chunk.write('\r');
                chunk.write('\n');
            }
            buf = chunk.toByteArray();
            pos = 0;
            return true;
        }
    }

    private static String formatHeaders(List<String[]> headers) {
        StringBuilder sb = new StringBuilder();
        for (String[] h : headers) {
            sb.append(h[0]).append(": ").append(h[1]).append('\n');
        }
        return sb.toString();
    }

    private static String formatBody(byte[] body, long totalLength) {
        StringBuilder sb = new StringBuilder();
        int end = Math.min(body.length, MAX_BODY_CHARS);
        for (int i = 0; i < end; i++) {
            byte b = body[i];
            if (b >= 0x20 && b <= 0x7E) {
                sb.append((char) b);
            } else if (b == '\r') {
                sb.append("\\r");
            } else if (b == '\n') {
                sb.append("\\n\n");
            } else if (b == '\t') {
                sb.append("\\t");
            } else {
                sb.append(String.format("[0x%02x]", b & 0xFF));
            }
        }
        long total = Math.max(totalLength, body.length);
        if (total > end) {
            sb.append("\n... [truncated ").append(total - end).append(" bytes]");
        }
        return sb.toString();
    }

    private static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\r') {
                int next = in.read();
                if (next == '\n') {
                    return sb.toString();
                }
                sb.append((char) c);
                if (next != -1) {
                    sb.append((char) next);
                }
            } else if (c == '\n') {
                return sb.toString();
            } else {
                sb.append((char) c);
            }
        }
        return c == -1 && sb.length() == 0 ? null : sb.toString();
    }

    private static void quietClose(Socket s) {
        try {
            s.close();
        } catch (IOException e) {
            LOG.debug("best-effort close of {} failed", s, e);
        }
    }

    @Override
    public void close() {
        BY_PORT.remove(port(), this);
        try {
            serverSocket.close();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not close the wiretap's listening socket", e);
        } finally {
            executor.shutdownNow();
        }
    }

    private static final class BodyFacts {
        static final BodyFacts NONE = new BodyFacts(new byte[0], 0, null, 0, null, Immutable.mapOf());

        private final byte[] prefix;
        private final long wireLength;
        private final byte[] decodedPrefix;
        private final long payloadLength;
        private final byte[] digest;
        private final Map<String, List<String>> trailers;

        private BodyFacts(byte[] prefix,
                long wireLength,
                byte[] decodedPrefix,
                long payloadLength,
                byte[] digest,
                Map<String, List<String>> trailers) {
            this.prefix = prefix;
            this.wireLength = wireLength;
            this.decodedPrefix = decodedPrefix;
            this.payloadLength = payloadLength;
            this.digest = digest;
            this.trailers = trailers;
        }

        public byte[] prefix() {
            return prefix;
        }

        public long wireLength() {
            return wireLength;
        }

        public byte[] decodedPrefix() {
            return decodedPrefix;
        }

        public long payloadLength() {
            return payloadLength;
        }

        public byte[] digest() {
            return digest;
        }

        public Map<String, List<String>> trailers() {
            return trailers;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BodyFacts that = (BodyFacts) o;
            return prefix == that.prefix
                && wireLength == that.wireLength
                && decodedPrefix == that.decodedPrefix
                && payloadLength == that.payloadLength
                && digest == that.digest
                && Objects.equals(trailers, that.trailers);
        }

        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + Objects.hashCode(prefix);
            result = 31 * result + Long.hashCode(wireLength);
            result = 31 * result + Objects.hashCode(decodedPrefix);
            result = 31 * result + Long.hashCode(payloadLength);
            result = 31 * result + Objects.hashCode(digest);
            result = 31 * result + Objects.hashCode(trailers);
            return result;
        }

        @Override
        public String toString() {
            return "BodyFacts[prefix=" + ByteArrays.describe(prefix)
                + ", wireLength=" + wireLength
                + ", decodedPrefix=" + ByteArrays.describe(decodedPrefix)
                + ", payloadLength=" + payloadLength
                + ", digest=" + ByteArrays.describe(digest)
                + ", trailers=" + trailers
                + "]";
        }
    }

    private static final class LiveBody {

        private final BoundedTapInputStream source;
        private final ChunkedCapturingInputStream capturer;
        private final ByteArrayOutputStream tap;
        private final BoundedTapInputStream manipulated;
        private final ByteArrayOutputStream manipulatedTap;

        private volatile BodyFacts finished;

        LiveBody(BoundedTapInputStream source, ChunkedCapturingInputStream capturer,
                 ByteArrayOutputStream tap, BoundedTapInputStream manipulated,
                 ByteArrayOutputStream manipulatedTap) {
            this.source = source;
            this.capturer = capturer;
            this.tap = tap;
            this.manipulated = manipulated;
            this.manipulatedTap = manipulatedTap;
        }

        void finish() {
            finished = new BodyFacts(tap.toByteArray(), source.consumed(),
                    capturer == null ? null : capturer.decodedBody(),
                    capturer != null ? capturer.fullBodyLength() : source.consumed(),
                    capturer != null ? capturer.fullBodyDigest() : source.fullBodyDigest(),
                    capturer == null ? Immutable.mapOf() : capturer.trailers());
        }

        BodyFacts facts(boolean wantManipulated) {
            BodyFacts done = finished;
            BodyFacts original = done != null
                    ? done
                    : new BodyFacts(tap.toByteArray(), source.consumed(),
                            capturer == null ? null : capturer.decodedBody(),
                            capturer != null ? capturer.fullBodyLength() : source.consumed(),
                            null, Immutable.mapOf());
            if (!wantManipulated || manipulatedTap == null) {
                return original;
            }
            return new BodyFacts(manipulatedTap.toByteArray(), manipulated.consumed(),
                    original.decodedPrefix(), original.payloadLength(), original.digest(),
                    original.trailers());
        }
    }

    private static final class LiveExchange {

        private final HttpMessage requestHead;
        private final Runnable onChange;

        private final long startNanos;
        private volatile long endNanos;

        private volatile ExchangeState state = ExchangeState.REQUEST_STREAMING;
        private volatile boolean responseHeadArrived;

        private volatile HttpMessage manipulatedRequestHead;
        private volatile HttpMessage responseHead;
        private volatile HttpMessage manipulatedResponseHead;
        private volatile LiveBody requestBody;
        private volatile LiveBody responseBody;

        private final long epoch;

        private final java.util.function.Consumer<LiveExchange> onSettled;

        LiveExchange(HttpMessage requestHead, long startNanos, Runnable onChange, long epoch,
                     java.util.function.Consumer<LiveExchange> onSettled) {
            this.requestHead = requestHead;
            this.startNanos = startNanos;
            this.onChange = onChange;
            this.epoch = epoch;
            this.onSettled = onSettled;
        }

        long epoch() {
            return epoch;
        }

        ExchangeState state() {
            return state;
        }

        boolean settled() {
            return state.terminal();
        }

        boolean hasResponseHead() {
            return responseHeadArrived;
        }

        void manipulatedRequestHead(HttpMessage head) {
            this.manipulatedRequestHead = head;
            onChange.run();
        }

        void requestBody(LiveBody body) {
            this.requestBody = body;
            onChange.run();
        }

        void responseBody(LiveBody body) {
            this.responseBody = body;
            onChange.run();
        }

        java.util.function.Consumer<LiveBody> requestSink() {
            return this::requestBody;
        }

        java.util.function.Consumer<LiveBody> responseSink() {
            return this::responseBody;
        }

        void requestRead() {
            advanceTo(ExchangeState.AWAITING_RESPONSE);
        }

        void requestAborted() {
            advanceTo(ExchangeState.REQUEST_ABORTED);
        }

        void noResponse() {
            advanceTo(ExchangeState.NO_RESPONSE);
        }

        void responseHead(HttpMessage original, HttpMessage manipulated) {
            this.responseHead = original;
            this.manipulatedResponseHead = manipulated;
            responseHeadArrived = true;
            advanceTo(ExchangeState.RESPONSE_STREAMING);
        }

        void responseStreamed(boolean truncated) {
            advanceTo(truncated ? ExchangeState.RESPONSE_TRUNCATED : ExchangeState.COMPLETE);
        }

        void failIfUnsettled(Throwable cause) {
            if (state.terminal()) {
                return;
            }
            if (cause == null) {
                LOG.error("exchange left in {} with no failure to explain it — a handleExchange exit"
                        + " path is not settling its exchange; later quiescence waits will pay their"
                        + " full timeout", state);
            } else {
                LOG.debug("exchange failed in {}: {}", state, cause.toString());
            }
            advanceTo(ExchangeState.FAILED);
        }

        private void advanceTo(ExchangeState next) {
            ExchangeState current = state;
            if (current.terminal()) {
                if (next != ExchangeState.FAILED) {
                    throw new IllegalStateException(
                            "cannot move a settled exchange from " + current + " to " + next);
                }
                return;
            }
            if (next.rank() < current.rank()) {
                throw new IllegalStateException("cannot move an exchange backwards from "
                        + current + " to " + next);
            }
            if (next.terminal()) {
                endNanos = System.nanoTime();
            }
            state = next;
            onChange.run();
            if (next.terminal()) {
                onSettled.accept(this);
            }
        }

        CapturedExchange snapshot() {
            ExchangeState observed = state;
            long end = observed.terminal() ? endNanos : System.nanoTime();
            LiveBody request = requestBody;
            LiveBody response = responseBody;
            HttpMessage responseHeadNow = responseHead;
            HttpMessage manipulatedRequestHeadNow = manipulatedRequestHead;
            HttpMessage manipulatedResponseHeadNow = manipulatedResponseHead;
            return new CapturedExchange(
                    toCapturedRequest(requestHead, facts(request, false)),
                    responseHeadNow == null ? null
                            : toCapturedResponse(responseHeadNow, facts(response, false)),
                    manipulatedRequestHeadNow == null ? null
                            : toCapturedRequest(manipulatedRequestHeadNow, facts(request, true)),
                    manipulatedResponseHeadNow == null ? null
                            : toCapturedResponse(manipulatedResponseHeadNow, facts(response, true)),
                    startNanos, end, observed);
        }

        private static BodyFacts facts(LiveBody body, boolean manipulated) {
            return body == null ? BodyFacts.NONE : body.facts(manipulated);
        }
    }

    private static final class HttpMessage {
        private boolean isResponse;
        private String startLine;
        private String method;
        private String path;
        private String statusCode = "";
        private final List<String[]> headers = new ArrayList<>();
        private byte[] body = new byte[0];
        private long bodyTotalLength;
        private byte[] decodedBody;
        private byte[] fullBodyDigest;
        private long fullBodyLength;
        private Map<String, List<String>> trailingHeaders = Immutable.mapOf();
        private String connectHost;

        String header(String name) {
            for (String[] h : headers) {
                if (h[0].equalsIgnoreCase(name)) {
                    return h[1];
                }
            }
            return null;
        }

        boolean connectionClose() {
            String conn = header("Connection");
            return conn != null && conn.equalsIgnoreCase("close");
        }

        boolean expectsContinue() {
            String expect = header("Expect");
            return expect != null && expect.toLowerCase().contains("100-continue");
        }

        void writeStartLineAndHeaders(OutputStream out) throws IOException {
            out.write(startLine.getBytes(StandardCharsets.US_ASCII));
            out.write(CRLF);
            for (String[] h : headers) {
                out.write((h[0] + ": " + h[1]).getBytes(StandardCharsets.US_ASCII));
                out.write(CRLF);
            }
            out.write(CRLF);
            out.flush();
        }

        HttpMessage copy() {
            HttpMessage m = new HttpMessage();
            m.isResponse = isResponse;
            m.startLine = startLine;
            m.method = method;
            m.path = path;
            m.statusCode = statusCode;
            for (String[] h : headers) {
                m.headers.add(new String[]{h[0], h[1]});
            }
            m.body = body;
            m.bodyTotalLength = bodyTotalLength;
            m.decodedBody = decodedBody;
            m.fullBodyDigest = fullBodyDigest;
            m.fullBodyLength = fullBodyLength;
            m.trailingHeaders = trailingHeaders;
            m.connectHost = connectHost;
            return m;
        }

        long contentLength() {
            String cl = header("Content-Length");
            return cl == null ? -1 : Long.parseLong(cl.trim());
        }

        boolean isChunked() {
            String te = header("Transfer-Encoding");
            return te != null && te.toLowerCase().contains("chunked");
        }

        boolean isAwsChunked() {
            String ce = header("Content-Encoding");
            return ce != null && ce.toLowerCase().contains("aws-chunked");
        }
    }

    private static final class BodyCapture {
        private final byte[] original;
        private final long originalLength;
        private final byte[] manipulated;
        private final long manipulatedLength;
        private final boolean truncated;

        private BodyCapture(byte[] original, long originalLength, byte[] manipulated, long manipulatedLength, boolean truncated) {
            this.original = original;
            this.originalLength = originalLength;
            this.manipulated = manipulated;
            this.manipulatedLength = manipulatedLength;
            this.truncated = truncated;
        }

        public byte[] original() {
            return original;
        }

        public long originalLength() {
            return originalLength;
        }

        public byte[] manipulated() {
            return manipulated;
        }

        public long manipulatedLength() {
            return manipulatedLength;
        }

        public boolean truncated() {
            return truncated;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BodyCapture that = (BodyCapture) o;
            return original == that.original
                && originalLength == that.originalLength
                && manipulated == that.manipulated
                && manipulatedLength == that.manipulatedLength
                && truncated == that.truncated;
        }

        @Override
        public int hashCode() {
            int result = 0;
            result = 31 * result + Objects.hashCode(original);
            result = 31 * result + Long.hashCode(originalLength);
            result = 31 * result + Objects.hashCode(manipulated);
            result = 31 * result + Long.hashCode(manipulatedLength);
            result = 31 * result + Boolean.hashCode(truncated);
            return result;
        }

        @Override
        public String toString() {
            return "BodyCapture[original=" + ByteArrays.describe(original)
                + ", originalLength=" + originalLength
                + ", manipulated=" + ByteArrays.describe(manipulated)
                + ", manipulatedLength=" + manipulatedLength
                + ", truncated=" + truncated
                + "]";
        }

        static BodyCapture passthrough(byte[] original, long length) {
            return new BodyCapture(original, length, null, 0, false);
        }

        static BodyCapture empty() {
            return passthrough(new byte[0], 0);
        }

        BodyCapture asTruncated() {
            return new BodyCapture(original, originalLength, manipulated, manipulatedLength, true);
        }
    }

    private static final class ClientGoneException extends IOException {
        private static final long serialVersionUID = 1L;

        ClientGoneException(IOException cause) {
            super("client went away mid-response", cause);
        }
    }

    private static final class ClientAbortedRequestException extends IOException {
        private static final long serialVersionUID = 1L;

        ClientAbortedRequestException(String message) {
            super("client abandoned the request body: " + message);
        }
    }

    private static final class ClientReadInputStream extends InputStream {
        private final InputStream delegate;

        ClientReadInputStream(InputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read() throws IOException {
            try {
                return delegate.read();
            } catch (ClientAbortedRequestException e) {
                throw e;
            } catch (IOException e) {
                throw new ClientAbortedRequestException(e.toString());
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            try {
                return delegate.read(b, off, len);
            } catch (ClientAbortedRequestException e) {
                throw e;
            } catch (IOException e) {
                throw new ClientAbortedRequestException(e.toString());
            }
        }
    }

    private static OutputStream clientFacing(OutputStream dst, boolean tolerateClientGoingAway) {
        if (!tolerateClientGoingAway || dst instanceof ClientWriteOutputStream) {
            return dst;
        }
        return new ClientWriteOutputStream(dst);
    }

    private static final class StallGuard implements SdkAutoCloseable {

        private final Thread watchdog;
        private final AtomicBoolean finished = new AtomicBoolean();

        StallGuard(Socket socket, ClientWriteOutputStream progress, long stallMillis, int connId) {
            this.watchdog = new Thread(() -> {
                long lastSeen = -1;
                long lastChangeNanos = System.nanoTime();
                while (!finished.get()) {
                    long written = progress.written();
                    if (written != lastSeen) {
                        lastSeen = written;
                        lastChangeNanos = System.nanoTime();
                    } else if ((System.nanoTime() - lastChangeNanos) / 1_000_000L >= stallMillis) {
                        LOG.debug("[{}] client stopped reading the response after {} byte(s) without"
                                + " closing; closing the socket so the exchange can be recorded",
                                connId, written);
                        quietClose(socket);
                        return;
                    }
                    try {
                        Thread.sleep(STALL_POLL_MILLIS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }, "wiretap-stall-guard-" + connId);
            this.watchdog.setDaemon(true);
            this.watchdog.start();
        }

        @Override
        public void close() {
            finished.set(true);
            watchdog.interrupt();
        }
    }

    private static final class ClientWriteOutputStream extends OutputStream {
        private final OutputStream delegate;
        private final AtomicLong written = new AtomicLong();

        ClientWriteOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }

        long written() {
            return written.get();
        }

        @Override
        public void write(int b) throws IOException {
            try {
                delegate.write(b);
            } catch (IOException e) {
                throw new ClientGoneException(e);
            }
            written.incrementAndGet();
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            try {
                delegate.write(b, off, len);
            } catch (IOException e) {
                throw new ClientGoneException(e);
            }
            written.addAndGet(len);
        }

        @Override
        public void flush() throws IOException {
            try {
                delegate.flush();
            } catch (IOException e) {
                throw new ClientGoneException(e);
            }
        }
    }

    private static final class LastByteHoldbackOutputStream extends OutputStream {
        private final OutputStream delegate;
        private int held = -1;

        LastByteHoldbackOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int b) throws IOException {
            if (held != -1) {
                delegate.write(held);
            }
            held = b & 0xFF;
        }

        @Override
        public void write(byte[] buf, int off, int len) throws IOException {
            if (len <= 0) {
                return;
            }
            if (held != -1) {
                delegate.write(held);
                held = -1;
            }
            delegate.write(buf, off, len - 1);
            held = buf[off + len - 1] & 0xFF;
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }

        void releaseLast() throws IOException {
            if (held != -1) {
                delegate.write(held);
                held = -1;
            }
            delegate.flush();
        }
    }

    private interface ResponseProvider {
        void sendRequestHead(HttpMessage request) throws IOException;

        BodyCapture forwardRequestBody(InputStream in, HttpMessage request, BodyHook hook,
                java.util.function.Consumer<LiveBody> sink) throws IOException;

        HttpMessage readResponseHead() throws IOException;

        InputStream responseBodySource(HttpMessage response) throws IOException;
    }

    private static final class SocketResponder implements ResponseProvider {
        private final OutputStream upstreamOut;
        private final PushbackInputStream upstreamIn;

        SocketResponder(Socket upstream) throws IOException {
            this.upstreamOut = upstream.getOutputStream();
            this.upstreamIn = new PushbackInputStream(upstream.getInputStream(), 1);
        }

        @Override
        public void sendRequestHead(HttpMessage request) throws IOException {
            request.writeStartLineAndHeaders(upstreamOut);
        }

        @Override
        public BodyCapture forwardRequestBody(InputStream in, HttpMessage request, BodyHook hook,
                                              java.util.function.Consumer<LiveBody> sink) throws IOException {
            return pumpBody(in, request, upstreamOut, hook, sink);
        }

        @Override
        public HttpMessage readResponseHead() throws IOException {
            return S3Wiretap.readResponseHead(upstreamIn);
        }

        @Override
        public InputStream responseBodySource(HttpMessage response) {
            return upstreamIn;
        }
    }

    private static final class MockQueueResponder implements ResponseProvider {
        private final BiFunction<HttpMessage, byte[], Response> selector;
        private boolean shouldEmitContinueNext = false;
        private HttpMessage currentRequest;
        private byte[] currentBody = new byte[0];

        MockQueueResponder(BiFunction<HttpMessage, byte[], Response> selector) {
            this.selector = selector;
        }

        @Override
        public void sendRequestHead(HttpMessage request) {
            this.currentRequest = request;
            this.currentBody = new byte[0];
            shouldEmitContinueNext = request.expectsContinue();
        }

        @Override
        public BodyCapture forwardRequestBody(InputStream in, HttpMessage request, BodyHook hook,
                                              java.util.function.Consumer<LiveBody> sink) throws IOException {
            BodyCapture capture = pumpBody(in, request, DISCARD, hook, sink);
            this.currentBody = capture.original();
            return capture;
        }

        @Override
        public HttpMessage readResponseHead() {
            if (shouldEmitContinueNext) {
                shouldEmitContinueNext = false;
                return synthHead("HTTP/1.1 100 Continue", "100", Immutable.listOf());
            }
            Response next = selector.apply(currentRequest, currentBody);
            List<String[]> headers = new ArrayList<>();
            for (Map.Entry<String, List<String>> e : next.headers().entrySet()) {
                for (String v : e.getValue()) {
                    headers.add(new String[]{e.getKey(), v});
                }
            }
            int len = next.body() == null ? 0 : next.body().length;
            if (!hasHeader(headers, "Content-Length") && !hasHeader(headers, "Transfer-Encoding")) {
                headers.add(new String[]{"Content-Length", String.valueOf(len)});
            }
            HttpMessage msg = synthHead(
                    "HTTP/1.1 " + next.statusCode() + " " + Response.reasonPhrase(next.statusCode()),
                    String.valueOf(next.statusCode()), headers);
            msg.body = next.body() == null ? new byte[0] : next.body();
            return msg;
        }

        @Override
        public InputStream responseBodySource(HttpMessage response) {
            return new ByteArrayInputStream(response.body == null ? new byte[0] : response.body);
        }

        private static HttpMessage synthHead(String startLine, String code,
                                             List<String[]> headers) {
            HttpMessage m = new HttpMessage();
            m.isResponse = true;
            m.startLine = startLine;
            m.statusCode = code;
            m.headers.addAll(headers);
            m.body = new byte[0];
            return m;
        }

        private static boolean hasHeader(List<String[]> headers, String name) {
            for (String[] h : headers) {
                if (h[0].equalsIgnoreCase(name)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static long transfer(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[16384];
        long total = 0;
        int read;
        while ((read = in.read(buffer, 0, buffer.length)) >= 0) {
            out.write(buffer, 0, read);
            total += read;
        }
        return total;
    }

    private static byte[] readUpTo(InputStream in, int n) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(Math.min(n, 16384));
        byte[] buffer = new byte[Math.min(n, 16384)];
        int remaining = n;
        while (remaining > 0) {
            int read = in.read(buffer, 0, Math.min(remaining, buffer.length));
            if (read < 0) {
                break;
            }
            bytes.write(buffer, 0, read);
            remaining -= read;
        }
        return bytes.toByteArray();
    }
}
