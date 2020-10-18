
package FFT

import chisel3._
import chisel3.experimental._

class MyComplex extends Bundle
  with HasDataConfig {
  val re = FixedPoint(DataWidth.W, BinaryPoint.BP)
  val im = FixedPoint(DataWidth.W, BinaryPoint.BP)
}

class MyFixedPoint extends Bundle
  with HasDataConfig {
  val value = FixedPoint(DataWidth.W, BinaryPoint.BP)
}

class EnergyOperationIO extends Bundle {
  val op1 = Input(new MyComplex())
  val op2= Input(new MyComplex())
  val res = Output(new MyFixedPoint())
}

class ComplexOperationIO extends Bundle {
  val op1 = Input(new MyComplex())
  val op2= Input(new MyComplex())
  val res = Output(new MyComplex())
}

class ComplexAdd extends Module {
  val io = IO(new ComplexOperationIO)
  io.res.re := io.op1.re + io.op2.re
  io.res.im := io.op1.im + io.op2.im
}
object ComplexAdd {
  def apply(op1: MyComplex, op2: MyComplex):MyComplex = {
    val inst = Module(new ComplexAdd)
    inst.io.op1 := op1
    inst.io.op2 := op2
    inst.io.res
  }
}

class ComplexSub extends Module {
  val io = IO(new ComplexOperationIO)
  io.res.re := io.op1.re - io.op2.re
  io.res.im := io.op1.im - io.op2.im
}
object ComplexSub {
  def apply(op1: MyComplex, op2: MyComplex):MyComplex = {
    val inst = Module(new ComplexSub)
    inst.io.op1 := op1
    inst.io.op2 := op2
    inst.io.res
  }
}


class ComplexEnergy extends Module{
  val io = IO(new EnergyOperationIO)
  io.res.value := io.op1.re * io.op2.re + io.op1.im * io.op2.im
}
object ComplexEnergy{
  def apply(op1: MyComplex, op2: MyComplex):MyFixedPoint = {
    val inst = Module(new ComplexEnergy)
    inst.io.op1 := op1
    inst.io.op2 := op2
    inst.io.res
  }
}



class ComplexMul extends Module
  with HasElaborateConfig {
  val io = IO(new ComplexOperationIO)
  if (useGauss) {
    val k1 = io.op2.re * (io.op1.re + io.op1.im)
    val k2 = io.op1.re * (io.op2.im - io.op2.re)
    val k3 = io.op1.im * (io.op2.re + io.op2.im)
    io.res.re := k1 - k3
    io.res.im := k1 + k2
  } else {
    io.res.re := io.op1.re * io.op2.re - io.op1.im * io.op2.im
    io.res.im := io.op1.re * io.op2.im + io.op1.im * io.op2.re
  }
}
object ComplexMul {
  def apply(op1: MyComplex, op2: MyComplex):MyComplex = {
    val inst = Module(new ComplexMul)
    inst.io.op1 := op1
    inst.io.op2 := op2
    inst.io.res
  }
}

class ButterflyIO extends Bundle
  with HasDataConfig {
  val in1 = Input(new MyComplex())
  val in2 = Input(new MyComplex())
  val wn = Input(new MyComplex())
  val out1 = Output(new MyComplex())
  val out2 = Output(new MyComplex())
}

class Butterfly extends Module {
  val io = IO(new ButterflyIO())
  val add1 = ComplexAdd(io.in1, io.in2)
  val sub2 = ComplexSub(io.in1, io.in2)
  val mul2 = ComplexMul(sub2, io.wn)
  io.out1 := add1
  io.out2 := mul2
}
object Butterfly {
  def apply(in1: MyComplex, in2: MyComplex, wn: MyComplex): (MyComplex, MyComplex) = {
    val inst = Module(new Butterfly)
    inst.io.in1 := in1
    inst.io.in2 := in2
    inst.io.wn := wn
    (inst.io.out1, inst.io.out2)
  }
}

class Switch extends Module {
  val io = IO(new Bundle{
    val in1 = Input(new MyComplex)
    val in2 = Input(new MyComplex)
    val sel = Input(Bool())
    val out1 = Output(new MyComplex)
    val out2 = Output(new MyComplex)
  })
  io.out1 := Mux(io.sel, io.in2, io.in1)
  io.out2 := Mux(io.sel, io.in1, io.in2)
}
object Switch {
  def apply(in1: MyComplex, in2: MyComplex, sel: Bool): (MyComplex, MyComplex) = {
    val inst = Module(new Switch)
    inst.io.in1 := in1
    inst.io.in2 := in2
    inst.io.sel := sel
    (inst.io.out1, inst.io.out2)
  }
}