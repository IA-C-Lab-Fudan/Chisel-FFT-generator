
package FFT

trait HasDataConfig {
  val DataWidth = 32
  val BinaryPoint = 16
}

trait HasElaborateConfig {
  val FFTLength = 128
  val useGauss = false
  val supportIFFT = true
}
