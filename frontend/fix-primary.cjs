const fs = require('fs');

const file = 'src/views/user/ProfileView.vue';
let content = fs.readFileSync(file, 'utf8');

// Fix .btn-primary color
content = content.replace(/\.btn-primary\s*{[^}]*color:\s*white;[^}]*}/, (match) => {
  return match.replace('color: white;', 'color: var(--app-bg-main);');
});

// Also fix .avatar-circle border which uses var(--app-text-main) but background was white. Let's make it background: var(--app-bg-modal).
content = content.replace(/background:\s*white;/gi, 'background: var(--app-bg-modal);');
content = content.replace(/background-color:\s*white;/gi, 'background-color: var(--app-bg-modal);');
// color: white -> color: var(--app-text-main) if it was inside a text div probably. Actually wait 
fs.writeFileSync(file, content, 'utf8');
console.log('Fixed btn-primary in', file);
