
package FFT

import chisel3._
import chisel3.util._

class BRAM(dep: Int, dw: Int) extends BlackBox(
  Map("ADDR_WIDTH" -> dep,
  "DATA_WIDTH" -> dw))
  with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clock = Input(Bool())
    val en = Input(Bool())
    val addr = Input(UInt(dep.W))
    val dout = Output(UInt(dw.W))
  })

  addResource("/BRAM.v")
}
