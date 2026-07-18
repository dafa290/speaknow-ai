import sys

filepath = 'src/main/resources/static/assets/js/auth-helper.js'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace document.body.appendChild(loader); with a wrapper
new_loader_logic = '''
    // Wait for body to be available before appending loader
    function appendLoader() {
        if (!document.body) {
            setTimeout(appendLoader, 10);
            return;
        }
        document.body.appendChild(loader);
    }
    appendLoader();
'''
content = content.replace("document.body.appendChild(loader);", new_loader_logic)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)
