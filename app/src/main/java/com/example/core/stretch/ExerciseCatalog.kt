package com.example.core.stretch

/**
 * Body Zones targeted during stretch suggestions.
 */
enum class BodyZone(val displayName: String) {
    NECK("Neck"),
    SHOULDER_HAND("Shoulder & Hand"),
    BACK("Back & Spine"),
    LEGS("Legs")
}

/**
 * Stretch exercise model for healthy posture and ergonomic relief.
 */
data class StretchExercise(
    val id: String,
    val name: String,
    val zone: BodyZone,
    val setsText: String,
    val durationSeconds: Int = 45,
    val twoSided: Boolean = false,
    val mediaFileName: String,
    val isStanding: Boolean = false,
    val isFixed: Boolean = false
)

/**
 * ExerciseCatalog: Single, easy-to-edit catalog of all fixed and shuffled exercises.
 */
object ExerciseCatalog {

    // 4 Fixed Exercises (included every full break)
    val fixedExercises = listOf(
        StretchExercise(
            id = "chin_tuck",
            name = "Chin tuck",
            zone = BodyZone.NECK,
            setsText = "5 sec hold x 5 times",
            durationSeconds = 45,
            twoSided = false,
            mediaFileName = "chin_tuck.mp4",
            isStanding = false,
            isFixed = true
        ),
        StretchExercise(
            id = "wrist_stretch",
            name = "Wrist stretch",
            zone = BodyZone.SHOULDER_HAND,
            setsText = "15 sec x 2 hands",
            durationSeconds = 45,
            twoSided = true,
            mediaFileName = "wrist_stretch.mp4",
            isStanding = false,
            isFixed = true
        ),
        StretchExercise(
            id = "standing_back_extension",
            name = "Standing back extension",
            zone = BodyZone.BACK,
            setsText = "10 sec x 3 times",
            durationSeconds = 45,
            twoSided = false,
            mediaFileName = "standing_back_extension.mp4",
            isStanding = true,
            isFixed = true
        ),
        StretchExercise(
            id = "calf_raises",
            name = "Calf raises",
            zone = BodyZone.LEGS,
            setsText = "15 reps x 1 set",
            durationSeconds = 45,
            twoSided = false,
            mediaFileName = "calf_raises.mp4",
            isStanding = true,
            isFixed = true
        )
    )

    // Seated substitutes for standing exercises when "Only sitting exercises" is ON
    val seatedPostureHold = StretchExercise(
        id = "seated_posture_hold",
        name = "Seated posture hold",
        zone = BodyZone.BACK,
        setsText = "10 sec x 3 times",
        durationSeconds = 45,
        twoSided = false,
        mediaFileName = "seated_posture_hold.mp4",
        isStanding = false,
        isFixed = false
    )

    val anklePumps = StretchExercise(
        id = "ankle_pumps",
        name = "Ankle pumps",
        zone = BodyZone.LEGS,
        setsText = "15 times",
        durationSeconds = 45,
        twoSided = false,
        mediaFileName = "ankle_pumps.mp4",
        isStanding = false,
        isFixed = false
    )

    // 8 Shuffled Pool Exercises (2 picked each break from DIFFERENT zones)
    val shuffledPool = listOf(
        StretchExercise(
            id = "neck_side_stretch",
            name = "Neck side stretch",
            zone = BodyZone.NECK,
            setsText = "15 sec x 2 sides",
            durationSeconds = 45,
            twoSided = true,
            mediaFileName = "neck_side_stretch.mp4"
        ),
        StretchExercise(
            id = "upper_trap_stretch",
            name = "Upper trap stretch",
            zone = BodyZone.NECK,
            setsText = "15 sec x 2 sides",
            durationSeconds = 45,
            twoSided = true,
            mediaFileName = "upper_trap_stretch.mp4"
        ),
        StretchExercise(
            id = "isometric_neck_press",
            name = "Isometric neck press",
            zone = BodyZone.NECK,
            setsText = "5 sec x 5 times",
            durationSeconds = 45,
            twoSided = false,
            mediaFileName = "isometric_neck_press.mp4"
        ),
        StretchExercise(
            id = "shoulder_rolls",
            name = "Shoulder rolls",
            zone = BodyZone.SHOULDER_HAND,
            setsText = "10 forward + 10 backward",
            durationSeconds = 45,
            twoSided = false,
            mediaFileName = "shoulder_rolls.mp4"
        ),
        StretchExercise(
            id = "overhead_reach",
            name = "Overhead reach",
            zone = BodyZone.SHOULDER_HAND,
            setsText = "15 sec x 2 times",
            durationSeconds = 45,
            twoSided = false,
            mediaFileName = "overhead_reach.mp4"
        ),
        StretchExercise(
            id = "seated_twist",
            name = "Seated twist",
            zone = BodyZone.BACK,
            setsText = "15 sec x 2 sides",
            durationSeconds = 45,
            twoSided = true,
            mediaFileName = "seated_twist.mp4"
        ),
        StretchExercise(
            id = "seated_hamstring_stretch",
            name = "Seated hamstring stretch",
            zone = BodyZone.LEGS,
            setsText = "15 sec x 2 sides",
            durationSeconds = 45,
            twoSided = true,
            mediaFileName = "seated_hamstring_stretch.mp4"
        ),
        StretchExercise(
            id = "ankle_rotations",
            name = "Ankle rotations",
            zone = BodyZone.LEGS,
            setsText = "10 rotations x 2 feet",
            durationSeconds = 45,
            twoSided = true,
            mediaFileName = "ankle_rotations.mp4"
        )
    )
}
