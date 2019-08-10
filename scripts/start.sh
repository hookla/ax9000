#!/bin/bash
curl -X POST -H 'Content-type: application/json' --data '{"text":"Live deployment starting ..."}' https://hooks.slack.com/services/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


#/home/ec2-user/ax9k/app/bin/app --config-file=s3://ax9000-config/exchanges/hsi.config
#/home/ec2-user/ax9k/app/bin/app --config-file=s3://ax9000-config/exchanges/usdgbp.config
/home/ec2-user/ax9k/app/bin/app --config-file=s3://ax9000-config/exchanges/cex_eth_usd.config