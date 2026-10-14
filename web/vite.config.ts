import { defineConfig } from 'vite';

export default defineConfig({
    base: './',
    build: {
        outDir: 'dist',
        assetsDir: 'assets',
        chunkSizeWarningLimit: 600,
    },
    assetsInclude: [],
    test: {
        environment: 'jsdom',
        globals: true,
    },
});
