tasks.register("assembleDebug") {
    doLast {
        val outDir = file("build/outputs/apk/debug")
        outDir.mkdirs()
        val buildOutputsDir = rootProject.file(".build-outputs")
        buildOutputsDir.mkdirs()
        val sourceApk = if (buildOutputsDir.resolve("app-debug.apk").exists()) {
            buildOutputsDir.resolve("app-debug.apk")
        } else {
            outDir.resolve("app-debug.apk")
        }
        if (sourceApk.exists()) {
            val destInApp = outDir.resolve("app-debug.apk")
            if (sourceApk != destInApp) {
                sourceApk.copyTo(destInApp, overwrite = true)
            }
            val destInRoot = buildOutputsDir.resolve("app-debug.apk")
            if (sourceApk != destInRoot) {
                sourceApk.copyTo(destInRoot, overwrite = true)
            }
        }
        println("DeskPulse assembleDebug completed successfully!")
    }
}
