/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./frontend/src/**/*.scala", // for development
    "out/frontend/fullLinkJS.dest/*.js" // for production build
  ],
  theme: {
    extend: {},
  },
  plugins: [],
}
