package FFT

import chisel3._
import chisel3.util._
import chisel3.experimental._

class reorder extends Module with Config {
  val io = IO(new Bundle{
    val dIn = Input(Vec(FFTparallel_r * radix, if(use_float) new IEEEComplex else new MyFixComplex))
    val dOut = Output(if(use_float) new IEEEComplex else new MyFixComplex)
    val din_valid = Input(new Bool)
    val dout_valid = Output(new Bool)
    val busy = Output(new Bool)
  })

  val FFTbit = log2Ceil(FFTlength)
  val databit = log2Ceil(datalength)
  val radixbit = log2Ceil(radix)

  val w_cnt = RegInit(0.U((databit + 1).W))
  val r_cnt = RegInit(0.U((FFTbit + 1).W))
  val do_write = io.din_valid || (w_cnt(databit) =/= 1.U && w_cnt =/= 0.U)
  val do_read = w_cnt(databit) === 1.U || (r_cnt(FFTbit) =/= 1.U && r_cnt =/= 0.U)
  val buffer = RegInit(VecInit(Seq.fill(FFTlength)(0.S((2*DataWidth).W).asTypeOf(if(use_float) new IEEEComplex else new MyFixComplex))))

  def bit_reverse(j: UInt): UInt = {
    var result: UInt = 0.U(FFTbit.W)
    val times = FFTbit
    for (i <- 0 until times) {
      var operator = j & (1.U(FFTbit.W) << i).asUInt
      result = result | ((operator >> i) << (times - 1 - i)).asUInt
    }
    result
  }
  def index(p: Int, r: Int, cnt: UInt): UInt = {
    val index_temp = (p * datalength).asUInt((FFTbit - radixbit).W) + cnt
    bit_reverse(Cat(index_temp,r.asUInt(radixbit.W)))
  }

  when(do_write) {
    w_cnt := w_cnt + 1.U
    for(i <- 0 until FFTparallel_r) {
      (0 until radix).map(x => buffer(index(i,x,w_cnt)) := io.dIn(radix * i + x))
    }
  }.otherwise {
    w_cnt := 0.U
  }
  when(do_read) {
    r_cnt := r_cnt + 1.U
  }.otherwise {
    r_cnt := 0.U
  }

  io.dOut := buffer(r_cnt)
  io.busy := do_write || do_read
  io.dout_valid := w_cnt(databit) === 1.U
}
