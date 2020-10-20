# Chisel-STFT

This Chisel project implements a STFT hardware design,and the source and test files are organized as follows.

```
src/
  main/
    scala/
      FFT/ <Chisel的FFT模块源代码文件>
        Butterfly.scala
        Config.scala
        FFT.scala
		Reorder.scala
		TOP.scala
        VerilogEmitter.scala
	  STFT/ <Chisel的STFT模块源代码文件>
        Hanning.scala//加上汉宁窗
        HanningWithFIFO.scala//输出加上FIFO
        Myfifo.scala
		SpectrogramGen.scala//频谱能量生成模块
		SpectrogramGenWithFIFO.scala//输出加上FIFO
		STFT.scala//主模块
        VerilogEmitter.scala
  test/
    scala/
      STFT/ <Chisel的测试代码文件>
        FFTTest.scala
		HanningTest.scala
		HanningWithFIFOTest.scala
		SpectrogramGenWithFIFOTest.scala
		SpectrogramTest.scala
		STFTTest.scala
		TOPTest.scala
```

The FPGA folder contains the tcl script and other necessary files for the FPGA flow running on Vivado 2019.2

## How to generate Verilog ?

We designed *VerilogEmitter.scala* to handle all the generations of Verilog code.You can find it in src/main/scala/STFT.

In IntelliJ, you can click the green button near the *elaborateSTFT*  object defined in *src/main/scala/STFT/VerilogEmitter.scala* to run this object and generate Verilog codes.

The object *elaborateSTFT*  is defined as follows, and some arguments (like the destination directory, etc) could be configured in this process.

```
object elaborateSTFT extends App {
  (new chisel3.stage.ChiselStage).execute(
    Array("-X", "verilog"),
    Seq(ChiselGeneratorAnnotation(() => new STFT()),
      TargetDirAnnotation("Verilog"))
  )
}
```

## How to run the test ?

The testbench is defined in *STFTTest.scala*, and you could run the object *STFTTest.scala* like what we did before to get the simulation results (like the error rate and the overflow rate, etc).

For each module,we also designed a test,like *HanningTest* is for Hanning,etc,you can run it to test module *Hanning* simulation results.

## How to run the FPGA flow ?

We have already generated a Verilog file *STFT.v*, for the FPGA project,. You can import the tcl script directly in the Vivado to run the FPGA implementation and evaluation.
