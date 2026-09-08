package it.unibo.sentinel.core.simulation

import it.unibo.sentinel.UnitTest
import it.unibo.sentinel.core.mission.{Mission, MissionId}
import it.unibo.sentinel.core.robot.RobotId
import it.unibo.sentinel.core.simulation.Event.*
import it.unibo.sentinel.core.warehouse.Position
import org.mockito.Mockito.*
import it.unibo.sentinel.core.scenario.Scenario
import it.unibo.sentinel.core.scenario.Spawn

class ReportSpec extends UnitTest:

  private val lastTick = Tick(10)
  private val r1 = RobotId("R1")
  private val r2 = RobotId("R2")
  private val m1 = MissionId("M1")
  private val m2 = MissionId("M2")
  private val m3 = MissionId("M3")
  private val p1 = Position(0, 0)
  private val p2 = Position(1, 0)
  private val p3 = Position(2, 0)
  private val p4 = Position(3, 0)

  private trait ReportFixture:
    val scenario: Scenario = mock[Scenario]()

    when(scenario.spawns).thenReturn(Seq.empty[Spawn])
    when(scenario.missions).thenReturn(Seq.empty[Mission])

    def createReport(history: History = Vector.empty): Statistics.Report =
      Statistics.report(scenario, history, lastTick)

    def createSpawn(id: RobotId): Spawn =
      val spawn = mock[Spawn]()
      when(spawn.id).thenReturn(id)
      spawn

  private trait MissionsFixture extends ReportFixture:
    val mission1: Mission = mock[Mission]()
    val mission2: Mission = mock[Mission]()

    when(mission1.id).thenReturn(m1)
    when(mission2.id).thenReturn(m2)
    when(scenario.missions).thenReturn(Seq(mission1, mission2))

    val history: History = Vector(
      (MissionCompleted(m1), Tick(1)),
      (MissionFailed(m2), Tick(2))
    )

  private trait RobotUsageFixture extends ReportFixture:
    val robots: Seq[Spawn] =
      Seq(createSpawn(r1), createSpawn(r2))

    when(scenario.spawns).thenReturn(robots)

    val history: History = Vector(
      (MissionAssigned(r1, m1), Tick(0)),
      (MissionAssigned(r2, m2), Tick(0)),
      (MissionAssigned(r1, m3), Tick(1))
    )

  "A simulation report" when:

    "used to understand the simulated workload" should:

      "show the elapsed time since the start of the simulation" in new ReportFixture:
        val report = createReport()
        report.ticks shouldBe lastTick.value

      "show the number of deployed robots in the fleet" in new ReportFixture:
        val robots = Seq(createSpawn(r1), createSpawn(r2))
        when(scenario.spawns).thenReturn(robots)
        val report = createReport()
        report.numOfRobots shouldBe 2

      "show the number of loaded missions" in new MissionsFixture:
        val report = createReport()
        report.numOfMissions shouldBe 2

    "used to assess mission outcomes" should:

      "show the number of completed missions" in new MissionsFixture:
        val report = createReport(history)
        report.numOfCompletedMissions shouldBe 1

      "show the number of failed missions" in new MissionsFixture:
        val report = createReport(history)
        report.numOfFailedMissions shouldBe 1

      "show the completion rate" in new MissionsFixture:
        val report = createReport(history)
        report.completionRate shouldBe Some(0.5)

    "used to compare productivity" should:

      "show the throughput" in new MissionsFixture:
        val report = createReport(history)
        val expectedThroughput = 1d / lastTick.value

        report.throughput shouldBe Some(expectedThroughput)

      "show the throughput per robot" in new MissionsFixture:
        val robots = Seq(createSpawn(r1), createSpawn(r2))
        when(scenario.spawns).thenReturn(robots)
        val report = createReport(history)
        val expectedThroughput = 1d / (lastTick.value * robots.size)
        report.throughputPerRobot shouldBe Some(expectedThroughput)

    "used to evaluate times and distances" should:
      val history: History = Vector(
        (MissionAssigned(r1, m1), Tick(0)),
        (MissionAssigned(r2, m2), Tick(0)),
        (MissionCompleted(m1), Tick(2)),
        (MissionCompleted(m2), Tick(4))
      )

      "show the average completion time" in new ReportFixture:
        val report = createReport(history)
        report.averageCompletionTime shouldBe Some(3.0)

      "show the maximum completion time" in new ReportFixture:
        val report = createReport(history)
        report.maxCompletionTime shouldBe Some(4)

      "show the total distance traveled" in new ReportFixture:
        val history: History = Vector(
          (RobotMoved(r1, p1, p2), Tick(0)),
          (RobotMoved(r2, p2, p3), Tick(0)),
          (RobotMoved(r2, p3, p4), Tick(1))
        )
        val report = createReport(history)
        report.totalDistance shouldBe 3

      "show the average distance per mission" in new ReportFixture:
        val history: History = Vector(
          (MissionAssigned(r1, m1), Tick(0)),
          (MissionAssigned(r2, m2), Tick(0)),
          (RobotMoved(r1, p1, p2), Tick(1)),
          (RobotMoved(r2, p2, p3), Tick(1)),
          (RobotMoved(r2, p3, p4), Tick(2)),
          (MissionCompleted(m1), Tick(1)),
          (MissionCompleted(m2), Tick(2))
        )
        val report = createReport(history)
        report.averageDistancePerMission shouldBe Some(1.5)

    "used to assess collisions" should:

      "show the number of blocks" in new ReportFixture:
        val history: History = Vector(
          (RobotBlocked(r1, p1), Tick(0)),
          (RobotUnblocked(r1), Tick(1)),
          (RobotMoved(r1, p1, p2), Tick(1)),
          (RobotBlocked(r2, p2), Tick(1))
        )
        val report = createReport(history)
        report.numOfBlocks shouldBe 2

    "used to assess how assignments are distributed" should:

      "identify the robot who was assigned the most missions" in new RobotUsageFixture:
        val report = createReport(history)
        report.mostUsedRobot shouldBe Set(r1)

      "identify the robot who was assigned the least missions" in new RobotUsageFixture:
        val report = createReport(history)
        report.leastUsedRobot shouldBe Set(r2)
