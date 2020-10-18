
package STFT

import chisel3._
import chiseltest._
import org.scalatest._
import org.scalatest.Matchers
import chisel3.Bool
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.WriteVcdAnnotation
import chisel3.experimental._

import scala.math._
import FFT._
import chisel3.experimental.FixedPoint

class TOPTest extends FlatSpec
  with ChiselScalatestTester
  with Matchers
  with HasDataConfig
  with HasCosineNumberConfig {
  behavior of "Tester6"
  it should "test TOP" in {
    var H: Bool = true.B
    var L: Bool = false.B
    test(new TOP).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      val r = new scala.util.Random
      var bound: Double = math.pow(2.0, BinaryPoint)
//      c.io.dout.ready.poke(H)
      println("Start_Load_Signal")
      for(i <- 0 until 5000){
        var signal_in = (-bound.toInt / 2 + r.nextInt(bound.toInt)) * 2 / bound
        if (i % 550 == 0) {
          c.io.din.valid.poke(H)
        }
        if (i % 550 == 1) {
          c.io.din.valid.poke(L)
        }
       if (i % 800 == 0) {
          c.io.dout.ready.poke(L)
        }
        if (i % 800 == 3) {
          c.io.dout.ready.poke(H)
        }
        c.io.din.bits.re.poke(signal_in.F(DataWidth.W, BinaryPoint.BP))
        c.io.din.bits.im.poke(0.F(DataWidth.W, BinaryPoint.BP))
        c.clock.step()
      }
      c.io.din.valid.poke(L)
    }
  }
}