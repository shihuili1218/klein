
docker ps -aq | xargs docker rm && docker rmi shihuili1218/klein-jepsen-control shihuili1218/klein-jepsen-node

klein-jepsen/docker/bin/up

docker exec -it jepsen-control bash

lein run test --time-limit 40 --concurrency 10 --test-count 10 --username root --password 123456