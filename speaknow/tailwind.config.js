/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/main/resources/static/**/*.html"],
  theme: {
    extend: {
        colors: {
            primary: {
                50: '#f0f5ff', 100: '#e5edff', 200: '#cddbfe', 300: '#b4c6fc',
                400: '#8da2fb', 500: '#6875f5', 600: '#5850ec', 700: '#5145cd',
                800: '#42389d', 900: '#362f78'
            },
            surface: {
                50: '#f8fafc', 100: '#f1f5f9', 200: '#e2e8f0', 300: '#cbd5e1',
                400: '#94a3b8', 500: '#64748b', 600: '#475569', 700: '#334155',
                800: '#1e293b', 900: '#0f172a'
            }
        },
        fontFamily: {
            sans: ['Inter', 'system-ui', 'sans-serif'],
            serif: ['EB Garamond', 'Georgia', 'serif']
        }
    },
  },
  plugins: [],
}
