import scala.util.matching.Regex
/**
  *
  * Author: helin <1006604973@qq.com>.
  * Date: 9/14/16 20:41
  */

object Svm3gram{
  def main(args: Array[String]): Unit = {
    val final
    val spark = SparkSession
      .builder
      .appName("Svm3gram")
      .getOrCreate()

    val trainFile = spark.read.("/Users/helin/Documents/Pro/Spark/Pro/maleware-classification/local_data/subtrain/B7O90Mfiz5EPpJyTdXFb.asm")
    val Decimal = """\s([a-fA-F0-9]{2}\s)+\s*([a-z]+)""".r




    val input = """.text:00409A44 EB 0A							       jmp     short loc_409A50""""
    for(s<-Decimal findAllIn input)
      s.getClass
  }

}


