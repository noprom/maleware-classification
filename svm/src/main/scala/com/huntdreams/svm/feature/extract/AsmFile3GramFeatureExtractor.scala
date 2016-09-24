package com.huntdreams.svm.feature.extract

import java.io.{FileReader, BufferedReader, PrintWriter}

import org.apache.spark.sql.SparkSession

import scala.collection.mutable

/**
  * AsmFile3GramFeatureExtractor
  *
  * Author: Noprom <tyee.noprom@qq.com>
  * Date: 9/24/16 12:30 PM.
  */
object AsmFile3GramFeatureExtractor extends Serializable {

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
                            |  AsmFile3GramFeatureExtractor <masterUrl> <dataPath> <trainDataPath> <trainLabels>
                            |Run it locally:
                            |  AsmFile3GramFeatureExtractor <dataPath> <trainDataPath> <trainLabels>
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
      .appName("AsmFile3GramFeatureExtractor")
      .config("spark.master", masterUrl)
      .getOrCreate()

    // 以参数的形式传递过来
    var outPutFileName = dataPath + "/asm_file_3gram_feature"
    if (trainDataPath.contains("subtrain")) {
      outPutFileName += ".subtrain"
    }
    val writer = new PrintWriter(outPutFileName + ".csv")
    val svmWriter = new PrintWriter(outPutFileName + ".svm.txt")
    val reader = new BufferedReader(new FileReader(trainLabels))

    // 遍历所有文件
//    var line: String = null
//    while ((line = reader.readLine()) != Nil) {
//      if (line != null && !line.isEmpty) {
//        val Array(fileName, label) = line.split(",")
        // TODO: FOR DEBUG
        val fileName = "0B2RwKm6dq9fjUWDNIOa"
        if (!fileName.equals("Id")) {
//          writer.write(fileName + "," + label)
//          print(fileName + "," + label)
//          // Write SVM data
//          svmWriter.write(label)
          val filePath = trainDataPath + "/" + fileName
          val asmFile = spark.sparkContext.textFile(filePath + ".asm")
          val opcode = asmFile.filter(line => line.contains(".text")).map(line => )
          writer.write("\n")
          svmWriter.write("\n")
          print("\n")
        }
//      }
//    }
    writer.close()
  }

  def getOpSeq(line: String): mutable.ArraySeq[String] = {
    val regex ="""\s([a-fA-F0-9]{2}\s)+\s*([a-z]+)""".r
    var opSeq = new mutable.ArraySeq[String](0)
    for (s <- regex.findAllMatchIn(line)) {
      val ops = s.group(2)
      if (!ops.equals("align")) {
        opSeq = opSeq :+ ops
      }
    }
    opSeq
  }
}