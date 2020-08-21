
package FFT

import chisel3._
import chisel3.experimental._
import chisel3.util._
import scala.math._

class Reorder extends Module with HasDataConfig with HasElaborateConfig{ //整理倒位序数据成正序数据
  val io = IO(new Bundle{
    val in1 = Input(new MyComplex)
    val in2 = Input(new MyComplex)
    val in_valid = Input(Bool())
    val out = Output(new MyComplex)
    val out_valid = Output(Bool())
  })

  val databuffer = RegInit(VecInit(Seq.fill(FFTLength)(0.S((2*DataWidth).W).asTypeOf(new MyComplex)))) //数据缓存

  val times = log2Ceil(FFTLength / 2)
  val in_counter = RegInit(0.U(times.W))    //接收数据的次数，每次接受2数据
  val out_counter = RegInit(0.U((times + 1).W))   //输出数据的次数，每次输出1数据
  val index1 = Reverse(Cat(in_counter, 0.U(1.W)))  //位反序
  val index2 = Reverse(Cat(in_counter, 1.U(1.W)))  //周期0 就会有in_counter == 0，此时index1 = b000……00， index2 == b100……00.
  when(io.in_valid && !io.out_valid){                           //每次有效读入2数据
    in_counter := in_counter + 1.U
    databuffer(index1) := io.in1
    databuffer(index2) := io.in2
  }

  io.out_valid := ShiftRegister(io.in_valid, FFTLength / 2) || ShiftRegister(io.in_valid, FFTLength)
  when(io.out_valid) {
    out_counter := out_counter + 1.U
  }
  io.out := databuffer(out_counter)
}

class FFTReorder extends Module
  with HasDataConfig
  with HasElaborateConfig {
  val io = IO(new Bundle{
    val mode = if(supportIFFT) Some(Input(Bool())) else None
    val dIn = Input(new MyComplex)
    val din_valid = Input(Bool())
    val dOut = Output(new MyComplex)
    val dout_valid: Bool = Output(Bool())
  })

  val fftblock: FFT = Module(new FFT)
  fftblock.io.dIn := io.dIn
  fftblock.io.din_valid := io.din_valid

  val reorderblock = Module(new Reorder)
  reorderblock.io.in1 := fftblock.io.dOut1
  reorderblock.io.in2 := fftblock.io.dOut2
  reorderblock.io.in_valid := fftblock.io.dout_valid

  io.dOut := reorderblock.io.out
  io.dout_valid := reorderblock.io.out_valid

}
