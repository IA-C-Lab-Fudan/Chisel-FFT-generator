
package FFT

import chisel3._
import chisel3.util._

/**
  *
  * FFT top with output FIFO and decoupled IO
  *
  */

class TOP extends Module
  with HasDataConfig
  with HasElaborateConfig {
  val io = IO(new Bundle{
    val mode = if(supportIFFT) Some(Input(Bool())) else None
    val din = Flipped(DecoupledIO(new MyComplex))
    val dout = DecoupledIO(new MyComplex)
  })
  val FFT = Module(new FFTReorder)
  val FIFOcmd = Wire(DecoupledIO(new MyComplex))
  FIFOcmd.valid := FFT.io.dout_valid
  FIFOcmd.bits := FFT.io.dOut
  val FIFO = Queue(FIFOcmd, 2 * FFTLength)
  FIFO.ready := io.dout.ready

  io.din.ready := FIFOcmd.ready && !FFT.io.busy
  FFT.io.din_valid := io.din.fire()
  FFT.io.dIn := io.din.bits

  io.dout.valid := FIFO.fire()
  io.dout.bits := FIFO.bits
}
