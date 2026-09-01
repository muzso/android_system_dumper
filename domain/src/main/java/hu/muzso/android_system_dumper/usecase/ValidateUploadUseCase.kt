package hu.muzso.android_system_dumper.usecase

import hu.muzso.android_system_dumper.common.NetworkUtils
import hu.muzso.android_system_dumper.model.upload.UploadParameters
import hu.muzso.android_system_dumper.platform.ResourceProvider
import javax.inject.Inject

class ValidateUploadUseCase @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val networkUtils: NetworkUtils
) {
    sealed class ValidationResult {
        object Success : ValidationResult()
        sealed class Error : ValidationResult() {
            data class InvalidBatchSize(val min: Int, val max: Int) : Error()
            data class InvalidProxy(val spec: String) : Error()
            object NoUploadSelected : Error()
        }
    }

    /**
     * Validates the provided upload parameters.
     * 
     * @param parameters The upload parameters to validate.
     * @return A [ValidationResult] indicating success or the specific validation error.
     */
    fun execute(parameters: UploadParameters): ValidationResult {
        val minBatchSize = resourceProvider.getMinBatchSizeMb()
        val maxBatchSize = resourceProvider.getMaxBatchSizeMb()
        val batchSizeMb = parameters.customBatchSizeMb
        
        if (batchSizeMb < minBatchSize || batchSizeMb > maxBatchSize) {
            return ValidationResult.Error.InvalidBatchSize(minBatchSize, maxBatchSize)
        }

        if (parameters.proxySpecification.isNotEmpty() && networkUtils.getProxyFromSpecification(parameters.proxySpecification) == null) {
            return ValidationResult.Error.InvalidProxy(parameters.proxySpecification)
        }

        if (!parameters.shouldUploadZips && !parameters.shouldUploadFileLists) {
            return ValidationResult.Error.NoUploadSelected
        }

        return ValidationResult.Success
    }
}
