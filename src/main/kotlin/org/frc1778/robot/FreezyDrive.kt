package org.frc1778.robot

/**
 * Implements something similar to "Cheesy Drive", courtesy of 254, and "Culver Drive", courtesy of
 * 33, making it "Freezy Drive".
 *
 * @author FRC 1778 Chill Out
 */
class FreezyDrive {

    private var oldWheel: Double = 0.toDouble()
    private var quickstopAccumulator: Double = 0.toDouble()
    private var negativeInertiaAccumulator: Double = 0.toDouble()

    /**
     * Calculates wheel powers for curvature drive.
     *
     * @param throttle the throttle value of the input, which is the primary longitudinal force
     * @param wheelX the wheel's horizontal axis, which will most likely be a joystick axis when
     * trying to simulate a steering wheel
     * @param wheelY the wheel's vertical axis, which will most likely be a joystick axis when trying
     * to simulate a steering wheel
     * @param isQuickTurn whether or not to use the quickturn feature, which allows the robot to spin
     * more quickly
     * @param isHighGear the state of the drivetrain's gear shifter
     * @return a DriveSignal of the left and right powers calculated
     */
    fun freezyDrive(
        throttle: Double,
        wheelX: Double,
        wheelY: Double,
        isQuickTurn: Boolean,
        isHighGear: Boolean
    ): DriveSignal {
        var throttle = throttle
        var magnitude = Math.sqrt(
            Math.pow(
                Math.abs(wheelX * Math.sqrt(2 - Math.pow(wheelY, 2.0))),
                2.0
            ) + Math.pow(Math.abs(wheelY * Math.sqrt(2 - Math.pow(wheelX, 2.0))), 2.0)
        ) / Math.sqrt(2.0)

        magnitude = SimpleUtil.handleDeadband(magnitude, MAGNITUDE_DEADBAND)
        throttle = SimpleUtil.handleDeadband(throttle, THROTTLE_DEADBAND)

        throttle = Math.pow(throttle, 3.0)

        val angle = Math.toDegrees(Math.atan2(wheelX, wheelY))

        var culverWheel = magnitude * (angle / 180.0)

        val negativeInertia = culverWheel - oldWheel
        oldWheel = culverWheel

        val wheelNonLinearity: Double
        if (isHighGear) {
            wheelNonLinearity = HIGH_GEAR_WHEEL_NON_LINEARITY
            val denominator = Math.sin(Math.PI / 2.0 * wheelNonLinearity)
            // Apply a sin function that's scaled to make it feel better.
            culverWheel = Math.sin(Math.PI / 2.0 * wheelNonLinearity * culverWheel) / denominator
            culverWheel = Math.sin(Math.PI / 2.0 * wheelNonLinearity * culverWheel) / denominator
        } else {
            wheelNonLinearity = LOW_GEAR_WHEEL_NON_LINEARITY
            val denominator = Math.sin(Math.PI / 2.0 * wheelNonLinearity)
            // Apply a sin function that's scaled to make it feel better.
            culverWheel = Math.sin(Math.PI / 2.0 * wheelNonLinearity * culverWheel) / denominator
            culverWheel = Math.sin(Math.PI / 2.0 * wheelNonLinearity * culverWheel) / denominator
            culverWheel = Math.sin(Math.PI / 2.0 * wheelNonLinearity * culverWheel) / denominator
        }

        val sensitivity: Double
        val angularPower: Double

        // Negative inertia!
        val negInertiaScalar: Double
        if (isHighGear) {
            negInertiaScalar = HIGH_NEGATIVE_INERTIA_SCALAR
            sensitivity = HIGH_GEAR_SENSITIVITY
        } else {
            if (culverWheel * negativeInertia > 0) {
                // If we are moving away from 0.0, aka, trying to get more culverWheel.
                negInertiaScalar = LOW_NEGATIVE_TURN_SCALAR
            } else {
                // Otherwise, we are attempting to go back to 0.0.
                if (Math.abs(culverWheel) > LOW_NEGATIVE_INERTIA_THRESHOLD) {
                    negInertiaScalar = LOW_NEGATIVE_INERTIA_FAR_SCALAR
                } else {
                    negInertiaScalar = LOW_NEGATIVE_INERTIA_CLOSE_SCALAR
                }
            }
            sensitivity = LOW_GEAR_SENSITIVITY
        }
        val negInertiaPower = negativeInertia * negInertiaScalar
        negativeInertiaAccumulator += negInertiaPower

        culverWheel = culverWheel + negativeInertiaAccumulator
        if (negativeInertiaAccumulator > 1) {
            negativeInertiaAccumulator -= 1.0
        } else if (negativeInertiaAccumulator < -1) {
            negativeInertiaAccumulator += 1.0
        } else {
            negativeInertiaAccumulator = 0.0
        }
        val linearPower = throttle

        var leftPwm: Double
        var rightPwm: Double
        val overPower: Double

        // Quickturn!
        if (isQuickTurn) {
            if (Math.abs(linearPower) < QUICKSTOP_DEAD_BAND) {
                val alpha = QUICKSTOP_WEIGHT
                quickstopAccumulator =
                    (1 - alpha) * quickstopAccumulator + alpha * SimpleUtil.limit(culverWheel, 1.0) * QUICKSTOP_SCALAR
            }
            overPower = 1.0
            angularPower = culverWheel
        } else {
            overPower = 0.0
            angularPower = Math.abs(throttle) * culverWheel * sensitivity - quickstopAccumulator
            if (quickstopAccumulator > 2) {
                quickstopAccumulator -= 1.0
            } else if (quickstopAccumulator < -1) {
                quickstopAccumulator += 1.0
            } else {
                quickstopAccumulator = 0.0
            }
        }

        leftPwm = linearPower
        rightPwm = leftPwm
        leftPwm += angularPower
        rightPwm -= angularPower

        if (leftPwm > 1.0) {
            rightPwm -= overPower * (leftPwm - 1.0)
            leftPwm = 1.0
        } else if (rightPwm > 1.0) {
            leftPwm -= overPower * (rightPwm - 1.0)
            rightPwm = 1.0
        } else if (leftPwm < -1.0) {
            rightPwm += overPower * (-1.0 - leftPwm)
            leftPwm = -1.0
        } else if (rightPwm < -1.0) {
            leftPwm += overPower * (-1.0 - rightPwm)
            rightPwm = -1.0
        }
        return DriveSignal(leftPwm, rightPwm)
    }

    /**
     * Calculates wheel powers for curvature drive.
     *
     * @param throttle the throttle value of the input, which is the primary longitudinal force
     * @param culverWheel the axis for turning, which will act like the steering wheel
     * @param isQuickTurn whether or not to use the quickturn feature, which allows the robot to spin
     * more quickly
     * @param isHighGear the state of the drivetrain's gear shifter
     * @return a DriveSignal of the left and right powers calculated
     */
    fun freezyDrive(
        throttle: Double,
        culverWheel: Double,
        isQuickTurn: Boolean,
        isHighGear: Boolean
    ): DriveSignal {
        var throttle = throttle
        var culverWheel = culverWheel
        culverWheel = SimpleUtil.handleDeadband(culverWheel, WHEEL_DEADBAND)
        throttle = SimpleUtil.handleDeadband(throttle, THROTTLE_DEADBAND)

        throttle = Math.pow(throttle, 3.0)

        val negativeInertia = culverWheel - oldWheel
        oldWheel = culverWheel

        val wheelNonLinearity: Double
        if (isHighGear) {
            wheelNonLinearity = HIGH_GEAR_WHEEL_NON_LINEARITY
            val denominator = Math.sin(Math.PI / 2.0 * wheelNonLinearity)
            // Apply a sin function that's scaled to make it feel better.
            culverWheel = Math.sin(Math.PI / 2.0 * wheelNonLinearity * culverWheel) / denominator
            culverWheel = Math.sin(Math.PI / 2.0 * wheelNonLinearity * culverWheel) / denominator
        } else {
            wheelNonLinearity = LOW_GEAR_WHEEL_NON_LINEARITY
            val denominator = Math.sin(Math.PI / 2.0 * wheelNonLinearity)
            // Apply a sin function that's scaled to make it feel better.
            culverWheel = Math.sin(Math.PI / 2.0 * wheelNonLinearity * culverWheel) / denominator
            culverWheel = Math.sin(Math.PI / 2.0 * wheelNonLinearity * culverWheel) / denominator
            culverWheel = Math.sin(Math.PI / 2.0 * wheelNonLinearity * culverWheel) / denominator
        }

        val sensitivity: Double
        val angularPower: Double

        // Negative inertia!
        val negInertiaScalar: Double
        if (isHighGear) {
            negInertiaScalar = HIGH_NEGATIVE_INERTIA_SCALAR
            sensitivity = HIGH_GEAR_SENSITIVITY
        } else {
            if (culverWheel * negativeInertia > 0) {
                // If we are moving away from 0.0, aka, trying to get more culverWheel.
                negInertiaScalar = LOW_NEGATIVE_TURN_SCALAR
            } else {
                // Otherwise, we are attempting to go back to 0.0.
                if (Math.abs(culverWheel) > LOW_NEGATIVE_INERTIA_THRESHOLD) {
                    negInertiaScalar = LOW_NEGATIVE_INERTIA_FAR_SCALAR
                } else {
                    negInertiaScalar = LOW_NEGATIVE_INERTIA_CLOSE_SCALAR
                }
            }
            sensitivity = LOW_GEAR_SENSITIVITY
        }
        val negInertiaPower = negativeInertia * negInertiaScalar
        negativeInertiaAccumulator += negInertiaPower

        culverWheel = culverWheel + negativeInertiaAccumulator
        if (negativeInertiaAccumulator > 1) {
            negativeInertiaAccumulator -= 1.0
        } else if (negativeInertiaAccumulator < -1) {
            negativeInertiaAccumulator += 1.0
        } else {
            negativeInertiaAccumulator = 0.0
        }
        val linearPower = throttle

        var leftPwm: Double
        var rightPwm: Double
        val overPower: Double

        // Quickturn!
        if (isQuickTurn) {
            if (Math.abs(linearPower) < QUICKSTOP_DEAD_BAND) {
                val alpha = QUICKSTOP_WEIGHT
                quickstopAccumulator =
                    (1 - alpha) * quickstopAccumulator + alpha * SimpleUtil.limit(culverWheel, 1.0) * QUICKSTOP_SCALAR
            }
            overPower = 1.0
            angularPower = culverWheel
        } else {
            overPower = 0.0
            angularPower = Math.abs(throttle) * culverWheel * sensitivity - quickstopAccumulator
            if (quickstopAccumulator > 2) {
                quickstopAccumulator -= 1.0
            } else if (quickstopAccumulator < -1) {
                quickstopAccumulator += 1.0
            } else {
                quickstopAccumulator = 0.0
            }
        }

        leftPwm = linearPower
        rightPwm = leftPwm
        leftPwm += angularPower
        rightPwm -= angularPower

        if (leftPwm > 1.0) {
            rightPwm -= overPower * (leftPwm - 1.0)
            leftPwm = 1.0
        } else if (rightPwm > 1.0) {
            leftPwm -= overPower * (rightPwm - 1.0)
            rightPwm = 1.0
        } else if (leftPwm < -1.0) {
            rightPwm += overPower * (-1.0 - leftPwm)
            leftPwm = -1.0
        } else if (rightPwm < -1.0) {
            leftPwm += overPower * (-1.0 - rightPwm)
            rightPwm = -1.0
        }
        return DriveSignal(leftPwm, rightPwm)
    }

    companion object {
        private val THROTTLE_DEADBAND = 0.02
        private val MAGNITUDE_DEADBAND = 0.02
        private val WHEEL_DEADBAND = 0.02

        private val HIGH_GEAR_WHEEL_NON_LINEARITY = 0.65
        private val LOW_GEAR_WHEEL_NON_LINEARITY = 0.65

        private val HIGH_NEGATIVE_INERTIA_SCALAR = 4.0

        private val LOW_NEGATIVE_INERTIA_THRESHOLD = 0.65
        private val LOW_NEGATIVE_TURN_SCALAR = 3.5
        private val LOW_NEGATIVE_INERTIA_CLOSE_SCALAR = 4.0
        private val LOW_NEGATIVE_INERTIA_FAR_SCALAR = 5.0

        private val HIGH_GEAR_SENSITIVITY = 0.95
        private val LOW_GEAR_SENSITIVITY = 1.3

        private val QUICKSTOP_DEAD_BAND = 0.2
        private val QUICKSTOP_WEIGHT = 0.1
        private val QUICKSTOP_SCALAR = 5.0
    }
}
