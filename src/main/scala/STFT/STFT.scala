
package STFT

import FFT._
import chisel3._
import chisel3.experimental._
import chisel3.util._
import scala.math._

class STFT extends Module
  with HasDataConfig
  with HasCosineNumberConfig {
  val io = IO(new Bundle{
      val signal = Flipped(Decoupled(new MyFixedPoint))
      val cosines = Flipped(Decoupled(new MyFixedPoint))
      val spectrogram = Decoupled(new MyFixedPoint)
  })

  def R2RI(a: MyFixedPoint): MyComplex = {
    val out = Wire(new MyComplex)
    out.re := a.value
    out.im := 0.F(DataWidth.W, BinaryPoint.BP)
    out
  }

  val HanningBlock: HanningWithFIFO = Module(new HanningWithFIFO)
  val FFTBlock: TOP = Module(new TOP)
  val SpectrogramGenBlock: SpectrogramGenWithFIFO = Module(new SpectrogramGenWithFIFO)

  io.signal <> HanningBlock.io.signal
  io.cosines <> HanningBlock.io.cosines

//  val HanningRI = Decoupled(new MyComplex)
//  HanningRI.bits := R2RI(HanningBlock.io.HanningSignal.bits)
//  HanningRI.valid := HanningBlock.io.HanningSignal.valid
//  HanningRI.ready := HanningBlock.io.HanningSignal.ready

//  FFTBlock.io.din <>  HanningRI

  FFTBlock.io.din.bits := R2RI(HanningBlock.io.HanningSignal.bits)
  FFTBlock.io.din.valid := HanningBlock.io.HanningSignal.valid
  HanningBlock.io.HanningSignal.ready := FFTBlock.io.din.ready

  FFTBlock.io.dout <> SpectrogramGenBlock.io.FFTsignal

  io.spectrogram <> SpectrogramGenBlock.io.Spectrogram

}
