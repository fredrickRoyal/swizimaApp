// Dev-server proxy: forwards /api/** to the Swizima API gateway (154.72.196.32:9080)
// Deletes req.headers.origin in `bypass` BEFORE http.request() constructs outgoing headers,
// because Node's `socket` event fires after `(req).pipe(proxyReq)` has already flushed headers on POST!
const target = process.env.API_GATEWAY_URL || 'http://154.72.196.32:9080';

export default {
  '/api': {
    target,
    secure: false,
    changeOrigin: true,
    bypass: (req) => {
      delete req.headers['origin'];
      delete req.headers['Origin'];
      return null;
    },
    configure: (proxy) => {
      proxy.on('start', (req) => {
        delete req.headers['origin'];
        delete req.headers['Origin'];
      });
    },
  },
};

