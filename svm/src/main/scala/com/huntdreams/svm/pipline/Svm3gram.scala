//import org.apache.spark.sql.{DataFrame, Dataset, SQLContext, SparkSession}
//
//import scala.collection.mutable
//
////import scala.collection.mutable.WrappedArray[String]
//import scala.sys.process._
//import org.apache.spark.ml.feature.NGram
//
//import scala.io.Source
//import org.apache.spark.{SparkConf, SparkContext}
//
///**
//  *
//  * Author: helin <1006604973@qq.com>.
//  * Date: 9/14/16 20:41
//  */
//
//object Svm3gram {
//  def main(args: Array[String]): Unit = {
//
//    val spark = SparkSession.builder.appName("Svm3gram").getOrCreate()
//
//    //获取当强工作路径
//    val getCurrentDirectory = new java.io.File(".").getCanonicalPath
//    //获取local_data/subtrain
//    val path = getCurrentDirectory + "/local_data/subtrain01/"
//    val trainList = Seq("ls", path).!!.split("\n").filter(_.endsWith(".asm"))
//
//
//    //获取所有trian文件中出现的ops及其次数
//    def map3Gram(path: String) = {
//      //获取train文件列表
//      val trainFileList = Seq("ls", path).!!.split("\n").filter(_.endsWith(".asm"))
//      var map3gram = collection.mutable.Map[String, DataFrame]()
//      for (filename <- trainFileList) {
//        map3gram += (filename -> get3Gram(getopSeq(path + filename)))
//      }
//      map3gram
//    }
//    var counts = collection.mutable.Map[String, Int]("ops" -> 0)
//    counts += (ops -> ((if (counts.contains(ops)) counts(ops) else 0) + 1))
//
//    //getopSeq
//    def getopSeq(filePath: String): scala.collection.mutable.ArraySeq[String] = {
//      val Decimal ="""\s([a-fA-F0-9]{2}\s)+\s*([a-z]+)""".r
//      var opSeq = new mutable.ArraySeq[String](0)
//      val trainFile = spark.read.text(filePath).filter(_.toString().startsWith("[.text")).collect().map(
//        l => {
//          for (s <- Decimal.findAllMatchIn(l.toString())) {
//            var ops = s.group(2)
//            if (!ops.equals("align")) opSeq = opSeq :+ ops
//          }
//        }
//      )
//      opSeq
//    }
//    //get3Gram
//    def get3Gram(opSeq: scala.collection.mutable.ArraySeq[String]): DataFrame = {
//      val df = sc.parallelize(Seq(opSeq.toArray)).toDF
//      val nGram = new org.apache.spark.ml.feature.NGram()
//        .setInputCol("value")
//        .setOutputCol("3gram")
//        .setN(3)
//      nGram.transform(df)
//    }
//
//    //getFeature
//    def getFeature(map3gram: collection.mutable.Map[String, DataFrame]) = {
//      var mapFeatureCounts = collection.mutable.Map[String, Int]("ops" -> 0)
//      map3gram.map {
//        case (s, v) => {
//          val gram3 = v.select("3gram").collect()(0).get(0).asInstanceOf[WrappedArray[String]]
//          if (gram3.length != 0) {
//            val ops3 = gram3.apply(0).toString
//            mapFeatureCounts += (ops3 -> ((if (mapFeatureCounts.contains(ops3)) mapFeatureCounts(ops3) else 0) + 1))
//          }
//        }
//      }
//      mapFeatureCounts
//    }
//
//    val s = map3Gram(path)
//    val w = getFeature(map3Gram(path)).filter(_._2 > 500)
//
//
//
//    val t = s.get(filename).get.select("3gram").collect()(0).get(0).asInstanceOf[WrappedArray[String]]
//    t.apply(0).toString()
//
//    s.get(filename).get.select("3gram").getClass
//    def getKeySet(counts: scala.collection.mutable.Map[String, Int]) = counts.filter(_._2 > 500).keySet.toArray
//    def getfileFeature(array: Array[String], path: String): DataFrame = {
//      var features = new mutable.ArraySeq[String](0)
//
//      val trainNameList = Seq("ls", path).!!.split("\n").filter(_.endsWith(".asm"))
//      val Decimal =
//        """\s([a-fA-F0-9]{2}\s)+\s*([a-z]+)""".r
//      fileName_features.put()
//    }
//
//
//
//
//    val t = new Dataset[String]()
//
//
//    val df = rdd.map { case s0 => X(s0) }.toDF()
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//    var cache = collection.mutable.Map[String, Int]()
//
//    cache = cache + (ops -> 1)
//    cache.update(ops, 2)
//
//
//
//    val peopleRDD = spark.sparkContext.textFile("examples/src/main/resources/people.txt")
//    peopleRDD.filter()
//
//
//    println(s.substring(s.length() - 5, s.length()))
//    val input = """.text:0040101C 68 F0 BA	7C BC						       push    0BC7CBAF0h"""
//    for (s <- Decimal findAllMatchIn input) {
//      val ops = s.group(2)
//      cache = cache + (ops -> 1)
//    }
//
//
//    val Decimal ="""\s([a-fA-F0-9]{2}\s)+\s*([a-z]+)""".r
//    for (s <- Decimal1 findAllMatchIn input) yield s.group(2)
//    val m = (Decimal findAllIn input).toList
//
//    input match {
//      case Decimal(arg1, arg2) => s"$arg1,$arg2"
//    }
//  }
//
//}
//
//
