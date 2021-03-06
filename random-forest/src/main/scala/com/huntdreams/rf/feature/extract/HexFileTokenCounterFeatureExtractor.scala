package com.huntdreams.rf.feature.extract

import java.io.{BufferedReader, FileReader, PrintWriter}

import org.apache.spark.sql.SparkSession

/**
  * HexFileTokenCounterFeatureExtractor
  * <p>Extract token count feature from hex dump-based file.</p>
  *
  * Usage:
  * To run it on a cluster, you can use:
  *   HexFileTokenCounterFeatureExtractor <masterUrl> <dataPath> <trainDataPath> <trainLabels>
  * Or, you can run it locally:
  *   HexFileTokenCounterFeatureExtractor <dataPath> <trainDataPath> <trainLabels>
  *
  * Arguments:
  *   `masterUrl` is the master url of the spark cluster.
  *   `dataPath` is the path of your data, not the train data path, and feature results will be generated here.
  *   `trainDataPath` is the path of your train data.
  *   `trainLabels` is the malware samples that you want to extract features from and train, it must be in the `dataPath`.
  *
  * Example:
  *   To run it on a spark cluster:
  *     $ bin/run.sh malware-classification-random-forest-1.0.0-jar-with-dependencies.jar com.huntdreams.rf.feature.extract.HexFileTokenCounterFeatureExtractor masterUrl dataPath trainDataPath trainLabels
  *   Or, run it locally:
  *     $ bin/run.sh malware-classification-random-forest-1.0.0-jar-with-dependencies.jar com.huntdreams.rf.feature.extract.HexFileTokenCounterFeatureExtractor dataPath trainDataPath trainLabels
  *   You can also hit the run button in Intellij IDEA to run it locally.
  *
  * Author: Noprom <tyee.noprom@qq.com>
  * Date: 9/14/16 4:44 PM.
  */
object HexFileTokenCounterFeatureExtractor extends Serializable {

  def main(args: Array[String]): Unit = {
    var masterUrl = "local"
    var dataPath = "/Users/noprom/Documents/Dev/Spark/Pro/malware-classification/data"
    var trainDataPath = dataPath + "/subtrain"
    var trainLabels = dataPath + "/subtrainLabels.csv"

    println("args.length = " + args.length)
    args.foreach(println)
    // 如果传递了参数过来, 则覆盖默认的设置
    if (args.length == 4) {
      dataPath = args(0)
      trainDataPath = args(1)
      trainLabels = args(2)
    } else if (args.length == 5) {
      masterUrl = args(0)
      dataPath = args(1)
      trainDataPath = args(2)
      trainLabels = args(3)
    } else if (args.length != 0) {
      System.err.println(s"""
        |Usage:
        |Run it on a cluster:
        |  HexFileTokenCounterFeatureExtractor <masterUrl> <dataPath> <trainDataPath> <trainLabels>
        |Run it locally:
        |  HexFileTokenCounterFeatureExtractor <dataPath> <trainDataPath> <trainLabels>
        |Arguments:
        |  `masterUrl` is the master url of the spark cluster.
        |  `dataPath` is the path of your data, not the train data path, and feature results will be generated here.
        |  `trainDataPath` is the path of your train data.
        |  `trainLabels` is the malware samples that you want to extract features from and train, it must be in the `dataPath`.
        """.stripMargin)
      System.exit(1)
    }

    val spark = SparkSession
      .builder()
      .appName("HexFileTokenCounterFeatureExtractor")
      .config("spark.master", masterUrl)
      .config("spark.dynamicAllocation.enabled", "false")
      .getOrCreate()

    // 以参数的形式传递过来
    var outPutFileName = dataPath + "/hex_file_token_count_feature"
    if (trainDataPath.contains("subtrain")) {
      outPutFileName += ".subtrain"
    }
    val writer = new PrintWriter(outPutFileName + ".csv")
    val svmWriter = new PrintWriter(outPutFileName + ".svm.txt")
    val reader = new BufferedReader(new FileReader(trainLabels))

    writer.write("ID,Label,")
    // 生成Hex Header
    var headers = List.empty[String]
    for (i <- 0 to 255) {
      val hex = numToHex(i)
      headers = headers :+ hex
      writer.write(hex)
      if (i != 255) {
        writer.write(",")
      }
    }
    writer.write("\n")
    // 遍历所有文件
    var line: String = null
    while ((line = reader.readLine()) != Nil) {
      if (line != null && !line.isEmpty) {
        val Array(fileName, label) = line.split(",")
        if (!fileName.equals("Id")) {
          writer.write(fileName + "," + label)
          print(fileName + "," + label)
          // Write SVM data
          svmWriter.write(label)
          val filePath = trainDataPath + "/" + fileName
          val hexFile = spark.sparkContext.textFile(filePath + ".bytes")

          // 统计 Hex 文件中的 Token TF
          val wordCounts = hexFile.flatMap(line => line.split(" "))
            .filter(word => headers.contains(word))
            .map(word => (word, 1))
            .reduceByKey((a, b) => a + b).cache().collect().toMap

          var cnt = 0
          for (hex <- headers) {
            // 写文件
            val count = wordCounts.getOrElse(hex, 0)
            cnt = cnt + 1
            print("," + count)
            writer.write("," + count.toString)
            svmWriter.write(" " + cnt.toString + ":" + count.toString)
          }
          writer.write("\n")
          svmWriter.write("\n")
          print("\n")
        }
      }
    }
    writer.close()
    svmWriter.close()
  }

  /**
    * 将十进制数字转为16进制字符串形式
    *
    * @param num 十进制数字
    * @return 16进制字符串
    */
  def numToHex(num: Int): String = {
    var hex = num.toHexString.toUpperCase()
    if (hex.length == 1) {
      hex = "0" + hex
    }
    hex
  }
}