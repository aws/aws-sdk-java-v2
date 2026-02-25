#!/usr/bin/env ruby
# frozen_string_literal: true

# Copies model.json files from aws-models into the SDK services directory.
#
# For each service directory under services/, this script:
# 1. Finds the matching aws-models directory by scanning the Smithy model
#    namespace (com.amazonaws.<namespace>) and matching it to the SDK service dir name.
# 2. Copies <aws-models-dir>/smithy/model.json to
#    services/<service-id>/src/main/resources/codegen-resources/model.json
# 3. Reports any SDK services that have no match in aws-models.

require 'json'
require 'fileutils'

AWS_MODELS_ROOT = "/Users/alexwoo/smithy/aws-models"
SERVICES_ROOT = "services"
SKIP_DIRS = %w[.settings new-service-template].freeze

# Build a mapping from Smithy namespace -> aws-models directory name.
# Each model.json has shapes like "com.amazonaws.<namespace>#ServiceName".
# The <namespace> portion matches the SDK service directory name.
def build_namespace_to_awsmodels_dir_map
  ns_map = {}

  Dir.glob(File.join(AWS_MODELS_ROOT, "*/smithy/model.json")).each do |model_path|
    aws_dir = File.basename(File.dirname(File.dirname(model_path)))
    begin
      data = JSON.parse(File.read(model_path))
      shapes = data["shapes"] || {}
      shapes.each do |shape_name, shape_def|
        next unless shape_def["type"] == "service"

        parts = shape_name.split("#").first.split(".")
        if parts.length >= 3
          namespace = parts[2]
          ns_map[namespace] = aws_dir
        end
        break
      end
    rescue JSON::ParserError, Errno::ENOENT => e
      warn "Warning: could not parse #{model_path}: #{e.message}"
    end
  end

  ns_map
end

def sdk_service_dirs
  Dir.children(SERVICES_ROOT)
     .select { |d| File.directory?(File.join(SERVICES_ROOT, d)) }
     .reject { |d| SKIP_DIRS.include?(d) }
     .sort
end

def main
  ns_map = build_namespace_to_awsmodels_dir_map
  services = sdk_service_dirs

  copied = 0
  not_in_aws_models = []

  services.each do |service_id|
    aws_dir = ns_map[service_id]

    if aws_dir.nil?
      not_in_aws_models << service_id
      next
    end

    src = File.join(AWS_MODELS_ROOT, aws_dir, "smithy", "model.json")
    unless File.exist?(src)
      not_in_aws_models << service_id
      next
    end

    dest_dir = File.join(SERVICES_ROOT, service_id, "src", "main", "resources", "codegen-resources")
    FileUtils.mkdir_p(dest_dir)

    dest = File.join(dest_dir, "model.json")
    FileUtils.cp(src, dest)
    copied += 1
  end

  puts "Copied #{copied} model.json files."
  puts

  if not_in_aws_models.any?
    puts "Services in this repo but NOT in aws-models (#{not_in_aws_models.size}):"
    not_in_aws_models.each { |s| puts "  #{s}" }
  else
    puts "All services matched."
  end
end

main
