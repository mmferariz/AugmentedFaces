package com.mmferariz.arcorelib.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.AttributeSet
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.AugmentedFace
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.AugmentedFaceNode
import com.mmferariz.arcorelib.R
import java.lang.Exception
import java.util.EnumSet
import java.util.HashMap

class ArCoreFragment @JvmOverloads constructor(
    context: Context,
    activity: Activity,
    attrs: AttributeSet? = null,
    ) : ArSceneView(context, attrs) {

    private var texture: Texture? = null
    private var isAdded = false
    private var isFrontCamera = true
    private val faceNodeMap = HashMap<AugmentedFace, AugmentedFaceNode>()

        init {
            if(initSession(activity)){
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
                    val augmentedFaces = frame.getUpdatedTrackables(AugmentedFace::class.java)

                    for (augmentedFace in augmentedFaces) {
                        if (isAdded) return@addOnUpdateListener

                        val augmentedFaceNode = AugmentedFaceNode(augmentedFace).apply {
                            setParent(scene)
                            setFaceMeshTexture(texture)
                        }
                        faceNodeMap[augmentedFace] = augmentedFaceNode
                        isAdded = true

                        val iterator = faceNodeMap.entries.iterator()
                        while (iterator.hasNext()) {
                            val entry = iterator.next()
                            if (entry.key.trackingState == TrackingState.STOPPED) {
                                entry.value.setParent(null)
                                iterator.remove()
                            }
                        }
                    }
                }
            }
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

    private fun initSession(activity: Activity): Boolean{
        if (session == null) {
            try {
                val session = createArSession(activity, isFrontCamera)
                if (session != null) {
                    val config = Config(session)
                    if (isFrontCamera) {
                        config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
                    }
                    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    config.focusMode = Config.FocusMode.AUTO;
                    session.configure(config)
                    setupSession(session)
                    return true
                }
                return false
            } catch (ex: Exception) {
                return false
            }
        } else {
            return true
        }
    }

}