package FFT

import chisel3.stage.ChiselStage

object generator extends App with Config {
  (new ChiselStage).emitVerilog(new FFTtop,Array("--target-dir",s"generated/${FFTlength}Point_${FFTparallel}parallel_${use_float}float_${DataWidth-1}width/"))
}
