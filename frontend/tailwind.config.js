/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      fontFamily: {
        mono: ['"JetBrains Mono"', '"Fira Code"', 'monospace'],
        sans: ['"Inter"', 'sans-serif'],
      },
      colors: {
        ink: {
          950: '#030712',
          900: '#0d1117',
          800: '#111827',
          700: '#1f2937',
          600: '#374151',
          500: '#6b7280',
        },
        indigo: {
          400: '#818cf8',
          500: '#6366f1',
          600: '#4f46e5',
        }
      }
    }
  },
  plugins: []
}
