package jp.coe.simpleble

import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import jp.coe.simpleble.databinding.ActivityImageBinding
import jp.coe.simpleble.observable.MainObservable
import java.net.URI

class ImageActivity : AppCompatActivity() {

    var mainObservable = MainObservable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityImageBinding>(this,R.layout.activity_image)
        binding.mainObservable = mainObservable

        val imagebyte = intent.getParcelableExtra<Uri>(IMAGE_URI)
        mainObservable.imageUri = imagebyte

    }

    companion object {
//        val IMAGE_BITMAP = "param1"
        val IMAGE_URI = "param2"
    }
}
