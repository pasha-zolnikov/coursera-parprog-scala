package scalashop

import java.util.concurrent.ForkJoinTask

import common._
import org.scalameter._

object HorizontalBoxBlurRunner {

  val standardConfig = config(
    Key.exec.minWarmupRuns -> 5,
    Key.exec.maxWarmupRuns -> 10,
    Key.exec.benchRuns -> 10,
    Key.verbose -> true
  ) withWarmer (new Warmer.Default)

  def main(args: Array[String]): Unit = {
    val radius = 3
    val width = 32
    val height = 32
    val src = new Img(width, height)
    val dst = new Img(width, height)
    val seqtime = standardConfig measure {
      HorizontalBoxBlur.blur(src, dst, 0, height, radius)
    }
    println(s"sequential blur time: $seqtime ms")

    val numTasks = 32
    val partime = standardConfig measure {
      HorizontalBoxBlur.parBlur(src, dst, numTasks, radius)
    }
    println(s"fork/join blur time: $partime ms")
    println(s"speedup: ${seqtime / partime}")
  }
}


/** A simple, trivially parallelizable computation. */
object HorizontalBoxBlur {

  /** Blurs the rows of the source image `src` into the destination image `dst`,
    * starting with `from` and ending with `end` (non-inclusive).
    *
    * Within each row, `blur` traverses the pixels by going from left to right.
    */
  def blur(src: Img, dst: Img, from: Int, end: Int, radius: Int): Unit = {
    // TODO implement this method using the `boxBlurKernel` method
    var r = from
    var c = 0

    while (r < end) {
      while (c < src.width) {
        dst.update(c, r, boxBlurKernel(src, c, r, radius))
        c += 1
      }
      c = 0
      r += 1
    }

  }

  /** Blurs the rows of the source image in parallel using `numTasks` tasks.
    *
    * Parallelization is done by stripping the source image `src` into
    * `numTasks` separate strips, where each strip is composed of some number of
    * rows.
    */
  def parBlur(src: Img, dst: Img, numTasks: Int, radius: Int): Unit = {
    // TODO implement using the `task` construct and the `blur` method

    val h = src.height
    val upperBound = Math.min(numTasks, h)
    val (step, residue) = if (h <= numTasks) (1, 0) else (h / numTasks, h % numTasks)

    var tasks = scala.collection.mutable.ListBuffer.empty[ForkJoinTask[Unit]]

    for (i <- 0 until upperBound) {
      val from = i * step
      val to = if (i == upperBound - 1) (i + 1) * step + residue else (i + 1) * step

      val strip = task {
        blur(src, dst, from, to, radius)
      }.fork()
      tasks += strip
    }

    for (t <- tasks.toList) {
      t.join()
    }
  }

}
