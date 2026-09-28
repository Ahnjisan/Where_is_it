import { defineConfig } from "vitest/config";
import { transformWithOxc } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = fileURLToPath(new URL(".", import.meta.url));

const nextJsAsJsx = {
  name: "next-js-as-jsx",
  enforce: "pre",
  async transform(code, id) {
    if (!id.includes("/src/") || !id.endsWith(".js")) return null;
    return transformWithOxc(code, id, { lang: "jsx" });
  },
};

export default defineConfig({
  plugins: [nextJsAsJsx, react()],
  test: {
    environment: "jsdom",
    globals: false,
    setupFiles: ["./vitest.setup.js"],
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
});
