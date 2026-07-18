import os
import re

html_dir = 'src/main/resources/static'
for root, dirs, files in os.walk(html_dir):
    for f in files:
        if f.endswith('.html'):
            filepath = os.path.join(root, f)
            with open(filepath, 'r', encoding='utf-8') as file:
                content = file.read()
            
            # Remove the tailwind.config script block
            content = re.sub(r'<script>\s*tailwind\.config\s*=\s*\{.*?</script>', '', content, flags=re.DOTALL)
            
            # If in index.html, remove duplicate const API_BASE_URL
            # In index.html, there's const API_BASE_URL = '/api'; in a script tag that clashes with auth-helper.js
            if f == 'index.html':
                content = content.replace("const API_BASE_URL = '/api';", "/* const API_BASE_URL = '/api'; */")
                
            with open(filepath, 'w', encoding='utf-8') as file:
                file.write(content)
