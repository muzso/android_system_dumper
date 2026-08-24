package hu.muzso.android_system_dumper.fixtures

import hu.muzso.android_system_dumper.domain.fixtures.FakeScanRepository
import hu.muzso.android_system_dumper.domain.repository.scan.ScanRepositoryContract
import hu.muzso.android_system_dumper.scan.ScanRepository

class FakeScanRepositoryContractTest : ScanRepositoryContract() {
    override fun createScanRepository(seedPaths: List<String>, excludeList: List<String>): ScanRepository {
        return FakeScanRepository()
    }
}
