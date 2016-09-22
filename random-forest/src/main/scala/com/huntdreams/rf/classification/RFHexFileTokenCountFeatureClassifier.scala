package com.huntdreams.rf.classification

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.RandomForestClassifier
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.{IndexToString, StringIndexer, VectorIndexer}
import org.apache.spark.ml.linalg.Vectors
import org.apache.spark.sql.SparkSession

/**
  * RFHexFileTokenCountFeatureClassifier
  * <p>Use random forest and hex file token count feature to classify the malware.</p>
  *
  * Usage:
  * To run it on a cluster, you can use:
  *   RFHexFileTokenCountFeatureClassifier <masterUrl> <hexFileTokenCountFeature> <numTrees>
  * Or, you can run it locally:
  *   RFHexFileTokenCountFeatureClassifier <hexFileTokenCountFeature> <numTrees>
  *
  * Arguments:
  *   `masterUrl` is the master url of the spark cluster.
  *   `hexFileTokenCountFeature` is the result of HexFileTokenCounterFeatureExtractor.
  *
  * Example:
  *   To run it on a spark cluster:
  *     $ bin/run.sh malware-classification-random-forest-1.0.0-jar-with-dependencies.jar com.huntdreams.rf.classification.RFHexFileTokenCountFeatureClassifier masterUrl hexFileTokenCountFeature numTrees
  *   Or, run it locally:
  *     $ bin/run.sh malware-classification-random-forest-1.0.0-jar-with-dependencies.jar com.huntdreams.rf.classification.RFHexFileTokenCountFeatureClassifier hexFileTokenCountFeature numTrees
  *   You can also hit the run button in Intellij IDEA to run it locally.
  *
  * Author: Noprom <tyee.noprom@qq.com>
  * Date: 9/15/16 2:34 PM.
  */
object RFHexFileTokenCountFeatureClassifier extends Serializable {

  def main(args: Array[String]): Unit = {
    // Load hex file token count features
    var masterUrl = "local"
    var tokenCountFeature = "/Users/noprom/Documents/Dev/Spark/Pro/malware-classification/data/hexFileTokenCountFeature.csv"
    var numTrees = 500

    // Change these values by params
    if (args.length == 3) {
      tokenCountFeature = args(0)
      numTrees = toInt(args(1), 500)
    } else if (args.length == 4) {
      masterUrl = args(0)
      tokenCountFeature = args(1)
      numTrees = toInt(args(2), 500)
    } else if (args.length != 0) {
      System.err.println(s"""
                            |Usage:
                            |Run it on a cluster:
                            |  RFHexFileTokenCountFeatureClassifier <masterUrl> <hexFileTokenCountFeature> <numTrees>
                            |Run it locally:
                            |  RFHexFileTokenCountFeatureClassifier <hexFileTokenCountFeature> <numTrees>
                            |Arguments:
                            |  `masterUrl` is the master url of the spark cluster.
                            |  `hexFileTokenCountFeature` is the result of HexFileTokenCounterFeatureExtractor.
                            |  `numTrees` is the number of trees in the forest.
        """.stripMargin)
      System.exit(1)
    }

    // Config spark
    val spark = SparkSession
      .builder()
      .appName("RFHexFileTokenCountFeatureClassifier")
      .config("spark.master", masterUrl)
      .getOrCreate()

    // Prepare all the data
    val rawRDD = spark.sparkContext.textFile(tokenCountFeature)
    val vecData = rawRDD.filter(line => !line.contains("ID")).map(line => {
      val arr = line.split(",")
      val label = arr(1)
      val features = arr.takeRight(256)

      (label, Vectors.dense(features.map(toDoubleDynamic)))
    }).collect().map(r => {
      (r._1, r._2)
    })
    val data = spark.createDataFrame(vecData).toDF("label", "features")
    data.show()

    // Index labels, adding metadata to the label column.
    // Fit on whole dataset to include all labels in index.
    val labelIndexer = new StringIndexer()
      .setInputCol("label")
      .setOutputCol("indexedLabel")
      .fit(data)
    // Automatically identify categorical features, and index them.
    // Set maxCategories so features with > 4 distinct values are treated as continuous.
    val featureIndexer = new VectorIndexer()
      .setInputCol("features")
      .setOutputCol("indexedFeatures")
      .setMaxCategories(9)
      .fit(data)

    // Split the data into training and test sets (30% held out for testing).
    val Array(trainingData, testData) = data.randomSplit(Array(0.7, 0.3))

    // Train a RandomForest model.
    val rf = new RandomForestClassifier()
      .setLabelCol("indexedLabel")
      .setFeaturesCol("indexedFeatures")
      .setNumTrees(numTrees)

    // Convert indexed labels back to original labels.
    val labelConverter = new IndexToString()
      .setInputCol("prediction")
      .setOutputCol("predictedLabel")
      .setLabels(labelIndexer.labels)

    // Chain indexers and forest in a Pipeline.
    val pipeline = new Pipeline().setStages(Array(labelIndexer, featureIndexer, rf, labelConverter))

    // Train model. This also runs the indexers.
    val model = pipeline.fit(trainingData)

    // Make predictions.
    val predictions = model.transform(testData)

    // Select example rows to display.
    predictions.select("predictedLabel", "label", "features").show(5)

    // Select (prediction, true label) and compute test error.
    val evaluator = new MulticlassClassificationEvaluator()
      .setLabelCol("indexedLabel")
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
    * Convert String or java.lang.Number to Double.
    *
    * @param x String or java.lang.Number
    * @return Double
    */
  def toDoubleDynamic(x: Any) = x match {
    case s: String => s.toDouble
    case jn: java.lang.Number => jn.doubleValue()
    case _ => throw new ClassCastException("cannot cast to double")
  }

  /**
    * Convert any seq to spark Vector.
    *
    * @param x any
    * @tparam T Vector
    * @return
    */
  def anySeqToSparkVector[T](x: Any) = x match {
    case a: Array[T] => Vectors.dense(a.map(toDoubleDynamic))
    case s: Seq[Any] => Vectors.dense(s.toArray.map(toDoubleDynamic))
    case v: Vector[Any] => v
    case _ => throw new ClassCastException("unsupported class")
  }

  /**
    * Convert a String to Int, if exception is thrown,
    * a default value will be returned.
    *
    * @param s String to convert
    * @param default The default value to be returned.
    * @return
    */
  def toInt(s: String, default: Int): Int = {
    try {
      s.toInt
    } catch {
      case e: Exception => default
    }
  }
}
