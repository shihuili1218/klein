#!/bin/bash
nohup java -Xmx512m -Xms512m  \
  -jar /klein/klein-server.jar \
  -Dklein.consensus.paxos.write=false  \
  $@ >> klein.log 2>&1 &
echo "Done!"
