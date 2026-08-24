import { check } from 'k6';
import crypto from 'k6/crypto';

import { options as dailyOptions } from './daily-operations.js';

const expectedStateSha256 = crypto.sha256(open(__ENV.STATE_FILE), 'hex');

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    checks: ['rate==1'],
  },
};

export default function () {
  check(dailyOptions.tags, {
    '일일 트래픽 결과에 실행 상태 식별 태그를 기록': (tags) =>
      tags &&
      tags.testid === __ENV.RUN_ID &&
      tags.phase === 'daily_operations' &&
      tags.campaign_day === __ENV.CAMPAIGN_DAY &&
      tags.dataset_epoch === __ENV.DATASET_EPOCH &&
      tags.state_file_sha256 === expectedStateSha256,
  });

  console.log('일일 트래픽 메타데이터 검증 완료: HTTP 요청 0회');
}
