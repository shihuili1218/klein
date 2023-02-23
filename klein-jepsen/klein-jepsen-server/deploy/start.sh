#!/bin/bash
set -x

nohup java -Xmx512m -Xms512m  \
  -Dklein.id=${KLEIN_ID}  \
  -Dklein.port=${KLEIN_PORT}  \
  -Dklein.ip=${KLEIN_IP}  \
  -Dklein.members=${KLEIN_MEMBERS} \
  -jar /klein/klein-server.jar \
  $@ >> klein.log 2>&1 &

echo "Done!"

tail -f klein.log
