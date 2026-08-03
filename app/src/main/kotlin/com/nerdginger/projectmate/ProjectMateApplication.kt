package com.nerdginger.projectmate

import android.app.Application

/**
 * Owns the dependency graph for the process.
 *
 * Wiring is done by hand rather than with Hilt — see docs/DECISIONS.md D-004.
 * [container] is populated in a later commit, once there is a database to put
 * in it.
 */
class ProjectMateApplication : Application()
