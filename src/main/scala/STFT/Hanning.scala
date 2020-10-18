
package STFT

import FFT._
import chisel3._
import chisel3.experimental._
import chisel3.util._
import scala.math._

class Hanning extends Module
  with HasDataConfig
  with HasCosineNumberConfig {
  val io = IO(new Bundle {
    val signal = Flipped(Decoupled(new MyFixedPoint))
    val cosines = Flipped(Decoupled(new MyFixedPoint))
    val HanningSignal = Decoupled(new MyFixedPoint)
  })

  //Initialize Cosine Table
  val cosinesbank     = Mem(CosineNumber, new MyFixedPoint)
  val cosinesbank_wcnt = RegInit(0.U(32.W))
  val cosinesbank_rcnt = RegInit(0.U(32.W))
  when(io.cosines.valid && io.cosines.ready){
    cosinesbank_wcnt := cosinesbank_wcnt + 1.U
    cosinesbank.write(cosinesbank_wcnt, io.cosines.bits)
  }
  io.cosines.ready := io.cosines.valid && (cosinesbank_wcnt < 720.U)

  //FSM transfer signal
  val s_idle :: s_trans :: ss_impl_zero :: Nil = Enum(3)
  val state = RegInit(init = s_idle)
  //state transfer
  when(state === s_idle){
    when(io.signal.valid && io.signal.ready && cosinesbank_rcnt<320.U){
      state := s_trans
    }.elsewhen(io.signal.valid && io.signal.ready && cosinesbank_rcnt<512.U){
      state := ss_impl_zero
    }
  }.elsewhen(state === s_trans){
    when(io.signal.valid && io.signal.ready && cosinesbank_rcnt<320.U){
      state := s_trans
    }.elsewhen(io.signal.valid && io.signal.ready && cosinesbank_rcnt<512.U){
      state := ss_impl_zero
    }.otherwise{
      state := s_idle
    }
  }.elsewhen(state === ss_impl_zero){
      when(io.HanningSignal.ready && io.HanningSignal.valid && cosinesbank_rcnt < 512.U){
        state := ss_impl_zero
      }.otherwise{           //pause or halt
        state := s_idle
       }
    }
  //output
  val hanning_signal = Reg(new MyFixedPoint)

  io.HanningSignal.bits := hanning_signal

  when(state === s_idle){
    io.signal.ready := io.HanningSignal.ready && io.signal.valid  //i_ready
    io.HanningSignal.valid := false.B           //o_valid
    when(cosinesbank_rcnt === 513.U){cosinesbank_rcnt:= 0.U} //halt
  }.elsewhen(state === s_trans){
    cosinesbank_rcnt := cosinesbank_rcnt + 1.U //cnt++
    hanning_signal.value := io.signal.bits.value * cosinesbank.read(cosinesbank_rcnt).value  //compute hanning window
    io.signal.ready := io.HanningSignal.ready //i_ready
    io.HanningSignal.valid := true.B        //o_valid
  }.elsewhen(state === ss_impl_zero){
    cosinesbank_rcnt := cosinesbank_rcnt + 1.U //cnt++
    hanning_signal.value := FixedPoint(0,DataWidth.W, BinaryPoint.BP)    //o_valid
    io.signal.ready := false.B   //i_ready
    io.HanningSignal.valid := true.B
  }.otherwise{
    io.signal.ready := true.B
    io.HanningSignal.valid := false.B

  }
}