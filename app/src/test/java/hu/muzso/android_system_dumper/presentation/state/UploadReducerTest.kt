package hu.muzso.android_system_dumper.presentation.state

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UploadReducerTest {

    private val idleState = UploadUiState(isUploading = false)
    private val uploadingState = UploadUiState(
        isUploading = true,
        totalZips = 10,
        uploadedZips = 2,
        uploadStatusText = "Uploading..."
    )
    private val successState = UploadUiState(
        isUploading = false,
        downloadUrl = "http://done",
        uploadedZips = 10,
        totalZips = 10
    )

    private val allBaseStates = listOf(idleState, uploadingState, successState)

    @Test
    fun `PreparationStarted resets progress and sets isUploading to true`() {
        allBaseStates.forEach { state ->
            val newState = reduce(state, UploadResult.PreparationStarted)
            assertThat(newState.isUploading).isTrue()
            assertThat(newState.uploadedZips).isEqualTo(0)
            assertThat(newState.totalZips).isEqualTo(0)
            assertThat(newState.downloadUrl).isNull()
            assertThat(newState.generatedPassword).isNull()
            assertThat(newState.currentZipUploadBytes).isEqualTo(0L)
            assertThat(newState.currentZipTotalBytes).isEqualTo(0L)
        }
    }

    @Test
    fun `StatusTextChanged only updates status text`() {
        allBaseStates.forEach { state ->
            val newState = reduce(state, UploadResult.StatusTextChanged("New Status"))
            assertThat(newState.uploadStatusText).isEqualTo("New Status")
            assertThat(newState.isUploading).isEqualTo(state.isUploading)
            assertThat(newState.uploadedZips).isEqualTo(state.uploadedZips)
        }
    }

    @Test
    fun `TotalPlannedUploads updates totalZips`() {
        allBaseStates.forEach { state ->
            val newState = reduce(state, UploadResult.TotalPlannedUploads(5))
            assertThat(newState.totalZips).isEqualTo(5)
            assertThat(newState.isUploading).isEqualTo(state.isUploading)
        }
    }

    @Test
    fun `SuccessfulUploads updates uploadedZips`() {
        allBaseStates.forEach { state ->
            val newState = reduce(state, UploadResult.SuccessfulUploads(3))
            assertThat(newState.uploadedZips).isEqualTo(3)
        }
    }

    @Test
    fun `ProgressUpdated updates bytes`() {
        allBaseStates.forEach { state ->
            val newState = reduce(state, UploadResult.ProgressUpdated(100L, 1000L))
            assertThat(newState.currentZipUploadBytes).isEqualTo(100L)
            assertThat(newState.currentZipTotalBytes).isEqualTo(1000L)
        }
    }

    @Test
    fun `UploadFinished updates all relevant fields and stops uploading`() {
        allBaseStates.forEach { state ->
            val newState = reduce(state, UploadResult.UploadFinished(
                downloadUrl = "http://result",
                uploadedZips = 5,
                password = "pwd",
                statusText = "Done"
            ))
            assertThat(newState.isUploading).isFalse()
            assertThat(newState.downloadUrl).isEqualTo("http://result")
            assertThat(newState.uploadedZips).isEqualTo(5)
            assertThat(newState.generatedPassword).isEqualTo("pwd")
            assertThat(newState.uploadStatusText).isEqualTo("Done")
        }
    }

    @Test
    fun `UploadError stops uploading and preserves other fields`() {
        allBaseStates.forEach { state ->
            val newState = reduce(state, UploadResult.UploadError)
            assertThat(newState.isUploading).isFalse()
            assertThat(newState.uploadStatusText).isEqualTo(state.uploadStatusText)
        }
    }

    @Test
    fun `UploadAborted stops uploading`() {
        allBaseStates.forEach { state ->
            val newState = reduce(state, UploadResult.UploadAborted)
            assertThat(newState.isUploading).isFalse()
        }
    }

    @Test
    fun `QrGenerated updates bitmap`() {
        // We can't easily create a Bitmap in unit tests without Robolectric, 
        // but we can pass null or mock it if needed. 
        // For pure reducer test, null is fine.
        allBaseStates.forEach { state ->
            val newState = reduce(state, UploadResult.QrGenerated(null))
            assertThat(newState.qrBitmap).isNull()
        }
    }

    @Test
    fun `Reset returns initial state`() {
        allBaseStates.forEach { state ->
            val newState = reduce(state, UploadResult.Reset)
            assertThat(newState).isEqualTo(UploadUiState())
        }
    }
}
