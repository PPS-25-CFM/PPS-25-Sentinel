package it.unibo.sentinel.boundary.gui.fx

import it.unibo.sentinel.boundary.gui.toolkit.StatisticsView
import it.unibo.sentinel.boundary.gui.fx.FxUtils.onFx
import it.unibo.sentinel.core.simulation.Statistics.Report
import it.unibo.sentinel.core.robot.{RobotId, value}
import monix.eval.Task
import scalafx.geometry.Insets
import scalafx.scene.Scene
import scalafx.scene.control.{Label, ScrollPane}
import scalafx.scene.layout.{ColumnConstraints, GridPane, VBox}

/** Presents the final report without accessing the running simulation. */
final class FxStatisticsView extends FxView with StatisticsView:
  private lazy val reportContent = new VBox:
    spacing = 24
    padding = Insets(32)
    style = "-fx-background-color: #0F172A;"

  override lazy val scene: Scene = new Scene(new ScrollPane:
    fitToWidth = true
    style = "-fx-background: #0F172A; -fx-background-color: #0F172A;"
    content = reportContent)

  override def render(report: Report): Task[Unit] = onFx:
    reportContent.children = Seq(
      label("Simulation completed", 28, "#F8FAFC"),
      label("Final statistics", 16, "#94A3B8"),
      section(
        "Overview",
        Seq(
          "Executed ticks" -> report.ticks.toString,
          "Robots" -> report.numOfRobots.toString,
          "Total missions" -> report.numOfMissions.toString,
          "Completed missions" -> report.numOfCompletedMissions.toString,
          "Failed missions" -> report.numOfFailedMissions.toString,
          "Completion rate" -> decimal(report.completionRate.map(_ * 100), "%")
        )
      ),
      section(
        "Performance",
        Seq(
          "Throughput" -> decimal(report.throughput, "missions/tick"),
          "Throughput per robot" -> decimal(
            report.throughputPerRobot,
            "missions/tick/robot"
          )
        )
      ),
      section(
        "Times and distances",
        Seq(
          "Average completion time" -> decimal(
            report.averageCompletionTime,
            "ticks"
          ),
          "Maximum completion time" -> report.maxCompletionTime.fold("N/A")(n =>
            s"$n ticks"
          ),
          "Total distance" -> s"${report.totalDistance} cells",
          "Average distance per completed mission" -> decimal(
            report.averageDistancePerMission,
            "cells/mission"
          )
        )
      ),
      section(
        "Robot usage",
        Seq(
          "Block events" -> report.numOfBlocks.toString,
          "Most used robots (by assignments)" -> robots(report.mostUsedRobot),
          "Least used robots (by assignments)" -> robots(report.leastUsedRobot)
        )
      )
    )

  private def decimal(number: Option[Double], unit: String): String =
    number.fold("N/A")(n => f"$n%.2f $unit%s")

  private def robots(ids: Set[RobotId]): String =
    if ids.isEmpty then "N/A" else ids.toSeq.map(_.value).sorted.mkString(", ")

  private def label(text: String, size: Int, color: String): Label =
    new Label(text):
      wrapText = true
      minWidth = 0
      maxWidth = Double.MaxValue
      style = s"-fx-text-fill: $color; -fx-font-size: ${size}px;"

  private def section(title: String, rows: Seq[(String, String)]): VBox =
    val grid = new GridPane:
      hgap = 32
      vgap = 12
      columnConstraints = Seq(55d, 45d).map: width =>
        new ColumnConstraints:
          percentWidth = width
          minWidth = 0
    rows.zipWithIndex.foreach:
      case ((name, value), index) =>
        grid.add(label(name, 14, "#94A3B8"), 0, index)
        grid.add(label(value, 14, "#F8FAFC"), 1, index)
    new VBox:
      spacing = 16
      padding = Insets(20)
      style = "-fx-background-color: #1E293B; -fx-background-radius: 8;"
      children = Seq(label(title, 18, "#F8FAFC"), grid)
