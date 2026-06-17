package com.pfa.interview.data.repository

import android.content.Context
import android.net.Uri
import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.remote.api.CvApi
import com.pfa.interview.data.remote.dto.CvAnalysisResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class CvRepositoryImpl @Inject constructor(
    private val api: CvApi
) : CvRepository {

    override suspend fun analyzeCv(uri: Uri, context: Context): Resource<CvAnalysisResponse> {
        return try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: return Resource.Error("Impossible d'ouvrir le fichier")
            val bytes = stream.readBytes()
            stream.close()

            if (bytes.size > 5 * 1024 * 1024) {
                return Resource.Error("Le fichier dépasse 5 MB")
            }

            val requestBody = bytes.toRequestBody("application/pdf".toMediaType())
            val part = MultipartBody.Part.createFormData("file", "cv.pdf", requestBody)

            val resp = api.analyzeCv(part)
            if (resp.isSuccessful) Resource.Success(resp.body()!!)
            else Resource.Error("Erreur ${resp.code()}: ${resp.errorBody()?.string()}")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Erreur inconnue lors de l'analyse du CV")
        }
    }
}
