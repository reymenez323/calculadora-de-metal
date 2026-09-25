package com.reymildo.calculadoradelmetal.domain.machining

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

object MachiningCalculator {
    fun spindleRpm(cuttingSpeedMMin: Double, diameterMm: Double): Double =
        cuttingSpeedMMin * 1000.0 / (PI * diameterMm)

    fun cuttingSpeed(rpm: Double, diameterMm: Double): Double = PI * diameterMm * rpm / 1000.0

    fun milling(input: MillingInput, machine: MachineLimits): Result<MachiningResult> = runCatching {
        requirePositive(input.cutterDiameterMm, "cutterDiameter")
        requirePositive(input.cuttingSpeedMMin, "cuttingSpeed")
        require(input.effectiveTeeth > 0) { "effectiveTeeth" }
        requirePositive(input.feedPerToothMm, "feedPerTooth")
        requirePositive(input.axialDepthPerPassMm, "axialDepth")
        requirePositive(input.radialWidthMm, "radialWidth")
        requirePositive(input.totalDepthMm, "totalDepth")
        requirePositive(input.pathLengthPerLevelMm, "pathLength")
        require(input.finishAllowanceMm >= 0 && input.finishAllowanceMm < input.totalDepthMm) { "finishAllowance" }

        val theoreticalRpm = spindleRpm(input.cuttingSpeedMMin, input.cutterDiameterMm)
        val adjustment = adjustRpm(theoreticalRpm, machine)
        val programmedFeed = input.feedPerToothMm * input.effectiveTeeth * adjustment.rpm
        val feed = programmedFeed.coerceAtMost(machine.maxFeedMmMin)
        val plan = planPasses(input.totalDepthMm, input.axialDepthPerPassMm, input.finishAllowanceMm)
        val time = input.pathLengthPerLevelMm * plan.totalPasses / feed
        val removalRate = input.axialDepthPerPassMm * input.radialWidthMm * feed / 1000.0
        val volume = input.pathLengthPerLevelMm * input.radialWidthMm * input.totalDepthMm / 1000.0
        val warnings = buildList {
            addAll(adjustment.warnings)
            if (feed < programmedFeed) add("feed_limited")
        }
        checkedResult(
            theoreticalRpm, adjustment.rpm, input.cutterDiameterMm, feed,
            feed / adjustment.rpm, plan, time, removalRate, volume, warnings,
        )
    }

    fun turning(input: TurningInput, machine: MachineLimits): Result<MachiningResult> = runCatching {
        requirePositive(input.initialDiameterMm, "initialDiameter")
        require(input.finalDiameterMm >= 0) { "finalDiameter" }
        if (input.operation == TurningOperation.BORING) {
            require(input.finalDiameterMm > input.initialDiameterMm) { "finalDiameter" }
        } else {
            require(input.finalDiameterMm < input.initialDiameterMm) { "finalDiameter" }
        }
        requirePositive(input.cuttingLengthMm, "cuttingLength")
        requirePositive(input.cuttingSpeedMMin, "cuttingSpeed")
        requirePositive(input.feedPerRevolutionMm, "feedPerRevolution")

        val referenceDiameter = when (input.operation) {
            TurningOperation.BORING -> max(input.finalDiameterMm, 0.001)
            else -> input.initialDiameterMm
        }
        val theoreticalRpm = spindleRpm(input.cuttingSpeedMMin, referenceDiameter)
        val operationMax = when (input.operation) {
            TurningOperation.THREADING -> machine.maxThreadingRpm
            TurningOperation.PARTING -> machine.maxPartingRpm
            else -> null
        }
        val adjustment = adjustRpm(theoreticalRpm, operationMax?.let { machine.copy(maxRpm = minOf(machine.maxRpm, it)) } ?: machine)
        val feedPerRev = if (input.operation == TurningOperation.THREADING) {
            requirePositive(input.threadPitchMm, "threadPitch")
            require(input.threadStarts > 0) { "threadStarts" }
            input.threadPitchMm * input.threadStarts
        } else input.feedPerRevolutionMm
        val programmedFeed = feedPerRev * adjustment.rpm
        val effectiveRpm = if (input.operation == TurningOperation.THREADING && programmedFeed > machine.maxFeedMmMin) {
            machine.maxFeedMmMin / feedPerRev
        } else adjustment.rpm
        val feed = (feedPerRev * effectiveRpm).coerceAtMost(machine.maxFeedMmMin)

        val (plan, distance) = when (input.operation) {
            TurningOperation.THREADING -> {
                require(input.threadPasses > 0) { "threadPasses" }
                fixedPasses(input.threadPasses) to input.cuttingLengthMm * input.threadPasses
            }
            TurningOperation.GROOVING -> {
                requirePositive(input.depthPerPassMm, "depthPerPass")
                requirePositive(input.grooveWidthMm, "grooveWidth")
                requirePositive(input.toolWidthMm, "toolWidth")
                val radial = (input.initialDiameterMm - input.finalDiameterMm) / 2.0
                val depthPlan = planPasses(radial, input.depthPerPassMm, 0.0)
                val widthPasses = ceil(input.grooveWidthMm / input.toolWidthMm).toInt()
                fixedPasses(depthPlan.totalPasses * widthPasses) to radial * widthPasses
            }
            TurningOperation.PARTING -> {
                val radial = (input.initialDiameterMm - input.finalDiameterMm) / 2.0
                fixedPasses(1) to radial
            }
            TurningOperation.FACING -> {
                requirePositive(input.depthPerPassMm, "depthPerPass")
                val depthPlan = planPasses(input.cuttingLengthMm, input.depthPerPassMm, input.finishAllowanceMm)
                depthPlan to ((input.initialDiameterMm - input.finalDiameterMm) / 2.0) * depthPlan.totalPasses
            }
            TurningOperation.TURNING, TurningOperation.BORING -> {
                requirePositive(input.depthPerPassMm, "depthPerPass")
                val radial = abs(input.initialDiameterMm - input.finalDiameterMm) / 2.0
                val depthPlan = planPasses(radial, input.depthPerPassMm, input.finishAllowanceMm)
                depthPlan to input.cuttingLengthMm * depthPlan.totalPasses
            }
        }

        val radialOperation = input.operation in setOf(
            TurningOperation.FACING, TurningOperation.GROOVING, TurningOperation.PARTING,
        )
        val time = if (input.constantSurfaceSpeed && radialOperation) {
            val cssSweeps = when (input.operation) {
                TurningOperation.FACING -> plan.totalPasses
                TurningOperation.GROOVING -> ceil(input.grooveWidthMm / input.toolWidthMm).toInt()
                else -> 1
            }
            cssRadialTime(
                input.initialDiameterMm,
                input.finalDiameterMm,
                input.cuttingSpeedMMin,
                feedPerRev,
                machine.maxRpm,
            ) * cssSweeps
        } else distance / feed

        val radialRemoval = abs(input.initialDiameterMm - input.finalDiameterMm) / 2.0
        val nominalDepth = when (input.operation) {
            TurningOperation.TURNING, TurningOperation.BORING -> minOf(radialRemoval, input.depthPerPassMm)
            TurningOperation.FACING -> minOf(input.cuttingLengthMm, input.depthPerPassMm)
            TurningOperation.GROOVING, TurningOperation.PARTING, TurningOperation.THREADING -> null
        }
        val removalRate = nominalDepth?.let { input.cuttingSpeedMMin * it * feedPerRev }
        val volume = when (input.operation) {
            TurningOperation.TURNING, TurningOperation.BORING ->
                PI * abs(input.initialDiameterMm * input.initialDiameterMm - input.finalDiameterMm * input.finalDiameterMm) *
                    input.cuttingLengthMm / 4000.0
            else -> null
        }
        val warnings = buildList {
            addAll(adjustment.warnings)
            if (feed < programmedFeed) add("feed_limited")
            if (input.constantSurfaceSpeed && radialOperation) add("css_rpm_limit_applied")
        }
        checkedResult(
            theoreticalRpm, effectiveRpm, referenceDiameter, feed, feed / effectiveRpm,
            plan, time, removalRate, volume, warnings,
        )
    }

    private fun planPasses(total: Double, perPass: Double, finish: Double): PassPlan {
        requirePositive(total, "totalDepth")
        requirePositive(perPass, "depthPerPass")
        require(finish >= 0 && finish < total) { "finishAllowance" }
        val roughTotal = total - finish
        val roughPasses = ceil(roughTotal / perPass).toInt().coerceAtLeast(1)
        val last = roughTotal - perPass * (roughPasses - 1)
        val finishPasses = if (finish > 0) 1 else 0
        return PassPlan(roughPasses, finishPasses, roughPasses + finishPasses, perPass, last)
    }

    private fun fixedPasses(count: Int) = PassPlan(count, 0, count, 0.0, 0.0)

    private data class RpmAdjustment(val rpm: Double, val warnings: List<String>)

    private fun adjustRpm(theoretical: Double, machine: MachineLimits): RpmAdjustment {
        requirePositive(machine.maxRpm, "maxRpm")
        requirePositive(machine.maxFeedMmMin, "maxFeed")
        val limited = theoretical.coerceAtMost(machine.maxRpm)
        val warnings = mutableListOf<String>()
        if (limited < theoretical) warnings += "rpm_limited"
        if (machine.kind == MachineKind.CONVENTIONAL && machine.steppedRpm.isNotEmpty()) {
            val chosen = machine.steppedRpm.filter { it.isFinite() && it > 0 && it <= limited }.maxOrNull()
                ?: throw IllegalArgumentException("no_compatible_rpm")
            if (chosen < limited) warnings += "stepped_rpm"
            return RpmAdjustment(chosen, warnings)
        }
        return RpmAdjustment(limited, warnings)
    }

    private fun cssRadialTime(
        outerDiameter: Double,
        innerDiameter: Double,
        cuttingSpeed: Double,
        feedPerRev: Double,
        maxRpm: Double,
    ): Double {
        val outerR = outerDiameter / 2.0
        val innerR = innerDiameter.coerceAtLeast(0.0) / 2.0
        val transitionR = cuttingSpeed * 1000.0 / (2.0 * PI * maxRpm)
        val cssInner = max(innerR, transitionR).coerceAtMost(outerR)
        val cssTime = PI * (outerR * outerR - cssInner * cssInner) / (feedPerRev * cuttingSpeed * 1000.0)
        val cappedTime = if (innerR < transitionR) (minOf(transitionR, outerR) - innerR) / (feedPerRev * maxRpm) else 0.0
        return cssTime + cappedTime
    }

    private fun checkedResult(
        theoreticalRpm: Double,
        adjustedRpm: Double,
        diameter: Double,
        feed: Double,
        feedPerRev: Double,
        passes: PassPlan,
        time: Double,
        removalRate: Double?,
        volume: Double?,
        warnings: List<String>,
    ): MachiningResult {
        val values = listOfNotNull(theoreticalRpm, adjustedRpm, feed, feedPerRev, time, removalRate, volume)
        require(values.all { it.isFinite() && it >= 0.0 }) { "non_finite_result" }
        return MachiningResult(
            theoreticalRpm = theoreticalRpm,
            adjustedRpm = adjustedRpm,
            actualCuttingSpeedMMin = cuttingSpeed(adjustedRpm, diameter),
            feedMmMin = feed,
            feedPerRevolutionMm = feedPerRev,
            passes = passes,
            cuttingTimeMin = time,
            removalRateCm3Min = removalRate,
            removedVolumeCm3 = volume,
            warnings = warnings,
        )
    }

    private fun requirePositive(value: Double, name: String) {
        require(value.isFinite() && value > 0.0) { name }
    }
}
