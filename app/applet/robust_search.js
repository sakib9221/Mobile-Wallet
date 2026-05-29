const fs = require('fs');
const path = require('path');

function robustSearch(dir, depth = 0) {
    if (depth > 8) return;
    let files;
    try {
        files = fs.readdirSync(dir);
    } catch (e) {
        // Log skip
        return;
    }
    
    for (const file of files) {
        const fullPath = path.join(dir, file);
        let stats;
        try {
            stats = fs.statSync(fullPath);
        } catch (e) {
            continue;
        }
        
        if (stats.isDirectory()) {
            if (file === 'proc' || file === 'sys' || file === 'dev' || file === 'etc' || file === 'lib' || file === 'var' || file === 'usr' || file === 'boot' || file === 'run' || file === 'sys' || file === '.git' || file === 'node_modules' || file === '.gradle' || file === 'build') {
                continue;
            }
            robustSearch(fullPath, depth + 1);
        } else {
            if (file.toLowerCase().includes('input_file') || file.toLowerCase().includes('avatar') || file.toLowerCase().includes('attachment') || file.endsWith('.png') || file.endsWith('.jpg') || file.endsWith('.jpeg')) {
                console.log(`FOUND: ${fullPath} (${stats.size} bytes)`);
            }
        }
    }
}

console.log("Searching root '/'...");
robustSearch('/');
console.log("Search finished.");
