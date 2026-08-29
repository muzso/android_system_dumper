package hu.muzso.android_system_dumper.network

import com.google.common.truth.Truth.assertThat
import hu.muzso.android_system_dumper.domain.fixtures.FakeFileLogger
import org.junit.jupiter.api.Test

class BatchLogicTest {

  private val fakeLogger = FakeFileLogger()

  @Test
  fun batchLogic_emptyFiles() {
    val logic = BatchingLogic(FakeFileLogger())
    val result = logic.splitIntoBatches(emptyList(), emptyMap(), 100L, 0)
    assertThat(result).isEmpty()
  }

  @Test
  fun batchLogic_simpleCase() {
    val files = listOf("fileA", "fileB", "fileC")
    val fileSizes = mapOf("fileA" to 10L, "fileB" to 20L, "fileC" to 15L)
    val batchLimit = 30L

    val result = BatchingLogic(fakeLogger).splitIntoBatches(files, fileSizes, batchLimit, 0)

    // fileA (10) + fileB (20) = 30 (limit reached)
    // fileC (15) in next batch
    assertThat(result).hasSize(2)
    assertThat(result[0]).containsExactly("fileA", "fileB").inOrder()
    assertThat(result[1]).containsExactly("fileC")
  }

  @Test
  fun batchLogic_fileExceedingLimit() {
    val files = listOf("fileA", "fileB", "fileC")
    val fileSizes = mapOf("fileA" to 5L, "fileB" to 50L, "fileC" to 5L)
    val batchLimit = 20L

    val result = BatchingLogic(fakeLogger).splitIntoBatches(files, fileSizes, batchLimit, 0)

    // fileA gets grouped (5), then fileB is 50 (>20 limit) so fileB gets its own batch,
    // then fileC (5)
    assertThat(result).hasSize(3)
    assertThat(result[0]).containsExactly("fileA")
    assertThat(result[1]).containsExactly("fileB")
    assertThat(result[2]).containsExactly("fileC")
  }

  @Test
  fun batchLogic_withMaxBatches() {
    val files = listOf("fileA", "fileB", "fileC")
    val fileSizes = mapOf("fileA" to 10L, "fileB" to 20L, "fileC" to 15L)
    val batchLimit = 20L // Each file will be in its own batch

    val result = BatchingLogic(fakeLogger).splitIntoBatches(files, fileSizes, batchLimit, 1)

    assertThat(result).hasSize(1)
    assertThat(result[0]).containsExactly("fileA")
  }

  @Test
  fun batchLogic_missingFileSizes() {
    val files = listOf("fileA", "fileB")
    val fileSizes = emptyMap<String, Long>()
    val batchLimit = 10L

    val result = BatchingLogic(fakeLogger).splitIntoBatches(files, fileSizes, batchLimit, 0)

    // fileA (0) + fileB (0) = 0
    assertThat(result).hasSize(1)
    assertThat(result[0]).containsExactly("fileA", "fileB")
  }

  @Test
  fun batchLogic_exactBatchLimit() {
    val files = listOf("fileA", "fileB")
    val fileSizes = mapOf("fileA" to 10L, "fileB" to 10L)
    val batchLimit = 10L

    val result = BatchingLogic(fakeLogger).splitIntoBatches(files, fileSizes, batchLimit, 0)

    assertThat(result).hasSize(2)
    assertThat(result[0]).containsExactly("fileA")
    assertThat(result[1]).containsExactly("fileB")
  }

  @Test
  fun batchLogic_fileLargerThanLimitAfterExistingBatch() {
    val files = listOf("fileA", "fileB")
    val fileSizes = mapOf("fileA" to 5L, "fileB" to 20L)
    val batchLimit = 10L

    val result = BatchingLogic(fakeLogger).splitIntoBatches(files, fileSizes, batchLimit, 0)

    assertThat(result).hasSize(2)
    assertThat(result[0]).containsExactly("fileA")
    assertThat(result[1]).containsExactly("fileB")
  }

  @Test
  fun batchLogic_fileLargerThanLimitAtStart() {
    val files = listOf("large")
    val fileSizes = mapOf("large" to 100L)
    val batchLimit = 50L

    val result = BatchingLogic(fakeLogger).splitIntoBatches(files, fileSizes, batchLimit, 0)

    assertThat(result).hasSize(1)
    assertThat(result[0]).containsExactly("large")
  }

  @Test
  fun batchLogic_maxBatchesReachedExactlyAfterAdding() {
    val files = listOf("f1", "f2")
    val fileSizes = mapOf("f1" to 100L, "f2" to 100L)
    val batchLimit = 50L

    val result = BatchingLogic(fakeLogger).splitIntoBatches(files, fileSizes, batchLimit, 1)

    assertThat(result).hasSize(1)
    assertThat(result[0]).containsExactly("f1")
  }

  @Test
  fun batchLogic_maxBatchesReachedInsideSmallFileLoop() {
    val files = listOf("f1", "f2", "f3")
    val fileSizes = mapOf("f1" to 10L, "f2" to 10L, "f3" to 10L)
    val batchLimit = 15L // f1+f2 = 20 > 15

    val result = BatchingLogic(fakeLogger).splitIntoBatches(files, fileSizes, batchLimit, 1)

    // f1 added to currentBatch.
    // f2 + currentBatchSize(10) = 20 > 15.
    // batches.add(currentBatch) -> batches.size = 1.
    // currentBatch has f2.
    // maxBatches(1) reached. break.
    assertThat(result).hasSize(1)
    assertThat(result[0]).containsExactly("f1")
  }

  @Test
  fun batchLogic_fileSizeZero() {
    val files = listOf("f1")
    val fileSizes = mapOf("f1" to 0L)
    val batchLimit = 10L

    val result = BatchingLogic(fakeLogger).splitIntoBatches(files, fileSizes, batchLimit, 0)

    assertThat(result).hasSize(1)
    assertThat(result[0]).containsExactly("f1")
  }
}