package com.drivesolutions.safedrive.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.drivesolutions.safedrive.R
import com.drivesolutions.safedrive.ml.SsdMobilenetV1TfliteMetadataV2
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

class ObjectDetectionFragment : Fragment() {
    private val paint = Paint()
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var model: SsdMobilenetV1TfliteMetadataV2
    private lateinit var bitmap: Bitmap
    private lateinit var imageView: ImageView
    private lateinit var cameraDevice: CameraDevice
    private lateinit var handler: Handler
    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_object_detection, container, false)

        imageView = view.findViewById(R.id.imageView)
        textureView = view.findViewById(R.id.textureView)

        get_permission()

        // Initialize the image processor and model
        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR))
            .build()
        model = SsdMobilenetV1TfliteMetadataV2.newInstance(requireContext())

        // Set up the background thread
        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // Set up the TextureView listener
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                open_camera()
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {}

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                // Capture the current bitmap from the texture view
                bitmap = textureView.bitmap!!

                // Get the original dimensions of the bitmap
                val originalWidth = bitmap.width
                val originalHeight = bitmap.height

                // Process the image
                var image = TensorImage.fromBitmap(bitmap)
                image = imageProcessor.process(image)

                // Run model inference
                val outputs = model.process(image)

                // Create a mutable bitmap to draw bounding boxes
                val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutableBitmap)

                // Iterate over detection results
                for (detectionResult in outputs.detectionResultList) {
                    val score = detectionResult.scoreAsFloat
                    if (score < 0.5) continue // Filter low-confidence detections

                    // Get bounding box location
                    val location = detectionResult.locationAsRectF

                    // Scale bounding box coordinates
                    val scaledLeft = location.left * originalWidth / 300 // Assuming input size is 300
                    val scaledTop = location.top * originalHeight / 300
                    val scaledRight = location.right * originalWidth / 300
                    val scaledBottom = location.bottom * originalHeight / 300

                    // Create a RectF using the scaled coordinates
                    val scaledLocation = android.graphics.RectF(scaledLeft, scaledTop, scaledRight, scaledBottom)

                    // Set up the Paint object for drawing the bounding box
                    paint.color = android.graphics.Color.RED
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 5f

                    // Draw the bounding box
                    canvas.drawRect(scaledLocation, paint)

                    // Set up Paint for label text
                    paint.style = Paint.Style.FILL
                    paint.textSize = 50f
                    paint.color = android.graphics.Color.WHITE

                    // Draw the category and score above the bounding box
                    canvas.drawText("${detectionResult.categoryAsString}: ${"%.2f".format(score)}", scaledLocation.left, scaledLocation.top - 10, paint)
                }

                // Update the ImageView with the new bitmap
                imageView.setImageBitmap(mutableBitmap)
            }
        }

        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release model resources
        model.close()
    }

    @SuppressLint("MissingPermission")
    private fun open_camera() {
        cameraManager.openCamera(cameraManager.cameraIdList[0], object : CameraDevice.StateCallback() {
            @SuppressLint("Recycle")
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera

                val surfaceTexture = textureView.surfaceTexture
                val surface = Surface(surfaceTexture)

                val captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequest.addTarget(surface)

                cameraDevice.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        session.setRepeatingRequest(captureRequest.build(), null, null)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                }, handler)
            }

            override fun onDisconnected(camera: CameraDevice) {}
            override fun onError(camera: CameraDevice, error: Int) {}
        }, handler)
    }

    private fun get_permission() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101) // Correctly use requestPermissions
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            get_permission()
        }
    }
}
