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

# Chisel-FFT with reorder buffer

For the complete process of FFT, a reorder buffer hardware is attached to the FFT module, which can be found in

```
src/
  main/
    scala/
      FFT/ <Chisel的源代码文件>
        Butterfly.scala
        Config.scala
        FFT.scala
        Reorder.scala
        VerilogEmitter.scala
```

The reorder buffer change the sequences with bit-reversed order to the sequences with normal order. And a new top module is got by connecting the two modules.

## How to generater verilog

We designed *VerilogEmitter.scala* to handle all the generations of Verilog code.

In IntelliJ, you can click the green button near the *elaborateFFTReorder*  object defined in *VerilogEmitter.scala* to run this object and generate Verilog codes.

The object *elaborateFFTReorder*  is defined as follows, and some arguments (like the destination directory, etc) could be configured in this process.

```
object elaborateFFTReorder extends App {
  (new chisel3.stage.ChiselStage).execute(
    Array("-X", "verilog", "--full-stacktrace"),
    Seq(ChiselGeneratorAnnotation(() => new FFTReorder()),
      TargetDirAnnotation("Verilog"))
  )
}
```

Or you can run the following command in your terminal.

```
 sbt 'test:runMain FFT.elaborateFFTReorder'
```

# How to run the test ?

The testbench is defined in *FFTTest3.scala*, and you could run the object *FFTTestMain3* like what we did before to get the simulation results (like the error rate and the overflow rate, etc).

The object *FFTTestMain3* is defined as follows.

```
object FFTTestMain3 extends App {
  iotesters.Driver.execute(Array("--backend-name", "verilator"), () => new FFTReorder) {
    c => new FFTTest3(c)
  }
}
```
 
Or you can run the following command in your terminal.

```
sbt 'test:runMain FFT.FFTTestMain'
```

Moreover, we can view the wave of the simualation of the generated verilog by the file with an extension *.vcd*.

## How to run the FPGA flow ?

You need to generate the verilog file for the FPGA project and run the the FPGA implementation and evaluation by yourself 
