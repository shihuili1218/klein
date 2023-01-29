#!/bin/bash
nohup java -Xmx512m -Xms512m -jar klein-server.jar $@ >> klein.log 2>&1 &
echo "Done!"
