package com.huntdreams.rf.classification

import com.huntdreams.rf.feature.extract.HexFileTokenCounterFeatureExtractor
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

  case class TokenCount(id: String)

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
    var schemaString = ""
    for (i <- 0 to 255) {
      val hex = HexFileTokenCounterFeatureExtractor.numToHex(i)
      schemaString += hex
      if (i != 255) {
        schemaString += " "
      }
    }

    // Generate the schema based on the string of schema
    val fields = schemaString.split(" ").map(fieldName => StructField(fieldName, StringType, nullable = true))
    val schema = StructType(fields)

    // Convert records of the RDD (people) to Rows
    val rowRDD = tokenRDD
      .filter(line => !line.contains("ID"))
      .map(_.split(","))
      .map(attributes => Row.fromSeq(attributes.toSeq))

    // Apply the schema to the RDD
    val tokenDF = spark.createDataFrame(rowRDD, schema)
    tokenDF.show()

    //
  }
}
