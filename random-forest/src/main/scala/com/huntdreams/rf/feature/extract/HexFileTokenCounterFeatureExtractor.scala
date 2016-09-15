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
  *     $ bin/run.sh malware-classification-random-forest-1.0.0-jar-with-dependencies.jar masterUrl dataPath trainDataPath trainLabels
  *   Or, run it locally:
  *     $ bin/run.sh malware-classification-random-forest-1.0.0-jar-with-dependencies.jar dataPath trainDataPath trainLabels
  *   You can also hit the run button in Intellij IDEA to run it locally.
  *
  * Author: Noprom <tyee.noprom@qq.com>
  * Date: 9/14/16 4:44 PM.
  */
object HexFileTokenCounterFeatureExtractor extends Serializable {

  case class MaleWare(id: String, label: Int) extends Serializable

  def main(args: Array[String]): Unit = {
    var masterUrl = "local"
    var dataPath = "/Users/noprom/Documents/Dev/Spark/Pro/malware-classification/data"
    var trainDataPath = dataPath + "/subtrain"
    var trainLabels = dataPath + "/subtrainLabels.csv"

    // 如果传递了参数过来, 则覆盖默认的设置
    if (args.length == 3) {
      dataPath = args(0)
      trainDataPath = args(1)
      trainLabels = args(2)
    } else if (args.length == 4) {
      masterUrl = args(0)
      dataPath = args(1)
      trainDataPath = args(2)
      trainLabels = args(3)
    }

    val spark = SparkSession
      .builder()
      .appName("HexFileTokenCounterFeatureExtractor")
      .config("spark.master", masterUrl)
      .getOrCreate()

    // 以参数的形式传递过来
    val outPutFileName = dataPath + "/hexFileTokenCountFeature.csv"
    val writer = new PrintWriter(outPutFileName)
    val reader = new BufferedReader(new FileReader(trainLabels))

    writer.write("ID,")
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
    while ((line = reader.readLine()) != null) {
      val Array(fileName, label) = line.split(",")
      writer.write(fileName)
      print(fileName)
      if (!fileName.equals("Id")) {
        val filePath = trainDataPath + "/" + fileName
        val hexFile = spark.sparkContext.textFile(filePath + ".bytes")

        // 统计 Hex 文件中的 Token TF
        val wordCounts = hexFile.flatMap(line => line.split(" "))
          .filter(word => headers.contains(word))
          .map(word => (word, 1))
          .reduceByKey((a, b) => a + b).cache().collect().toMap

        for (hex <- headers) {
          // 写进文件
          val count = wordCounts(hex)
          print("," + count)
          writer.write("," + count.toString)
        }
        writer.write("\n")
        print("\n")
      }
    }
    writer.close()
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
