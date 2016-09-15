package com.huntdreams.rf.classification

import com.huntdreams.rf.feature.extract.HexFileTokenCounterFeatureExtractor
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.{RandomForestClassificationModel, RandomForestClassifier}
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator
import org.apache.spark.ml.feature.{IndexToString, VectorIndexer, StringIndexer}
import org.apache.spark.ml.linalg.{DenseVector, Vectors}
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.types.{StructType, StringType, StructField}

/**
  * RFHexFileTokenCountFeatureClassifier
  * <p>Use random forest and hex file token count feature to classify the malware.</p>
  *
  * Usage:
  *
  * Author: Noprom <tyee.noprom@qq.com>
  * Date: 9/15/16 2:34 PM.
  */
object RFHexFileTokenCountFeatureClassifier extends Serializable {

  def main(args: Array[String]): Unit = {
    // Load hex file token count features
    var masterUrl = "local"
    var tokenCountFeature = "/Users/noprom/Documents/Dev/Spark/Pro/malware-classification/data/hexFileTokenCountFeature.csv1"

    // TODO: change these values by params
    // Config spark
    val spark = SparkSession
      .builder()
      .appName("RFHexFileTokenCountFeatureClassifier")
      .config("spark.master", masterUrl)
      .getOrCreate()

    val tokenRDD = spark.sparkContext.textFile(tokenCountFeature)

    // Prepare the features
    val schemaString = "id label features"
    // 256个特征, 统一用一个 feature 字段表示
    //    for (i <- 0 to 255) {
    //      val hex = HexFileTokenCounterFeatureExtractor.numToHex(i)
    //      schemaString += hex
    //      if (i != 255) {
    //        schemaString += " "
    //      }
    //    }

    // Generate the schema based on the string of schema
    val fields = schemaString.split(" ").map(fieldName => fieldName match {
      case "features" => StructField(fieldName, StringType, nullable = true)
      case _ => StructField(fieldName, Vectors, nullable = true)
    })
    val schema = StructType(fields)

    // Convert records of the RDD to Rows
    val rowRDD = tokenRDD
      .filter(line => !line.contains("ID"))
      .map(_.split(","))
      .map(attributes => {
        val features = attributes.takeRight(256)
        val seq = Seq(attributes(0), attributes(1), anySeqToSparkVector(features))
        Row.fromSeq(seq)
      })

    // Apply the schema to the RDD
    val data = spark.createDataFrame(rowRDD, schema)
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
      .setMaxCategories(4)
      .fit(data)

    // Split the data into training and test sets (30% held out for testing).
    val Array(trainingData, testData) = data.randomSplit(Array(0.7, 0.3))

    // Train a RandomForest model.
    val rf = new RandomForestClassifier()
      .setLabelCol("indexedLabel")
      .setFeaturesCol("indexedFeatures")
      .setNumTrees(10)

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
    println("Test Error = " + (1.0 - accuracy))

    val rfModel = model.stages(2).asInstanceOf[RandomForestClassificationModel]
    println("Learned classification forest model:\n" + rfModel.toDebugString)
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
}
