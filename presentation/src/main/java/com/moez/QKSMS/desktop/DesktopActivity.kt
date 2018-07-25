package com.moez.QKSMS.desktop

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.pm.PackageManager

import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.moez.QKSMS.R
import com.moez.QKSMS.common.QkDialog
import com.moez.QKSMS.common.base.QkThemedActivity
import com.moez.QKSMS.common.util.extensions.setBackgroundTint
import com.moez.QKSMS.common.util.extensions.setVisible
import com.moez.QKSMS.feature.settings.SettingsState
import com.moez.QKSMS.util.Preferences
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.settings_activity.*
import kotlinx.android.synthetic.main.settings_switch_widget.view.*
import kotlinx.android.synthetic.main.settings_theme_widget.*
import javax.inject.Inject

class DesktopActivity : QkThemedActivity(), DesktopView {

    @Inject
    lateinit var nightModeDialog: QkDialog
    @Inject
    lateinit var textSizeDialog: QkDialog
    @Inject
    lateinit var sendDelayDialog: QkDialog
    @Inject
    lateinit var mmsSizeDialog: QkDialog
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var svBarcode: SurfaceView
    private lateinit var tvBarCode: TextView
    private lateinit var detector: BarcodeDetector
    private lateinit var cameraSource: CameraSource

    private val progressDialog by lazy {
        ProgressDialog(this).apply {
            isIndeterminate = true
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
    }

    override fun render(state: SettingsState) {
        if (progressDialog.isShowing && !state.syncing) progressDialog.dismiss()
        else if (!progressDialog.isShowing && state.syncing) progressDialog.show()

        themePreview.setBackgroundTint(state.theme)
        night.summary = state.nightModeSummary
        nightModeDialog.adapter.selectedItem = state.nightModeId
        nightStart.setVisible(state.nightModeId == Preferences.NIGHT_MODE_AUTO)
        nightStart.summary = state.nightStart
        nightEnd.setVisible(state.nightModeId == Preferences.NIGHT_MODE_AUTO)
        nightEnd.summary = state.nightEnd

        black.setVisible(state.nightModeId != Preferences.NIGHT_MODE_OFF)
        black.checkbox.isChecked = state.black

        autoEmoji.checkbox.isChecked = state.autoEmojiEnabled

        delayed.summary = state.sendDelaySummary
        sendDelayDialog.adapter.selectedItem = state.sendDelayId

        delivery.checkbox.isChecked = state.deliveryEnabled

        textSize.summary = state.textSizeSummary
        textSizeDialog.adapter.selectedItem = state.textSizeId
        systemFont.checkbox.isChecked = state.systemFontEnabled

        unicode.checkbox.isChecked = state.stripUnicodeEnabled

        mmsSize.summary = state.maxMmsSizeSummary
        mmsSizeDialog.adapter.selectedItem = state.maxMmsSizeId    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_desktop)
        showBackButton(true)

        svBarcode= findViewById(R.id.sv_barcode)
        tvBarCode= findViewById(R.id.tv_barcode)

        codeDetector()
        cameraSource = CameraSource.Builder(this, detector).setRequestedPreviewSize(1024, 768)
                .setRequestedFps(25f).setAutoFocusEnabled(true).build()
        surfaceViewDetector()
    }

    private fun surfaceViewDetector() {
        svBarcode.holder.addCallback(object : SurfaceHolder.Callback2{
            override fun surfaceRedrawNeeded(p0: SurfaceHolder?) {}
            override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {}
            override fun surfaceDestroyed(p0: SurfaceHolder?) {
                cameraSource.stop()
            }
            override fun surfaceCreated(holder: SurfaceHolder?) {
                if(ContextCompat.checkSelfPermission(this@DesktopActivity, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                    cameraSource.start(holder)
                }
                else{
                    ActivityCompat.requestPermissions(this@DesktopActivity, arrayOf(android.Manifest.permission.CAMERA), 123)
                }
            }
        })
    }

    private fun codeDetector() {
        detector = BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build()
        detector.setProcessor(object : Detector.Processor<Barcode>{
            override fun release() {}
            override fun receiveDetections(detections: Detector.Detections<Barcode>?) {
                var barCodes = detections?.detectedItems
                if(barCodes!!.size()>0){
                    tvBarCode.post { tvBarCode.text= barCodes.valueAt(0).displayValue }
                }
            }
        })
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode ==123){
            if (grantResults.isNotEmpty() && grantResults[0]== PackageManager.PERMISSION_GRANTED)
                cameraSource.start(svBarcode.holder)
            else Toast.makeText(this, "Scanner won't work without permission", Toast.LENGTH_LONG).show()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        release()
    }

    private fun release() {
        detector.release()
        cameraSource.stop()
        cameraSource.release()
    }



}
