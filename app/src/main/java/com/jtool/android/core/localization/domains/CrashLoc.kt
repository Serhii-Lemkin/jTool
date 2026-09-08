package com.jtool.android.core.localization.domains

import com.jtool.android.core.localization.Loc

object CrashLoc {
    val title get() = Loc.s("crash_title", "Oops! App Crashed")
    val msg get() = Loc.s("crash_msg", "A fatal error occurred. You can report this to the developer.")
    val reportBug get() = Loc.s("crash_reportBug", "Report bug")
    val reporting get() = Loc.s("crash_reporting", "Reporting...")
    val automatedReport get() = Loc.s("crash_automatedReport", "Automated Crash Report")
    val sentSuccess get() = Loc.s("crash_sentSuccess", "Report sent! Thank you.")
    val goHome get() = Loc.s("crash_goHome", "Go to Home")
}
