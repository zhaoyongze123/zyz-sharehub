const fs = require('fs');

const file = 'src/views/user/ProfileView.vue';
let content = fs.readFileSync(file, 'utf8');

// For .control-input and .control-textarea, add background-color and color
content = content.replace(/\.control-input\s*{([^}]*)}/, (match, p1) => {
  if (!p1.includes('background-color:')) {
    return `.control-input {${p1}\n  background-color: var(--app-bg-modal);\n  color: var(--app-text-main);\n}`;
  }
  return match;
});

content = content.replace(/\.control-textarea\s*{([^}]*)}/, (match, p1) => {
  if (!p1.includes('background-color:')) {
    return `.control-textarea {${p1}\n  background-color: var(--app-bg-modal);\n  color: var(--app-text-main);\n}`;
  }
  return match;
});

fs.writeFileSync(file, content, 'utf8');
console.log('Fixed inputs in', file);
