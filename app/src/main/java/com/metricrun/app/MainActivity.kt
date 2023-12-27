package com.metricrun.app

import android.content.Intent
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.metricrun.app.databinding.MainLayoutBinding

class MainActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {

    private lateinit var binding: MainLayoutBinding
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configuración de Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.videoTextureView.surfaceTextureListener = this

        // Manejar el botón de inicio de sesión con Google
        binding.botonIngresar.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            task.addOnFailureListener {
                Log.e("MainActivity", "Error durante el inicio de sesión: ", it)
            }
            handleSignInResult(task)
        }
    }


    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Log.d("MainActivity", "Inicio de sesión exitoso: ${account.displayName}")

            val intent = Intent(this, HomeActivity::class.java).apply {
                putExtra("USER_NAME", account.displayName)
            }
            startActivity(intent)

        } catch (e: ApiException) {
            Log.e("MainActivity", "Inicio de sesión fallido", e)
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        mediaPlayer = MediaPlayer().apply {
            setDataSource("http://54.221.216.132/metricrun/videoback.mp4")
            setSurface(Surface(surface))
            prepareAsync()
            setOnPreparedListener { start() }
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        // Manejar cambios en la superficie si es necesario
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        mediaPlayer?.release()
        mediaPlayer = null
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // Actualizar la vista aquí si es necesario
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}

private const val RC_SIGN_IN = 100 // Código de solicitud para el inicio de sesión
