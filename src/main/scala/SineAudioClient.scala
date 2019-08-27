package xyz.hyperreal.audioservers_test

import java.nio.FloatBuffer
import java.util.ServiceLoader
import java.util.logging.Level
import java.util.logging.Logger

import scala.jdk.CollectionConverters._

import org.jaudiolibs.audioservers.AudioClient
import org.jaudiolibs.audioservers.AudioConfiguration
import org.jaudiolibs.audioservers.AudioServerProvider
import org.jaudiolibs.audioservers.ext.ClientID
import org.jaudiolibs.audioservers.ext.Connections


/**
 * Basic example for processing audio using the AudioServer API.
 *
 * A simple AudioClient that outputs a sine wave.
 *
 * Run main() - if not using NetBeans, make sure to configure JVM arguments.
 * -Xincgc is recommended.
 * -Djna.nosys=true may be required if using the JACK AudioServer and
 * an older version of JNA is installed on your system.
 *
 * @author Neil C Smith
 */

object SineAudioClient extends App with AudioClient {

  /* Search for an AudioServerProvider that matches the required library name
         * using the ServiceLoader mechanism. This removes the need for a direct
         * dependency on any particular server implementation.
         *
         * It is also possible to create particular AudioServerProvider's
         * directly.
         */
  val lib = /*"JavaSound"*/ "JACK"

  var provider: AudioServerProvider = null

  val loader = ServiceLoader.load(classOf[AudioServerProvider])

  def findProvider: Unit =
    for (p <- loader.iterator.asScala) {
      if (lib.equals(p.getLibraryName)) {
        provider = p
        return
      }
    }

  findProvider

  if (provider == null) {
    throw new NullPointerException("No AudioServer found that matches : " + lib)
  }

  println( s"Using provider: ${provider.getLibraryName}" )

  /* Create an instance of our client - see methods in the implementation
   * below for more information.
   */
  val client = SineAudioClient

  /* Create an audio configuration.
   *
   * The configuration is a hint to the AudioServer. Some servers (eg. JACK)
   * will ignore the sample rate and buffersize here.
   * The correct values will be passed to the client during configuration.
   *
   * Various extension objects can be added to the AudioConfiguration.
   * The ClientID and Connections parameters here will be used by the JACK server.
   *
   */
  val config = new AudioConfiguration(
    44100.0f, //sample rate
    0, // input channels
    2, // output channels
    256, //buffer size
    // extensions
    new ClientID("Sine"),
    Connections.OUTPUT)


  /* Use the AudioServerProvider to create an AudioServer for the client.
   */
  val server = provider.createServer(config, client)

  /* Create a Thread to run our server. All servers require a Thread to run in.
   */
  val runner = new Thread(new Runnable {
    def run = {
      // The server's run method can throw an Exception so we need to wrap it
      try {
        server.run
      } catch {
        case ex: Exception =>
          Logger.getLogger(SineAudioClient.getClass.getName).log(Level.SEVERE, null, ex)
      }
    }
  })
  // set the Thread priority as high as possible.
  runner.setPriority(Thread.MAX_PRIORITY)
  // and start processing audio - you'll have to kill the program manually!
  runner.start


// AudioClient implementation

val FREQ = 440.0f
var data: Array[Float] = _
var buffer: Array[Float] = _
var idx: Int = _

def configure(context: AudioConfiguration) = {
  /* Check the configuration of the passed in context, and set up any
   * necessary resources. Throw an Exception if the sample rate, buffer
   * size, etc. cannot be handled. DO NOT assume that the context matches
   * the configuration you passed in to create the server - it will
   * be a best match.
   */
  if (context.getOutputChannelCount != 2) {
    throw new IllegalArgumentException("SineAudioClient can only work with stereo output")
  }

  val size = (context.getSampleRate / FREQ).toInt
  data = new Array[Float]( size )

  for (i <- 0 until size) {
      data(i) = (0.2 * math.sin(i.toDouble / size * math.Pi * 2)).toFloat
  }
}

  def process(time: Long, inputs: java.util.List[FloatBuffer], outputs: java.util.List[FloatBuffer], nframes: Int) = {
  // get left and right channels from array list
  val left = outputs.get(0)
  val right = outputs.get(1)

  if (buffer == null || buffer.length != nframes) {
    buffer = new Array[Float]( nframes )
  }

  // always use nframes as the number of samples to process
  for (i <- 0 until nframes) {
    buffer(i) = data(idx)
    idx += 1
    if (idx == data.length) {
    idx = 0
  }
}

  left.put(buffer)
  right.put(buffer)

  true
}

  def shutdown = {
  //dispose resources.
  data = null
}

}