package FFT

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}
import chisel3.util._
import scala.math._
import chisel3._
import chisel3.stage.{ChiselStage, ChiselGeneratorAnnotation}

class Complex(val re: Double, val im: Double) {
  def +(rhs: Complex): Complex = new Complex(re + rhs.re, im + rhs.im)
  def -(rhs: Complex): Complex = new Complex(re - rhs.re, im - rhs.im)
  def *(rhs: Complex): Complex = new Complex(re * rhs.re - im * rhs.im, rhs.re * im + re * rhs.im)
  //def r = re

  def magnitude: Double = hypot(re, im)
  def phase: Double = atan2(im, re)

  override def toString: String = s"Complex($re, $im)"
}

class FFTTest(c:FFTtop) extends PeekPokeTester(c) with Config {
    def fft(x: Array[Complex]): Array[Complex] = {
      fft(x, 0, x.length, 1)
    }

    private def fft(x: Array[Complex], start: Int, n: Int, stride: Int) : Array[Complex] = {
      if (n == 1) {
        return Array(x(start))
      }
      val X = fft(x, start, n / 2, 2 * stride) ++ fft(x, start + stride, n / 2, 2 * stride)
      for (k <- 0 until n / 2) {
        val t = X(k)
        val arg = (-2 * Pi * k) / n
        val c = new Complex(cos(arg), sin(arg)) * X(k + n / 2)
        X(k) = t + c
        X(k + n / 2) = t - c
      }
      X
    }
    
    def range(a: Int, upBound: Int, downBound: Int): Int = {
      assert(upBound < 32)
      assert(downBound >= 0)
      return (a >> downBound) & (0xffffffff >>> (31 - upBound + downBound))
    }

    def reverse(a: Int, len: Int): Int = {
      var res: Int = 0
      for(i <- 0 until len) {
        res = res | range(a, i, i) << (len - 1 - i)
      }
      res
    }

    val r = new scala.util.Random
    var bound: Double = pow(2.0, BinaryPoint)
    var error : Double = 0
    var ovNum: Int = 0
    var iterNum: Int = 1

    for (t <- 0 until iterNum) {
      var a = new Array[Complex](FFTlength)
      var cnt = 0
      for (i <- 0 until datalength) {
        for (j <- 0 until FFTparallel_r) {
          for (k <- 0 until radix) {
            val aNum = k * (FFTlength / radix) + j * datalength + i
            val diNum = radix * j + k
            val re = -bound.toInt / 2 + r.nextInt(bound.toInt)
            val im = -bound.toInt / 2 + r.nextInt(bound.toInt)
            val reFlt16 = floatConvert.float2HPF((2*re / bound).toFloat)
            val imFlt16 = floatConvert.float2HPF((2*im / bound).toFloat)
            val reFlt32 = java.lang.Float.floatToIntBits((2*re / bound).toFloat)
            val imFlt32 = java.lang.Float.floatToIntBits((2*im / bound).toFloat)
            val reFlt64 = java.lang.Double.doubleToRawLongBits((2*re / bound).toFloat)
            val imFlt64 = java.lang.Double.doubleToRawLongBits((2*im / bound).toFloat)
            a(aNum) = new Complex(2 * re / bound, 2 * im / bound)
            if(use_float) {
              if (float_point_format == 32) {
                poke(c.io.dIn(diNum).re, reFlt32)
                poke(c.io.dIn(diNum).im, imFlt32)
              } else if (float_point_format == 64) {
                poke(c.io.dIn(diNum).re, reFlt64)
                poke(c.io.dIn(diNum).im, imFlt64)
              } else if (float_point_format == 16) {
                poke(c.io.dIn(diNum).re, reFlt16.S(16.W))
                poke(c.io.dIn(diNum).im, imFlt16.S(16.W))
              }
            }
            else {
              poke(c.io.dIn(diNum).re, re)
              poke(c.io.dIn(diNum).im, im)
            }
          }
        }
        if (i == 0) {
          poke(c.io.din_valid, 1)
        } else {
          poke(c.io.din_valid, 0)
        }
        step(1)
        cnt += 1
      }
      var ref = fft(a)
      
      step(FFTparallel)

      var errorOne: Double = 0
      var error1 : Double = 0
      var ovNum1: Int = 0
      var eps: Double = 1e-9
      if(do_reorder) {
        step(datalength)
        for(i <- 0 until FFTlength) {
          var ref1 = ref(i)
          var data = peek(c.io.dOut(0))
          if(use_float) {
            if(float_point_format == 32) {
              error1 = abs(((java.lang.Float.intBitsToFloat(data("re").toInt) - ref1.re) / (ref1.re + eps) + (java.lang.Float.intBitsToFloat(data("im").toInt) - ref1.im) / (ref1.im + eps)) / 2.0)
            } else if(float_point_format == 64) {
              error1 = abs(((java.lang.Double.longBitsToDouble(data("re").toLong) - ref1.re) / (ref1.re + eps) + (java.lang.Double.longBitsToDouble(data("im").toLong) - ref1.im) / (ref1.im + eps)) / 2.0)
            } else if(float_point_format == 16) {
              error1 = abs(((floatConvert.HPF2float(data("re").toInt) - ref1.re) / (ref1.re + eps) + (floatConvert.HPF2float(data("im").toInt) - ref1.im) / (ref1.im + eps)) / 2.0)
            }
          } else {
              error1 = abs((((2 * data("re").toDouble / bound) - ref1.re) / (ref1.re + eps) + ((2 * data("im").toDouble / bound) - ref1.im) / (ref1.im + eps)) / 2.0)
          }
          if (error1 <= 0.5) {
              errorOne += error1
          } else {
              ovNum1 += 1
          }
          step(1)
        }
      } else {
        for (i <- 0 until datalength) {
          for (j <- 0 until FFTparallel_r) {
            for (k <- 0 until radix) {
              var refNum = j * FFTlength / FFTparallel_r + k + radix * i
              var doNum = radix * j + k
              var ref1 = ref(reverse(refNum, log2Ceil(FFTlength)))
              var data = peek(c.io.dOut(doNum))
              if(use_float) {
                if(float_point_format == 32) {
                  error1 = abs(((java.lang.Float.intBitsToFloat(data("re").toInt) - ref1.re) / (ref1.re + eps) + (java.lang.Float.intBitsToFloat(data("im").toInt) - ref1.im) / (ref1.im + eps)) / 2.0)
                } else if(float_point_format == 64) {
                  error1 = abs(((java.lang.Double.longBitsToDouble(data("re").toLong) - ref1.re) / (ref1.re + eps) + (java.lang.Double.longBitsToDouble(data("im").toLong) - ref1.im) / (ref1.im + eps)) / 2.0)
                } else if(float_point_format == 16) {
                  error1 = abs(((floatConvert.HPF2float(data("re").toInt) - ref1.re) / (ref1.re + eps) + (floatConvert.HPF2float(data("im").toInt) - ref1.im) / (ref1.im + eps)) / 2.0)
                }
              } else {
                  error1 = abs((((2 * data("re").toDouble / bound) - ref1.re) / (ref1.re + eps) + ((2 * data("im").toDouble / bound) - ref1.im) / (ref1.im + eps)) / 2.0)
              }
              if (error1 <= 0.5) {
                  errorOne += error1
              } else {
                  ovNum1 += 1
              }
            }
          }
          step(1)
        }
      }
      errorOne = errorOne / (FFTlength - ovNum1)
      ovNum += ovNum1
      error += errorOne
      var errorOnePercent = errorOne * 100
      println(f"in this sample, Error rate: $errorOnePercent%.2f | number of ovs : $ovNum1%d")
      //printf("In this sample, Error rate: %.2f%% | number of ovs : %d\n", errorOnePercent, ovNum1)
    }
    error /= iterNum
    print("Total error rate is: " + error * 100 + "%\n")
    print(ovNum + " of " + iterNum * FFTlength + "overflowed! " + "the overflow ratio is " + 100 * ovNum / (FFTlength * iterNum).toDouble + "%" + "\n")
  //  print(s"${datalength + FFTparallel} \n")
  }

object FFTTestMain extends App {
  iotesters.Driver.execute(args, () => new FFTtop) {
    c => new FFTTest(c)
  }
}

