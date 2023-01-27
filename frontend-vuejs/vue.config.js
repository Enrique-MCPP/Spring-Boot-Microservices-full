const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
    transpileDependencies: true,
    devServer: {
        port: 3000,
        open: true,
        proxy: {
            '/product/*': {
                target: 'http://localhost:9191',
                changeOrigin: true
            },
            '/demo/*': {
                target: 'http://localhost:8070',
                changeOrigin: true
            },
            '/foo/*': {
                target: 'http://localhost:8089',
                changeOrigin: true
            }
        }
    }
})