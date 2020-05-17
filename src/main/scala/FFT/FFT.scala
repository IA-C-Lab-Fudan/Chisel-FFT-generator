
package FFT

import chisel3._
import chisel3.experimental._
import chisel3.util._
import scala.math._

// R2MDC
class FFT extends Module
  with HasDataConfig
  with HasElaborateConfig {
  val io = IO(new Bundle {
    // 0: FFT  1: IFFT
    val mode = if(supportIFFT) Some(Input(Bool())) else None
    val dIn = Input(new MyComplex)
    val din_valid = Input(Bool())
    val dOut1 = Output(new MyComplex)
    val dOut2 = Output(new MyComplex)
    val dout_valid = Output(Bool())
  })

  val mode = io.mode.getOrElse(false.B)
  val stages = log2Ceil(FFTLength)
  def sinTable(k: Int): Vec[FixedPoint] = {
    val times = (0 until FFTLength / 2 by pow(2, k).toInt)
      .map(i => -(i * 2 * Pi) / FFTLength.toDouble)
    val inits = times.map(t => FixedPoint.fromDouble(sin(t), DataWidth.W, BinaryPoint.BP))
    VecInit(inits)
  }
  def cosTable(k: Int): Vec[FixedPoint] = {
    val times = (0 until FFTLength / 2 by pow(2, k).toInt)
      .map(i => -(i * 2 * Pi) / FFTLength.toDouble)
    val inits = times.map(t => FixedPoint.fromDouble(cos(t), DataWidth.W, BinaryPoint.BP))
    VecInit(inits)
  }
  def sinTable2(k: Int): Vec[FixedPoint] = {
    val times = (0 until FFTLength / 2 by pow(2, k).toInt)
      .map(i => (i * 2 * Pi) / FFTLength.toDouble)
    val inits = times.map(t => FixedPoint.fromDouble(sin(t), DataWidth.W, BinaryPoint.BP))
    VecInit(inits)
  }
  def cosTable2(k: Int): Vec[FixedPoint] = {
    val times = (0 until FFTLength / 2 by pow(2, k).toInt)
      .map(i => (i * 2 * Pi) / FFTLength.toDouble)
    val inits = times.map(t => FixedPoint.fromDouble(cos(t), DataWidth.W, BinaryPoint.BP))
    VecInit(inits)
  }
  def wnTable(k: Int)(idx: UInt): MyComplex = {
    val res = Wire(new MyComplex)
    res.re := Mux(mode, cosTable2(k)(idx), cosTable(k)(idx))
    res.im := Mux(mode, sinTable2(k)(idx), sinTable(k)(idx))
    res
  }
  def timesInvn(a: MyComplex): MyComplex = {
    val b = Wire(new MyComplex)
    b.re := a.re >> stages
    b.im := a.im >> stages
    b
  }

  val cnt = RegInit(0.U((stages).W))
  when(io.din_valid){
    cnt := cnt + 1.U
  }
  val cntD1 = RegNext(cnt)

  val out1 = VecInit(Seq.fill(stages + 1)(0.S((2 * DataWidth).W).asTypeOf(new MyComplex)))
  val out2 = VecInit(Seq.fill(stages + 1)(0.S((2 * DataWidth).W).asTypeOf(new MyComplex)))
  out1(0) := io.dIn
  out2(0) := io.dIn

  for (i <- 0 until stages - 1) {
    val wnCtrl = cnt(stages-2-i, 0)
    val wn = wnTable(i)(wnCtrl)
    val BF12 = Butterfly(ShiftRegister(out1(i), (FFTLength / pow(2, i + 1)).toInt), out2(i), wn)
    val swCtrl = cnt(stages-2-i)
    val sw12 = Switch(BF12._1, ShiftRegister(BF12._2, (FFTLength / pow(2, i + 2)).toInt), swCtrl)
    out1(i + 1) := sw12._1
    out2(i + 1) := sw12._2
  }
  val out1D1 = RegNext(out1(stages - 1))
  out1(stages) := ComplexAdd(out1D1, out2(stages - 1))
  out2(stages) := ComplexSub(out1D1, out2(stages - 1))
  val dout1 = Mux(mode, timesInvn(out1(stages)), out1(stages))
  val dout2 = Mux(mode, timesInvn(out2(stages)), out2(stages))

  io.dOut1 := RegNext(dout1)
  io.dOut2 := RegNext(dout2)
  io.dout_valid := cntD1 === (FFTLength - 1).asUInt()
}
