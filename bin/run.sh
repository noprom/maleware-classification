#!/usr/bin/env bash

# 判断参数个数
if [ $# != 2 ]; then
    echo "Usage: $0 the-file-you-want-to-submit.jar class-name";
    exit 0;
fi

# 判断提交的文件是否存在
FILENAME=$1
CLASS_NAME=$2
if [ ! -f "$FILENAME" ]; then
    echo "$FILENAME does not exist.";
    exit 0;
fi
cur_dir=$(cd "$(dirname "$0")"; pwd)
FILENAME="$cur_dir/$FILENAME"
# 提取主机名
HOSTNAME=`hostname`
echo "Running spark on : $HOSTNAME";

# 提交Spark Job
SUBMIT_URL=spark://$HOSTNAME:7077
$SPARK_HOME/bin/spark-submit \
  --class $CLASS_NAME \
  --master $SUBMIT_URL \
  --executor-memory 6G \
  --total-executor-cores 4 \
  $FILENAME