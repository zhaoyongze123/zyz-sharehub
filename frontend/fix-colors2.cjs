const fs = require('fs');

const files = [
  'src/layouts/ConsoleLayout.vue',
  'src/views/user/ProfileView.vue'
];

const colorMap = {
  '#4b5563': 'var(--app-text-sub)',
  '#eff6ff': 'var(--app-bg-hover)',
  // '#8c5a45' is probably specific, let's leave it
};

files.forEach(file => {
  let content = fs.readFileSync(file, 'utf8');
  for (const [hex, variable] of Object.entries(colorMap)) {
    const regex = new RegExp(hex, 'gi');
    content = content.replace(regex, variable);
  }
  
  // also replace any remaining 'white' background strings
  content = content.replace(/background:\s*white/gi, 'background: var(--app-bg-modal)');
  content = content.replace(/background-color:\s*white/gi, 'background-color: var(--app-bg-modal)');
  
  fs.writeFileSync(file, content, 'utf8');
  console.log('Replaced colors 2 in', file);
});
