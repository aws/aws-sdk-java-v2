#!/usr/bin/env python3
import os
import argparse
import json
import re
from pathlib import Path

# Load the example metadata from JSON file.
def load_example_metadata(metadata_file_path):
    with open(metadata_file_path, 'r', encoding='utf-8') as f:
        return json.load(f)

# Map service keys from metadata to actual package names.
def map_service_name(metadata_service_key):
    return metadata_service_key.replace('-', '')

# Generate the HTML content for code examples section.
def generate_code_examples_html(examples_data):
    if not examples_data:
        return ""
    
    # Group examples by category
    categories = {}
    for example in examples_data:
        category = example.get('category', 'Api')
        if category not in categories:
            categories[category] = []
        categories[category].append(example)
    
    # Define category display names and order
    category_mapping = {
        'Hello': 'Getting Started',
        'Api': 'API Actions',
        'Basics': 'Basics', 
        'Scenarios': 'Scenarios',
        'Serverless examples': 'Serverless Examples'
    }
    
    # Ordered categories (common ones first)
    ordered_categories = ['Hello', 'Api', 'Basics', 'Scenarios', 'Serverless examples']
    
    html_content = []
    html_content.append('<h3 id="code-examples">Code Examples</h3>')
    html_content.append('<p>The following code examples show how to use this service with the AWS SDK for Java v2:</p>')
    
    # Process ordered categories first
    for category in ordered_categories:
        if category in categories:
            display_name = category_mapping.get(category, category)
            html_content.append(f'<h4>{display_name}</h4>')
            html_content.append('<ul>')
            
            for example in categories[category]:
                title = example.get('title', '')
                url = example.get('url', '')
                html_content.append(f'<li><a href="{url}">{title}</a></li>')
            
            html_content.append('</ul>')
            
            # Remove from categories dict so we don't process it again
            del categories[category]
    
    # Process any remaining categories
    for category, examples in categories.items():
        display_name = category_mapping.get(category, category)
        html_content.append(f'<h4>{display_name}</h4>')
        html_content.append('<ul>')
        
        for example in examples:
            title = example.get('title', '')
            url = example.get('url', '')
            html_content.append(f'<li><a href="{url}">{title}</a></li>')
        
        html_content.append('</ul>')
    
    return '\n'.join(html_content)

# Inject code examples HTML into package-summary.html file.
def inject_examples_into_html(html_file_path, examples_html):
    try:
        with open(html_file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Inject before the bottom navbar comment
        injection_pattern = r'(<!--\s*======= START OF BOTTOM NAVBAR ======\s*-->)'
        examples_section = f'\n<div class="block">\n{examples_html}\n</div>\n'
        
        if re.search(injection_pattern, content, re.IGNORECASE | re.DOTALL):
            modified_content = re.sub(injection_pattern, examples_section + r'\1', content, count=1, flags=re.IGNORECASE | re.DOTALL)
            
            # Write the modified HTML back
            with open(html_file_path, 'w', encoding='utf-8') as f:
                f.write(modified_content)
            
            print(f"Injected code examples into {html_file_path.name}")
            return True
        else:
            print(f"Could not find injection point in {html_file_path}")
            return False
            
    except Exception as e:
        print(f"Error processing {html_file_path}: {str(e)}")
        return False

# Find all package-summary.html files in the documentation.
def find_package_summary_files(docs_path):
    package_files = []
    services_path = Path(docs_path) / 'software' / 'amazon' / 'awssdk' / 'services'
    
    if services_path.exists():
        for service_dir in services_path.iterdir():
            if service_dir.is_dir():
                package_summary = service_dir / 'package-summary.html'
                if package_summary.exists():
                    package_files.append((service_dir.name, package_summary))
    
    return package_files

def inject_code_examples(metadata_file_path, docs_path):
    metadata = load_example_metadata(metadata_file_path)
    package_files = find_package_summary_files(docs_path)
    
    services_with_examples = metadata.get('services', {})
    processed_count = 0
    injected_count = 0
    
    for service_name, html_file_path in package_files:
        processed_count += 1
        
        # Try to find matching service in metadata
        metadata_key = None
        for key in services_with_examples.keys():
            if map_service_name(key) == service_name:
                metadata_key = key
                break
        
        if metadata_key and metadata_key in services_with_examples:
            examples = services_with_examples[metadata_key].get('examples', [])
            if examples:
                print(f"Injecting examples for {service_name} ({len(examples)} examples)")
                examples_html = generate_code_examples_html(examples)
                
                if inject_examples_into_html(html_file_path, examples_html):
                    injected_count += 1
                else:
                    print(f"Failed to inject examples for {service_name}")
        else:
            print(f"No examples found for {service_name}")
    
    print(f"Processed {processed_count} services, injected examples into {injected_count} services")

def main():
    parser = argparse.ArgumentParser(description="Inject code examples into API reference guide.")
    parser.add_argument("--exampleMetadata", required=True, help="Path to example-meta.json file")
    parser.add_argument("--docsPath", required=True, help="Path to generated javadoc directory")
    
    args = parser.parse_args()
    
    if not Path(args.exampleMetadata).exists():
        print(f"Error: Example metadata file not found: {args.exampleMetadata}")
        return 1
    
    if not Path(args.docsPath).exists():
        print(f"Error: Documentation path not found: {args.docsPath}")
        return 1
    
    inject_code_examples(args.exampleMetadata, args.docsPath)
    print("Code examples injection completed!")
    return 0

if __name__ == "__main__":
    exit(main())
