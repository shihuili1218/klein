#!/bin/bash
pid=`ps -ef |grep klein-server |grep java |awk -F' ' '{print $2}'`
if [ "$pid" != "" ]
then
    echo "kill $pid"
    kill $pid
fi
echo "Done!"

rm -rf /data

echo "Cleanup!"