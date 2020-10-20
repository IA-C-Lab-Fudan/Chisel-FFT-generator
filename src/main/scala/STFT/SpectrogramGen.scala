
package STFT

import FFT._
import chisel3._
import chisel3.experimental._
import chisel3.util._
import scala.math._

class SpectrogramGen extends Module
  with HasDataConfig
  with HasCosineNumberConfig
  with HasElaborateConfig {
  val io = IO(new Bundle {
    val FFTsignal = Flipped(Decoupled(new MyComplex))
    val Spectrogram = Decoupled(new MyFixedPoint)
  })
  val SpectrogramReg = Reg(new MyFixedPoint)
  val SpectrogramCnt = RegInit(0.U(10.W))
  val Spectrogram_valid = RegInit(false.B)

  when(io.FFTsignal.valid  && io.Spectrogram.ready){
    SpectrogramReg := ComplexEnergy(io.FFTsignal.bits,io.FFTsignal.bits)
    when(SpectrogramCnt < (FFTLength/2 + 1).U){
      Spectrogram_valid := true.B
      SpectrogramCnt := SpectrogramCnt + 1.U
    }.elsewhen(SpectrogramCnt < FFTLength.U) {
      Spectrogram_valid := false.B
      SpectrogramCnt := SpectrogramCnt + 1.U
    }.otherwise{
      Spectrogram_valid := false.B
      SpectrogramCnt := 0.U
    }
  }



//    Spectrogram_valid := true.B
//    SpectrogramCnt := SpectrogramCnt + 1.U
//      when(SpectrogramCnt > (FFTLength/2 + 1).U && SpectrogramCnt < FFTLength.U){
//        Spectrogram_valid := false.B
//        when((SpectrogramCnt === FFTLength.U)){
//          SpectrogramCnt := 0.U
//        }
//      }
//  }

  io.FFTsignal.ready := io.Spectrogram.ready
  io.Spectrogram.bits := SpectrogramReg
  io.Spectrogram.valid := Spectrogram_valid
}