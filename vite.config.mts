// https://vitejs.dev

import { defineConfig, Plugin } from "vite";
import { viteStaticCopy } from "vite-plugin-static-copy";
import basicSsl from "@vitejs/plugin-basic-ssl";

const isProd = process.env.NODE_ENV == "production";

// TODO: in dev patch index.html with localhost config
// TODO: in prod, leave it untouched, so the backend can patch it

const shoelaceIconsPath =
  "node_modules/@shoelace-style/shoelace/dist/assets/icons";

export default defineConfig({
  define: {
    AUTHN_URL: JSON.stringify(process.env.AUTHN_URL),
  },
  plugins: [
    viteStaticCopy({
      targets: [
        {
          // copy shoelace icons for production build
          src: `node_modules/@shoelace-style/shoelace/dist/assets/icons/*.svg`,
          dest: "shoelace/assets/icons",
        },
        {
          // copy shoelace themes for production build
          src: `node_modules/@shoelace-style/shoelace/dist/themes/*.css`,
          dest: "shoelace/themes",
        },
      ],
    }),
    // basicSsl() // generate cert for https: true
  ],
  resolve: {
    alias: [
      {
        // to resolve scalajs import in main.js
        find: /^scalajs:(.*)$/,
        replacement: `/out/frontend/${isProd ? "full" : "fast"}LinkJS.dest/$1`,
      },
      {
        // resolve shoelace icons in dev
        find: /\/assets\/icons\/(.+)/,
        replacement: `node_modules/@shoelace-style/shoelace/dist/assets/icons/$1`,
      },
      {
        // resolve shoelace themes in dev
        find: /\/shoelace\/themes\/(.+)/,
        replacement: `node_modules/@shoelace-style/shoelace/dist/themes/$1`,
      },
    ],
  },
  server: {
    proxy: {
      // to avoid CORS issues, proxy the requests to the backend
      "/RpcApi/": "http://localhost:8081",
    },
    watch: {
      // changes in scala files should not reload browser.
      // Only the compiled files should trigger reloads.
      ignored: ["**/*.scala"],
    },
  },
  build: {
    sourcemap: true,
  },
});
