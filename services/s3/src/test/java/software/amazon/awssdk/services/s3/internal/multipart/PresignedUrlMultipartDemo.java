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
 * 🚀 SPARK Intern Fair Demo: S3 Presigned URL Multipart Downloads
 * 
 * Demonstrating AWS SDK Java v2's new multipart download capability for presigned URLs
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
        System.out.println("🎬 Starting S3 Presigned URL Multipart Download Demo");
        System.out.println("============================================================");
        System.out.println("📦 Bucket: " + BUCKET_NAME);
        System.out.println("🔑 Object: " + OBJECT_KEY);
        System.out.println();
        
        // Generate presigned URL once for both demos
        URL presignedUrl = generatePresignedUrl();
        
        // Demo 1: Traditional Single-Stream Download
        demonstrateTraditionalDownload(presignedUrl);
        
        System.out.println("\n============================================================");
        
        // Demo 2: New Multipart Download
        demonstrateMultipartDownload(presignedUrl);
        
        System.out.println("\n🎉 Demo Complete! Questions?");
    }
    
    /**
     * Generate a presigned URL for our demo object
     */
    private static URL generatePresignedUrl() {
        try (S3Presigner presigner = S3Presigner.create()) {
            // Define the S3 object request
            GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(OBJECT_KEY)
                .build();
            
            // Create the presign request
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10)) // URL expires in 10 minutes
                .getObjectRequest(objectRequest)
                .build();
            
            // Generate the presigned URL
            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
            
            // Retrieve the URL
            URL presignedUrl = presignedRequest.url();
            System.out.println("🔗 Generated presigned URL: " + presignedUrl.toExternalForm().substring(0, 100) + "...");
            System.out.println("⏰ URL expires in 10 minutes");
            System.out.println();
            
            return presignedUrl;
        }
    }
    
    /**
     * 📊 Presigned URL Single-Stream Download
     * Downloads entire file in one stream using presigned URLs
     */
    private static void demonstrateTraditionalDownload(URL presignedUrl) throws Exception {
        System.out.println("📥 DEMO 1: Presigned URL Single-Stream Download");
        System.out.println("   → Downloads entire file in one request using presigned URL");
        System.out.println("   → Using same presigned URL for fair comparison");
        
        // Standard S3 client - no multipart configuration
        S3AsyncClient standardClient = S3AsyncClient.builder()
            .region(Region.US_EAST_1)
            .build();
        
        Instant start = Instant.now();
        System.out.println("   ⏱️  Starting download at: " + start);
        
        // Single stream presigned URL download
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
            .presignedUrl(presignedUrl)
            .build();
        
        System.out.println("   🔍 Single-stream approach:");
        System.out.println("      • Single HTTP connection to presigned URL");
        System.out.println("      • Downloads entire file sequentially");
        System.out.println("      • Limited by single connection bandwidth");
        
        try {
            // Execute download
            CompletableFuture<ResponseBytes<GetObjectResponse>> future = 
                standardClient.presignedUrlExtension()
                             .getObject(request, AsyncResponseTransformer.toBytes());
            
            ResponseBytes<GetObjectResponse> response = future.get();
            
            Duration elapsed = Duration.between(start, Instant.now());
            long seconds = elapsed.getSeconds();
            long bytes = response.asByteArray().length;
            
            System.out.println("   ✅ Download completed in: " + seconds + " seconds");
            System.out.println("   📊 Downloaded: " + formatBytes(bytes));
            System.out.println("   📊 Throughput: " + formatThroughput(bytes, seconds));
            
        } catch (Exception e) {
            System.out.println("   ❌ Download failed: " + e.getMessage());
            throw e;
        } finally {
            standardClient.close();
        }
    }
    
    /**
     * 🚀 KEY INNOVATION: Multipart download capability for presigned URLs
     * CUSTOMER OBSESSION: Faster downloads through parallel connections
     * INVENT AND SIMPLIFY: Same API, smarter implementation
     * THINK BIG: Scales to handle massive files efficiently
     */
    private static void demonstrateMultipartDownload(URL presignedUrl) throws Exception {
        System.out.println("🔥 DEMO 2: Presigned URL Multipart Download (Key Innovation!)");
        System.out.println("   → First-ever multipart download support for presigned URLs");
        System.out.println("   → Using same presigned URL for fair comparison");
        
        // 🎯 THINK BIG: Configure for optimal performance across file sizes
        MultipartConfiguration config = MultipartConfiguration.builder()
            .minimumPartSizeInBytes(8L * 1024 * 1024)    // 8MB parts for good parallelism
            .thresholdInBytes(16L * 1024 * 1024)         // Use multipart for files > 16MB
            .build();
        
        // 🔧 INVENT AND SIMPLIFY: Same S3AsyncClient, enhanced with multipart capability
        S3AsyncClient multipartClient = S3AsyncClient.builder()
            .region(Region.US_EAST_1)
            .multipartConfiguration(config)  // ← This enables the innovation!
            .build();
        
        Instant start = Instant.now();
        System.out.println("   ⏱️  Starting multipart download at: " + start);
        
        // 📋 CUSTOMER OBSESSION: Same simple API, but faster performance
        PresignedUrlDownloadRequest request = PresignedUrlDownloadRequest.builder()
            .presignedUrl(presignedUrl)
            .build();
        
        // Show the multipart magic happening
        System.out.println("   🔍 Multipart innovation for presigned URLs:");
        System.out.println("      • Step 1: First part request (bytes=0-8388607) downloads 8MB + discovers total size");
        System.out.println("      • Step 2: Parse Content-Range header to get total object size");
        System.out.println("      • Step 3: Calculate remaining parts needed for parallel download");
        System.out.println("      • Step 4: Generate Range headers for remaining parts (bytes=8388608-16777215...)");
        System.out.println("      • Step 5: Download remaining parts concurrently while processing first part");
        System.out.println("      • Step 6: Assemble all parts in correct order");
        
        try {
            // Execute multipart download
            CompletableFuture<ResponseBytes<GetObjectResponse>> future = 
                multipartClient.presignedUrlExtension()
                              .getObject(request, AsyncResponseTransformer.toBytes());
            
            ResponseBytes<GetObjectResponse> response = future.get();
            
            Duration elapsed = Duration.between(start, Instant.now());
            long seconds = elapsed.getSeconds();
            long bytes = response.asByteArray().length;
            
            System.out.println("   ✅ Download completed in: " + seconds + " seconds");
            System.out.println("   📊 Downloaded: " + formatBytes(bytes));
            System.out.println("   📊 Throughput: " + formatThroughput(bytes, seconds));
            System.out.println("   ⚡ Multiple parallel connections used automatically");
            
            System.out.println("   🚀 Key innovation: Multipart downloads now work with presigned URLs!");
            
        } catch (Exception e) {
            System.out.println("   ❌ Multipart download failed: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            multipartClient.close();
        }
    }
    
    /**
     * Format bytes in human readable format
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    /**
     * Calculate and format throughput
     */
    private static String formatThroughput(long bytes, long seconds) {
        if (seconds == 0) return "N/A";
        double mbps = (bytes / (1024.0 * 1024)) / seconds;
        return String.format("%.2f MB/s", mbps);
    }
}
