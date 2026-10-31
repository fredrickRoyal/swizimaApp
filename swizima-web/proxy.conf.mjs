// Dev-server proxy: forwards /api/** to the Swizima API gateway (default port 9080).
// The browser still sends `Origin: http://localhost:4200`, so that origin must be in
// the gateway's CORS_ALLOWED_ORIGINS (it is by default).
// Override the target if needed, e.g.:
//   API_GATEWAY_URL=http://localhost:8090 npm start
const target = process.env.API_GATEWAY_URL || 'http://localhost:9080';

export default {
  '/api': {
    target,
    secure: false,
    changeOrigin: true,
    logLevel: 'warn',
  },
};
