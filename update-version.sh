#!/bin/bash

# Script to update versions for migration to JReleaser
# Updates main pom.xml version and all parent references

NEW_VERSION="3.5.5"
OLD_VERSION="2.31.68-SNAPSHOT"
echo "Setting versions to $NEW_VERSION"

# Update main pom.xml version
echo "Updating main pom.xml version"
sed -i '' "s|<version>$OLD_VERSION</version>|<version>$NEW_VERSION</version>|g" pom.xml

# Update awsjavasdk.version property
echo "Updating awsjavasdk.version property"
sed -i '' "s|<awsjavasdk.version>.*</awsjavasdk.version>|<awsjavasdk.version>$NEW_VERSION</awsjavasdk.version>|g" pom.xml

# Update parent references in all pom.xml files - more robust approach
echo "Updating parent references in all pom files"
find . -name "pom.xml" -not -path "./pom.xml" | while read -r POM; do
  echo "Processing $POM"

  # Check if this pom has a parent reference to aws-sdk-java-pom
  if grep -q "<artifactId>aws-sdk-java-pom</artifactId>" "$POM"; then
    # Extract the parent section
    PARENT_SECTION=$(grep -A 10 "<parent>" "$POM" | grep -B 10 "</parent>" | head -n 20)

    # Check if the parent section contains the old version
    if echo "$PARENT_SECTION" | grep -q "<version>$OLD_VERSION</version>"; then
      # Replace the version in the parent section
      sed -i '' "s|<version>$OLD_VERSION</version>|<version>$NEW_VERSION</version>|g" "$POM"
      echo "  Updated parent reference in $POM"
    fi
  fi
done

# Special handling for v2-migration which might use ${awsjavasdk.version}-PREVIEW
echo "Special handling for v2-migration"
find . -name "pom.xml" -path "*/v2-migration*" | while read -r POM; do
  echo "Processing $POM for special version pattern"
  sed -i '' "s|<version>\${awsjavasdk.version}-PREVIEW</version>|<version>$NEW_VERSION-PREVIEW</version>|g" "$POM"
done

echo "Done updating versions to $NEW_VERSION"