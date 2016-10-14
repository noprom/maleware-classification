# 微软恶意代码分类 Kaggle 竞赛, Spark 分布式实现

## Competition
- [malware-classification](https://www.kaggle.com/c/malware-classification)

## Requirements
- Python 2.7.10
- Scala 2.11.8
- JDK 1.7.0_79
- Spark 2.0.0
- Maven 3.3.9

## Build
构建这个项目, 执行以下命令即可:

    bin/build.sh
### bin/build.sh详细参数
```
Usage:
-h                    print help message.
module_name           compile module named module_name, modules available: random-forest
```

## Run
运行这个项目, 执行以下命令即可:

    bin/run.sh jar-file-in-the-jars-directory class-name [params]
### bin/run.sh详细参数
```
Usage: ./bin/run.sh the-file-you-want-to-submit.jar classname [param1 param2 param3 ...]
Available commands:

========== Feature extractor ==========

HexFileTokenCounterFeatureExtractor:
  $ bin/run.sh malware-classification-random-forest-1.0.0-jar-with-dependencies.jar com.huntdreams.rf.feature.extract.HexFileTokenCounterFeatureExtractor masterUrl dataPath trainDataPath trainLabels
  $ bin/run.sh malware-classification-random-forest-1.0.0-jar-with-dependencies.jar com.huntdreams.rf.feature.extract.HexFileTokenCounterFeatureExtractor dataPath trainDataPath trainLabels

==========  Classification   ==========

HexFileTokenCountFeatureRFClassifier:
  $ bin/run.sh malware-classification-random-forest-1.0.0-jar-with-dependencies.jar com.huntdreams.rf.classification.HexFileTokenCountFeatureRFClassifier masterUrl hexFileTokenCountFeature 500 StringIndexer
  $ bin/run.sh malware-classification-random-forest-1.0.0-jar-with-dependencies.jar com.huntdreams.rf.classification.HexFileTokenCountFeatureRFClassifier hexFileTokenCountFeature 500 StringIndexer
```

## Evaluation
### `HexFileTokenCountFeatureRFClassifier`
使用不同的特征转化之后的结果:
`NoTransformer`
    Accuracy = 0.8638132295719845

`StringIndexer`
    Accuracy = 0.8295964125560538

`Normalizer`
    Accuracy = 0.933953488372093 L1 Norm
    Accuracy = 0.8333333333333334 L2 Norm
    
`StandardScaler`
    Accuracy = 0.8702290076335878
    
`MinMaxScaler`
    Accuracy = 0.8285714285714286

## 代码目录说明
```
.
|____.gitignore			git仓库文件忽略
|____bin				可执行脚本目录
| |____build.sh 		构建项目脚本
| |____run.sh 			运行特征提取和分类算法脚本
| |____run.txt 			运行命令纪录
|____jars 				可执行jar包
| |____malware-classification-random-forest-1.0.0-jar-with-dependencies.jar
| |____malware-classification-random-forest-1.0.0.jar
|____pom.xml
|____random-forest 		随机森林算法模块
| |____pom.xml
| |____src 				源代码目录
| | |____main
| | | |____resources
| | | |____scala
| | | | |____com
| | | | | |____huntdreams
| | | | | | |____rf
| | | | | | | |____classification
| | | | | | | | |____HexFileTokenCountFeatureRFClassifier.scala 	二进制文件 HexCount 特征提取
| | | | | | | |____feature
| | | | | | | | |____extract
| | | | | | | | | |____HexFileTokenCounterFeatureExtractor.scala 	随机森林对 HexCount 特征进行分类
| | | | | | | |____util
| | | | | | | | |____Util.scala 									工具类
|____README.md 														说明文件
|____src
| |____main
| | |____java
| | |____resources
| |____test
| | |____java
```
