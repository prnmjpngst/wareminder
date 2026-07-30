import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'

export default defineConfig({
  plugins: [svelte()],
  build: {
    outDir: '../app/src/main/assets/web',
    emptyOutDir: true,
    sourcemap: false
  },
  base: ''
})
