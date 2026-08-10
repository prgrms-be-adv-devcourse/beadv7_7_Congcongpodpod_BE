FROM docker.elastic.co/elasticsearch/elasticsearch:9.5.0

# 로컬 개발 환경은 인터넷 연결을 사용해 본체와 같은 버전의 공식 Nori 플러그인을 설치합니다.
RUN bin/elasticsearch-plugin install --batch analysis-nori
