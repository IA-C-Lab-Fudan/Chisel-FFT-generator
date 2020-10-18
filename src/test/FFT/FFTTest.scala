

package FFT

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import chisel3.util._

class Complex(val re: Double, val im: Double) {
  def +(rhs: Complex): Complex = new Complex(re + rhs.re, im + rhs.im)
  def -(rhs: Complex): Complex = new Complex(re - rhs.re, im - rhs.im)
  def *(rhs: Complex): Complex = new Complex(re * rhs.re - im * rhs.im, rhs.re * im + re * rhs.im)
  //def r = re

  def magnitude: Double = Math.hypot(re, im)
  def phase: Double = Math.atan2(im, re)

  override def toString: String = s"Complex($re, $im)"
}

// only support FFT
class FFTTest(c:FFT) extends PeekPokeTester(c)
  with HasDataConfig
  with HasElaborateConfig {
  require(!supportIFFT)
  def fft(x: Array[Complex]): Array[Complex] = {
    require(x.length > 0 && (x.length & (x.length - 1)) == 0, "array size should be power of two")
    fft(x, 0, x.length, 1)
  }

  def fft(x: Array[Double]): Array[Complex] = fft(x.map(re => new Complex(re, 0.0)))
  def rfft(x: Array[Double]): Array[Complex] = fft(x).take(x.length / 2 + 1)

  private def fft(x: Array[Complex], start: Int, n: Int, stride: Int) : Array[Complex] = {
    if (n == 1) {
      return Array(x(start))
    }

    val X = fft(x, start, n / 2, 2 * stride) ++ fft(x, start + stride, n / 2, 2 * stride)

    for (k <- 0 until n / 2) {
      val t = X(k)
      val arg = -2 * math.Pi * k / n
      val c = new Complex(math.cos(arg), math.sin(arg)) * X(k + n / 2)
      X(k) = t + c
      X(k + n / 2) = t - c
    }
    X
  }

  def range(a: Int, upBound: Int, downBound: Int) : Int = {
    assert(upBound < 32)
    assert(downBound >= 0)
    return (a >> downBound) & (0xffffffff >>> (31 - upBound + downBound))
  }

  def reverse(a: Int, len: Int): Int = {
    var res: Int = 0
    for(i <- 0 until len) {
      res = res | range(a, i, i) << (len-1-i)
    }
    res
  }

  val r = new scala.util.Random
  var bound: Double = math.pow(2.0, BinaryPoint)
  var error: Double = 0
  var ovNum: Int = 0
  var iterNum: Int = 100

  for (t <- 0 until iterNum) {
    var a = new Array[Complex](FFTLength)
    var cnt = 0
    for (i <- 0 until FFTLength) {
      var re = -bound.toInt / 2 + r.nextInt(bound.toInt)
      var im = -bound.toInt / 2 + r.nextInt(bound.toInt)
      a(cnt) = new Complex(2 * re / bound, 2 * im / bound)
      poke(c.io.dIn.re, re)
      poke(c.io.dIn.im, im)
      poke(c.io.din_valid, 1)
      step(1)
      cnt += 1
    }
    var ref = fft(a)

    var errorOne: Double = 0
    var error1: Double = 0
    var ovNum1: Int = 0
    var eps: Double = 1e-9
    for (i <- 0 until FFTLength / 2) {
      var ref1 = ref(reverse(i * 2, log2Ceil(FFTLength)))
      var d1 = peek(c.io.dOut1)
      error1 = math.abs((((2 * d1("re").toDouble / bound) - ref1.re) / (ref1.re + eps) + ((2 * d1("im").toDouble / bound) - ref1.im) / (ref1.im + eps)) / 2.0)
      if (error1 <= 0.5) {
        errorOne += error1
      } else {
        ovNum1 += 1
      }
      var ref2 = ref(reverse(i * 2 + 1, log2Ceil(FFTLength)))
      var d2 = peek(c.io.dOut2)
      error1 = math.abs((((2 * d2("re").toDouble / bound) - ref2.re) / (ref2.re + eps) + ((2 * d2("im").toDouble / bound) - ref2.im) / (ref2.im + eps)) / 2.0)
      if (error1 <= 0.5) {
        errorOne += error1
      } else {
        ovNum1 += 1
      }
      step(1)
    }
    errorOne = errorOne / (FFTLength - ovNum1)
    ovNum += ovNum1
    error += errorOne
    var errorOnePercent = errorOne*100
    printf("In this sample, Error rate: %.2f%% | number of ovs: %d\n", errorOnePercent, ovNum1)
  }
  error /= iterNum
  print("Total error rate is: " + error*100 + "%\n")
  print(ovNum + " of " + iterNum * FFTLength + " overflowed! " + "The overlow ratio is " + 100 * ovNum / (FFTLength * iterNum).toDouble  + "%" + "\n")
}

// support FFT / IFFT
class FFTTest2(c:FFT) extends PeekPokeTester(c)
  with HasDataConfig
  with HasElaborateConfig {
  require(supportIFFT)
  def fft(x: Array[Complex]): Array[Complex] = {
    require(x.length > 0 && (x.length & (x.length - 1)) == 0, "array size should be power of two")
    fft(x, 0, x.length, 1)
  }

  def fft(x: Array[Double]): Array[Complex] = fft(x.map(re => new Complex(re, 0.0)))
  def rfft(x: Array[Double]): Array[Complex] = fft(x).take(x.length / 2 + 1)

  private def fft(x: Array[Complex], start: Int, n: Int, stride: Int) : Array[Complex] = {
    if (n == 1) {
      return Array(x(start))
    }

    val X = fft(x, start, n / 2, 2 * stride) ++ fft(x, start + stride, n / 2, 2 * stride)

    for (k <- 0 until n / 2) {
      val t = X(k)
      val arg = -2 * math.Pi * k / n
      val c = new Complex(math.cos(arg), math.sin(arg)) * X(k + n / 2)
      X(k) = t + c
      X(k + n / 2) = t - c
    }
    X
  }

  def ifft(x: Array[Complex]): Array[Complex] = {
    val n = x.length
    val res = new Array[Complex](n)
    for (i <- 0 until n) {
      res(i) = new Complex(0, 0)
    }
    for (i <- 0 until n) {
      for (j <- 0 until n) {
        val arg = 2 * math.Pi * j * i / n
        res(i) += new Complex(math.cos(arg), math.sin(arg)) * x(j)
      }
      res(i) = new Complex(1.0 / n, 0) * res(i)
    }
    res
  }

  def range(a: Int, upBound: Int, downBound: Int) : Int = {
    assert(upBound < 32)
    assert(downBound >= 0)
    return (a >> downBound) & (0xffffffff >>> (31 - upBound + downBound))
  }

  def reverse(a: Int, len: Int): Int = {
    var res: Int = 0
    for(i <- 0 until len) {
      res = res | range(a, i, i) << (len-1-i)
    }
    res
  }

  val r = new scala.util.Random
  var bound: Double = math.pow(2.0, BinaryPoint)
  var error: Double = 0
  var ovNum: Int = 0
  var iterNum: Int = 100

  for (t <- 0 until iterNum) {
    var a = new Array[Complex](FFTLength)
    var cnt = 0
    for (i <- 0 until FFTLength) {
      var re = -bound.toInt / 2 + r.nextInt(bound.toInt)
      var im = -bound.toInt / 2 + r.nextInt(bound.toInt)
      a(cnt) = new Complex(2 * re / bound, 2 * im / bound)
      poke(c.io.dIn.re, re)
      poke(c.io.dIn.im, im)
      poke(c.io.din_valid, 1)
      poke(c.io.mode.get, 0)
      step(1)
      cnt += 1
    }
    var ref = fft(a)

    var errorOne: Double = 0
    var error1: Double = 0
    var ovNum1: Int = 0
    var eps: Double = 1e-9
    for (i <- 0 until FFTLength / 2) {
      var ref1 = ref(reverse(i * 2, log2Ceil(FFTLength)))
      var d1 = peek(c.io.dOut1)
      error1 = math.abs((((2 * d1("re").toDouble / bound) - ref1.re) / (ref1.re + eps) + ((2 * d1("im").toDouble / bound) - ref1.im) / (ref1.im + eps)) / 2.0)
      if (error1 <= 0.5) {
        errorOne += error1
      } else {
        ovNum1 += 1
      }
      var ref2 = ref(reverse(i * 2 + 1, log2Ceil(FFTLength)))
      var d2 = peek(c.io.dOut2)
      error1 = math.abs((((2 * d2("re").toDouble / bound) - ref2.re) / (ref2.re + eps) + ((2 * d2("im").toDouble / bound) - ref2.im) / (ref2.im + eps)) / 2.0)
      if (error1 <= 0.5) {
        errorOne += error1
      } else {
        ovNum1 += 1
      }
      step(1)
    }
    errorOne = errorOne / (FFTLength - ovNum1)
    ovNum += ovNum1
    error += errorOne
    var errorOnePercent = errorOne*100
    printf("In this FFT sample, Error rate: %.2f%% | number of ovs: %d\n", errorOnePercent, ovNum1)
  }
  error /= iterNum
  print("Total error rate in FFT is: " + error*100 + "%\n")
  print(ovNum + " of " + iterNum * FFTLength + " overflowed! " + "The overlow ratio is " + 100 * ovNum / (FFTLength * iterNum).toDouble  + "%" + "\n")


  error = 0
  ovNum = 0
  for (t <- 0 until iterNum) {
    var a = new Array[Complex](FFTLength)
    var cnt = 0
    for (i <- 0 until FFTLength) {
      var re = -bound.toInt / 2 + r.nextInt(bound.toInt)
      var im = -bound.toInt / 2 + r.nextInt(bound.toInt)
      a(cnt) = new Complex(2 * re / bound, 2 * im / bound)
      poke(c.io.dIn.re, re)
      poke(c.io.dIn.im, im)
      poke(c.io.din_valid, 1)
      poke(c.io.mode.get, 1)
      step(1)
      cnt += 1
    }
    var ref = ifft(a)

    var errorOne: Double = 0
    var error1: Double = 0
    var ovNum1: Int = 0
    var eps: Double = 1e-9
    for (i <- 0 until FFTLength / 2) {
      var ref1 = ref(reverse(i * 2, log2Ceil(FFTLength)))
      var d1 = peek(c.io.dOut1)
      error1 = math.abs((((2 * d1("re").toDouble / bound) - ref1.re) / (ref1.re + eps) + ((2 * d1("im").toDouble / bound) - ref1.im) / (ref1.im + eps)) / 2.0)
      if (error1 <= 0.5) {
        errorOne += error1
      } else {
        ovNum1 += 1
      }
      var ref2 = ref(reverse(i * 2 + 1, log2Ceil(FFTLength)))
      var d2 = peek(c.io.dOut2)
      error1 = math.abs((((2 * d2("re").toDouble / bound) - ref2.re) / (ref2.re + eps) + ((2 * d2("im").toDouble / bound) - ref2.im) / (ref2.im + eps)) / 2.0)
      if (error1 <= 0.5) {
        errorOne += error1
      } else {
        ovNum1 += 1
      }
      step(1)
    }
    errorOne = errorOne / (FFTLength - ovNum1)
    ovNum += ovNum1
    error += errorOne
    var errorOnePercent = errorOne*100
    printf("In this IFFT sample, Error rate: %.2f%% | number of ovs: %d\n", errorOnePercent, ovNum1)
  }
  error /= iterNum
  print("Total error rate in IFFT is: " + error*100 + "%\n")
  print(ovNum + " of " + iterNum * FFTLength + " overflowed! " + "The overlow ratio is " + 100 * ovNum / (FFTLength * iterNum).toDouble  + "%" + "\n")
}

// only support FFTtop
class FFTTest3(c:FFTReorder) extends PeekPokeTester(c)
  with HasDataConfig
  with HasElaborateConfig {
  require(!supportIFFT)
  def fft(x: Array[Complex]): Array[Complex] = {
    require(x.length > 0 && (x.length & (x.length - 1)) == 0, "array size should be power of two")
    fft(x, 0, x.length, 1)
  }

  def fft(x: Array[Double]): Array[Complex] = fft(x.map(re => new Complex(re, 0.0)))
  def rfft(x: Array[Double]): Array[Complex] = fft(x).take(x.length / 2 + 1)

  private def fft(x: Array[Complex], start: Int, n: Int, stride: Int) : Array[Complex] = {
    if (n == 1) {
      return Array(x(start))
    }

    val X = fft(x, start, n / 2, 2 * stride) ++ fft(x, start + stride, n / 2, 2 * stride)

    for (k <- 0 until n / 2) {
      val t = X(k)
      val arg = -2 * math.Pi * k / n
      val c = new Complex(math.cos(arg), math.sin(arg)) * X(k + n / 2)
      X(k) = t + c
      X(k + n / 2) = t - c
    }
    X
  }

  def range(a: Int, upBound: Int, downBound: Int) : Int = {
    assert(upBound < 32)
    assert(downBound >= 0)
    return (a >> downBound) & (0xffffffff >>> (31 - upBound + downBound))
  }

  def reverse(a: Int, len: Int): Int = {
    var res: Int = 0
    for(i <- 0 until len) {
      res = res | range(a, i, i) << (len-1-i)
    }
    res
  }

  val r = new scala.util.Random
  var bound: Double = math.pow(2.0, BinaryPoint)
  var error: Double = 0
  var ovNum: Int = 0

    var a = new Array[Complex](FFTLength)
    var cnt = 0
    for (i <- 0 until FFTLength) {
      var re = -bound.toInt / 2 + r.nextInt(bound.toInt)
      var im = -bound.toInt / 2 + r.nextInt(bound.toInt)
      a(cnt) = new Complex(2 * re / bound, 2 * im / bound)
      poke(c.io.dIn.re, re)
      poke(c.io.dIn.im, im)
      poke(c.io.din_valid, 1)
      step(1)
      cnt += 1
    }
    var ref = fft(a)

    var errorOne: Double = 0
    var error1: Double = 0
    var ovNum1: Int = 0
    var eps: Double = 1e-9
    step(FFTLength / 2)
    for (i <- 0 until FFTLength) {
      var ref1 = ref(i)
      var d1 = peek(c.io.dOut)
      error1 = math.abs((((2 * d1("re").toDouble / bound) - ref1.re) / (ref1.re + eps) + ((2 * d1("im").toDouble / bound) - ref1.im) / (ref1.im + eps)) / 2.0)
      if (error1 <= 0.5) {
        errorOne += error1
      } else {
        ovNum1 += 1
      }
      step(1)
    }
    errorOne = errorOne / (FFTLength - ovNum1)
    ovNum += ovNum1
    error += errorOne
    var errorOnePercent = errorOne*100
    printf("In this sample, Error rate: %.2f%% | number of ovs: %d\n", errorOnePercent, ovNum1)
}

object FFTTestMain extends App {
  iotesters.Driver.execute(args, () => new FFT) {
    c => new FFTTest(c)
  }
}
object FFTTestMain2 extends App {
  iotesters.Driver.execute(args, () => new FFT) {
    c => new FFTTest2(c)
  }
}
object FFTTestMain3 extends App {
  iotesters.Driver.execute(Array("--backend-name", "verilator"), () => new FFTReorder) {
    c => new FFTTest3(c)
  }
}
