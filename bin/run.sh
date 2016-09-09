#!/usr/bin/env bash

# 判断参数个数
if [ $# != 2 ]; then
    echo "Usage: $0 the-file-you-want-to-submit.jar class-name";
    exit 0;
fi

# 判断提交的文件是否存在
filename=$1
class_name=$2
cur_dir=$(cd "$(dirname "$0")"; pwd)
jar_dir=$cur_dir/../jars
filename="$jar_dir/$filename"
if [ ! -f "$filename" ]; then
    echo "$filename does not exist.";
    exit 0;
fi


# 提取主机名
hostname=`hostname`
echo "Running spark on : $hostname";

# 提交Spark Job
submit_url=spark://$hostname:7077
$SPARK_HOME/bin/spark-submit \
  --class $class_name \
  --master $submit_url \
  --executor-memory 6G \
  --total-executor-cores 4 \
  $filename