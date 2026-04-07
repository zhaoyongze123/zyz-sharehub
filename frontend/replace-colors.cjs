const fs = require('fs');

const files = [
  'src/layouts/ConsoleLayout.vue',
  'src/views/user/ProfileView.vue'
];

const colorMap = {
  '#fafafa': 'var(--app-bg-main)',
  '#ffffff': 'var(--app-bg-nav)',
  '#f3f4f6': 'var(--app-bg-hover)',
  '#f9fafb': 'var(--app-bg-soft)',
  '#e5e7eb': 'var(--app-border)',
  '#d1d5db': 'var(--app-border-focus)',
  '#111827': 'var(--app-text-main)',
  '#374151': 'var(--app-text-sub)',
  '#6b7280': 'var(--app-text-muted)',
  '#9ca3af': 'var(--app-text-light)',
  '#2563eb': 'var(--app-accent)',
  '#10b981': 'var(--app-toast-bg)',
  '#dc2626': 'var(--app-danger)',
  '#fee2e2': 'var(--app-danger-border)',
  '#fef2f2': 'var(--app-danger-bg)'
};

files.forEach(file => {
  let content = fs.readFileSync(file, 'utf8');
  for (const [hex, variable] of Object.entries(colorMap)) {
    const regex = new RegExp(hex, 'gi');
    content = content.replace(regex, variable);
  }
  fs.writeFileSync(file, content, 'utf8');
  console.log('Replaced colors in', file);
});
