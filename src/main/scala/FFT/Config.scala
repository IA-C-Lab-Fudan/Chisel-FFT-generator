
package FFT

trait HasDataConfig {
  val DataWidth = 32
  val BinaryPoint = 16
}

trait HasElaborateConfig {
  val FFTLength = 512
  val useGauss = false
  val supportIFFT = false
}
