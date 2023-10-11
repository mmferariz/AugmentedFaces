package com.mmferariz.arcorelib.components

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.widget.Toast
import com.google.ar.core.AugmentedFace
import com.google.ar.core.Config
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.AugmentedFaceNode
import com.mmferariz.arcorelib.R
import java.util.HashMap

class ArCoreFragment @JvmOverloads constructor(
    context: Context,
    activity: Activity,
    attrs: AttributeSet? = null,
    ) : ArSceneView(context, attrs) {

    private var texture: Texture? = null
    private var isAdded = false
    private val faceNodeMap = HashMap<AugmentedFace, AugmentedFaceNode>()

        init {
            val config = Config(session)
            config.augmentedFaceMode = Config.AugmentedFaceMode.MESH3D
            setupSession(session)

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