#!/usr/bin/env node
'use strict';

const http = require('http');

const PORT = parseInt(process.env.PORT || '4010', 10);
const DISH_ID = process.env.MOCK_DISH_ID || '999';
const ACCOUNTS = (process.env.MOCK_ACCOUNTS || 'tester01@lastdish.test,tester02@lastdish.test')
  .split(',')
  .map((email) => email.trim())
  .filter(Boolean);
let stockQuantity = parseInt(process.env.MOCK_STOCK || '5', 10);

function sendJson(res, status, body) {
  res.writeHead(status, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(body));
}

function readJsonBody(req) {
  return new Promise((resolve, reject) => {
    let raw = '';
    req.on('data', (chunk) => {
      raw += chunk;
    });
    req.on('end', () => {
      if (!raw) return resolve({});
      try {
        resolve(JSON.parse(raw));
      } catch (error) {
        reject(error);
      }
    });
    req.on('error', reject);
  });
}

function emailFromAuth(req) {
  const header = req.headers['authorization'] || '';
  if (!header.startsWith('Bearer token-')) return null;
  return header.slice('Bearer token-'.length);
}

const server = http.createServer((req, res) => {
  const url = new URL(req.url, `http://localhost:${PORT}`);

  if (req.method === 'POST' && url.pathname === '/api/v1/auth/login') {
    readJsonBody(req).then((body) => {
      if (!ACCOUNTS.includes(body.email)) {
        return sendJson(res, 401, {
          success: false,
          error: { code: 'AUTH_FAIL', message: '로그인 실패' },
        });
      }
      sendJson(res, 200, {
        success: true,
        data: { accessToken: `token-${body.email}`, refreshToken: `refresh-${body.email}` },
      });
    });
    return;
  }

  if (req.method === 'GET' && url.pathname === '/api/v1/carts/members') {
    const email = emailFromAuth(req);
    if (!email) {
      return sendJson(res, 401, {
        success: false,
        error: { code: 'AUTH_FAIL', message: '인증 실패' },
      });
    }
    const index = ACCOUNTS.indexOf(email);
    sendJson(res, 200, {
      success: true,
      data: {
        cartId: 1,
        memberId: index + 1,
        items: [
          {
            cartItemId: index + 1,
            dishId: Number(DISH_ID),
            dishName: 'mock dish',
            unitPrice: 1000,
            quantity: 1,
            subtotalPrice: 1000,
            status: 'ACTIVE',
            orderable: true,
            lastAppliedDishPriceVersion: 1,
          },
        ],
        totalPrice: 1000,
      },
    });
    return;
  }

  if (req.method === 'POST' && url.pathname.startsWith('/api/v1/orders/cartItems/')) {
    const email = emailFromAuth(req);
    if (!email) {
      return sendJson(res, 401, {
        success: false,
        error: { code: 'AUTH_FAIL', message: '인증 실패' },
      });
    }
    readJsonBody(req).then(() => {
      if (stockQuantity <= 0) {
        return sendJson(res, 409, {
          success: false,
          error: { code: 'D003', message: '재고가 부족합니다.' },
        });
      }
      stockQuantity -= 1;
      sendJson(res, 200, { success: true, data: { orderId: Date.now() } });
    });
    return;
  }

  if (req.method === 'GET' && url.pathname === `/api/v1/dishes/${DISH_ID}`) {
    return sendJson(res, 200, { success: true, data: { dishId: Number(DISH_ID), stockQuantity } });
  }

  sendJson(res, 404, { success: false, error: { code: 'NOT_FOUND', message: 'Not found' } });
});

server.listen(PORT, () => {
  console.log(
    `[mock] listening on :${PORT}, dishId=${DISH_ID}, stock=${stockQuantity}, accounts=${ACCOUNTS.join(',')}`,
  );
});
