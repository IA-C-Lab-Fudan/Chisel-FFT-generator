
package STFT

import FFT._
import chisel3._
import chisel3.util._
import chisel3.experimental._


class MyFifo(val depth : Int, val threshold : Int) extends Module
  with HasDataConfig
  with HasCosineNumberConfig{
  val io = IO(new Bundle {
    val datain = Input(new MyFixedPoint)
    val dataout = Output(new MyFixedPoint)
    val wr = Input(Bool())
    val rd = Input(Bool())
    val full = Output(Bool())
    val empty = Output(Bool())
    val MorethanN = Output(Bool())
    val count = Output(UInt())
  })

  val count = RegInit(0.U(32.W))
  val mem = Mem(depth,new MyFixedPoint)
  val wp = RegInit(0.U(32.W))
  val rp = RegInit(0.U(32.W))
  val dataout = Reg(new MyFixedPoint())


  def IndexAdd(index : UInt) : UInt = {
    val temp = WireInit(index)
    when(index === (depth - 1).U) { temp := 0.U }
      .otherwise { temp := index + 1.U }
    temp
  }

  when(io.wr === true.B && io.rd === true.B) {
    when(count === 0.U) { io.dataout := io.datain }
      .otherwise {
        io.dataout := mem(rp)
        rp := IndexAdd(rp)
        mem(wp) := io.datain
        wp := IndexAdd(wp)
      }
  } .elsewhen (io.wr === true.B && io.rd === false.B) {
    io.dataout.value := FixedPoint(0,DataWidth.W, BinaryPoint.BP)
    when(count < depth.U) {
      mem(wp) := io.datain
      wp := IndexAdd(wp)
      count := count + 1.U
    }
  } .elsewhen (io.wr === false.B && io.rd === true.B) {
    when(count > 0.U) {
      io.dataout := mem(rp)
      rp := IndexAdd(rp)
      count := count - 1.U
    } .otherwise {
      io.dataout.value := FixedPoint(0,DataWidth.W, BinaryPoint.BP)
    }
  } .otherwise {
    io.dataout.value := FixedPoint(0,DataWidth.W, BinaryPoint.BP)
  }

  io.full := (depth.U === count)
  io.empty := (count === 0.U)
  io.MorethanN := (count > threshold.U)
  io.count := count

}