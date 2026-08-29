package hu.muzso.android_system_dumper.common

import java.io.FilterOutputStream
import java.io.OutputStream

/**
 * An [OutputStream] that wraps another [OutputStream] and prevents it from being closed.
 *
 * This is useful when using libraries that automatically close the underlying stream 
 * (like [net.lingala.zip4j.io.outputstream.ZipOutputStream]), but you want to keep the 
 * underlying stream open for further writes.
 */
class NonClosingOutputStream(out: OutputStream) : FilterOutputStream(out) {
    override fun close() {
        // Do nothing, we don't want to close the underlying stream
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        out.write(b, off, len)
    }
}
