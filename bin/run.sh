#!/usr/bin/env bash

# 判断参数个数
if [ $# -lt 2 ]; then
    echo "Usage: $0 the-file-you-want-to-submit.jar classname [param1 param2 param3 ...]";
    echo -e "Available commands:\n"
    echo -e "========== Feature extractor ==========\n"
    echo -e "HexFileTokenCounterFeatureExtractor:"
    echo "  $ bin/run.sh malware-classification-random-forest-1.0.0-jar-with-dependencies.jar com.huntdreams.rf.feature.extract.HexFileTokenCounterFeatureExtractor <masterUrl> <dataPath> <trainDataPath> <trainLabels>"
    echo "  $ bin/run.sh malware-classification-random-forest-1.0.0-jar-with-dependencies.jar com.huntdreams.rf.feature.extract.HexFileTokenCounterFeatureExtractor <dataPath> <trainDataPath> <trainLabels>"
    echo -e "\n==========  Classification   ==========\n"
    echo -e "HexFileTokenCountFeatureRFClassifier:"
    echo "  $ bin/run.sh malware-classification-random-forest-1.0.0-jar-with-dependencies.jar com.huntdreams.rf.classification.HexFileTokenCountFeatureRFClassifier <masterUrl> <hexFileTokenCountFeature> <featureTransformer> <numTrees> <trainSize> <testSize>"
    echo "  $ bin/run.sh malware-classification-random-forest-1.0.0-jar-with-dependencies.jar com.huntdreams.rf.classification.HexFileTokenCountFeatureRFClassifier <hexFileTokenCountFeature> <featureTransformer> <numTrees> <trainSize> <testSize>"
    exit 1;
fi

# 判断提交的文件是否存在
filename=$1
classname=$2
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
sudo $SPARK_HOME/bin/spark-submit \
  --class $classname \
  --master $submit_url \
  --executor-memory 7G \
  --total-executor-cores 4 \
  $filename \
  $3 $4 $5 $6 $7 $8 $9 $10