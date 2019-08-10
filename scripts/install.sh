#!/bin/bash

chmod 777 /home/ec2-user/ax9k
unzip -o /home/ec2-user/ax9k/app.zip -d /home/ec2-user/ax9k/

/home/ec2-user/ax9k/stop.sh
nohup /home/ec2-user/ax9k/start.sh > /dev/null 2>&1 </dev/null &

echo all done!

