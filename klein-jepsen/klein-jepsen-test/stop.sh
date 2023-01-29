#!/bin/bash
pid=`ps -ef |grep atomic-server |grep java |awk -F' ' '{print $2}'`
if [ "$pid" != "" ]
then
    echo "kill $pid"
    kill $pid
fi
echo "Done!"
