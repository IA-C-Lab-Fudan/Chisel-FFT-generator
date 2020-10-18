
package STFT


import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import firrtl.options.TargetDirAnnotation

object elaborateHanning extends App {
  (new chisel3.stage.ChiselStage).execute(
    Array("-X", "verilog"),
    Seq(ChiselGeneratorAnnotation(() => new Hanning()),
      TargetDirAnnotation("Verilog"))
  )
}

object elaborateHanningWithFIFO extends App {
  (new chisel3.stage.ChiselStage).execute(
    Array("-X", "verilog"),
    Seq(ChiselGeneratorAnnotation(() => new HanningWithFIFO()),
      TargetDirAnnotation("Verilog"))
  )
}

object elaborateSpectrogram extends App {
  (new chisel3.stage.ChiselStage).execute(
    Array("-X", "verilog"),
    Seq(ChiselGeneratorAnnotation(() => new SpectrogramGen()),
      TargetDirAnnotation("Verilog"))
  )
}

object elaborateSpectrogramWithFIFO extends App {
  (new chisel3.stage.ChiselStage).execute(
    Array("-X", "verilog"),
    Seq(ChiselGeneratorAnnotation(() => new SpectrogramGenWithFIFO()),
      TargetDirAnnotation("Verilog"))
  )
}

object elaborateSTFT extends App {
  (new chisel3.stage.ChiselStage).execute(
    Array("-X", "verilog"),
    Seq(ChiselGeneratorAnnotation(() => new STFT()),
      TargetDirAnnotation("Verilog"))
  )
}

object elaboratefifo extends App {
  (new chisel3.stage.ChiselStage).execute(
    Array("-X", "verilog"),
    Seq(ChiselGeneratorAnnotation(() => new MyFifo(1024,512)),
      TargetDirAnnotation("Verilog"))
  )
}