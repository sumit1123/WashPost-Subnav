package com.wapo.kmpshared.logger.data

import com.wapo.kmpshared.util.div
import kotlinx.io.files.Path
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

internal actual fun platformLogRoot(): Path {
    val base = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)[0] as String
    return Path(base) / "Logger"
}
