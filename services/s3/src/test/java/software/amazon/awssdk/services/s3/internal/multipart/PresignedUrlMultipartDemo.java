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

package software.amazon.awssdk.services.s3.internal.multipart;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.multipart.MultipartConfiguration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presignedurl.model.PresignedUrlDownloadRequest;
import software.amazon.awssdk.regions.Region;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * 🚀 S3 Presigned URL Download Demo
 * 
 * Demonstrating AWS SDK Java v2's new presigned URL download capability with multipart support
 * Built with Amazon's Leadership Principles in mind:
 * 
 * 📋 CUSTOMER OBSESSION: Faster downloads for large files improve customer experience
 * 🔧 INVENT AND SIMPLIFY: Same simple API, automatic multipart optimization under the hood  
 * 🎯 THINK BIG: Scales from small files to massive datasets seamlessly
 */
public class PresignedUrlMultipartDemo {
    
    private static final String BUCKET_NAME = "jency-test-bucket";
    private static final String OBJECT_KEY = "Amazon Q.dmg";
    
    public static void main(String[] args) throws Exception {
        System.out.println("🎬 S3 Presigned URL Download Demo");
        System.out.println("============================================================");
        System.out.println("📦 Bucket: " + BUCKET_NAME);
        System.out.println("🔑 Object: " + OBJECT_KEY);
        System.out.println();
        
        // Step 1: Generate presigned URL for browser demonstration
        URL presignedUrl = generateAndExplainPresignedUrl();
        
        // Step 2: Pause for browser demonstration
        waitForBrowserDemo();
        
        // Step 3: Explain limitations of browser approach
        explainBrowserLimitations();
        
        // Step 4: Show SDK solution
        demonstrateSDKSolution(presignedUrl);
        
        System.out.println("\n🎉 Demo Complete! Questions?");
    }
    
    /**
     * Step 1: Generate presigned URL and prepare for browser demo
     */
    private static URL generateAndExplainPresignedUrl() {
        System.out.println("🔐 STEP 1: GENERATING PRESIGNED URL FOR BROWSER DEMO");
        System.out.println("============================================================");
        
        try (S3Presigner presigner = S3Presigner.create()) {
            GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(OBJECT_KEY)
                .build();
            
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(objectRequest)
                .build();
            
            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
            URL presignedUrl = presignedRequest.url();
            
            // Display URL for browser copy-paste
            System.out.println("🔗 PRESIGNED URL:");
            System.out.println("┌" + repeatString("─", 100) + "┐");
            System.out.println("│ " + presignedUrl.toExternalForm());
            System.out.println("└" + repeatString("─", 100) + "┘");
            System.out.println();
            
            // Explain what makes this URL special
            System.out.println("💡 What makes this URL special:");
            System.out.println("   ✅ No AWS credentials needed - authentication is embedded");
            System.out.println("   ✅ Time-limited access - expires in 10 minutes");
            System.out.println("   ✅ Direct S3 access - bypasses your application");
            System.out.println("   ✅ Works in any HTTP client - browser, curl, wget, etc.");
            System.out.println();
            
            return presignedUrl;
        }
    }
    
    /**
     * Step 2: Pause for browser demonstration
     */
    private static void waitForBrowserDemo() {
        System.out.println("🌐 BROWSER DEMONSTRATION");
        System.out.println("============================================================");
        System.out.println("👆 Now let's paste this URL in a browser and see what happens...");
        System.out.println();
        System.out.println("   • Notice how the download starts immediately");
        System.out.println("   • No login required - the URL contains authentication");
        System.out.println("   • Browser shows basic download progress");
        System.out.println();
        
        waitForKeyPress("Press Enter after browser demo to continue...");
    }
    
    /**
     * Step 3: Explain limitations of browser/raw HTTP approach
     */
    private static void explainBrowserLimitations() {
        System.out.println("⚠️  LIMITATIONS OF BROWSER/RAW HTTP APPROACH");
        System.out.println("============================================================");
        System.out.println("❌ No automatic retry logic if network fails");
        System.out.println("❌ No metrics collection for monitoring");
        System.out.println("❌ No structured error handling");
        System.out.println("❌ No streaming with backpressure control");
        System.out.println("❌ No integration with application logging");
        System.out.println("❌ Single-stream download only (no multipart optimization)");
        System.out.println("❌ No progress callbacks for application integration");
        System.out.println();
        System.out.println("🤔 Question: How do we get SDK benefits WITH presigned URLs?");
        System.out.println();
        
        waitForKeyPress("Press Enter to see the SDK solution...");
    }
    
    /**
     * Step 4: Demonstrate SDK solution with benefits
     */
    private static void demonstrateSDKSolution(URL presignedUrl) {
        System.out.println("🚀 SDK SOLUTION: PRESIGNED URL DOWNLOADS WITH FULL SDK BENEFITS");
        System.out.println("============================================================");
        System.out.println("💡 Our innovation: AsyncPresignedUrlExtension");
        System.out.println("   • Same presigned URL from browser demo");
        System.out.println("   • But now with full SDK capabilities");
        System.out.println("   • PLUS multipart download support");
        System.out.println();
        
        try {
            // Demo 1: Single-stream with SDK benefits
            demonstrateSDKSingleStream(presignedUrl);
            
            System.out.println("\n" + repeatString("=", 60));
            
            // Demo 2: Multipart innovation
            demonstrateSDKMultipart(presignedUrl);
            
            // Show final comparison
            showSDKBenefitsSummary();
            
        } catch (Exception e) {
            System.out.println("❌ Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Demo SDK single-stream with benefits
     */
    private static void demonstrateSDKSingleStream(URL presignedUrl) throws Exception {
        System.out.println("📥 SDK APPROACH 1: Single-Stream with SDK Benefits");
        System.out.println("   → Same URL, but now with retry logic, metrics, error handling");
        
        S3AsyncClient client = S3AsyncClient.builder()
            .build();
        
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
            .presignedUrl(presignedUrl)
            .build();
        
        Instant start = Instant.now();
        System.out.println("   ⏱️  Starting single-stream SDK download...");
        
        try {
            CompletableFuture<ResponseBytes<GetObjectResponse>> future = 
                client.presignedUrlExtension()
                      .getObject(request, AsyncResponseTransformer.toBytes());
            
            ResponseBytes<GetObjectResponse> response = future.get();
            
            Duration elapsed = Duration.between(start, Instant.now());
            long seconds = elapsed.getSeconds();
            long bytes = response.asByteArray().length;
            
            System.out.println("   ✅ Single-stream download completed!");
            System.out.println("   ⏱️  Time taken: " + seconds + " seconds");
            System.out.println("   📊 Downloaded: " + formatBytes(bytes));
            System.out.println("   📊 Throughput: " + formatThroughput(bytes, seconds));
            System.out.println("   🛡️  With automatic retries, metrics, and error handling!");
            
        } finally {
            client.close();
        }
    }
    
    /**
     * Demo SDK multipart innovation
     */
    private static void demonstrateSDKMultipart(URL presignedUrl) throws Exception {
        System.out.println("🔥 SDK APPROACH 2: Multipart Download");
        System.out.println("   → Same URL + Same API + Multipart Performance");
        
        MultipartConfiguration config = MultipartConfiguration.builder()
            .minimumPartSizeInBytes(8L * 1024 * 1024)    // 8MB parts
            .thresholdInBytes(16L * 1024 * 1024)         // Use multipart for files > 16MB
            .build();

        S3AsyncClient multipartClient = S3AsyncClient.builder()
            .multipartConfiguration(config)
            .build();
        
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
            .presignedUrl(presignedUrl)
            .build();
        
        Instant start = Instant.now();
        System.out.println("   ⏱️  Starting multipart download...");
        System.out.println("   🔍 Innovation: Using Range headers instead of partNumber");
        System.out.println("      • Range: bytes=0-8388607 (first 8MB + size discovery)");
        System.out.println("      • Range: bytes=8388608-16777215 (second 8MB)");
        System.out.println("      • Multiple parallel connections automatically!");
        
        try {
            CompletableFuture<ResponseBytes<GetObjectResponse>> future = 
                multipartClient.presignedUrlExtension()
                              .getObject(request, AsyncResponseTransformer.toBytes());
            
            ResponseBytes<GetObjectResponse> response = future.get();
            
            Duration elapsed = Duration.between(start, Instant.now());
            long seconds = elapsed.getSeconds();
            long bytes = response.asByteArray().length;
            
            System.out.println("   ✅ Multipart download completed!");
            System.out.println("   ⏱️  Time taken: " + seconds + " seconds");
            System.out.println("   📊 Downloaded: " + formatBytes(bytes));
            System.out.println("   📊 Throughput: " + formatThroughput(bytes, seconds));
            System.out.println("   🚀 Multiple parallel connections used!");
            System.out.println("   🌟 FIRST-EVER multipart presigned URL downloads!");
            
        } finally {
            multipartClient.close();
        }
    }
    
    /**
     * Show SDK benefits summary
     */
    private static void showSDKBenefitsSummary() {
        System.out.println("\n🏆 SDK BENEFITS SUMMARY");
        System.out.println("============================================================");
        System.out.println("Browser/Raw HTTP          →    AWS SDK Solution");
        System.out.println("─────────────────────────────────────────────────────────");
        System.out.println("❌ No retry logic         →    ✅ Automatic retries");
        System.out.println("❌ No metrics            →    ✅ Built-in metrics");
        System.out.println("❌ Basic error handling  →    ✅ Structured exceptions");
        System.out.println("❌ No streaming control  →    ✅ Backpressure support");
        System.out.println("❌ Single-stream only    →    ✅ Multipart optimization");
        System.out.println("❌ No progress callbacks →    ✅ Progress tracking");
        System.out.println("❌ Manual integration    →    ✅ Familiar S3AsyncClient API");
        System.out.println();
        System.out.println("🎯 Result: Same presigned URL, dramatically better experience!");
    }
    
    // Utility methods
    private static void waitForKeyPress(String message) {
        System.out.println(message);
        try {
            System.in.read();
        } catch (Exception e) {
            // Ignore
        }
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    private static String formatThroughput(long bytes, long seconds) {
        if (seconds == 0) return "N/A";
        double mbps = (bytes / (1024.0 * 1024)) / seconds;
        return String.format("%.2f MB/s", mbps);
    }
    
    /**
     * Utility method to repeat a string
     */
    private static String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}
