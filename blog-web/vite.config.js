import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import { loadEnv } from 'vite'
import { createSvgIconsPlugin } from 'vite-plugin-svg-icons'
import devUtilityPlugin from './vite-plugin-dev-utility'

export default defineConfig(({ command, mode }) => {

    const env = loadEnv(mode, process.cwd());
    return {
        server: {
            // 默认只绑本机回环地址，避免开发服务被同网段其它主机直接访问；
            // 需要局域网/真机调试时在 .env.local 里设 VITE_APP_DEV_HOST=0.0.0.0 显式放开。
            host: env.VITE_APP_DEV_HOST || 'localhost',
            // 应用端口 (默认:3000)
            port: Number(env.VITE_APP_PORT),
            // 运行是否自动打开浏览器
            open: false,
            proxy: {
              /** 代理前缀为 /dev-api 的请求  */
              [env.VITE_APP_BASE_API]: {
                changeOrigin: true,
                // 接口地址
                target: env.VITE_APP_API_URL,
                rewrite: (path) =>
                  path.replace(new RegExp("^" + env.VITE_APP_BASE_API), ""),
              },
            },
            allowedHosts: ["test.com"]
        },
        plugins: [
          devUtilityPlugin(),
          vue(),
          createSvgIconsPlugin({
            // 指定需要缓存的图标文件夹
            iconDirs: [path.resolve(process.cwd(), 'src/assets/icons')],
            // 指定symbolId格式
            symbolId: 'icon-[dir]-[name]',
          }),
        ],
        resolve: {
          alias: {
            '@': path.resolve(__dirname, './src')
          }
        },
        css: {
            preprocessorOptions: {
              scss: {
                api: 'modern-compiler',
                additionalData: `
                  @use "@/styles/variables.scss" as *;
                  @use "@/styles/mixins.scss" as *;
                `
              }
            }
        }
    }

})
