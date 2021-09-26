Chisel FFT-Generator
=======================
This Chisel project implements a R2MDC (Radix-2 Multipath Delay Commutator) FFT hardware design, and the source and test files are organized as follows.
```
src/
  main/
    scala/
      FFT/ <Chisel的源代码文件>
        Modules.scala
        Config.scala
        FFT.scala
        Float.scala
        generator.scala
        reorder.scala
        Top.scala
  test/
    scala/
      FFT/ <Chisel的测试代码文件>
        FFTTest.scala
```

How to generate verilog
-----
We use generator.scala to handle all the generations of Verilog code.

What's new
------
We provide more configurations and parameters to achieve flexible configuration of the structure to meet the needs of different application scenarios.
All config is below:
```
float_point_format --> the data format of floatpoint data, support 16, 32, 64

use_float --> true: floatpoint data ; false: fixedpoint data

(FixDataWidth, BinaryPoint) --> the data format of fixedpoint data

radix --> radix of FFT, support 2, 4, 8

FFTstage --> FFTlength is pow(radix, FFTstage)

FFTparallel --> the number of parallel pipelines is pow(radix, FFTparallel)

useGauss --> true: use Gauss multiplier; false: use common multiplier

do_reorder --> true: serial and in-order output; false: parallel but out-of-order output

```
