
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

class STFTTest extends FlatSpec
  with ChiselScalatestTester
  with Matchers
  with HasDataConfig
  with HasCosineNumberConfig{
  behavior of "Tester5"
  it should "test STFT" in{
    var H:Bool = true.B
    var L:Bool = false.B
    test(new STFT).withAnnotations(Seq(WriteVcdAnnotation)){c=>
      //forever High
      c.io.spectrogram.ready.poke(H)
      //initial cosines table
      val r = new scala.util.Random
      var bound: Double = math.pow(2.0, BinaryPoint)
      //println("Start_Load_Cosine")
      //println(c.io.cosines.ready.peek())
      for(i <- 0 until CosineNumber) {
//        var cosinevalue = (-bound.toInt / 2 + r.nextInt(bound.toInt)) * 2 / bound
          var cosinevalue = i/719.0*2.0-1.0
        //println(i)
        //println(i + " : " + cosinevalue)
        c.io.cosines.valid.poke(H)
        c.io.cosines.bits.value.poke(cosinevalue.F(DataWidth.W,BinaryPoint.BP))
        c.clock.step()
        c.io.cosines.valid.poke(L)
        c.clock.step(5)
      }
      c.io.cosines.valid.poke(L)
      c.clock.step()
      println(c.io.cosines.ready.peek())

      println("Start_Load_Signal")
      for(i <- 0 until 3000){
//        var signal_in = (-bound.toInt / 2 + r.nextInt(bound.toInt)) * 2 / bound
          var signal_in = i%320.0/319.0*0.5
//          var signal_in = i%320.0/319.0*0.5 + 0.5
          c.io.signal.valid.poke(H)
          c.io.signal.bits.value.poke(signal_in.F(DataWidth.W, BinaryPoint.BP))
        c.clock.step()
      }
      c.io.signal.valid.poke(L)
    }
  }
}
