
package STFT
import FFT._
import chisel3._
import chisel3.experimental._
import chisel3.util._
import scala.math._

class SpectrogramGenWithFIFO extends Module
  with HasDataConfig
  with HasCosineNumberConfig
  with HasElaborateConfig {
  val io = IO(new Bundle {
    val FFTsignal = Flipped(Decoupled(new MyComplex))
    val Spectrogram = Decoupled(new MyFixedPoint)
  })
  val SpectrogramGenblock: SpectrogramGen = Module(new SpectrogramGen)
  val SpectrogramFIFO: MyFifo = Module(new MyFifo(depth = 2 * FFTLength,threshold = FFTLength))

  io.FFTsignal <> SpectrogramGenblock.io.FFTsignal//busrt i_valid


  SpectrogramGenblock.io.Spectrogram.ready :=  ((2 * FFTLength).U - SpectrogramFIFO.io.count > (FFTLength/2 +1).U)
  SpectrogramFIFO.io.datain  := SpectrogramGenblock.io.Spectrogram.bits
  SpectrogramFIFO.io.wr      := SpectrogramGenblock.io.Spectrogram.valid
  SpectrogramFIFO.io.rd      := io.Spectrogram.ready && io.Spectrogram.valid
  io.Spectrogram.valid       := !SpectrogramFIFO.io.empty || (SpectrogramFIFO.io.wr && SpectrogramFIFO.io.empty)// && BurstValid
  io.Spectrogram.bits        := SpectrogramFIFO.io.dataout

}
