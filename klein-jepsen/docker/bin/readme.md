
docker ps -aq | xargs docker rm && docker rmi jepsen_control  jepsen_n1 jepsen_n2 jepsen_n3 jepsen_n4 jepsen_n5

klein-jepsen/docker/bin/up



docker exec -it jepsen-control bash

lein run test --time-limit 40 --concurrency 10 --test-count 10 --username root --password 123456