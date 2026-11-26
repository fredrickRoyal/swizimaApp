// Dev-server proxy: forwards /api/** to the Swizima API gateway (154.72.196.32:9080)
const target = process.env.API_GATEWAY_URL || 'http://154.72.196.32:9080';

export default {
  '/api': {
    target,
    secure: false,
    changeOrigin: true,
    logLevel: 'warn',
  },
};

