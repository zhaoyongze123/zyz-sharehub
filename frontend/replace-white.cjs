const fs = require('fs');

const files = [
  'src/layouts/ConsoleLayout.vue',
  'src/views/user/ProfileView.vue'
];

files.forEach(file => {
  let content = fs.readFileSync(file, 'utf8');
  // replace exact color names
  content = content.replace(/background:\s*white/gi, 'background: var(--app-bg-modal)');
  content = content.replace(/background-color:\s*white/gi, 'background-color: var(--app-bg-modal)');
  // We leave "color: white" as that might be for toasts/buttons. Wait, is there a color: white?
  // 527: color: white; / 298: color: white; - This is likely fine for tooltips/buttons where we actually want white text in both light and dark.
  fs.writeFileSync(file, content, 'utf8');
  console.log('Replaced white backgrounds in', file);
});
