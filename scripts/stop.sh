#!/bin/bash
curl localhost:4567/shutdown
pkill -KILL java
rm -f /opt/codedeploy-agent/temp/*
curl -X POST -H 'Content-type: application/json' --data '{"text":"Live deployment killed."}' https://hooks.slack.com/services/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


