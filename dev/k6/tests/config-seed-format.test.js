import { check } from 'k6';

import { accountEmail } from '../lib/config.js';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ['rate==1'],
  },
};

export default function () {
  check(accountEmail(1), {
    '시드 계정 1번은 4자리 이메일을 만든다': (email) => email === 'seller0001@seed.lastdish.kr',
  });
  check(accountEmail(301), {
    '시드 계정 301번도 4자리 이메일을 만든다': (email) => email === 'seller0301@seed.lastdish.kr',
  });
}
