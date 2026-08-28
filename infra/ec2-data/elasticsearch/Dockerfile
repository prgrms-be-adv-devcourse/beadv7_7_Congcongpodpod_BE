FROM docker.elastic.co/elasticsearch/elasticsearch:9.5.0-amd64

# Elasticsearch 플러그인은 본체와 버전이 정확히 같아야 한다.
# Private EC2가 인터넷에 연결되지 않아 ZIP을 이미지 빌드 컨텍스트에 포함해 설치한다.
COPY --chown=elasticsearch:root analysis-nori-9.5.0.zip /tmp/analysis-nori-9.5.0.zip

RUN bin/elasticsearch-plugin install \
      --batch \
      file:///tmp/analysis-nori-9.5.0.zip \
    && rm /tmp/analysis-nori-9.5.0.zip
