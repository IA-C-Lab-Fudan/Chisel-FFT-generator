
package STFT

import FFT._
import chisel3._
import chisel3.experimental._
import chisel3.util._
import scala.math._

class SpectrogramGen extends Module
  with HasDataConfig
  with HasCosineNumberConfig {
  val io = IO(new Bundle {
    val FFTsignal = Flipped(Decoupled(new MyComplex))
    val Spectrogram = Decoupled(new MyFixedPoint)
  })
  val SpectrogramReg = Reg(new MyFixedPoint)
  val SpectrogramCnt = RegInit(0.U(10.W))
  val Spectrogram_valid = RegInit(false.B)
  io.FFTsignal.ready := io.Spectrogram.ready
  when(io.FFTsignal.valid  && io.Spectrogram.ready){
    SpectrogramReg := ComplexEnergy(io.FFTsignal.bits,io.FFTsignal.bits)
    Spectrogram_valid := true.B
//    io.FFTsignal.ready   := true.B
    SpectrogramCnt := SpectrogramCnt + 1.U
  }.elsewhen(SpectrogramCnt < 512.U && SpectrogramCnt =/= 0.U) { //constantly transfer 512 FFT result
    SpectrogramCnt := SpectrogramCnt + 1.U
//    io.FFTsignal.ready   := true.B
    when(SpectrogramCnt < 258.U){
      SpectrogramReg := ComplexEnergy(io.FFTsignal.bits, io.FFTsignal.bits)
      Spectrogram_valid := true.B
    }.otherwise{
      Spectrogram_valid := false.B
    }

  }.otherwise{
    SpectrogramCnt := 0.U
    Spectrogram_valid := false.B
//    io.FFTsignal.ready   := true.B
//    when(io.Spectrogram.ready === true.B){io.FFTsignal.ready := true.B}

  }
  io.Spectrogram.bits := SpectrogramReg
  io.Spectrogram.valid := Spectrogram_valid
}