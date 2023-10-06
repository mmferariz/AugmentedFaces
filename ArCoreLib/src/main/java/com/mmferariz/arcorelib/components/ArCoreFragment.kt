package com.mmferariz.arcorelib.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.AugmentedFace
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.AugmentedFaceNode
import com.mmferariz.arcorelib.R
import com.mmferariz.arcorelib.utils.CustomArFragment
import java.util.HashMap

class ArCoreFragment @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0,
    ) : LinearLayout(context, attrs, defStyle, defStyleRes) {

    private var texture: Texture? = null
    private var isAdded = false
    private val faceNodeMap = HashMap<AugmentedFace, AugmentedFaceNode>()

        init {
            LayoutInflater.from(context).inflate(R.layout.component_ar_layout, this, true)
            val customArFragment = (context as AppCompatActivity).supportFragmentManager.findFragmentById(R.id.arFragment) as CustomArFragment?

            Texture.builder()
                .setSource(context, R.drawable.fox_face_mesh_texture)
                .build()
                .thenAccept { textureModel -> texture = textureModel }
                .exceptionally {
                    Toast.makeText(context, "Cannot load texture", Toast.LENGTH_SHORT).show()
                    null
                }

            customArFragment?.arSceneView?.setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST)
            customArFragment?.arSceneView?.scene?.addOnUpdateListener {
                if (texture == null) return@addOnUpdateListener

                val frame = customArFragment.arSceneView.arFrame ?: return@addOnUpdateListener
                val augmentedFaces = frame.getUpdatedTrackables(AugmentedFace::class.java)

                for (augmentedFace in augmentedFaces) {
                    if (isAdded) return@addOnUpdateListener

                    val augmentedFaceNode = AugmentedFaceNode(augmentedFace).apply {
                        setParent(customArFragment.arSceneView.scene)
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