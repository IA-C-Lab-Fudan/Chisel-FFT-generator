package FFT

import chisel3._
import chisel3.experimental._
import chisel3.util._
import hardfloat._
import scala.math._

abstract class MyComplex extends Bundle {
  val re:Bits
  val im:Bits
}

class IEEEComplex extends MyComplex with Config {
  val re = UInt((expWidth + sigWidth).W)
  val im = UInt((expWidth + sigWidth).W)
}

class MyFixComplex extends MyComplex with Config {
  val re = FixedPoint(FixDataWidth.W, BinaryPoint.BP)
  val im = FixedPoint(FixDataWidth.W, BinaryPoint.BP)
}

class MyFloatComplex extends MyComplex with Config {
  val re = UInt((expWidth + sigWidth + 1).W)
  val im = UInt((expWidth + sigWidth + 1).W)
}

class ComplexRecode extends Module with Config {
      val io = IO(new Bundle {
        val in = Input(new IEEEComplex)
        val out = Output(new MyFloatComplex)
  })
  io.out.re := recFNFromFN(expWidth, sigWidth, io.in.re)
  io.out.im := recFNFromFN(expWidth, sigWidth, io.in.im)
}

object ComplexRecode {
  def apply(in:IEEEComplex): MyFloatComplex = {
    val inst = Module(new ComplexRecode)
    inst.io.in := in
    inst.io.out
  }
}

class ComplexDecode extends Module with Config {
  val io = IO(new Bundle {
    val in = Input(new MyFloatComplex)
    val out = Output(new IEEEComplex)
  })
  io.out.re := fNFromRecFN(expWidth, sigWidth, io.in.re)
  io.out.im := fNFromRecFN(expWidth, sigWidth, io.in.im)
}
object ComplexDecode {
  def apply(in:MyFloatComplex):IEEEComplex = {
    val inst = Module(new ComplexDecode)
    inst.io.in := in
    inst.io.out
  }
}
//class MyComplex extends Bundle with Config {
//}
class ComplexIO[T <: MyComplex](complex:T) extends Bundle {
  val op1 = Input(complex)
  val op2 = Input(complex)
  val res = Output(complex)
}
class ComplexAdd[T <: MyComplex](complex:T) extends Module with Config {
  val io = IO(new ComplexIO(complex))
  if(use_float) {
    io.res.re := FloatAdd(io.op1.re.asUInt,io.op2.re.asUInt)
    io.res.im := FloatAdd(io.op1.im.asUInt, io.op2.im.asUInt)
  }else{
    io.res.re := io.op1.re.asFixedPoint(BinaryPoint.BP) + io.op2.re.asFixedPoint(BinaryPoint.BP)
    io.res.im := io.op1.im.asFixedPoint(BinaryPoint.BP) + io.op2.im.asFixedPoint(BinaryPoint.BP)
  }
}
object ComplexAdd extends Config{
  def apply(op1: MyComplex, op2: MyComplex): MyComplex = {
    val complex = if(use_float) new MyFloatComplex else new MyFixComplex
    val inst = Module(new ComplexAdd(complex))
    inst.io.op1 := op1
    inst.io.op2 := op2
    inst.io.res
  }
}
class ComplexSub[T <: MyComplex](complex:T) extends Module with Config {
  val io = IO(new ComplexIO(complex))
  if(use_float) {
    io.res.re := FloatSub(io.op1.re.asUInt(), io.op2.re.asUInt())
    io.res.im := FloatSub(io.op1.im.asUInt(), io.op2.im.asUInt())
  } else {
    io.res.re := io.op1.re.asFixedPoint(BinaryPoint.BP) - io.op2.re.asFixedPoint(BinaryPoint.BP)
    io.res.im := io.op1.im.asFixedPoint(BinaryPoint.BP) - io.op2.im.asFixedPoint(BinaryPoint.BP)
  }
}
object ComplexSub extends Config{
  def apply(op1: MyComplex, op2: MyComplex): MyComplex = {
    val complex = if(use_float) new MyFloatComplex else new MyFixComplex
    val inst = Module(new ComplexSub(complex))
    inst.io.op1 := op1
    inst.io.op2 := op2
    inst.io.res
  }
}
class ComplexTran[T <: MyComplex](complex:T) extends Module with Config {
  val io = IO(new Bundle {
    val in = Input(complex)
    val out = Output(complex)
  })
  if(use_float) {
    io.out.re := io.in.im
    io.out.im := Cat(!io.in.re(float_point_format),io.in.re(float_point_format-1,0))
  }
  else {
    io.out.re := io.in.im
    io.out.im := -io.in.re.asFixedPoint(BinaryPoint.BP)
  }
}
object ComplexTran extends Config {
  def apply(in: MyComplex): MyComplex = {
    val complex = if(use_float) new MyFloatComplex else new MyFixComplex
    val inst = Module(new ComplexTran(complex))
    inst.io.in := in
    inst.io.out
  }
}
class ComplexMul[T <: MyComplex](complex:T) extends Module with Config {
  val io = IO(new ComplexIO(complex))
  if(use_float) {
    if(useGauss) {
      val k1 = FloatMul(io.op2.re.asUInt(), FloatAdd(io.op1.re.asUInt(), io.op1.im.asUInt()))
      val k2 = FloatMul(io.op1.re.asUInt(), FloatSub(io.op2.im.asUInt(), io.op2.re.asUInt()))
      val k3 = FloatMul(io.op1.im.asUInt(), FloatAdd(io.op2.re.asUInt(), io.op2.im.asUInt()))
      io.res.re := FloatSub(k1, k3)
      io.res.im := FloatAdd(k1, k2)
    }
    else {
      io.res.re := FloatSub(FloatMul(io.op1.re.asUInt(),io.op2.re.asUInt()),FloatMul(io.op1.im.asUInt(),io.op2.im.asUInt()))
      io.res.im := FloatAdd(FloatMul(io.op1.re.asUInt(),io.op2.im.asUInt()),FloatMul(io.op1.im.asUInt(),io.op2.re.asUInt()))
    }
  }else {
    if (useGauss) {
      val k1 = io.op2.re.asFixedPoint(BinaryPoint.BP) * (io.op1.re.asFixedPoint(BinaryPoint.BP) + io.op1.im.asFixedPoint(BinaryPoint.BP))
      val k2 = io.op1.re.asFixedPoint(BinaryPoint.BP) * (io.op2.im.asFixedPoint(BinaryPoint.BP) - io.op2.re.asFixedPoint(BinaryPoint.BP))
      val k3 = io.op1.im.asFixedPoint(BinaryPoint.BP) * (io.op2.re.asFixedPoint(BinaryPoint.BP) + io.op2.im.asFixedPoint(BinaryPoint.BP))
      io.res.re := k1 - k3
      io.res.im := k1 + k2
    } else {
      io.res.re := io.op1.re.asFixedPoint(BinaryPoint.BP) * io.op2.re.asFixedPoint(BinaryPoint.BP) - io.op1.im.asFixedPoint(BinaryPoint.BP) * io.op2.im.asFixedPoint(BinaryPoint.BP)
      io.res.im := io.op1.re.asFixedPoint(BinaryPoint.BP) * io.op2.im.asFixedPoint(BinaryPoint.BP) + io.op1.im.asFixedPoint(BinaryPoint.BP) * io.op2.re.asFixedPoint(BinaryPoint.BP)
    }
  }
}
object ComplexMul extends Config {
  def apply(op1: MyComplex, op2: MyComplex): MyComplex = {
    val complex = if(use_float) new MyFloatComplex else new MyFixComplex
    val inst = Module(new ComplexMul(complex))
    inst.io.op1 := op1
    inst.io.op2 := op2
    inst.io.res
  }
}
class ButterflyAdd[T <: MyComplex](complex:T) extends Module with Config {
  val io = IO(new Bundle {
    val in = Input(Vec(radix, complex))
    val out = Output(Vec(radix, complex))
  })
  if (radix == 2) {
    io.out(0) := ComplexAdd(io.in(0), io.in(1))
    io.out(1) := ComplexSub(io.in(0), io.in(1))
  } else if (radix == 4) {
    io.out(0) := ComplexAdd(ComplexAdd(io.in(0), io.in(2)), ComplexAdd(io.in(1), io.in(3)))
    io.out(1) := ComplexSub(ComplexAdd(io.in(0), io.in(2)), ComplexAdd(io.in(1), io.in(3)))
    io.out(2) := ComplexAdd(ComplexSub(io.in(0), io.in(2)), ComplexTran(ComplexSub(io.in(1), io.in(3))))
    io.out(3) := ComplexSub(ComplexSub(io.in(0), io.in(2)), ComplexTran(ComplexSub(io.in(1), io.in(3))))
  } else if (radix == 8) {
    val mulre:Bits = if(use_float) {
      if (float_point_format == 32) recFNFromFN(expWidth, sigWidth, java.lang.Float.floatToIntBits(cos(-Pi / 4).toFloat).S(float_point_format.W).asUInt)
      else if(float_point_format == 64) recFNFromFN(expWidth, sigWidth, java.lang.Double.doubleToRawLongBits(cos(-Pi / 4)).S(float_point_format.W).asUInt)
      else recFNFromFN(expWidth, sigWidth, floatConvert.float2HPF(cos(-Pi / 4).toFloat).S(32.W).asUInt()(15,0))
    }  else FixedPoint.fromDouble(cos(- Pi / 4), FixDataWidth.W, BinaryPoint.BP)
    val mulim:Bits = if(use_float) {
      if(float_point_format == 32) recFNFromFN(expWidth, sigWidth, java.lang.Float.floatToIntBits(sin(- Pi / 4).toFloat).S(float_point_format.W).asUInt)
      else if(float_point_format == 64)recFNFromFN(expWidth, sigWidth, java.lang.Double.doubleToRawLongBits(sin(-Pi / 4)).S(float_point_format.W).asUInt)
      else recFNFromFN(expWidth, sigWidth, floatConvert.float2HPF(sin(-Pi / 4).toFloat).S(32.W).asUInt()(15,0))
    } else FixedPoint.fromDouble(sin(- Pi / 4), FixDataWidth.W, BinaryPoint.BP)
    val mul1 = Cat(mulre,mulim).asTypeOf(if(use_float) new MyFloatComplex else new MyFixComplex)
    val temp_data = VecInit(Seq.fill(8)(0.S.asTypeOf(complex)))
    (0 until 4).map(x => temp_data(x) := ComplexAdd(io.in(x), io.in(x + 4)))
    (0 until 4).map(x => temp_data(4 + x) := ComplexSub(io.in(x), io.in(x + 4)))
    val A02 = ComplexAdd(temp_data(0), temp_data(2))
    val S02 = ComplexSub(temp_data(0), temp_data(2))
    val A13 = ComplexAdd(temp_data(1), temp_data(3))
    val S13 = ComplexSub(temp_data(1), temp_data(3))
    val A46 = ComplexAdd(temp_data(4), ComplexTran(temp_data(6)))
    val S46 = ComplexSub(temp_data(4), ComplexTran(temp_data(6)))
    val A57 = ComplexAdd(temp_data(5), ComplexTran(temp_data(7)))
    val S57 = ComplexSub(temp_data(5), ComplexTran(temp_data(7)))
    io.out(0) := ComplexAdd(A02, A13)
    io.out(1) := ComplexSub(A02, A13)
    io.out(2) := ComplexAdd(S02, ComplexTran(S13))
    io.out(3) := ComplexSub(S02, ComplexTran(S13))
    io.out(4) := ComplexAdd(A46, ComplexMul(A57, mul1))
    io.out(5) := ComplexSub(A46,ComplexMul(A57,mul1))
    io.out(6) := ComplexAdd(S46,ComplexTran(ComplexMul(S57, mul1)))
    io.out(7) := ComplexSub(S46,ComplexTran(ComplexMul(S57, mul1)))
  }
}
object ButterflyAdd extends Config {
  def apply(in: Vec[MyComplex with Config]): Vec[MyComplex with Config] = {
    val complex = if(use_float) new MyFloatComplex else new MyFixComplex
    val inst = Module(new ButterflyAdd(complex))
    inst.io.in := in
    inst.io.out
  }
}
class ButterflyMul[T <: MyComplex](complex:T) extends Module with Config {
  val io = IO(new Bundle {
    val in = Input(Vec(radix, complex))
    val out = Output(Vec(radix, complex))
    val wn = Input(Vec(radix - 1, complex))
  })
  val temp = VecInit(Seq.fill(radix)(0.S.asTypeOf(complex)))
  if (radix == 2) {
    temp(0) := ComplexAdd(io.in(0), io.in(1))
    temp(1) := ComplexSub(io.in(0), io.in(1))
  } else if (radix == 4) {
    temp(0) := ComplexAdd(ComplexAdd(io.in(0), io.in(2)), ComplexAdd(io.in(1), io.in(3)))
    temp(1) := ComplexSub(ComplexAdd(io.in(0), io.in(2)), ComplexAdd(io.in(1), io.in(3)))
    temp(2) := ComplexAdd(ComplexSub(io.in(0), io.in(2)), ComplexTran(ComplexSub(io.in(1), io.in(3))))
    temp(3) := ComplexSub(ComplexSub(io.in(0), io.in(2)), ComplexTran(ComplexSub(io.in(1), io.in(3))))
  } else if (radix == 8) {
    val mulre:Bits = if(use_float) {
      if (float_point_format == 32) recFNFromFN(expWidth, sigWidth, java.lang.Float.floatToIntBits(cos(-Pi / 4).toFloat).S(float_point_format.W).asUInt)
      else if(float_point_format == 64) recFNFromFN(expWidth, sigWidth, java.lang.Double.doubleToRawLongBits(cos(-Pi / 4)).S(float_point_format.W).asUInt)
      else recFNFromFN(expWidth, sigWidth, floatConvert.float2HPF(cos(-Pi / 4).toFloat).S(32.W).asUInt()(15,0))
    }  else FixedPoint.fromDouble(cos(- Pi / 4), FixDataWidth.W, BinaryPoint.BP)
    val mulim:Bits = if(use_float) {
      if(float_point_format == 32) recFNFromFN(expWidth, sigWidth, java.lang.Float.floatToIntBits(sin(- Pi / 4).toFloat).S(float_point_format.W).asUInt)
      else if(float_point_format == 64)recFNFromFN(expWidth, sigWidth, java.lang.Double.doubleToRawLongBits(sin(-Pi / 4)).S(float_point_format.W).asUInt)
      else recFNFromFN(expWidth, sigWidth, floatConvert.float2HPF(sin(-Pi / 4).toFloat).S(32.W).asUInt()(15,0))
    } else FixedPoint.fromDouble(sin(- Pi / 4), FixDataWidth.W, BinaryPoint.BP)
    val mul1 = Cat(mulre,mulim).asTypeOf(if(use_float) new MyFloatComplex else new MyFixComplex)
    val temp_data = VecInit(Seq.fill(8)(0.S.asTypeOf(complex)))
    (0 until 4).map(x => temp_data(x) := ComplexAdd(io.in(x), io.in(x + 4)))
    (0 until 4).map(x => temp_data(4 + x) := ComplexSub(io.in(x), io.in(x + 4)))
    val A02 = ComplexAdd(temp_data(0), temp_data(2))
    val S02 = ComplexSub(temp_data(0), temp_data(2))
    val A13 = ComplexAdd(temp_data(1), temp_data(3))
    val S13 = ComplexSub(temp_data(1), temp_data(3))
    val A46 = ComplexAdd(temp_data(4), ComplexTran(temp_data(6)))
    val S46 = ComplexSub(temp_data(4), ComplexTran(temp_data(6)))
    val A57 = ComplexAdd(temp_data(5), ComplexTran(temp_data(7)))
    val S57 = ComplexSub(temp_data(5), ComplexTran(temp_data(7)))
    temp(0) := ComplexAdd(A02, A13)
    temp(1) := ComplexSub(A02, A13)
    temp(2) := ComplexAdd(S02, ComplexTran(S13))
    temp(3) := ComplexSub(S02, ComplexTran(S13))
    temp(4) := ComplexAdd(A46, ComplexMul(A57, mul1))
    temp(5) := ComplexSub(A46,ComplexMul(A57,mul1))
    temp(6) := ComplexAdd(S46,ComplexTran(ComplexMul(S57, mul1)))
    temp(7) := ComplexSub(S46,ComplexTran(ComplexMul(S57, mul1)))
  }
  if(radix == 2) {
    io.out(0) := temp(0)
    io.out(1) := ComplexMul(temp(1), io.wn(0))
  } else if (radix == 4) {
    io.out(0) := temp(0)
    io.out(1) := ComplexMul(temp(1), io.wn(1))
    io.out(2) := ComplexMul(temp(2), io.wn(0))
    io.out(3) := ComplexMul(temp(3), io.wn(2))
  } else if (radix == 8) {
    io.out(0) := temp(0)
    io.out(1) := ComplexMul(temp(1), io.wn(3))
    io.out(2) := ComplexMul(temp(2), io.wn(1))
    io.out(3) := ComplexMul(temp(3), io.wn(5))
    io.out(4) := ComplexMul(temp(4), io.wn(0))
    io.out(5) := ComplexMul(temp(5), io.wn(4))
    io.out(6) := ComplexMul(temp(6), io.wn(2))
    io.out(7) := ComplexMul(temp(7), io.wn(6))
  }
}
object ButterflyMul extends Config{
  def apply(in: Vec[MyComplex with Config], wn: List[MyComplex with Config]): Vec[MyComplex with Config] = {
    val complex = if(use_float) new MyFloatComplex else new MyFixComplex
    val inst = Module(new ButterflyMul(complex))
    inst.io.in := in
    inst.io.wn := wn
    inst.io.out
  }
}
class Switch[T <: MyComplex](delay: Int, complex:T) extends Module with Config {
  val io = IO(new Bundle{
    val in = Input(Vec(radix, complex))
    val out = Output(Vec(radix, complex))
    val sel = Input(UInt(log2Ceil(radix).W))
  })
  def VecShift(in: Vec[T], t: Int): Vec[T] = {
    val l_temp = in.toList
    val v_temp = (0 until t).map(x => l_temp(x + radix - t)).toList ::: (t until radix).map(x => l_temp(x - t)).toList
    VecInit(v_temp)
  }
  val swdata = VecInit(Seq.fill(radix)(0.S((2 * DataWidth).W).asTypeOf(complex)))
  (0 until radix).map(x => swdata(x) := ShiftRegister(io.in(x), x * delay))
  (0 until radix).map(x => io.out(x) := ShiftRegister(VecShift(swdata, x)(io.sel), (radix - 1 - x) * delay))
}
object Switch extends Config{
  def apply(in: Vec[MyComplex with Config], sel: UInt, delay:Int): Vec[MyComplex with Config] = {
    val complex = if(use_float) new MyFloatComplex else new MyFixComplex
    val inst = Module(new Switch(delay, complex))
    inst.io.in := in
    inst.io.sel := sel
    inst.io.out
  }
}
class Exchange[T <: MyComplex](complex:T) extends Module with Config {
  val io = IO(new Bundle {
    val in = Input(Vec(radix, Vec(radix, complex)))
    val out = Output(Vec(radix, Vec(radix, complex)))
  })
  for(i <- 0 until radix) {
    for(j <- 0 until radix) {
      io.out(i)(j) := io.in(j)(i)
    }
  }
}
object Exchange extends Config{
  def apply(in: List[Vec[MyComplex with Config]]): Vec[Vec[MyComplex with Config]] = {
    val complex = if(use_float) new MyFloatComplex else new MyFixComplex
    val inst = Module(new Exchange(complex))
    inst.io.in := in
    inst.io.out
  }
}