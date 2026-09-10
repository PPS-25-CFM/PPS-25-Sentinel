package it.unibo.sentinel.boundary.gui.toolkit

import it.unibo.sentinel.core.simulation.Statistics.Report

/** Displays the report of a completed simulation. */
trait StatisticsView extends View[Report], Dismissable
