package com.wapo.flagship.external

import android.graphics.*

class BitmapUtils {
    companion object {
        @JvmStatic
        fun parseBitmap(
            data: ByteArray?,
            fromFile: Boolean,
            maxWidth: Int,
            maxHeight: Int,
        ): Any? {
            if (data == null) {
                return null
            }

            var filePath: String? = null
            if (fromFile) {
                filePath = String(data)
            }
            var bitmap: Bitmap?
            val decodeOptions = BitmapFactory.Options()
            if (maxWidth == 0 && maxHeight == 0 || maxHeight < 0 || maxWidth < 0) {
                decodeOptions.inPreferredConfig = Bitmap.Config.RGB_565
                if (fromFile) {
                    bitmap = BitmapFactory.decodeFile(filePath, decodeOptions)
                } else {
                    bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, decodeOptions)
                }
            } else {
                // If we have to resize this image, first get the natural bounds.
                decodeOptions.inJustDecodeBounds = true
                if (fromFile) {
                    BitmapFactory.decodeFile(filePath, decodeOptions)
                } else {
                    BitmapFactory.decodeByteArray(data, 0, data.size, decodeOptions)
                }
                val actualWidth = decodeOptions.outWidth
                val actualHeight = decodeOptions.outHeight

                // Then compute the dimensions we would ideally like to decode to.
                val desiredWidth =
                    getResizedDimension(
                        maxWidth,
                        maxHeight,
                        actualWidth,
                        actualHeight,
                    )
                val desiredHeight =
                    getResizedDimension(
                        maxHeight,
                        maxWidth,
                        actualHeight,
                        actualWidth,
                    )

                // Decode to the nearest power of two scaling factor.
                decodeOptions.inJustDecodeBounds = false
                // TODO(ficus): Do we need this or is it okay since API 8 doesn't support it?
                // decodeOptions.inPreferQualityOverSpeed = PREFER_QUALITY_OVER_SPEED;
                decodeOptions.inSampleSize =
                    findBestSampleSize(
                        actualWidth,
                        actualHeight,
                        desiredWidth,
                        desiredHeight,
                    )
                // bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
                if (fromFile) {
                    bitmap = BitmapFactory.decodeFile(filePath, decodeOptions)
                } else {
                    bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, decodeOptions)
                }

                if (bitmap != null && desiredWidth > 0 && desiredHeight > 0) {
                    bitmap = Bitmap.createScaledBitmap(bitmap, desiredWidth, desiredHeight, true)
                }
            }
            return bitmap
        }

        @JvmStatic
        private fun getResizedDimension(
            maxPrimary: Int,
            maxSecondary: Int,
            actualPrimary: Int,
            actualSecondary: Int,
        ): Int {
            // If no dominant value at all, just return the actual.
            if (maxPrimary == 0 && maxSecondary == 0) {
                return actualPrimary
            }

            // If primary is unspecified, scale primary to match secondary's scaling ratio.
            if (maxPrimary == 0) {
                val ratio = maxSecondary.toDouble() / actualSecondary.toDouble()
                return (actualPrimary * ratio).toInt()
            }

            if (maxSecondary == 0) {
                return maxPrimary
            }

            val ratio = actualSecondary.toDouble() / actualPrimary.toDouble()
            var resized = maxPrimary
            if (resized * ratio > maxSecondary) {
                resized = (maxSecondary / ratio).toInt()
            }
            return resized
        }

        @JvmStatic
        private fun findBestSampleSize(
            actualWidth: Int,
            actualHeight: Int,
            desiredWidth: Int,
            desiredHeight: Int,
        ): Int {
            val wr = actualWidth.toDouble() / desiredWidth
            val hr = actualHeight.toDouble() / desiredHeight
            val ratio = Math.min(wr, hr) + 0.25
            return Math.round(ratio).toInt()
        }
    }
}
