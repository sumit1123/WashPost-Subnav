package com.wapo.flagship

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.wapo.flagship.content.image.ImageService
import com.wapo.flagship.views.WaPoImageView

class ImageTestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val linearLayout = LinearLayout(this)
        setContentView(linearLayout)
        linearLayout.orientation = LinearLayout.VERTICAL
        val waPoImageView = WaPoImageView(this)
        val lp =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
            )
        linearLayout.addView(waPoImageView, lp)

        val images =
            arrayOf(
                "https://upload.wikimedia.org/wikipedia/commons/d/d9/Big_Bear_Valley,_California.jpg",
                "http://apod.nasa.gov/apod/image/9712/orionfull_jcc_big.jpg",
                "http://allwallpapersnew.com/wp-content/gallery/big-pictures-of-animals/Big-Cats-wild-animals-3633223-1600-1200.jpg",
                "http://www.wired.com/wp-content/uploads/2015/03/Big-Data_LEO-PIA.png",
                "http://tpwd.texas.gov/state-parks/big-bend-ranch/gallery/BBRSP_4158.jpg",
            )
        val url = "http://www.telegraph.co.uk/content/dam/science/2016/03/14/cat_3240574b-large_trans++pJliwavx4coWFCaEkEsb3kvxIt-lGGWCWqwLa_RXJU8.jpg"
        val service =
            ImageService(
                FlagshipApplication.getInstance().animatedImageLoader,
                FlagshipApplication.getInstance().requestQueue,
            )
        waPoImageView.setImageUrl(url, service)

//        images.forEachIndexed { i, url ->
//            val imageView = linearLayout.getChildAt(i) as NetworkAnimatedImageView
//            imageView.setImageUrl(url,
//                    FlagshipApplication.getInstance().animatedImageLoader, images.size - i)
//
//        }
//
//        lastImage.setImageUrl(images.last(), FlagshipApplication.getInstance().animatedImageLoader, 100)
    }
}
