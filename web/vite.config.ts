import { defineConfig } from "vitest/config";

export default defineConfig({
  build: {
    target: "chrome69",
    manifest: true,
  },
  test: {
    include: ["src/**/*.test.ts", "test/**/*.test.ts"],
  },
});
