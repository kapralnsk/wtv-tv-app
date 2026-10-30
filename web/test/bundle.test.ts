import { parse } from "acorn";
import { mkdtemp, readFile, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { gzipSync } from "node:zlib";
import { build, type Manifest } from "vite";
import { afterAll, beforeAll, expect, test } from "vitest";

// AGENTS.md, Performance budgets.
const INITIAL_JS_GZIP_CEILING = 500 * 1024;

let outDir: string;
let manifest: Manifest;

beforeAll(async () => {
  outDir = await mkdtemp(join(tmpdir(), "wtv-bundle-"));
  await build({ root: join(import.meta.dirname, ".."), logLevel: "silent", build: { outDir } });
  manifest = JSON.parse(await readFile(join(outDir, ".vite/manifest.json"), "utf8")) as Manifest;
}, 60_000);

afterAll(async () => {
  await rm(outDir, { recursive: true, force: true });
});

function initialChunks(): string[] {
  const files = new Set<string>();
  const visit = (key: string) => {
    const chunk = manifest[key];
    if (!chunk || files.has(chunk.file)) return;
    files.add(chunk.file);
    chunk.imports?.forEach(visit);
  };
  Object.entries(manifest)
    .filter(([, chunk]) => chunk.isEntry)
    .forEach(([key]) => {
      visit(key);
    });
  return [...files].filter((file) => file.endsWith(".js"));
}

test("initial JS stays within the gzipped size budget", async () => {
  const files = initialChunks();
  expect(files).not.toHaveLength(0);
  let gzipped = 0;
  for (const file of files) gzipped += gzipSync(await readFile(join(outDir, file))).length;
  expect(gzipped).toBeLessThanOrEqual(INITIAL_JS_GZIP_CEILING);
});

// acorn's ES2020 level is the closest to Chromium M69: it accepts dynamic import() and
// import.meta, which M69 has, so the ES2020 syntax M69 lacks is rejected by hand.
const UNSUPPORTED_IN_M69: Record<string, (node: Record<string, unknown>) => boolean> = {
  "optional chaining": (node) => node.type === "ChainExpression",
  "nullish coalescing": (node) => node.type === "LogicalExpression" && node.operator === "??",
  "BigInt literal": (node) => node.type === "Literal" && "bigint" in node,
  "export * as ns": (node) => node.type === "ExportAllDeclaration" && node.exported != null,
};

function findUnsupported(node: unknown, found: Set<string>): void {
  if (Array.isArray(node)) {
    for (const child of node) findUnsupported(child, found);
    return;
  }
  if (typeof node !== "object" || node === null) return;
  const record = node as Record<string, unknown>;
  if (typeof record.type === "string") {
    for (const [feature, matches] of Object.entries(UNSUPPORTED_IN_M69)) {
      if (matches(record)) found.add(feature);
    }
  }
  for (const value of Object.values(record)) findUnsupported(value, found);
}

test("emitted JS uses only syntax Chromium M69 supports", async () => {
  const files = Object.values(manifest)
    .map((chunk) => chunk.file)
    .filter((file) => file.endsWith(".js"));
  expect(files).not.toHaveLength(0);
  for (const file of new Set(files)) {
    const source = await readFile(join(outDir, file), "utf8");
    const found = new Set<string>();
    findUnsupported(parse(source, { ecmaVersion: 2020, sourceType: "module" }), found);
    expect([...found], file).toEqual([]);
  }
});

test("built index.html references only relative assets, as classic scripts", async () => {
  const html = await readFile(join(outDir, "index.html"), "utf8");
  const urls = [...html.matchAll(/\b(?:src|href)="([^"]*)"/g)].map((match) => match[1] ?? "");
  expect(urls).not.toHaveLength(0);
  for (const url of urls) expect(url).toMatch(/^\.\//);
  expect(html).not.toContain('type="module"');
});
