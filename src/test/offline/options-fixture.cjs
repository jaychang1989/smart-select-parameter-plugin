/* Loopback-only deterministic REST fixture for browser integration checks. */
const http = require('node:http');
const port = Number(process.env.SMART_SELECT_FIXTURE_PORT || 18767);
let failing = false;
const server = http.createServer((request, response) => {
  const url = new URL(request.url, `http://127.0.0.1:${port}`);
  const send = (status, value) => {
    response.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' });
    response.end(JSON.stringify(value));
  };
  if (url.pathname === '/control') { failing = url.searchParams.get('down') === 'true'; return send(200, { failing }); }
  if (url.pathname === '/flaky' && failing) return send(503, { error: 'Controlled fixture outage' });
  if (url.pathname === '/error') return send(503, { error: 'Fixture temporarily unavailable' });
  if (url.pathname === '/health') return send(200, { ready: true });
  if (url.pathname === '/auth' && request.headers.authorization !== 'Bearer local-test-token') return send(401, {});
  let items;
  if (url.pathname === '/large') {
    items = Array.from({ length: 2000 }, (_, i) => ({ value: `service-${String(i).padStart(4, '0')}`, label: `服务 ${String(i).padStart(4, '0')}` }));
  } else {
    const env = url.searchParams.get('env');
    const suffix = env ? `-${env}` : '';
    items = [{ value: `price${suffix}`, label: `报价服务${suffix}` }, { value: `admin${suffix}`, label: `管理服务${suffix}` }];
  }
  if (url.pathname === '/slow') return setTimeout(() => send(200, { data: { items } }), url.searchParams.get('env') === 'dev' ? 1200 : 20);
  send(200, { data: { items } });
});
server.listen(port, '127.0.0.1', () => console.log(`Smart Select REST fixture ready on 127.0.0.1:${port}`));
