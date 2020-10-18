
package STFT
import FFT._
import chisel3._
import chisel3.experimental._
import chisel3.util._
import scala.math._

class SpectrogramGenWithFIFO extends Module
  with HasDataConfig
  with HasCosineNumberConfig {
  val io = IO(new Bundle {
    val FFTsignal = Flipped(Decoupled(new MyComplex))
    val Spectrogram = Decoupled(new MyFixedPoint)
  })
  val SpectrogramGenblock: SpectrogramGen = Module(new SpectrogramGen)
  val SpectrogramFIFO: MyFifo = Module(new MyFifo(depth = 1024,threshold = 512))

//  val BurstValid = RegInit(false.B)
//  val BurstReadCnt = RegInit(0.U(32.W))
//  val BurstTransEn = Wire(Bool())
//  BurstTransEn := BurstValid  || (BurstReadCnt<512.U) && (BurstReadCnt> 0.U)
//
//  when(io.FFTsignal.ready && BurstValid){
//    BurstReadCnt := BurstReadCnt + 1.U
//    BurstValid := false.B
//  }.elsewhen(BurstTransEn){
//    BurstReadCnt := BurstReadCnt + 1.U
//  }.otherwise{
//    BurstReadCnt := 0.U
//    when(SpectrogramFIFO.io.MorethanN){ BurstValid := true.B }
//  }

  io.FFTsignal <> SpectrogramGenblock.io.FFTsignal//busrt i_valid


  SpectrogramGenblock.io.Spectrogram.ready :=  ((900.U - SpectrogramFIFO.io.count) > 257.U) //fifo remain capacity more than 512
  SpectrogramFIFO.io.datain  := SpectrogramGenblock.io.Spectrogram.bits
  SpectrogramFIFO.io.wr      := SpectrogramGenblock.io.Spectrogram.valid
  SpectrogramFIFO.io.rd      := io.Spectrogram.ready
  io.Spectrogram.valid       := !SpectrogramFIFO.io.empty   && io.Spectrogram.ready// && BurstValid
  io.Spectrogram.bits        := SpectrogramFIFO.io.dataout

}
