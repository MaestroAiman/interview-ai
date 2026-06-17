package com.pfa.interview.data.repository

import android.content.Context
import android.net.Uri
import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.remote.dto.CvAnalysisResponse

interface CvRepository {
    suspend fun analyzeCv(uri: Uri, context: Context): Resource<CvAnalysisResponse>
}
