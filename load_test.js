import http from 'k6/http';
import { check, sleep } from 'k6';

// Konfigurasi skenario Load Testing
export const options = {
  stages: [
    { duration: '10s', target: 20 }, // Fase 1: Naik bertahap sampai 20 virtual user dalam 10 detik
    { duration: '20s', target: 20 }, // Fase 2: Tahan di 20 user selama 20 detik
    { duration: '10s', target: 0 },  // Fase 3: Turun perlahan jadi 0 user dalam 10 detik
  ],
  thresholds: {
    // APDEX: 95% dari request harus selesai di bawah 500 milidetik
    http_req_duration: ['p(95)<500'],
  },
};

export default function () {
  // Ganti URL ini sesuai dengan environment Anda. 
  // Gunakan host.docker.internal jika menjalankan k6 dari docker, atau localhost jika dari terminal asli.
  const url = 'http://host.docker.internal:8081/auth/login';

  const payload = JSON.stringify({
    identifier: 'user@example.com',
    password: 'password123',
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  // Kirim POST request
  const res = http.post(url, payload, params);

  // Verifikasi apakah server merespon (termasuk error kredensial salah, yang penting server tidak mati/500)
  check(res, {
    'status is 200 (OK) or 400/401/403 (Client Error)': (r) => r.status >= 200 && r.status < 500,
  });

  // Simulasi user diam/baca-baca layar selama 1 detik sebelum login lagi
  sleep(1);
}
