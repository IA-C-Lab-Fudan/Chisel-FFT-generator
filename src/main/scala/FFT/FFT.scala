
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
    val dIn = Input(new MyComplex)
    val din_valid = Input(Bool())
    val dOut1 = Output(new MyComplex)
    val dOut2 = Output(new MyComplex)
    val dout_valid = Output(Bool())
  })

  def sinTable(k: Int): Vec[FixedPoint] = {
    val times = (0 until FFTLength / 2 by pow(2, k).toInt)
      .map(i => -(i * 2 * Pi) / FFTLength.toDouble)
    val inits = times.map(t => FixedPoint.fromDouble(sin(t), DataWidth.W, BinaryPoint.BP))
    VecInit(inits)
  }
  def cosTable(k: Int): Vec[FixedPoint] = {
    val times = (0 until FFTLength / 2 by pow(2, k).toInt).map(i => (i * 2 * Pi) / FFTLength.toDouble)
    val inits = times.map(t => FixedPoint.fromDouble(cos(t), DataWidth.W, BinaryPoint.BP))
    VecInit(inits)
  }
  def wnTable(k: Int)(idx: UInt): MyComplex = {
    val res = Wire(new MyComplex)
    res.re := cosTable(k)(idx)
    res.im := sinTable(k)(idx)
    res
  }

  val stages = log2Ceil(FFTLength)
  val cnt = RegInit(0.U((stages).W))
  when(io.din_valid){
    cnt := cnt + 1.U
  }
  val cntD1 = RegNext(cnt)
  val cntD2 = RegNext(cntD1)

  val out1 = VecInit(Seq.fill(stages + 1)(0.S((2 * DataWidth).W).asTypeOf(new MyComplex)))
  val out2 = VecInit(Seq.fill(stages + 1)(0.S((2 * DataWidth).W).asTypeOf(new MyComplex)))
  if (useBRAM) {
    out1(0) := RegNext(io.dIn)
    out2(0) := RegNext(io.dIn)
  } else {
    out1(0) := io.dIn
    out2(0) := io.dIn
  }

  for (i <- 0 until stages - 1) {
    val wnCtrl = cnt(stages-2-i, 0)
    val wn = Wire(new MyComplex())
    val currentDep = 1 << (stages - 1 - i)
    if (useBRAM && currentDep > depBound) {
      val mem = Module(new BRAM(dep = log2Ceil(currentDep), dw = 2 * DataWidth))
      mem.io.en := true.B
      mem.io.addr := wnCtrl
      mem.io.clock := clock.asUInt()
      wn := mem.io.dout.asTypeOf(new MyComplex)
    } else {
      wn := wnTable(i)(wnCtrl)
    }
    val BF12 = Butterfly(ShiftRegister(out1(i), (FFTLength / pow(2, i + 1)).toInt), out2(i), wn)
    val swCtrl = Wire(Bool())
    if (useBRAM) {
      swCtrl := cntD1(stages - 2 - i)
    } else {
      swCtrl := cnt(stages - 2 - i)
    }
    val sw12 = Switch(BF12._1, ShiftRegister(BF12._2, (FFTLength / pow(2, i + 2)).toInt), swCtrl)
    out1(i + 1) := sw12._1
    out2(i + 1) := sw12._2
  }
  val out1D1 = RegNext(out1(stages - 1))
  out1(stages) := ComplexAdd(out1D1, out2(stages - 1))
  out2(stages) := ComplexSub(out1D1, out2(stages - 1))

  io.dOut1 := RegNext(out1(stages))
  io.dOut2 := RegNext(out2(stages))
  if (useBRAM) {
    io.dout_valid := cntD2 === (FFTLength - 1).asUInt()
  }
  else {
    io.dout_valid := cntD1 === (FFTLength - 1).asUInt()
  }
}
