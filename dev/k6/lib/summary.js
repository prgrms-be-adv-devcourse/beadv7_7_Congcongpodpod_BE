// k6는 실행이 끝나면 handleSummary()에 집계 결과를 넘긴다. 기본 동작은 화면 출력뿐이라
// 실행이 끝나면 수치가 스크롤 밖으로 사라진다. 여기서 같은 결과를 파일로도 남긴다.
//
// 결과 파일은 컨테이너의 /results에 쓰고, k6.sh가 그 경로를 dev/k6/results에 연결한다.
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

const RESULT_DIR = '/results';

// 파일 이름이 겹치면 이전 실행 결과를 덮어쓴다. RUN_ID와 라벨로 실행을 구분한다.
function resultBaseName() {
  const runId = __ENV.RUN_ID || 'norunid';
  const label = __ENV.RESULT_LABEL;
  return label ? `${runId}-${label}` : runId;
}

// 사용법: 각 시나리오 스크립트에서 `export const handleSummary = buildSummaryHandler();`
export function buildSummaryHandler(extra = {}) {
  return function handleSummary(data) {
    const base = `${RESULT_DIR}/${resultBaseName()}`;
    const text = textSummary(data, { indent: ' ', enableColors: false });

    return {
      // 화면에도 그대로 출력한다. 이게 없으면 실행 중 아무것도 안 보인다.
      stdout: textSummary(data, { indent: ' ', enableColors: true }),
      // 사람이 읽는 요약
      [`${base}-summary.txt`]: text,
      // 그래프·표로 가공할 원본. 커스텀 지표(step_*, flow_*)가 전부 들어 있다.
      [`${base}-summary.json`]: JSON.stringify(
        {
          runId: __ENV.RUN_ID || null,
          label: __ENV.RESULT_LABEL || null,
          // 실행 구간을 Grafana와 맞추려면 시각이 필요하다.
          finishedAtUtc: new Date().toISOString(),
          ...extra,
          metrics: data.metrics,
        },
        null,
        2,
      ),
    };
  };
}
