
docker ps -aq | xargs docker rm && docker rmi jepsen_control  jepsen_n1 jepsen_n2 jepsen_n3 jepsen_n4 jepsen_n5

sh klein-jepsen/docker/bin/up
