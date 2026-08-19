package it.unibo.sentinel.boundary.gui.fx.panels

import it.unibo.sentinel.core.mission.{Mission, MissionStatus}
import scalafx.geometry.Insets
import scalafx.scene.control.ControlIncludes.jfxMultipleSelectionModel2sfx
import scalafx.scene.control.{Label, ListView, ScrollPane, SelectionMode}
import scalafx.scene.layout.{Priority, VBox}
import scalafx.scene.paint.Color

/** Panel used to display all the current and past [[Mission]]s
  *
  * @param missions
  *   all the [[Mission]]s created in the simulation
  */
class MissionsPanel(missions: Iterable[Mission]) extends ScrollPane:

  style = "-fx-background-color: #0F172A; -fx-background: #0F172A;"
  fitToWidth = true

  private val contentBox = new VBox:
    spacing = 12.0
    padding = Insets(12.0)
    style = "-fx-background-color: #0F172A;"

  content = contentBox

  private val pending =
    createListSection("Pending missions", filterAndParse(MissionStatus.Pending))
  private val active =
    createListSection("Active missions", filterAndParse(MissionStatus.Assigned))
  private val completed = createListSection(
    "Completed missions",
    filterAndParse(MissionStatus.Completed)
  )
  private val failed =
    createListSection("Failed missions", filterAndParse(MissionStatus.Failed))

  contentBox.children = Seq(pending, active, completed, failed)

  /** @param status
    *   used to filter the missions
    * @return
    *   a list of descriptions, one for each of the filtered missions
    */
  private def filterAndParse(status: MissionStatus): Iterable[String] =
    missions.filter(_.status == status).map(parseMission)

  /** @param mission
    *   the mission to extract the description from
    * @return
    *   a brief description of the given mission
    */
  private def parseMission(mission: Mission): String =
    val destinationLabel = mission.currentDestination match
      case Some(p) => s" - move to $p"
      case _       => ""
    s"${mission.id}$destinationLabel - ${mission.duration} ticks remaining"

  /** @param sectionTitle
    *   title of the section
    * @param items
    *   items (text) to display
    * @return
    *   a [[VBox]] containing the given list of items, displaying them
    *   vertically
    */
  private def createListSection(
      sectionTitle: String,
      items: Iterable[String]
  ): VBox =
    val itemList = items.toSeq
    val header = new Label:
      text = sectionTitle.toUpperCase
      textFill = Color.web("#94A3B8")
      style =
        "-fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1px;"
    val visibleItems = Math.min(itemList.size, 5).max(1)
    val calculatedHeight = visibleItems * 28 + 6
    val listView = new ListView[String](itemList):
      prefHeight = calculatedHeight
      maxHeight = 150.0
      vgrow = Priority.Never
      selectionModel().selectionMode = SelectionMode.Single
      style = """
        -fx-background-color: #1E293B;
        -fx-control-inner-background: #1E293B;
        -fx-background-radius: 6px;
        -fx-border-color: #334155;
        -fx-border-radius: 6px;
        -fx-border-width: 1px;
        -fx-cell-size: 28px;
        -fx-font-size: 12px;
        -fx-text-fill: #F8FAFC;
      """

    new VBox:
      spacing = 4.0
      vgrow = Priority.Never
      children = Seq(header, listView)
