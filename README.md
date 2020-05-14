# Chisel-FFT
*NOTE: The branch **BRAM** contains codes using BRAM explicitely in FPGA flow.*

This Chisel project implements a [R2MDC](https://ieeexplore.ieee.org/document/7421167) (Radix-2 Multipath Delay Commutator) FFT hardware design, and the source and test files are organized as follows.

```
src/
  main/
    scala/
      FFT/ <Chisel的源代码文件>
        Butterfly.scala
        Config.scala
        FFT.scala
        VerilogEmitter.scala
  test/
    scala/
      FFT/ <Chisel的测试代码文件>
        FFTTest.scala
```

The FPGA folder contains the tcl script and other necessary files for the FPGA flow running on Vivado 2019.2

## How to generate Verilog ?

We designed *VerilogEmitter.scala* to handle all the generations of Verilog code.

In IntelliJ, you can click the green button near the *elaborateFFT*  object defined in *VerilogEmitter.scala* to run this object and generate Verilog codes.

The object *elaborateFFT*  is defined as follows, and some arguments (like the destination directory, etc) could be configured in this process.

```
object elaborateFFT extends App {
  (new chisel3.stage.ChiselStage).execute(
    Array("-X", "verilog", "--full-stacktrace"),
    Seq(ChiselGeneratorAnnotation(() => new FFT()),
      TargetDirAnnotation("Verilog"))
  )
}
```

Or you can run the following command in your terminal.

```
 sbt 'test:runMain FFT.elaborateFFT'
```

## How to run the test ?

The testbench is defined in *FFTTest.scala*, and you could run the object *FFTTestMain* like what we did before to get the simulation results (like the error rate and the overflow rate, etc).

The object *FFTTestMain* is defined as follows.

```
object FFTTestMain extends App {
  iotesters.Driver.execute(args, () => new FFT) {
    c => new FFTTest(c)
  }
}
```

Or you can run the following command in your terminal.

```
sbt 'test:runMain FFT.FFTTestMain'
```

## How to run the FPGA flow ?

We have already generated three Verilog files *FFT128*, *FFT256* and *FFT512* for the FPGA project, which adopts 128, 256 and 512 as the length of FFT. You can import the tcl script directly in the Vivado to run the FPGA implementation and evaluation.

For more information, please see our article published on https://zhuanlan.zhihu.com/p/137260660
