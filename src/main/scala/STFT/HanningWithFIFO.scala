
package STFT
import FFT._
import chisel3._
import chisel3.experimental._
import chisel3.util._
import scala.math._

class HanningWithFIFO extends Module
  with HasDataConfig
  with HasCosineNumberConfig {
    val io = IO(new Bundle {
    val signal = Flipped(Decoupled(new MyFixedPoint))
    val cosines = Flipped(Decoupled(new MyFixedPoint))
    val HanningSignal = Decoupled(new MyFixedPoint)
  })

  val Hanningblock: Hanning = Module(new Hanning)
  val HanningFIFO: MyFifo = Module(new MyFifo(depth = 1024,threshold = 512))

  val BurstValid = RegInit(false.B)
  val BurstReadCnt = RegInit(0.U(32.W))
  val BurstTransEn = Wire(Bool())
  BurstTransEn := BurstValid  || (BurstReadCnt<512.U) && (BurstReadCnt> 0.U)

  when(io.HanningSignal.ready && BurstValid){
    BurstReadCnt := BurstReadCnt + 1.U
    BurstValid := false.B
  }.elsewhen(BurstTransEn){
    BurstReadCnt := BurstReadCnt + 1.U
  }.otherwise{
    BurstReadCnt := 0.U
    when(HanningFIFO.io.MorethanN){ BurstValid := true.B }
  }

  Hanningblock.io.signal <> io.signal
  Hanningblock.io.cosines <> io.cosines

  io.HanningSignal.valid := BurstValid
  io.HanningSignal.bits  := HanningFIFO.io.dataout


  HanningFIFO.io.datain := Hanningblock.io.HanningSignal.bits
  HanningFIFO.io.wr := Hanningblock.io.HanningSignal.valid    //single write
  Hanningblock.io.HanningSignal.ready := !HanningFIFO.io.full
  //512 Burst read
  HanningFIFO.io.rd := BurstReadCnt =/= 0.U //&& io.HanningSignal.ready

}
