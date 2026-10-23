// NFR1 load test (k6): 50 concurrent users searching stations and registering parcels for 2 minutes.
// Run the app with the geocoding provider stubbed:   mvn spring-boot:run -Dspring-boot.run.profiles=loadtest
// Then:                                              k6 run load-tests/search-and-register.js
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  scenarios: {
    users: { executor: 'constant-vus', vus: 50, duration: '2m' },
  },
  thresholds: {
    http_req_duration: ['p(95)<1000'], // NFR1: 95% of requests below 1 s
    http_req_failed: ['rate<0.01'],    // NFR1: error rate below 1%
  },
};

// 409 (station full) is a correct business answer under load, not an error.
http.setResponseCallback(http.expectedStatuses({ min: 200, max: 399 }, 409));

export function setup() {
  const res = http.get(`${BASE}/api/stations`);
  return { stationIds: res.json().map((s) => s.id) };
}

const SIZES = ['S', 'M', 'L'];
const ADDRESSES = ['Rua de Aveiro 10', 'Avenida Dr. Lourenço Peixinho 50, Aveiro', 'Universidade de Aveiro'];

export default function (data) {
  const address = ADDRESSES[Math.floor(Math.random() * ADDRESSES.length)];
  const search = http.get(`${BASE}/api/stations/nearest?address=${encodeURIComponent(address)}`,
    { tags: { name: 'search' } });
  check(search, { 'search 200': (r) => r.status === 200 });

  const stationId = data.stationIds[Math.floor(Math.random() * data.stationIds.length)];
  const body = JSON.stringify({
    stationId,
    size: SIZES[Math.floor(Math.random() * SIZES.length)],
    recipientContact: `user${__VU}-${__ITER}@example.com`,
  });
  const reg = http.post(`${BASE}/api/parcels`, body,
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'register' } });
  check(reg, { 'register 201 or 409': (r) => r.status === 201 || r.status === 409 });

  // Free the compartment again so the stations do not stay full for the whole test.
  if (reg.status === 201) {
    http.post(`${BASE}/api/parcels/${reg.json().trackingCode}/cancel`, null, { tags: { name: 'cancel' } });
  }
  sleep(1);
}
