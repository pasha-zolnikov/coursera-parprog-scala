package scalashop

import java.util.concurrent.ForkJoinTask

import org.scalameter._
import common._

object VerticalBoxBlurRunner {

  val standardConfig = config(
    Key.exec.minWarmupRuns -> 5,
    Key.exec.maxWarmupRuns -> 10,
    Key.exec.benchRuns -> 10,
    Key.verbose -> true
  ) withWarmer (new Warmer.Default)

  def main(args: Array[String]): Unit = {
    val radius = 1
    val width = 32
    val height = 64
    val src = new Img(width, height)
    val dst = new Img(width, height)
    val seqtime = standardConfig measure {
      VerticalBoxBlur.blur(src, dst, 0, width, radius)
    }
    println(s"sequential blur time: $seqtime ms")

    val numTasks = 32
    val partime = standardConfig measure {
      VerticalBoxBlur.parBlur(src, dst, numTasks, radius)
    }
    println(s"fork/join blur time: $partime ms")
    println(s"speedup: ${seqtime / partime}")
  }

}

/** A simple, trivially parallelizable computation. */
object VerticalBoxBlur {

  /** Blurs the columns of the source image `src` into the destination image
    * `dst`, starting with `from` and ending with `end` (non-inclusive).
    *
    * Within each column, `blur` traverses the pixels by going from top to
    * bottom.
    */
  def blur(src: Img, dst: Img, from: Int, end: Int, radius: Int): Unit = {
    // TODO implement this method using the `boxBlurKernel` method
    var r = 0
    var c = from

    while (r < src.height) {
      while (c < end) {
        dst.update(c, r, boxBlurKernel(src, c, r, radius))
        c += 1
      }
      c = from
      r += 1
    }
  }

  /** Blurs the columns of the source image in parallel using `numTasks` tasks.
    *
    * Parallelization is done by stripping the source image `src` into
    * `numTasks` separate strips, where each strip is composed of some number of
    * columns.
    */
  def parBlur(src: Img, dst: Img, numTasks: Int, radius: Int): Unit = {
    // TODO implement using the `task` construct and the `blur` method
    val w = src.width

    var tasks = scala.collection.mutable.ListBuffer.empty[ForkJoinTask[Unit]]

    val (step, residue) = if (w <= numTasks) (1, 0) else (w / numTasks, w % numTasks)

    val numOfStripes = Math.min(numTasks, w)

    for (i <- 0 until numOfStripes) {
      val from = i * step
      val to = if (i == numOfStripes - 1) (i + 1) * step + residue else (i + 1) * step

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
