package com.mmferariz.arcorelib.components

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.AugmentedFace
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.AugmentedFaceNode
import com.mmferariz.arcorelib.R
import java.lang.Exception
import java.util.EnumSet
import java.util.HashMap

class CustomArView @JvmOverloads constructor(
    context: Context,
    activity: Activity,
    attrs: AttributeSet? = null,
    ) : ArSceneView(context, attrs) {

    private var texture: Texture? = null
    private var isAdded = false
    private var isFrontCamera = true
    private val faceNodeMap = HashMap<AugmentedFace, AugmentedFaceNode>()
    private var activity: Activity
    private lateinit var activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks

    init {
            this.activity = activity
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            Texture.builder()
                .setSource(context, R.drawable.fox_face_mesh_texture)
                .build()
                .thenAccept { textureModel -> texture = textureModel }
                .exceptionally {
                    Toast.makeText(context, "Cannot load texture", Toast.LENGTH_SHORT).show()
                    null
                }

            cameraStreamRenderPriority = Renderable.RENDER_PRIORITY_FIRST
            scene?.addOnUpdateListener {
                if (texture == null) return@addOnUpdateListener

                val frame = arFrame ?: return@addOnUpdateListener
                val augmentedFaces = session?.getAllTrackables(AugmentedFace::class.java) ?: listOf()

                for (augmentedFace in augmentedFaces) {
                    if (!faceNodeMap.containsKey(augmentedFace)) {
                        val faceNode = AugmentedFaceNode(augmentedFace)
                        faceNode.setParent(scene)
                        faceNode.faceMeshTexture = texture
                        faceNodeMap[augmentedFace] = faceNode
                    }
                }

                val iter = faceNodeMap.iterator()
                while (iter.hasNext()) {
                    val entry = iter.next()
                    val face = entry.key
                    if (face.trackingState == TrackingState.STOPPED) {
                        val faceNode = entry.value
                        faceNode.setParent(null)
                        iter.remove()
                    }
                }
            }

            setupLifeCycle(context)
        }

    fun createArSession(activity: Activity, isFrontCamera: Boolean): Session? {
        var session: Session? = null
        // if we have the camera permission, create the session
        if (hasCameraPermission(activity)) {
            session = when (ArCoreApk.getInstance().requestInstall(activity, true)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    null
                }
                else -> {
                    if (isFrontCamera) {
                        Session(activity, EnumSet.of(Session.Feature.FRONT_CAMERA))
                    } else {
                        Session(activity)
                    }
                }
            }
            session?.let {
                // Create a camera config filter for the session.
                val filter = CameraConfigFilter(it)

                // Return only camera configs that target 30 fps camera capture frame rate.
                filter.setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30))

                // Return only camera configs that will not use the depth sensor.
                filter.setDepthSensorUsage(EnumSet.of(CameraConfig.DepthSensorUsage.DO_NOT_USE))

                // Get list of configs that match filter settings.
                // In this case, this list is guaranteed to contain at least one element,
                // because both TargetFps.TARGET_FPS_30 and DepthSensorUsage.DO_NOT_USE
                // are supported on all ARCore supported devices.
                val cameraConfigList = it.getSupportedCameraConfigs(filter)

                // Use element 0 from the list of returned camera configs. This is because
                // it contains the camera config that best matches the specified filter
                // settings.
                it.cameraConfig = cameraConfigList[0]
            }

        }
        return session
    }

    fun hasCameraPermission(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun initSession(): Boolean{
        if (session == null) {
            try {
                val session = createArSession(activity, isFrontCamera)
                return if (session != null) {
                    val config = Config(session)
                    if (isFrontCamera) {
                        config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
                    }
                    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    config.focusMode = Config.FocusMode.AUTO;
                    session.configure(config)
                    setupSession(session)
                    true
                } else {
                    false
                }
            } catch (ex: Exception) {
                return false
            }
        } else {
            return true
        }
    }

    private fun setupLifeCycle(context: Context) {
        activityLifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
//                maybeEnableArButton()
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
                onResume()
            }

            override fun onActivityPaused(activity: Activity) {
                onPause()
            }

            override fun onActivityStopped(activity: Activity) {
//                onPause()
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {
//                onDestroy()
//                dispose()
            }
        }

        activity.application.registerActivityLifecycleCallbacks(this.activityLifecycleCallbacks)
    }

    fun onResume() {
        // request camera permission if not already requested
        if (hasCameraPermission(activity)) {
            //ArCoreUtils.requestCameraPermission(activity, RC_PERMISSIONS)
        }

        initSession()

        try {
            resume()
        } catch (ex: CameraNotAvailableException) {
            activity.finish()
            return
        }

    }

    fun onPause() {
        // if (this != null) {
        //     arSceneView?.pause()
        // }
    }
}