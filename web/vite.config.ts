import { readFileSync } from "node:fs";
import { defineConfig, type Plugin } from "vitest/config";

const { version } = JSON.parse(readFileSync(new URL("package.json", import.meta.url), "utf8")) as {
  version: string;
};

// Chromium blocks module scripts on file://, which is how a Tizen widget loads its page, so the
// single entry is emitted as a classic deferred script.
function classicScript(): Plugin {
  return {
    name: "classic-script",
    enforce: "post",
    transformIndexHtml: (html) =>
      html.replace(/<script type="module" crossorigin /g, "<script defer "),
  };
}

export default defineConfig({
  base: "./",
  define: { __APP_VERSION__: JSON.stringify(version) },
  plugins: [classicScript()],
  build: {
    target: "chrome69",
    manifest: true,
    rolldownOptions: { output: { format: "iife" } },
  },
  test: {
    include: ["src/**/*.test.ts", "test/**/*.test.ts"],
  },
});
