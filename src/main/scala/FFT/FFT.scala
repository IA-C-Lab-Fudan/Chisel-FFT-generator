package FFT

import chisel3._
import chisel3.experimental._
import chisel3.util._
import hardfloat._
import scala.math._

class TopIO extends Bundle with Config {
  val dIn = Input(Vec(FFTparallel_r * radix, if(use_float) new IEEEComplex else new MyFixComplex))
  val dOut = Output(Vec(FFTparallel_r * radix, if(use_float) new IEEEComplex else new MyFixComplex))
  val din_valid = Input(new Bool())
  val dout_valid = Output(new Bool())
  val busy = Output(new Bool())
}

object floatConvert {
  def float2HPF(data:Float):Int = {
    val intBits = java.lang.Float.floatToIntBits(data)
    var resultBits = ((intBits & 0xc0000000) >> 16) + ((intBits & 0x07ffe000) >> 13)
    if((intBits & 0x40000000) == 0) {
      if(((intBits & 0x38000000) != 0x38000000)) {
        //println(s"the float32 to half-precision float is negative overloaded\n")
        //println(s"the float is ${data}")
        //println(s"the float format is ${intBits.toHexString}")
        resultBits = 0
      }
    } else {
      if(((intBits & 0x38000000) != 0) | ((intBits & 0x07800000) == 0x07800000)) {
        println(s"the float32 to half-precision float is positive overloaded")
        println(s"the float is ${data}")
        println(s"the float format is ${intBits.toHexString}")
      }
    }
    //println(s"input is ${intBits.toHexString}\n")
    //println(s"first part is ${((intBits & 0xc0000000) >> 16).toHexString}")
    //println(s"second part is ${((intBits & 0x07ffe000) >> 13).toHexString}")
    //println(s"half is ${(((intBits & 0xc0000000) >> 16) + ((intBits & 0x07ffe000) >> 13)).toHexString}")
    resultBits
  }

  def HPF2float(data:Int):Float = {
    var temp:Int = 0
    if((data & 0x4000) == 0) {
      temp = (((data & 0xc000).toLong << 16) + ((0x7) << 27) + ((data & 0x3fff) << 13)).toInt
    } else {
      temp = (((data & 0xc000).toLong << 16) + ((data & 0x3fff) << 13)).toInt
    }
    //println(s"first part is ${((data & 0xc000) << 16).toHexString}")
    //println(s"second part is ${((data & 0x3fff) << 13).toHexString}")
    //println(s"third part is ${((0x7) << 27).toHexString}")
    //println(s"half is ${(temp).toHexString}")
    java.lang.Float.intBitsToFloat(temp)
  }
}

class FFT extends Module with Config{
  val complex = if(use_float) new MyFloatComplex else new MyFixComplex
  val io = IO( new TopIO)

//design wntable for every BF
  def sinTableFix(s: Int, n: Int): Vec[FixedPoint] = {
    val times = (0 until FFTlength / radix by pow(radix, s).toInt).map(i => -(2 * n * i * Pi) / FFTlength.toDouble)
    VecInit(times.map(i => FixedPoint.fromDouble(sin(i), FixDataWidth.W, BinaryPoint.BP)))
  }
  def cosTableFix(s: Int, n: Int): Vec[FixedPoint] = {
    val times = (0 until FFTlength / radix by pow(radix, s).toInt).map(i => -(2 * n * i * Pi) / FFTlength.toDouble)
    VecInit(times.map(i => FixedPoint.fromDouble(cos(i), FixDataWidth.W, BinaryPoint.BP)))
  }
  def wnTableFix(stage: Int, stride: Int)(idx: UInt): MyFixComplex = {
    val res = Wire(new MyFixComplex())
    res.re := cosTableFix(stage, stride)(idx)
    res.im := sinTableFix(stage, stride)(idx)
    res
  }

  def sinTableFlt(s: Int, n: Int): Vec[UInt] = {
    val times = (0 until FFTlength / radix by pow(radix, s).toInt).map(i => -(2 * n * i * Pi) / FFTlength.toDouble)
    var temp = Seq[UInt]()
    if (float_point_format == 32) {
      temp = times.map(i => recFNFromFN(expWidth, sigWidth, java.lang.Float.floatToIntBits(sin(i).toFloat).S(float_point_format.W).asUInt()))
    } else if(float_point_format == 64) {
      temp = times.map(i => recFNFromFN(expWidth, sigWidth, java.lang.Double.doubleToRawLongBits(sin(i)).S(float_point_format.W).asUInt()))
    } else if(float_point_format == 16) {
      temp = times.map(i => recFNFromFN(expWidth, sigWidth, floatConvert.float2HPF(sin(i).toFloat).S(16.W).asUInt()))
    }
    //println(s"float is ${java.lang.Float.floatToIntBits(2.3.toFloat)}\n the half is ${floatConvert.float2HPF(2.3.toFloat)}")
    VecInit(temp)
  }
  def cosTableFlt(s: Int, n: Int): Vec[UInt] = {
    val times = (0 until FFTlength / radix by pow(radix, s).toInt).map(i => -(2 * n * i * Pi) / FFTlength.toDouble)
    var temp = Seq[UInt]()
    if(float_point_format == 32) {
      temp = times.map(i => recFNFromFN(expWidth, sigWidth, java.lang.Float.floatToIntBits(cos(i).toFloat).S(float_point_format.W).asUInt()))
    } else if(float_point_format == 64){
      temp = times.map(i => recFNFromFN(expWidth, sigWidth, java.lang.Double.doubleToRawLongBits(cos(i)).S(float_point_format.W).asUInt()))
    } else if(float_point_format == 16) {
      temp = times.map(i => recFNFromFN(expWidth, sigWidth, floatConvert.float2HPF(cos(i).toFloat).S(16.W).asUInt()))
    }
    VecInit(temp)
  }
  def wnTableFlt(stage: Int, stride: Int)(idx: UInt): MyFloatComplex = {
    val res = Wire(new MyFloatComplex())
    res.re := cosTableFlt(stage, stride)(idx)
    res.im := sinTableFlt(stage, stride)(idx)
    res
  }
//cnt design
  val cntNum = log2Ceil(radix) * (FFTstage - FFTparallel)
  val cnt = Seq.fill(FFTparallel + 1)(RegInit(0.U((cntNum + 1).W)))
  for(i <- 0 until FFTparallel) {
    cnt(i + 1) := cnt(i)
  }
  val busy = cnt(0) =/= 0.U
  when(io.din_valid || busy) {
    cnt(0) := Mux(cnt(0) === (FFTparallel + 2 * datalength - 1).asUInt(), 0.U, cnt(0) + 1.U)
  }

//data define:
//data1 is the input of this stages' BF ; data2 is the output
//FFTstage means which stages' BF; FFTparallel_r means the number of the BF; radix means the input amount
  val data1 = Seq.fill(FFTstage)(Seq.fill(FFTparallel_r)(VecInit(Seq.fill(radix)(0.S((2 * DataWidth).W).asTypeOf(complex)))))
  val data2 = Seq.fill(FFTstage - 1)(Seq.fill(FFTparallel_r)(VecInit(Seq.fill(radix)(0.S((2 * DataWidth).W).asTypeOf(complex)))))
  val datao = VecInit(Seq.fill(radix * FFTparallel_r)(0.S((2 * DataWidth).W).asTypeOf(complex)))

//data calcute flow: data1 => data2
  for (i <- 0 until FFTstage - 1) {
    val wnCtrl = if(i < FFTparallel) {
      cnt(i)
    } else {
      cnt(FFTparallel)((log2Ceil(radix) * (FFTstage - i - 1) - 1),0)
    }
    val startlist = (0 until FFTparallel_r).map(x => ((x % ceil(pow(radix, (FFTparallel - i)).toInt)) * datalength).toInt)
    for(x <- 0 until FFTparallel_r) {
     val wnList =  (1 until radix).map(y => if(use_float) wnTableFlt(i, y)(startlist(x).asUInt + wnCtrl) else wnTableFix(i, y)(startlist(x).asUInt + wnCtrl)).toList
      data2(i)(x) := ButterflyMul(data1(i)(x), wnList)
    }
  }

  val dataotemp = (0 until FFTparallel_r).map(x => ButterflyAdd(data1(FFTstage - 1)(x)))
  datao := dataotemp.flatten

//data transport flow: data2 => data1
  val extimes = FFTparallel_r / radix
  def exlist(stage: Int): List[List[Int]] ={
    val group = pow(radix, stage).toInt
    val length = FFTparallel_r / group
    val stride = pow(radix, FFTparallel - stage - 1).toInt
    val listtemp = (0 until group).map(x => (0 until extimes/group).map(y => (0 until radix).map(z => x * length + y + z * stride).toList).toList).toList
    val list = listtemp.reduce((x,y) => x ++ y)
    list
  }
  for (i <- 0 until FFTparallel) {
    val list = exlist(i)
    for (j <- 0 until extimes) {
      val exdata = Exchange((0 until radix).map(x => data2(i)(list(j)(x))).toList)
      (0 until radix).map(x => data1(i + 1)(list(j)(x)) := RegNext(exdata(x)))
    }
  }

  for (i <- FFTparallel until FFTstage -1) {
    val delay = FFTlength / pow(radix, i + 2).toInt
    val sel = cnt(FFTparallel)((log2Ceil(radix) * (FFTstage - i - 1) - 1),log2Ceil(radix) * (FFTstage - i - 2))
    (0 until FFTparallel_r).map(x => data1(i + 1)(x) := Switch(data2(i)(x), sel, delay))
  }

  //input and output
  if(use_float) {
    for (i <-0 until FFTparallel_r) {
      data1(0)(i) := (0 until radix).map(x => ComplexRecode(io.dIn(radix * i + x).asTypeOf(new IEEEComplex)))
    }
  }
  else {
    for (i <-0 until FFTparallel_r) {
      data1(0)(i) := (0 until radix).map(x => io.dIn(radix * i + x))
    }
  }
  io.busy := busy
  io.dout_valid := RegNext(cnt(0)) === (FFTparallel + datalength - 1).asUInt()
  if(use_float) {
    io.dOut := (0 until radix * FFTparallel_r).map(i => RegNext(ComplexDecode(datao(i).asTypeOf(new MyFloatComplex))))
  }
  else {
    io.dOut := RegNext(datao)
  }
}