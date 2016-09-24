package com.huntdreams.rf.classification

import com.huntdreams.rf.util.Util
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.RandomForestClassifier
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature._
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.sql
import org.apache.spark.sql.{DataFrame, SparkSession}

/**
  * HexFileTokenCountFeatureRFClassifier
  * <p>Use random forest and hex file token count feature to classify the malware.</p>
  *
  * Usage:
  * To run it on a cluster, you can use:
  *   HexFileTokenCountFeatureRFClassifier <masterUrl> <hexFileTokenCountFeature> <numTrees> <featureTransformer>
  * Or, you can run it locally:
  *   HexFileTokenCountFeatureRFClassifier <hexFileTokenCountFeature> <numTrees> <featureTransformer>
  *
  * Arguments:
  *   `masterUrl` is the master url of the spark cluster.
  *   `hexFileTokenCountFeature` is the result of HexFileTokenCounterFeatureExtractor.
  *   `numTrees` is number of trees in the forest.
  *   `featureTransformer` is the transformer used to transform the features.
  *      Available options: Standard StringIndexer Normalizer StandardScaler MinMaxScaler
  *
  * Example:
  * To run it on a spark cluster:
  *   $ bin/run.sh malware-classification-random-forest-1.0.0-jar-with-dependencies.jar com.huntdreams.rf.classification.HexFileTokenCountFeatureRFClassifier masterUrl hexFileTokenCountFeature 500 StringIndexer
  * Or, run it locally:
  *   $ bin/run.sh malware-classification-random-forest-1.0.0-jar-with-dependencies.jar com.huntdreams.rf.classification.HexFileTokenCountFeatureRFClassifier hexFileTokenCountFeature 500 StringIndexer
  * You can also hit the run button in Intellij IDEA to run it locally.
  *
  * Author: Noprom <tyee.noprom@qq.com>
  * Date: 9/15/16 2:34 PM.
  */
object HexFileTokenCountFeatureRFClassifier extends Serializable {

  var numTrees = 500
  val NO_TRANSFORMER = "NoTransformer"
  val STRING_INDEXER_TRANSFORMER = "StringIndexer"
  val NORMALIZER_TRANSFORMER = "Normalizer"
  val STANDARD_SCALAR_TRANSFORMER = "StandardScaler"
  val MIN_MAX_SCALAR_TRANSFORMER = "MinMaxScaler"
  val availableTransformer = Array(NO_TRANSFORMER, STRING_INDEXER_TRANSFORMER, NORMALIZER_TRANSFORMER, STANDARD_SCALAR_TRANSFORMER, MIN_MAX_SCALAR_TRANSFORMER)

  def main(args: Array[String]): Unit = {
    var masterUrl = "local"
    // Feature transformer, defaults to Normalizer
    var featureTransformer = NORMALIZER_TRANSFORMER
    var tokenCountFeature = "/Users/noprom/Documents/Dev/Spark/Pro/malware-classification/data/hex_file_token_count_feature.csv"

    // Change these values by params
    if (args.length == 4) {
      tokenCountFeature = args(0)
      numTrees = Util.toInt(args(1), 500)
      featureTransformer = args(2)
    } else if (args.length == 5) {
      masterUrl = args(0)
      tokenCountFeature = args(1)
      numTrees = Util.toInt(args(2), 500)
      featureTransformer = args(3)
    } else if (args.length != 0 || !availableTransformer.contains(featureTransformer)) {
      System.err.println(
        s"""
           |Usage:
           |Run it on a cluster:
           |  HexFileTokenCountFeatureRFClassifier <masterUrl> <hexFileTokenCountFeature> <numTrees> <featureTransformer>
           |Run it locally:
           |  HexFileTokenCountFeatureRFClassifier <hexFileTokenCountFeature> <numTrees> <featureTransformer>
           |Arguments:
           |  `masterUrl` is the master url of the spark cluster.
           |  `hexFileTokenCountFeature` is the result of HexFileTokenCounterFeatureExtractor.
           |  `numTrees` is the number of trees in the forest.
           |  `featureTransformer` is the transformer used to transform the features.
           |      Available options: StringIndexer Normalizer StandardScaler
        """.stripMargin)
      System.exit(1)
    }

    // Config spark
    val spark = SparkSession
      .builder()
      .appName("HexFileTokenCountFeatureRFClassifier")
      .config("spark.master", masterUrl)
      .getOrCreate()

    // Get dataFrame
    val data = prepareData(spark, tokenCountFeature)

    // Set up pipeline
    val pipeline = setUpPipeline(data, featureTransformer)

    // Split the data into training and test sets (30% held out for testing).
    val Array(trainingData, testData) = data.randomSplit(Array(0.7, 0.3))

    // Train model. This also runs the indexers.
    val model = pipeline.fit(trainingData)

    // Make predictions.
    val predictions = model.transform(testData)

    // Select example rows to display.
    predictions.show(5)

    // Evaluator Label
    val evaluatorLabel = featureTransformer match {
      case STRING_INDEXER_TRANSFORMER => "indexedLabel"
      case _ => "label"
    }

    // Select (prediction, true label) and compute accuracy.
    val evaluator = new MulticlassClassificationEvaluator()
      .setLabelCol(evaluatorLabel)
      .setPredictionCol("prediction")
      .setMetricName("accuracy")
    val accuracy = evaluator.evaluate(predictions)
    println("Accuracy = " + accuracy)

    // Print Tree
    //val rfModel = model.stages(2).asInstanceOf[RandomForestClassificationModel]
    //println("Learned classification forest model:\n" + rfModel.toDebugString)
    spark.stop()
  }

  /**
    * Convert features file into a DataFrame
    *
    * @param spark spark
    * @param file  features file
    * @return a DataFrame
    */
  def prepareData(spark: SparkSession, file: String): sql.DataFrame = {
    // Prepare all the data
    val rawRDD = spark.sparkContext.textFile(file)
    val vecData = rawRDD.filter(line => !line.contains("ID")).map(line => {
      val arr = line.split(",")
      val label = arr(1)
      val features = arr.takeRight(256)

      (Util.toDoubleDynamic(label), Vectors.dense(features.map(Util.toDoubleDynamic)))
    }).collect().map(r => {
      (r._1, r._2)
    })
    val data = spark.createDataFrame(vecData).toDF("label", "features")
    data.show()
    data
  }

  /**
    * Set up the pipeline to transform the original data.
    *
    * @param data               dataframe
    * @param featureTransformer transformer
    * @return pipeline
    */
  def setUpPipeline(data: DataFrame, featureTransformer: String): Pipeline = {
    val pipeline = featureTransformer match {
      case NO_TRANSFORMER => noTransformerPipeline(data)
      case STRING_INDEXER_TRANSFORMER => stringIndexerPipeline(data)
      case NORMALIZER_TRANSFORMER => normalizerPipeline(data)
      case STANDARD_SCALAR_TRANSFORMER => standardScalerPipeline(data)
      case MIN_MAX_SCALAR_TRANSFORMER => minMaxScalarPipeline(data)
      case _ => throw new IllegalArgumentException("featureTransformer is illegal")
    }
    pipeline
  }

  /**
    * Use nothing to transform the data.
    *
    * @param data data
    * @return pipeline
    */
  def noTransformerPipeline(data: DataFrame): Pipeline = {
    val inputCol = "label"
    val featureCol = "features"

    // Train a RandomForest model.
    val rf = new RandomForestClassifier()
      .setLabelCol(inputCol)
      .setFeaturesCol(featureCol)
      .setNumTrees(numTrees)

    // Chain normalizer and forest in a Pipeline.
    val pipeline = new Pipeline().setStages(Array(rf))
    pipeline
  }

  /**
    * Use StringIndexer to transform the data.
    *
    * @param data data
    * @return pipeline
    */
  def stringIndexerPipeline(data: DataFrame): Pipeline = {
    val inputCol = "label"
    val transformInputCol = "indexedLabel"
    val featureCol = "features"
    val transformFeatureCol = "indexedFeatures"

    // Index labels, adding metadata to the label column.
    // Fit on whole dataset to include all labels in index.
    val labelIndexer = new StringIndexer()
      .setInputCol(inputCol)
      .setOutputCol(transformInputCol)
      .fit(data)

    // Automatically identify categorical features, and index them.
    // Set maxCategories so features with > 4 distinct values are treated as continuous.
    val featureIndexer = new VectorIndexer()
      .setInputCol(featureCol)
      .setOutputCol(transformFeatureCol)
      .setMaxCategories(9)
      .fit(data)

    // Train a RandomForest model.
    val rf = new RandomForestClassifier()
      .setLabelCol(transformInputCol)
      .setFeaturesCol(transformFeatureCol)
      .setNumTrees(numTrees)

    // Convert indexed labels back to original labels.
    val labelConverter = new IndexToString()
      .setInputCol("prediction")
      .setOutputCol("predictedLabel")
      .setLabels(labelIndexer.labels)

    // Chain indexers and forest in a Pipeline.
    val pipeline = new Pipeline().setStages(Array(labelIndexer, featureIndexer, rf, labelConverter))
    pipeline
  }

  /**
    * Use normalizer to transform the data.
    *
    * @param data data
    * @return pipeline
    */
  def normalizerPipeline(data: DataFrame): Pipeline = {
    val inputCol = "label"
    val featureCol = "features"
    val transformFeatureCol = "normFeatures"

    // Normalize each Vector using $L^1$ norm.
    val normalizer = new Normalizer()
      .setInputCol(featureCol)
      .setOutputCol(transformFeatureCol)
      .setP(1.0)

    val l1NormData = normalizer.transform(data)
    println(l1NormData.show())

    // Train a RandomForest model.
    val rf = new RandomForestClassifier()
      .setLabelCol(inputCol)
      .setFeaturesCol(transformFeatureCol)
      .setNumTrees(numTrees)

    // Chain normalizer and forest in a Pipeline.
    val pipeline = new Pipeline().setStages(Array(normalizer, rf))
    pipeline
  }

  /**
    * Use standard scaler to transform the data.
    *
    * @param data dataframe
    * @return pipeline
    */
  def standardScalerPipeline(data: DataFrame): Pipeline = {
    val inputCol = "label"
    val featureCol = "features"
    val transformFeatureCol = "scaledFeatures"

    val scaler = new StandardScaler()
      .setInputCol(featureCol)
      .setOutputCol(transformFeatureCol)
      .setWithStd(true)
      .setWithMean(false)
    // Compute summary statistics by fitting the StandardScaler.
    val scalerModel = scaler.fit(data)

    // Normalize each feature to have unit standard deviation.
    val scaledData = scalerModel.transform(data)
    scaledData.show()

    // Train a RandomForest model.
    val rf = new RandomForestClassifier()
      .setLabelCol(inputCol)
      .setFeaturesCol(transformFeatureCol)
      .setNumTrees(numTrees)

    // Chain normalizer and forest in a Pipeline.
    val pipeline = new Pipeline().setStages(Array(scaler, rf))
    pipeline
  }

  /**
    * Use MinMaxScalaer to transform the data.
    *
    * @param data dataframe
    * @return pipeline
    */
  def minMaxScalarPipeline(data: DataFrame): Pipeline = {
    val inputCol = "label"
    val featureCol = "features"
    val transformFeatureCol = "scaledFeatures"

    val scaler = new MinMaxScaler()
      .setInputCol(featureCol)
      .setOutputCol(transformFeatureCol)

    // Compute summary statistics and generate MinMaxScalerModel
    val scalerModel = scaler.fit(data)

    // rescale each feature to range [min, max].
    val scaledData = scalerModel.transform(data)
    scaledData.show()

    // Train a RandomForest model.
    val rf = new RandomForestClassifier()
      .setLabelCol(inputCol)
      .setFeaturesCol(transformFeatureCol)
      .setNumTrees(numTrees)

    // Chain normalizer and forest in a Pipeline.
    val pipeline = new Pipeline().setStages(Array(scaler, rf))
    pipeline
  }
}