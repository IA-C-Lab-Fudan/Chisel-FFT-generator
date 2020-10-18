package STFT

import chisel3._
import chiseltest._
import org.scalatest._
import org.scalatest.Matchers
import chisel3.Bool
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.WriteVcdAnnotation

import scala.math._
import  FFT._
import chisel3.experimental.FixedPoint

class SpectrogramTest extends FlatSpec
  with ChiselScalatestTester
  with Matchers
  with HasDataConfig
  with HasCosineNumberConfig {
  behavior of "Tester3"
  it should "test Spectrogram" in {
    var H: Bool = true.B
    var L: Bool = false.B
    test(new SpectrogramGen).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
      c.io.Spectrogram.ready.poke(H)
      c.io.FFTsignal.valid.poke(H)
      val r = new scala.util.Random
      var bound: Double = math.pow(2.0, BinaryPoint)
      println("Start_Load_FFTSignal")
      for (i <- 0 until 600) {

        if(i == 1)
          c.io.FFTsignal.valid.poke(L)
        var signal_in = (-bound.toInt / 2 + r.nextInt(bound.toInt)) * 2 / bound
        //if(i<510)
        //c.io.FFTsignal.valid.poke(H)
        //if (i>=510)
        //c.io.FFTsignal.valid.poke(L)
        c.io.FFTsignal.bits.re.poke(signal_in.F(DataWidth.W,BinaryPoint.BP))
        c.io.FFTsignal.bits.im.poke(1.F(DataWidth.W,BinaryPoint.BP))
        c.clock.step()
        if(i == 510) {
          c.io.Spectrogram.ready.poke(H)
        }
      }
      c.io.FFTsignal.valid.poke(L)

    }
  }
}