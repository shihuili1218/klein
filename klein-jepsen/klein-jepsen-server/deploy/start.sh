#!/bin/bash
set -x

java -Xmx512m -Xms512m  \
  -Dklein.id=${KLEIN_ID}  \
  -Dklein.port=${KLEIN_PORT}  \
  -Dklein.ip=${KLEIN_IP}  \
  -Dklein.members=${KLEIN_MEMBERS} \
  -jar /klein/klein-server.jar
