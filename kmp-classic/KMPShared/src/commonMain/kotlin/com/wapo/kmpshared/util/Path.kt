package com.wapo.kmpshared.util

import kotlinx.io.files.Path
import kotlinx.io.files.SystemPathSeparator

internal operator fun Path.div(child: String): Path = Path(this.toString() + SystemPathSeparator + child)
