
package FFT
import java.io._
import scala.math._

object twFileGen extends App
  with HasDataConfig
  with HasElaborateConfig {
  def sinTable(k: Int): IndexedSeq[BigInt] = {
    val times = (0 until FFTLength / 2 by pow(2, k).toInt)
      .map(i => -(i * 2 * Pi) / FFTLength.toDouble)
    val inits = times.map(t => BigInt(round(pow(2, BinaryPoint) * sin(t))))
    inits
  }
  def cosTable(k: Int): IndexedSeq[BigInt] = {
    val times = (0 until FFTLength / 2 by pow(2, k).toInt)
      .map(i => (i * 2 * Pi) / FFTLength.toDouble)
    val inits = times.map(t => BigInt(round(pow(2, BinaryPoint) * cos(t))))
    inits
  }
  val stages = chisel3.util.log2Ceil(FFTLength)

  for (i <- 0 until stages - 1) {
    // dep > 16
    if (i < stages - 5) {
      val dep = 1 << (stages - 1 - i)
      val writer1 = new PrintWriter(new File("./twFiles/tw" + dep.toString() + ".coe" ))
      val writer2 = new PrintWriter(new File("./twFiles/tw" + dep.toString() + ".txt" ))
      writer1.write("memory_initialization_radix=2;\n")
      writer1.write("memory_initialization_vector=")
      for (j <- 0 until dep) {
        val a = sinTable(i)(j)
        val b = cosTable(i)(j)
        var w : String = ""
        for (i <- 0 until DataWidth) {
          if(((a >> i) & 1) == 1) {
            w = "1" + w
          } else {
            w = "0" + w
          }
        }
        for (i <- 0 until DataWidth) {
          if(((b >> i) & 1) == 1) {
            w = "1" + w
          } else {
            w = "0" + w
          }
        }
        writer2.write(w + "\n")
        if (j == dep - 1) {
          w = w + ";\n"
        } else {
          w = w + ",\n"
        }
        writer1.write(w)
      }
      writer1.close()
      writer2.close()
    }
  }
}
