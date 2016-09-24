# 微软恶意代码分类 Kaggle 竞赛, Spark 分布式实现

## Competition
- [malware-classification](https://www.kaggle.com/c/malware-classification)

## Build
构建这个项目, 执行以下命令即可:

    bin/build.sh
    
## Run
运行这个项目, 执行以下命令即可:

    bin/run.sh jar-file-in-the-jars-directory class-name [params]
    
## Evaluation

* HexFileTokenCountFeatureRFClassifier
NoTransformer:
Accuracy = 0.8638132295719845

StringIndexer:
Accuracy = 0.8295964125560538

Normalizer:
    Accuracy = 0.933953488372093 L1 Norm
    Accuracy = 0.8333333333333334 L2 Norm
    
StandardScaler:
    Accuracy = 0.8702290076335878
    
MinMaxScaler:
    Accuracy = 0.8285714285714286