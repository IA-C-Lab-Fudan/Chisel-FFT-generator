
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
class BasicTest4 extends FlatSpec
  with ChiselScalatestTester
  with Matchers
  with HasDataConfig
  with HasCosineNumberConfig {
  behavior of "Tester4"
  it should "test SpectrogramGenWithFIFO" in { //what's this????
    var H: Bool = true.B
    var L: Bool = false.B
    test(new SpectrogramGenWithFIFO).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      c.io.Spectrogram.ready.poke(L)
      val r = new scala.util.Random
      var bound: Double = math.pow(2.0, BinaryPoint) //
      c.io.FFTsignal.valid.poke(H)
      for (i <- 0 until 2000) {
        if (i % 550 == 0) {
          c.io.FFTsignal.valid.poke(H)
        }
        if (i % 550 == 1) {
          c.io.FFTsignal.valid.poke(L)
        }
        //if(i%10 == 0 && i != 0) c.io.Spectrogram.ready.poke(H)
        //if(i%10 == 1 && i != 0) c.io.Spectrogram.ready.poke(L)
        var signal_in1 = (-bound.toInt / 2 + r.nextInt(bound.toInt)) * 2 / bound
        //var signal_in2 = (-bound.toInt / 2 + r.nextInt(bound.toInt)) * 2 / bound
        c.io.FFTsignal.bits.re.poke(signal_in1.F(DataWidth.W, BinaryPoint.BP))
        c.io.FFTsignal.bits.im.poke(0.F(DataWidth.W, BinaryPoint.BP))
        c.clock.step()
      }
      c.io.Spectrogram.ready.poke(L)
      for (i <- 0 until 1000) {
        c.io.Spectrogram.ready.poke(H)
        c.clock.step()
      }
    }
  }
}

