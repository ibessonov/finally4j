package com.github.ibessonov.finally4j.agent.transformer;

import org.objectweb.asm.ClassReader;

/**
 * Class reader implementation that scans the symbol table for the presence of "Finally" class. Classes that don't
 * have it present should not be transformed.
 */
class FinallyClassReader extends ClassReader {
    boolean hasFinallyReferenced;

    FinallyClassReader(byte[] classFile) {
        super(classFile);
    }

    @Override
    public String readUTF8(int index, char[] buf) {
        String utf8str = super.readUTF8(index, buf);

        if (!hasFinallyReferenced && utf8str != null) {
            hasFinallyReferenced = utf8str.equals(Constants.FINALLY_CLASS_INTERNAL_NAME);
        }

        return utf8str;
    }
}
