FROM 139487752069.dkr.ecr.ap-southeast-1.amazonaws.com/awslinux-jre10:latest

COPY app.zip app.zip

RUN unzip -o app.zip -d ax9k
RUN chmod 777 ax9k

WORKDIR /ax9k
USER nobody

EXPOSE 4567

