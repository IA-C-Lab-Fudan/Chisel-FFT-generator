
package FFT

import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import firrtl.options.TargetDirAnnotation

object elaborateButterfly extends App {
  (new chisel3.stage.ChiselStage).execute(
    Array("-X", "verilog"),
    Seq(ChiselGeneratorAnnotation(() => new Butterfly()),
      TargetDirAnnotation("Verilog"))
  )
}

object elaborateFFT extends App {
  (new chisel3.stage.ChiselStage).execute(
    Array("-X", "verilog", "--full-stacktrace"),
    Seq(ChiselGeneratorAnnotation(() => new FFT()),
      TargetDirAnnotation("Verilog"))
  )
}
