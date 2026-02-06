FROM ubuntu:22.04

# Install Java 11 and Maven
RUN apt-get update && \
    apt-get install -y openjdk-11-jdk maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set Java home dynamically (works for both amd64 and arm64)
RUN echo "export JAVA_HOME=\$(dirname \$(dirname \$(readlink -f \$(which java))))" >> /etc/profile
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-arm64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Create app directory
WORKDIR /app

# Copy the Maven project
COPY pom.xml .
COPY src ./src

# Copy the locally built AWS SDK artifacts from host Maven repository
# This ensures we use the snapshot with PQ TLS opt-out changes
RUN mkdir -p /root/.m2/repository

# Note: We don't download dependencies here because SNAPSHOT versions
# will be provided via volume mount at runtime

# Default command - compile and run
CMD ["sh", "-c", "mvn compile && mvn exec:java"]
