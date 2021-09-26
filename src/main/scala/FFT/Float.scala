package FFT

import chisel3._
import hardfloat._

class FloatOperationIO extends Bundle with Config {
  val op1 = Input(UInt((expWidth + sigWidth + 1).W))
  val op2 = Input(UInt((expWidth + sigWidth + 1).W))
  val res = Output(UInt((expWidth + sigWidth + 1).W))
}

class FloatAdd extends Module with Config {
  val io = IO(new FloatOperationIO)
  val float_adder = Module(new AddRecFN(expWidth, sigWidth))
  float_adder.io.subOp := false.B
  float_adder.io.a := io.op1
  float_adder.io.b := io.op2
  float_adder.io.roundingMode := 0.U
  float_adder.io.detectTininess := false.B
  io.res := float_adder.io.out
  val exceptionFlags = float_adder.io.exceptionFlags
}
object FloatAdd {
  def apply(op1: UInt, op2: UInt):UInt = {
    val inst = Module(new FloatAdd)
    inst.io.op1 := op1
    inst.io.op2 := op2
    inst.io.res
  }
}

class FloatSub extends Module with Config {
  val io = IO(new FloatOperationIO)
  val float_adder = Module(new AddRecFN(expWidth, sigWidth))
  float_adder.io.subOp := true.B
  float_adder.io.a := io.op1
  float_adder.io.b := io.op2
  float_adder.io.roundingMode := 0.U
  float_adder.io.detectTininess := false.B
  io.res := float_adder.io.out
  val exceptionFlags = float_adder.io.exceptionFlags
}
object FloatSub {
  def apply(op1: UInt, op2: UInt):UInt = {
    val inst = Module(new FloatSub)
    inst.io.op1 := op1
    inst.io.op2 := op2
    inst.io.res
  }
}

class FloatMul extends Module with Config {
  val io = IO(new FloatOperationIO)
  val float_adder = Module(new MulRecFN(expWidth, sigWidth))
  float_adder.io.a := io.op1
  float_adder.io.b := io.op2
  float_adder.io.roundingMode := 0.U
  float_adder.io.detectTininess := false.B
  io.res := float_adder.io.out
  val exceptionFlags = float_adder.io.exceptionFlags
}
object FloatMul {
  def apply(op1: UInt, op2: UInt):UInt = {
    val inst = Module(new FloatMul)
    inst.io.op1 := op1
    inst.io.op2 := op2
    inst.io.res
  }
}
